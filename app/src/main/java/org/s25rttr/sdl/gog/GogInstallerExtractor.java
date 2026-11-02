package org.s25rttr.sdl.gog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.s25rttr.sdl.gog.InnoExtractor.FileEntryInfo;

/**
 * Extrahiert die Spieldaten (DATA/ und GFX/) aus dem GOG-Installer von "The Settlers 2 Gold
 * Edition".
 * <p>
 * Der GOG-Installer legt die eigentlichen Spieldaten als zlib-komprimierte Dateien unter
 * {@code {tmp}} ab, benannt nach dem MD5-Hash des komprimierten Inhalts. Die Zuordnung
 * Hash -&gt; Zielpfad steckt in den {@code before_install}-Feldern der Datei-Einträge:
 * {@code before_install('&lt;md5-des-dekomprimierten-inhalts&gt;', '&lt;pfad&gt;', 1)}.
 * <p>
 * Diese Klasse entpackt den Installer, dekomprimiert die tmp-Dateien und schreibt nur die
 * für RTTR benötigten Verzeichnisse (DATA/ und GFX/) in das Zielverzeichnis.
 */
public final class GogInstallerExtractor {

    /** Fortschrittscallback; Methoden werden im aufrufenden Thread aufgerufen. */
    public interface Progress {
        void onProgress(String status, int percent);
    }

    private static final Pattern BEFORE_INSTALL_RE = Pattern.compile(
            "before_install(?:_dependency)?\\('([0-9a-f]{32})',\\s*'([^']+)'");

    private static final String[] KEEP_PREFIXES = { "DATA/", "GFX/" };

    private GogInstallerExtractor() {
    }

    /**
     * Entpackt den GOG-Installer und schreibt die Spieldaten (DATA/ und GFX/) nach outDir.
     *
     * @return die Anzahl der geschriebenen Dateien
     */
    public static int extract(File installer, File outDir, Progress progress) throws IOException {
        if (progress == null) {
            progress = new Progress() {
                @Override
                public void onProgress(String status, int percent) {
                }
            };
        }

        progress.onProgress("Reading installer...", 5);
        Map<String, List<String>> mapping = buildMapping(installer);

        File tempDir = createTempDir();
        try {
            progress.onProgress("Extracting installer...", 15);
            InnoExtractor.Options options = new InnoExtractor.Options();
            options.includeTemp = true;
            InnoExtractor.extract(installer, tempDir, options, null);

            progress.onProgress("Extracting game data...", 30);
            int written = processTmpFiles(tempDir, outDir, mapping, progress);
            return written;
        } finally {
            deleteRecursively(tempDir);
        }
    }

    /**
     * Baut die Zuordnung MD5(dekomprimierter Inhalt) -&gt; Zielpfade aus den
     * {@code before_install}-Feldern der Datei-Einträge. Ein Hash kann auf mehrere
     * Zielpfade zeigen (identische Dateien, z. B. PAL5.BBM und PALETTI0.BBM).
     */
    static Map<String, List<String>> buildMapping(File installer) throws IOException {
        Map<String, List<String>> mapping = new HashMap<String, List<String>>();
        for (FileEntryInfo entry : InnoExtractor.fileEntries(installer)) {
            addMapping(mapping, entry.beforeInstall);
            addMapping(mapping, entry.afterInstall);
        }
        return mapping;
    }

    private static void addMapping(Map<String, List<String>> mapping, String expression) {
        if (expression == null || expression.isEmpty()) {
            return;
        }
        Matcher m = BEFORE_INSTALL_RE.matcher(expression);
        while (m.find()) {
            List<String> paths = mapping.get(m.group(1));
            if (paths == null) {
                paths = new ArrayList<String>(2);
                mapping.put(m.group(1), paths);
            }
            if (!paths.contains(m.group(2))) {
                paths.add(m.group(2));
            }
        }
    }

    private static int processTmpFiles(File tempDir, File outDir, Map<String, List<String>> mapping,
                                       Progress progress) throws IOException {
        File tmpDir = new File(tempDir, "tmp");
        if (!tmpDir.isDirectory()) {
            throw new GogException("Installer enthält keine {tmp}-Dateien (kein GOG-Installer?)");
        }
        int total = countFiles(tmpDir);
        int[] done = { 0 };
        int written = processTmpFile(tmpDir, outDir, mapping, progress, total, done);
        return written;
    }

    private static int countFiles(File file) {
        if (file.isDirectory()) {
            int count = 0;
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    count += countFiles(child);
                }
            }
            return count;
        }
        return 1;
    }

    private static int processTmpFile(File file, File outDir, Map<String, List<String>> mapping,
                                      Progress progress, int total, int[] done) throws IOException {
        if (file.isDirectory()) {
            int written = 0;
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    written += processTmpFile(child, outDir, mapping, progress, total, done);
                }
            }
            return written;
        }

        byte[] compressed = readAllBytes(file);
        byte[] decompressed = maybeDecompress(compressed);
        String md5 = md5Hex(decompressed);
        List<String> paths = mapping.get(md5);
        done[0]++;
        if (paths == null) {
            return 0;
        }

        int written = 0;
        for (String path : paths) {
            if (!isNeeded(path)) {
                continue;
            }
            String normalized = path.replace('\\', '/');
            File target = new File(outDir, normalized);
            File parent = target.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                throw new GogException("Kann Ordner nicht anlegen: " + parent);
            }
            OutputStream out = new FileOutputStream(target);
            try {
                out.write(decompressed);
            } finally {
                out.close();
            }
            written++;
            int percent = 30 + (int) (65.0 * done[0] / total);
            progress.onProgress("Extracting " + normalized + "...", percent);
        }
        return written;
    }

    /** Nur die Verzeichnisse, die RTTR zum Spielen braucht. */
    static boolean isNeeded(String path) {
        String normalized = path.replace('\\', '/');
        for (String prefix : KEEP_PREFIXES) {
            if (normalized.regionMatches(true, 0, prefix, 0, prefix.length())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Dekomprimiert zlib-Daten. Dateien, die nicht mit einem gültigen zlib-Header beginnen
     * (z. B. GOG-Bilder, die unkomprimiert abgelegt sind), werden unverändert zurückgegeben.
     */
    private static byte[] maybeDecompress(byte[] data) throws IOException {
        if (data.length < 2 || (data[0] & 0xff) != 0x78) {
            return data;
        }
        try {
            return zlibDecompress(data);
        } catch (IOException ex) {
            return data;
        }
    }

    private static byte[] zlibDecompress(byte[] data) throws IOException {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(data);
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, data.length * 2));
            byte[] buffer = new byte[8192];
            while (!inflater.finished()) {
                int n = inflater.inflate(buffer);
                if (n == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) {
                        throw new GogException("Ungültige zlib-Daten im Installer");
                    }
                    if (inflater.getRemaining() == 0) {
                        break;
                    }
                }
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } catch (java.util.zip.DataFormatException ex) {
            throw new GogException("Ungültige zlib-Daten im Installer", ex);
        } finally {
            inflater.end();
        }
    }

    private static String md5Hex(byte[] data) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xf, 16));
                sb.append(Character.forDigit(b & 0xf, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new GogException("MD5 nicht verfügbar", ex);
        }
    }

    private static byte[] readAllBytes(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream((int) Math.min(file.length(), Integer.MAX_VALUE));
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    private static File createTempDir() throws IOException {
        File dir = File.createTempFile("gog-extract", "");
        if (!dir.delete() || !dir.mkdirs()) {
            throw new GogException("Kann temporären Ordner nicht anlegen");
        }
        return dir;
    }

    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
