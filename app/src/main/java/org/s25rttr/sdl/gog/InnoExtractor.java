package org.s25rttr.sdl.gog;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.InflaterInputStream;

import org.tukaani.xz.LZMA2InputStream;
import org.tukaani.xz.LZMAInputStream;

/**
 * Minimaler Inno-Setup-Extractor in reinem Java (kein NDK, kein Boost).
 *
 * Unterstützt: Inno Setup Data-Version 5.3.9 bis 5.6.x, Unicode, Daten im Installer
 * eingebettet (Slice 0), Kompression stored / zlib / LZMA1 / LZMA2, x86-CALL-Filter,
 * Prüfsummen (SHA-1 / MD5). Nicht unterstützt: Passwort-verschlüsselte Installer,
 * bzip2, externe setup-N.bin-Dateien, ANSI-(nicht-Unicode)-Installer, Inno 6.x.
 *
 * Das Format wurde anhand der Quellen von innoextract 1.9 (Zlib-Lizenz,
 * Daniel Scharrer) nachvollzogen.
 *
 * Abhängigkeit: org.tukaani:xz (für LZMA).
 */
public final class InnoExtractor {

    // ------------------------------------------------------------------ API

    public static final class Options {
        /** Sprachname aus dem Installer (z. B. "de-DE"); null = alle Sprachen. */
        public String language;
        /** Auch temporäre Dateien (DeleteAfterInstall, z. B. {tmp}) entpacken. */
        public boolean includeTemp = false;
        /** Alle Pfade in Kleinbuchstaben (praktisch für Android/Linux). */
        public boolean lowercase = false;
        /**
         * Ersetzung für Inno-Konstanten, z. B. variables.put("app", "") entpackt {app}
         * direkt ins Zielverzeichnis. Unbekannte Konstanten werden zum Ordnernamen
         * (wie bei innoextract: {app} -> "app", {tmp} -> "tmp").
         */
        public final Map<String, String> variables = new HashMap<String, String>();
        /** Optionaler Pfadfilter (Pfad wie in der Ausgabe, mit '/'); null = alles. */
        public PathFilter filter;
        /** Prüfsummen der entpackten Dateien verifizieren (empfohlen). */
        public boolean verifyChecksums = true;
    }

    public interface PathFilter {
        boolean accept(String path);
    }

    public static final class Entry {
        public final String path;
        public final long size;
        public final String languages;
        public final boolean temp;

        Entry(String path, long size, String languages, boolean temp) {
            this.path = path;
            this.size = size;
            this.languages = languages;
            this.temp = temp;
        }

        @Override
        public String toString() {
            return path + " (" + size + " B)" + (languages.isEmpty() ? "" : " [" + languages + "]")
                    + (temp ? " [temp]" : "");
        }
    }

    /** Fortschrittscallback; Methoden werden im aufrufenden Thread aufgerufen. */
    public static abstract class Progress {
        public void onStart(int fileCount, long totalBytes) { }
        public void onFile(String path) { }
        public void onBytes(long done, long total) { }
        /** true zurückgeben, um abzubrechen (wirft CancellationException). */
        public boolean isCancelled() { return false; }
    }

    public static final class InnoException extends IOException {
        private static final long serialVersionUID = 1L;
        public InnoException(String msg) { super(msg); }
        public InnoException(String msg, Throwable cause) { super(msg, cause); }
    }

    /** Liefert die Daten-Version des Installers, z. B. "5.6.2 (unicode)". */
    public static String dataVersion(File installer) throws IOException {
        RandomAccessFile f = new RandomAccessFile(installer, "r");
        try {
            Loader l = readLoader(f);
            String vs = readVersionString(f, l.headerOffset);
            return vs;
        } finally {
            f.close();
        }
    }

    /** Listet die Dateien, die extract() mit denselben Options erzeugen würde. */
    public static List<Entry> list(File installer, Options o) throws IOException {
        if (o == null) o = new Options();
        RandomAccessFile f = new RandomAccessFile(installer, "r");
        try {
            Setup s = load(f);
            List<Target> targets = selectTargets(s, o);
            List<Entry> result = new ArrayList<Entry>();
            for (Target t : targets) {
                result.add(new Entry(t.path, t.data.fileSize, t.file.languages, t.file.deleteAfterInstall));
            }
            return result;
        } finally {
            f.close();
        }
    }

    /** Namen der im Installer enthaltenen Sprachen. */
    public static List<String> languages(File installer) throws IOException {
        RandomAccessFile f = new RandomAccessFile(installer, "r");
        try {
            return load(f).languageNames;
        } finally {
            f.close();
        }
    }

    /**
     * Ein roher Datei-Eintrag aus dem Installer-Header. Wird von GOG-Installern genutzt, um
     * die eigentlichen Spieldaten (in {tmp} mit Hash-Namen abgelegt) auf ihre Zielpfade
     * abzubilden: Die Felder {@code beforeInstall}/{@code afterInstall} enthalten dann
     * Ausdrücke wie {@code before_install('&lt;md5&gt;', '&lt;pfad&gt;', 1)}.
     */
    public static final class FileEntryInfo {
        public final String source;
        public final String destination;
        public final String languages;
        public final String check;
        public final String afterInstall;
        public final String beforeInstall;
        public final long location;
        public final boolean deleteAfterInstall;

        FileEntryInfo(FileEntry fe) {
            this.source = fe.source;
            this.destination = fe.destination;
            this.languages = fe.languages;
            this.check = fe.check;
            this.afterInstall = fe.afterInstall;
            this.beforeInstall = fe.beforeInstall;
            this.location = fe.location;
            this.deleteAfterInstall = fe.deleteAfterInstall;
        }
    }

    /** Liefert die rohen Datei-Einträge des Installers (ohne Filterung). */
    public static List<FileEntryInfo> fileEntries(File installer) throws IOException {
        RandomAccessFile f = new RandomAccessFile(installer, "r");
        try {
            Setup s = load(f);
            List<FileEntryInfo> result = new ArrayList<FileEntryInfo>(s.files.size());
            for (FileEntry fe : s.files) {
                result.add(new FileEntryInfo(fe));
            }
            return result;
        } finally {
            f.close();
        }
    }

    /** Entpackt den Installer nach outDir. */
    public static void extract(File installer, File outDir, Options o, Progress progress) throws IOException {
        if (o == null) o = new Options();
        if (progress == null) progress = new Progress() { };
        RandomAccessFile f = new RandomAccessFile(installer, "r");
        try {
            Setup s = load(f);
            List<Target> targets = selectTargets(s, o);
            extractTargets(f, s, targets, outDir, o, progress);
        } finally {
            f.close();
        }
    }

    // ------------------------------------------------------------ Datenmodell

    private static int V(int a, int b, int c) {
        return a * 10000 + b * 100 + c;
    }

    private static final int C_STORED = 0, C_ZLIB = 1, C_BZIP2 = 2, C_LZMA1 = 3, C_LZMA2 = 4;

    private static final class Loader {
        long headerOffset;
        long dataOffset;
    }

    private static final class FileEntry {
        String source = "";
        String destination = "";
        String languages = "";
        String check = "";
        String afterInstall = "";
        String beforeInstall = "";
        long location;
        boolean deleteAfterInstall;
    }

    private static final class DataEntry {
        long firstSlice, lastSlice;
        long chunkOffset;
        long fileOffset, fileSize, chunkSize;
        byte[] checksum;          // SHA-1 oder MD5 (siehe checksumAlgo)
        String checksumAlgo;      // "SHA-1", "MD5" oder null
        long fileTime;            // Windows FILETIME
        boolean callInstructionOptimized;
        boolean chunkEncrypted;
        boolean chunkCompressed;
    }

    private static final class Setup {
        int version;
        boolean unicode;
        int compression;
        boolean encryptionUsed;
        int dataCount;
        long dataOffset;
        List<String> languageNames = new ArrayList<String>();
        List<FileEntry> files = new ArrayList<FileEntry>();
        List<DataEntry> data = new ArrayList<DataEntry>();
    }

    private static final class Target {
        final String path;
        final FileEntry file;
        final DataEntry data;

        Target(String path, FileEntry file, DataEntry data) {
            this.path = path;
            this.file = file;
            this.data = data;
        }
    }

    // ------------------------------------------------- Low-Level-Lesehilfen

    /** Little-Endian-Reader über einen InputStream. */
    private static final class Le {
        private final InputStream in;
        private final byte[] scratch = new byte[8192];

        Le(InputStream in) {
            this.in = in;
        }

        int u8() throws IOException {
            int b = in.read();
            if (b < 0) throw new EOFException("Header unerwartet zu Ende");
            return b;
        }

        int u16() throws IOException {
            int a = u8();
            int b = u8();
            return a | (b << 8);
        }

        long u32() throws IOException {
            long a = u8(), b = u8(), c = u8(), d = u8();
            return a | (b << 8) | (c << 16) | (d << 24);
        }

        int i32() throws IOException {
            return (int) u32();
        }

        long i64() throws IOException {
            long lo = u32();
            long hi = u32();
            return lo | (hi << 32);
        }

        byte[] bytes(int n) throws IOException {
            byte[] b = new byte[n];
            readFully(in, b, 0, n);
            return b;
        }

        void skip(long n) throws IOException {
            while (n > 0) {
                int chunk = (int) Math.min(n, scratch.length);
                readFully(in, scratch, 0, chunk);
                n -= chunk;
            }
        }

        /** Länge-präfixierter String; bei Unicode-Installern UTF-16LE. */
        String str() throws IOException {
            long len = u32();
            if (len > 256L * 1024 * 1024) throw new InnoException("Unplausible String-Länge: " + len);
            return new String(bytes((int) len), UTF16LE);
        }

        void skipStr() throws IOException {
            skip(u32());
        }
    }

    private static final Charset UTF16LE = Charset.forName("UTF-16LE");

    /** Bitfeld-Leser wie stored_flag_reader in innoextract. */
    private static final class Flags {
        private final Le r;
        private int pos = 0, buf = 0, bytes = 0;

        Flags(Le r) {
            this.r = r;
        }

        boolean add() throws IOException {
            if (pos == 0) {
                bytes++;
                buf = r.u8();
            }
            boolean v = (buf & (1 << pos)) != 0;
            pos = (pos + 1) % 8;
            return v;
        }

        void finish() throws IOException {
            if (bytes == 3) r.u8(); // 3-Byte-Mengen sind auf 4 Byte aufgefüllt
        }
    }

    private static void readFully(InputStream in, byte[] b, int off, int len) throws IOException {
        while (len > 0) {
            int n = in.read(b, off, len);
            if (n < 0) throw new EOFException("Unerwartetes Dateiende");
            off += n;
            len -= n;
        }
    }

    private static long leU32(byte[] b, int o) {
        return (b[o] & 0xffL) | ((b[o + 1] & 0xffL) << 8) | ((b[o + 2] & 0xffL) << 16) | ((b[o + 3] & 0xffL) << 24);
    }

    private static long readU32(RandomAccessFile f, long pos) throws IOException {
        byte[] b = new byte[4];
        f.seek(pos);
        f.readFully(b);
        return leU32(b, 0);
    }

    private static int readU16(RandomAccessFile f, long pos) throws IOException {
        byte[] b = new byte[2];
        f.seek(pos);
        f.readFully(b);
        return (b[0] & 0xff) | ((b[1] & 0xff) << 8);
    }

    /** Ein Bereich einer RandomAccessFile als InputStream (eigene Position, daher mehrfach nutzbar). */
    private static final class FileRange extends InputStream {
        private final RandomAccessFile f;
        private long pos;
        private final long end;

        FileRange(RandomAccessFile f, long start, long length) {
            this.f = f;
            this.pos = start;
            this.end = start + length;
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            int n = read(one, 0, 1);
            return n < 0 ? -1 : (one[0] & 0xff);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (len == 0) return 0;
            long left = end - pos;
            if (left <= 0) return -1;
            int n = (int) Math.min(len, left);
            f.seek(pos);
            int r = f.read(b, off, n);
            if (r > 0) pos += r;
            return r;
        }
    }

    /** Begrenzt einen Stream auf maximal n Bytes. */
    private static final class LimitStream extends InputStream {
        private final InputStream in;
        private long left;

        LimitStream(InputStream in, long n) {
            this.in = in;
            this.left = n;
        }

        @Override
        public int read() throws IOException {
            if (left <= 0) return -1;
            int b = in.read();
            if (b >= 0) left--;
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (left <= 0) return -1;
            int n = in.read(b, off, (int) Math.min(len, left));
            if (n > 0) left -= n;
            return n;
        }
    }

    /** Inno-Block: 4096-Byte-Stücke, jeweils mit vorangestelltem CRC32. */
    private static final class CrcBlockStream extends InputStream {
        private final InputStream in;
        private long remaining;
        private final byte[] buf = new byte[4096];
        private int pos = 0, len = 0;

        CrcBlockStream(InputStream in, long storedSize) {
            this.in = in;
            this.remaining = storedSize;
        }

        private boolean fill() throws IOException {
            while (true) {
                if (remaining == 0) return false;
                if (remaining < 4) throw new InnoException("Block: unerwartetes Ende");
                byte[] c = new byte[4];
                readFully(in, c, 0, 4);
                remaining -= 4;
                int n = (int) Math.min(buf.length, remaining);
                readFully(in, buf, 0, n);
                remaining -= n;
                CRC32 crc = new CRC32();
                crc.update(buf, 0, n);
                if (crc.getValue() != leU32(c, 0)) throw new InnoException("Block: CRC32-Fehler");
                pos = 0;
                len = n;
                if (n > 0) return true;
            }
        }

        @Override
        public int read() throws IOException {
            if (pos == len && !fill()) return -1;
            return buf[pos++] & 0xff;
        }

        @Override
        public int read(byte[] b, int off, int l) throws IOException {
            if (l == 0) return 0;
            if (pos == len && !fill()) return -1;
            int n = Math.min(l, len - pos);
            System.arraycopy(buf, pos, b, off, n);
            pos += n;
            return n;
        }
    }

    // ------------------------------------------------------ Loader (PE-Teil)

    private static final int RT_RCDATA = 10;
    private static final int RES_SETUP_LOADER = 11111;

    private static Loader readLoader(RandomAccessFile f) throws IOException {
        long resOffset = findLoaderResource(f);
        if (resOffset <= 0) {
            throw new InnoException("Kein Inno-Setup-Installer (Loader-Tabelle nicht gefunden)");
        }
        byte[] t = new byte[44];
        f.seek(resOffset);
        f.readFully(t);
        // 12 Byte Magic, dann revision, total, exeOffset, exeUncompressed, exeCrc, header, data, crc
        long revision = leU32(t, 12);
        if (revision != 1) {
            throw new InnoException("Unbekannte Loader-Revision: " + revision);
        }
        Loader l = new Loader();
        l.headerOffset = leU32(t, 32);
        l.dataOffset = leU32(t, 36);
        return l;
    }

    private static long findLoaderResource(RandomAccessFile f) throws IOException {
        if (f.length() < 0x40 || readU16(f, 0) != 0x5a4d) return 0; // "MZ"
        long pe = readU32(f, 0x3c);
        if (readU32(f, pe) != 0x00004550L) return 0; // "PE\0\0"
        long coff = pe + 4;
        int nsections = readU16(f, coff + 2);
        int optSize = readU16(f, coff + 16);
        long opt = coff + 20;
        long sectionTable = opt + optSize;
        int magic = readU16(f, opt);
        long dirBase = (magic == 0x20b) ? opt + 112 : opt + 96;
        long ndirs = readU32(f, dirBase - 4);
        if (ndirs < 3) return 0;
        long resRva = readU32(f, dirBase + 16);
        long resSize = readU32(f, dirBase + 20);
        if (resRva == 0 || resSize == 0) return 0;

        long[] va = new long[nsections], vs = new long[nsections], raw = new long[nsections];
        for (int i = 0; i < nsections; i++) {
            long s = sectionTable + 40L * i;
            long virtualSize = readU32(f, s + 8);
            long rawSize = readU32(f, s + 16);
            va[i] = readU32(f, s + 12);
            vs[i] = Math.max(virtualSize, rawSize);
            raw[i] = readU32(f, s + 20);
        }
        long resBase = rvaToOffset(resRva, va, vs, raw);
        if (resBase == 0) return 0;

        long type = findResourceEntry(f, resBase, RT_RCDATA, false);
        if (type == 0 || (type & 0x80000000L) == 0) return 0;
        long name = findResourceEntry(f, resBase + (type & 0x7fffffffL), RES_SETUP_LOADER, false);
        if (name == 0 || (name & 0x80000000L) == 0) return 0;
        long leaf = findResourceEntry(f, resBase + (name & 0x7fffffffL), 0, true); // erste Sprache
        if (leaf == 0 || (leaf & 0x80000000L) != 0) return 0;
        long dataEntry = resBase + leaf;
        long dataRva = readU32(f, dataEntry);
        return rvaToOffset(dataRva, va, vs, raw);
    }

    private static long rvaToOffset(long rva, long[] va, long[] vs, long[] raw) {
        for (int i = 0; i < va.length; i++) {
            if (rva >= va[i] && rva < va[i] + vs[i]) return rva - va[i] + raw[i];
        }
        return 0;
    }

    /** Liefert den Offset-Wert (Bit 31 = Unterverzeichnis) des Eintrags mit der gegebenen ID. */
    private static long findResourceEntry(RandomAccessFile f, long dir, long id, boolean first) throws IOException {
        int named = readU16(f, dir + 12);
        int ids = readU16(f, dir + 14);
        if (first) {
            if (named + ids == 0) return 0;
            return readU32(f, dir + 16 + 4);
        }
        long p = dir + 16 + 8L * named;
        for (int i = 0; i < ids; i++, p += 8) {
            if (readU32(f, p) == id) return readU32(f, p + 4);
        }
        return 0;
    }

    // ------------------------------------------------------ Header lesen

    private static final Pattern VERSION_RE =
            Pattern.compile("Inno Setup Setup Data \\((\\d+)\\.(\\d+)\\.(\\d+)\\)\\s*(\\((?:u|U)\\))?");

    private static String readVersionString(RandomAccessFile f, long headerOffset) throws IOException {
        byte[] raw = new byte[64];
        f.seek(headerOffset);
        f.readFully(raw);
        int end = 0;
        while (end < raw.length && raw[end] != 0) end++;
        String s = new String(raw, 0, end, Charset.forName("ISO-8859-1"));
        Matcher m = VERSION_RE.matcher(s);
        if (!m.find()) throw new InnoException("Unbekannte Setup-Daten-Version: \"" + s + "\"");
        return m.group(1) + "." + m.group(2) + "." + m.group(3) + (m.group(4) != null ? " (unicode)" : "");
    }

    private static Setup load(RandomAccessFile f) throws IOException {
        Loader l = readLoader(f);

        byte[] raw = new byte[64];
        f.seek(l.headerOffset);
        f.readFully(raw);
        int end = 0;
        while (end < raw.length && raw[end] != 0) end++;
        String vstr = new String(raw, 0, end, Charset.forName("ISO-8859-1"));
        Matcher m = VERSION_RE.matcher(vstr);
        if (!m.find()) throw new InnoException("Unbekannte Setup-Daten-Version: \"" + vstr + "\"");

        Setup s = new Setup();
        s.version = V(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
        s.unicode = m.group(4) != null;
        s.dataOffset = l.dataOffset;
        if (!s.unicode) throw new InnoException("Nur Unicode-Installer werden unterstützt: " + vstr);
        if (s.version < V(5, 3, 9) || s.version >= V(6, 0, 0)) {
            throw new InnoException("Nicht unterstützte Daten-Version: " + vstr);
        }

        long blockPos = l.headerOffset + 64;

        // ---- Block 1: Header + Einträge
        long[] next = new long[1];
        InputStream b1 = openBlock(f, blockPos, next);
        Le r = new Le(b1);
        readPrimary(r, s);
        blockPos = next[0];

        // ---- Block 2: Daten-Einträge
        InputStream b2 = openBlock(f, blockPos, next);
        Le r2 = new Le(b2);
        for (int i = 0; i < s.dataCount; i++) s.data.add(readDataEntry(r2, s.version));
        return s;
    }

    /** Öffnet einen Header-Block (CRC-Kopf, Stücke à 4096 Byte, LZMA1). */
    private static InputStream openBlock(RandomAccessFile f, long pos, long[] next) throws IOException {
        byte[] h = new byte[9];
        f.seek(pos);
        f.readFully(h);
        long expected = leU32(h, 0);
        CRC32 crc = new CRC32();
        crc.update(h, 4, 5);
        if (crc.getValue() != expected) throw new InnoException("Block-Header: CRC32-Fehler");
        long storedSize = leU32(h, 4);
        boolean compressed = h[8] != 0;
        long dataStart = pos + 9;
        next[0] = dataStart + storedSize;

        InputStream base = new BufferedInputStream(new FileRange(f, dataStart, storedSize), 1 << 16);
        InputStream blocks = new CrcBlockStream(base, storedSize);
        if (!compressed) return blocks;
        return newLzma1(blocks);
    }

    private static InputStream newLzma1(InputStream in) throws IOException {
        byte[] h = new byte[5];
        readFully(in, h, 0, 5);
        long dict = leU32(h, 1);
        if (dict > Integer.MAX_VALUE) dict = Integer.MAX_VALUE;
        return new LZMAInputStream(in, -1, h[0], (int) dict);
    }

    private static InputStream newLzma2(InputStream in) throws IOException {
        int prop = in.read();
        if (prop < 0 || prop > 40) throw new InnoException("Ungültige LZMA2-Eigenschaft: " + prop);
        long dict = (prop == 40) ? 0xffffffffL : ((long) (2 | (prop & 1)) << (prop / 2 + 11));
        int max = Integer.MAX_VALUE & ~15;
        return new LZMA2InputStream(in, (int) Math.min(dict, max));
    }

    // Anzahlen aus dem Haupt-Header
    private static void skipCondition(Le r) throws IOException {
        // components, tasks, languages, check, after_install, before_install
        for (int i = 0; i < 6; i++) r.skipStr();
    }

    private static void readPrimary(Le r, Setup s) throws IOException {
        final int v = s.version;

        // ---- Haupt-Header (Strings)
        r.skipStr(); // app_name
        r.skipStr(); // app_versioned_name
        r.skipStr(); // app_id
        r.skipStr(); // app_copyright
        r.skipStr(); // app_publisher
        r.skipStr(); // app_publisher_url
        if (v >= V(5, 1, 13)) r.skipStr(); // app_support_phone
        r.skipStr(); // app_support_url
        r.skipStr(); // app_updates_url
        r.skipStr(); // app_version
        r.skipStr(); // default_dir_name
        r.skipStr(); // default_group_name
        r.skipStr(); // base_filename
        r.skipStr(); // uninstall_files_dir
        r.skipStr(); // uninstall_name
        r.skipStr(); // uninstall_icon
        r.skipStr(); // app_mutex
        r.skipStr(); // default_user_name
        r.skipStr(); // default_user_organisation
        r.skipStr(); // default_serial
        r.skipStr(); // app_readme_file
        r.skipStr(); // app_contact
        r.skipStr(); // app_comments
        r.skipStr(); // app_modify_path
        if (v >= V(5, 3, 8)) r.skipStr(); // create_uninstall_registry_key
        if (v >= V(5, 3, 10)) r.skipStr(); // uninstallable
        if (v >= V(5, 5, 0)) r.skipStr(); // close_applications_filter
        if (v >= V(5, 5, 6)) r.skipStr(); // setup_mutex
        if (v >= V(5, 6, 1)) {
            r.skipStr(); // changes_environment
            r.skipStr(); // changes_associations
        }
        r.skipStr(); // license_text
        r.skipStr(); // info_before
        r.skipStr(); // info_after
        if (v >= V(5, 2, 1) && v < V(5, 3, 10)) r.skipStr(); // uninstaller_signature
        r.skipStr(); // compiled_code

        // ---- Zähler
        long languageCount = r.u32();
        long messageCount = r.u32();
        long permissionCount = r.u32();
        long typeCount = r.u32();
        long componentCount = r.u32();
        long taskCount = r.u32();
        long directoryCount = r.u32();
        long fileCount = r.u32();
        s.dataCount = (int) r.u32();
        long iconCount = r.u32();
        long iniCount = r.u32();
        long registryCount = r.u32();
        long deleteCount = r.u32();
        long uninstallDeleteCount = r.u32();
        long runCount = r.u32();
        long uninstallRunCount = r.u32();

        r.skip(20); // winver (Min/OnlyBelow)
        r.skip(4);  // back_color
        r.skip(4);  // back_color2
        if (v < V(5, 5, 7)) r.skip(4); // image_back_color
        if (v >= V(5, 5, 7)) r.skip(1); // image_alpha_format

        // Passwort: SHA-1 (>= 5.3.9) + Salt
        r.skip(20);
        r.skip(8);

        r.skip(8);  // extra_disk_space_required
        r.skip(4);  // slices_per_disk
        r.skip(1);  // uninstall_log_mode
        r.skip(1);  // dir_exists_warning
        r.skip(1);  // privileges_required
        r.skip(1);  // show_language_dialog
        r.skip(1);  // language_detection
        s.compression = r.u8();
        r.skip(1);  // architectures_allowed
        r.skip(1);  // architectures_installed_in_64bit_mode
        if (v >= V(5, 2, 1) && v < V(5, 3, 10)) r.skip(8); // signed uninstaller
        r.skip(1);  // disable_dir_page
        r.skip(1);  // disable_program_group_page
        if (v >= V(5, 5, 0)) r.skip(8); else r.skip(4); // uninstall_display_size

        // ---- Header-Flags (Reihenfolge wie in innoextract)
        Flags fl = new Flags(r);
        fl.add(); // DisableStartupPrompt
        fl.add(); // CreateAppDir
        fl.add(); // AllowNoIcons
        fl.add(); // AlwaysRestart
        fl.add(); // AlwaysUsePersonalGroup
        fl.add(); // WindowVisible
        fl.add(); // WindowShowCaption
        fl.add(); // WindowResizable
        fl.add(); // WindowStartMaximized
        fl.add(); // EnableDirDoesntExistWarning
        fl.add(); // Password
        fl.add(); // AllowRootDirectory
        fl.add(); // DisableFinishedPage
        if (v < V(5, 6, 1)) fl.add(); // ChangesAssociations
        fl.add(); // UsePreviousAppDir
        fl.add(); // BackColorHorizontal
        fl.add(); // UsePreviousGroup
        fl.add(); // UpdateUninstallLogAppName
        fl.add(); // UsePreviousSetupType
        fl.add(); // DisableReadyMemo
        fl.add(); // AlwaysShowComponentsList
        fl.add(); // FlatComponentsList
        fl.add(); // ShowComponentSizes
        fl.add(); // UsePreviousTasks
        fl.add(); // DisableReadyPage
        fl.add(); // AlwaysShowDirOnReadyPage
        fl.add(); // AlwaysShowGroupOnReadyPage
        fl.add(); // AllowUNCPath
        fl.add(); // UserInfoPage
        fl.add(); // UsePreviousUserInfo
        fl.add(); // UninstallRestartComputer
        fl.add(); // RestartIfNeededByRun
        fl.add(); // ShowTasksTreeLines
        fl.add(); // AllowCancelDuringInstall
        fl.add(); // WizardImageStretch
        fl.add(); // AppendDefaultDirName
        fl.add(); // AppendDefaultGroupName
        s.encryptionUsed = fl.add(); // EncryptionUsed
        if (v < V(5, 6, 1)) fl.add(); // ChangesEnvironment
        fl.add(); // SetupLogging
        fl.add(); // SignedUninstaller
        fl.add(); // UsePreviousLanguage
        fl.add(); // DisableWelcomePage
        if (v >= V(5, 5, 0)) {
            fl.add(); // CloseApplications
            fl.add(); // RestartApplications
            fl.add(); // AllowNetworkDrive
        }
        if (v >= V(5, 5, 7)) fl.add(); // ForceCloseApplications
        fl.finish();

        // ---- Sprachen
        for (long i = 0; i < languageCount; i++) {
            s.languageNames.add(r.str()); // name
            for (int k = 0; k < 9; k++) r.skipStr(); // language_name, 4 fonts, data, license, info_before, info_after
            r.skip(4);       // language_id
            r.skip(4 * 4);   // font sizes
            r.skip(1);       // right_to_left
        }
        // ---- Messages
        for (long i = 0; i < messageCount; i++) {
            r.skipStr(); // name
            r.skipStr(); // value
            r.skip(4);   // language
        }
        // ---- Permissions
        for (long i = 0; i < permissionCount; i++) r.skipStr();
        // ---- Types
        for (long i = 0; i < typeCount; i++) {
            for (int k = 0; k < 4; k++) r.skipStr(); // name, description, languages, check
            r.skip(20);  // winver
            r.skip(1);   // flags
            r.skip(1);   // type
            r.skip(8);   // size
        }
        // ---- Components
        for (long i = 0; i < componentCount; i++) {
            for (int k = 0; k < 5; k++) r.skipStr(); // name, description, types, languages, check
            r.skip(8);   // extra_disk_space_required
            r.skip(4);   // level
            r.skip(1);   // used
            r.skip(20);  // winver
            r.skip(1);   // flags
            r.skip(8);   // size
        }
        // ---- Tasks
        for (long i = 0; i < taskCount; i++) {
            for (int k = 0; k < 6; k++) r.skipStr(); // name, description, group_description, components, languages, check
            r.skip(4);   // level
            r.skip(1);   // used
            r.skip(20);  // winver
            r.skip(1);   // flags
        }
        // ---- Directories
        for (long i = 0; i < directoryCount; i++) {
            r.skipStr();     // name
            skipCondition(r);
            r.skip(4);       // attributes
            r.skip(20);      // winver
            r.skip(2);       // permission
            r.skip(1);       // options
        }
        // ---- Files
        for (long i = 0; i < fileCount; i++) {
            FileEntry fe = new FileEntry();
            fe.source = r.str();                 // source
            fe.destination = r.str();            // destination
            r.skipStr();                         // install_font_name
            r.skipStr();                         // strong_assembly_name (>= 5.2.5)
            r.skipStr();                         // components
            r.skipStr();                         // tasks
            fe.languages = r.str();              // languages
            fe.check = r.str();                  // check
            fe.afterInstall = r.str();           // after_install
            fe.beforeInstall = r.str();          // before_install
            r.skip(20);                          // winver
            fe.location = r.u32();
            r.skip(4);                   // attributes
            r.skip(8);                   // external_size
            r.skip(2);                   // permission
            Flags ff = new Flags(r);
            ff.add();                    // ConfirmOverwrite
            ff.add();                    // NeverUninstall
            ff.add();                    // RestartReplace
            fe.deleteAfterInstall = ff.add(); // DeleteAfterInstall
            for (int k = 0; k < 28; k++) ff.add(); // restliche 28 Flags (insgesamt 32)
            ff.finish();
            r.skip(1);                   // type
            s.files.add(fe);
        }
        // ---- Icons
        for (long i = 0; i < iconCount; i++) {
            for (int k = 0; k < 6; k++) r.skipStr(); // name, filename, parameters, working_dir, icon_file, comment
            skipCondition(r);
            r.skipStr();     // app_user_model_id (>= 5.3.5)
            r.skip(20);      // winver
            r.skip(4);       // icon_index
            r.skip(4);       // show_command
            r.skip(1);       // close_on_exit
            r.skip(2);       // hotkey
            Flags fi = new Flags(r);
            fi.add(); fi.add(); fi.add();            // NeverUninstall, CreateOnlyIfFileExists, UseAppPaths
            fi.add();                                // FolderShortcut (>= 5.0.3)
            if (v >= V(5, 4, 2)) fi.add();           // ExcludeFromShowInNewInstall
            if (v >= V(5, 5, 0)) fi.add();           // PreventPinning
            fi.finish();
        }
        // ---- INI-Einträge
        for (long i = 0; i < iniCount; i++) {
            for (int k = 0; k < 4; k++) r.skipStr(); // inifile, section, key, value
            skipCondition(r);
            r.skip(20);      // winver
            r.skip(1);       // flags
        }
        // ---- Registry
        for (long i = 0; i < registryCount; i++) {
            for (int k = 0; k < 3; k++) r.skipStr(); // key, name, value
            skipCondition(r);
            r.skip(20);      // winver
            r.skip(4);       // hive
            r.skip(2);       // permission
            r.skip(1);       // type
            Flags fr = new Flags(r);
            for (int k = 0; k < 12; k++) fr.add();
            fr.finish();
        }
        // ---- Delete / UninstallDelete
        for (long i = 0; i < deleteCount + uninstallDeleteCount; i++) {
            r.skipStr();     // name
            skipCondition(r);
            r.skip(20);      // winver
            r.skip(1);       // type
        }
        // ---- Run / UninstallRun
        for (long i = 0; i < runCount + uninstallRunCount; i++) {
            for (int k = 0; k < 7; k++) r.skipStr(); // name, parameters, working_dir, run_once_id, status_message, verb, description
            skipCondition(r);
            r.skip(20);      // winver
            r.skip(4);       // show_command
            r.skip(1);       // wait
            Flags fu = new Flags(r);
            for (int k = 0; k < 10; k++) fu.add();
            fu.finish();
        }
        // ---- Wizard-Bilder und Decompressor/Decrypt-DLL
        long imageCount = (v >= V(5, 6, 0)) ? r.u32() : 1;
        for (long i = 0; i < imageCount; i++) r.skipStr();
        long smallCount = (v >= V(5, 6, 0)) ? r.u32() : 1;
        for (long i = 0; i < smallCount; i++) r.skipStr();
        if (s.compression == C_BZIP2 || s.compression == C_ZLIB) r.skipStr(); // decompressor_dll
        if (s.encryptionUsed) r.skipStr(); // decrypt_dll
    }

    private static DataEntry readDataEntry(Le r, int v) throws IOException {
        DataEntry d = new DataEntry();
        d.firstSlice = r.u32();
        d.lastSlice = r.u32();
        d.chunkOffset = r.u32();
        d.fileOffset = r.i64();
        d.fileSize = r.i64();
        d.chunkSize = r.i64();
        if (v >= V(5, 3, 9)) {
            d.checksum = r.bytes(20);
            d.checksumAlgo = "SHA-1";
        } else {
            d.checksum = r.bytes(16);
            d.checksumAlgo = "MD5";
        }
        d.fileTime = r.i64();
        r.skip(8); // file_version
        Flags f = new Flags(r);
        f.add(); // VersionInfoValid
        f.add(); // VersionInfoNotValid
        f.add(); // TimeStampInUTC
        f.add(); // IsUninstallerExe
        d.callInstructionOptimized = f.add();
        f.add(); // Touch
        d.chunkEncrypted = f.add();
        d.chunkCompressed = f.add();
        f.add(); // SolidBreak
        if (v >= V(5, 5, 7)) {
            f.add(); // Sign
            f.add(); // SignOnce
        }
        f.finish();
        return d;
    }

    // ------------------------------------------------- Pfade & Sprach-Ausdrücke

    private static boolean isUnsafe(char c) {
        if (c < 32) return true;
        switch (c) {
            case '<': case '>': case ':': case '"': case '|': case '?': case '*':
                return true;
            default:
                return false;
        }
    }

    private static String replaceUnsafe(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(isUnsafe(c) ? '$' : c);
        }
        return sb.toString();
    }

    private static String lookup(String key, Options o) {
        String v = o.variables.get(key);
        return v != null ? v : replaceUnsafe(key);
    }

    /** Wie filename_map::expand_variables in innoextract; pos[0] ist der Leseindex. */
    private static String expand(String s, int[] pos, boolean close, Options o) {
        StringBuilder result = new StringBuilder();
        while (pos[0] < s.length()) {
            int p = pos[0];
            while (p < s.length() && s.charAt(p) != '{' && s.charAt(p) != '}') p++;
            result.append(replaceUnsafe(s.substring(pos[0], p)));
            pos[0] = p;
            if (p == s.length()) break;
            char c = s.charAt(p);
            pos[0]++;
            if (close && c == '}') break;
            if (!close && c == '}') {
                result.append('}');
                continue;
            }
            if (pos[0] < s.length() && s.charAt(pos[0]) == '{') {
                result.append('{');
                pos[0]++;
                continue;
            }
            result.append(lookup(expand(s, pos, true, o), o));
        }
        return result.toString();
    }

    static String convertPath(String raw, Options o) {
        if (o.lowercase) raw = raw.toLowerCase(Locale.ROOT);
        String expanded = expand(raw, new int[] {0}, false, o);
        // shorten_path: leere und "." Segmente entfernen, ".." auflösen
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < expanded.length()) {
            int j = i;
            while (j < expanded.length() && expanded.charAt(j) != '\\' && expanded.charAt(j) != '/') j++;
            String seg = expanded.substring(i, j);
            i = j + 1;
            if (seg.isEmpty() || seg.equals(".")) continue;
            if (seg.equals("..")) {
                int last = result.lastIndexOf("/");
                result.setLength(last < 0 ? 0 : last);
                continue;
            }
            if (result.length() > 0) result.append('/');
            result.append(seg);
        }
        return result.toString();
    }

    /** Sprach-Ausdrücke der Form "de-DE or en-US", "not de-DE", Klammern, and/or/not. */
    private static boolean exprMatch(String test, String expr) {
        try {
            Expr e = new Expr(expr, test);
            boolean r = e.parseOr();
            if (e.pos != e.tokens.size()) return true;
            return r;
        } catch (RuntimeException ex) {
            return true; // wie innoextract: bei Fehlern einschließen
        }
    }

    private static final class Expr {
        final List<String> tokens = new ArrayList<String>();
        final String test;
        int pos = 0;

        Expr(String expr, String test) {
            this.test = test;
            StringBuilder cur = new StringBuilder();
            for (int i = 0; i < expr.length(); i++) {
                char c = expr.charAt(i);
                if (Character.isWhitespace(c) || c == '(' || c == ')') {
                    if (cur.length() > 0) { tokens.add(cur.toString()); cur.setLength(0); }
                    if (c == '(' || c == ')') tokens.add(String.valueOf(c));
                } else {
                    cur.append(c);
                }
            }
            if (cur.length() > 0) tokens.add(cur.toString());
        }

        boolean parseOr() {
            boolean v = parseAnd();
            while (pos < tokens.size() && tokens.get(pos).equalsIgnoreCase("or")) {
                pos++;
                boolean r = parseAnd();
                v = v || r;
            }
            return v;
        }

        boolean parseAnd() {
            boolean v = parseNot();
            while (pos < tokens.size() && tokens.get(pos).equalsIgnoreCase("and")) {
                pos++;
                boolean r = parseNot();
                v = v && r;
            }
            return v;
        }

        boolean parseNot() {
            if (pos < tokens.size() && tokens.get(pos).equalsIgnoreCase("not")) {
                pos++;
                return !parseNot();
            }
            return parsePrimary();
        }

        boolean parsePrimary() {
            if (pos >= tokens.size()) throw new IllegalStateException("Ausdruck unvollständig");
            String t = tokens.get(pos++);
            if (t.equals("(")) {
                boolean v = parseOr();
                if (pos >= tokens.size() || !tokens.get(pos).equals(")")) throw new IllegalStateException("')' fehlt");
                pos++;
                return v;
            }
            return t.equalsIgnoreCase(test);
        }
    }

    // ------------------------------------------------------------ Auswahl

    private static List<Target> selectTargets(Setup s, Options o) {
        Map<String, Target> byPath = new LinkedHashMap<String, Target>();
        for (FileEntry fe : s.files) {
            if (fe.location >= s.data.size()) continue; // externe Datei (Copy-Befehl)
            if (!o.includeTemp && fe.deleteAfterInstall) continue;
            if (!fe.languages.isEmpty() && o.language != null && !exprMatch(o.language, fe.languages)) continue;
            String path = convertPath(fe.destination, o);
            if (path.isEmpty()) continue;
            if (o.filter != null && !o.filter.accept(path)) continue;
            // Kollisionen: spätere Einträge überschreiben frühere (Pfade ohne Groß-/Kleinschreibung)
            byPath.put(path.toLowerCase(Locale.ROOT), new Target(path, fe, s.data.get((int) fe.location)));
        }
        return new ArrayList<Target>(byPath.values());
    }

    // ------------------------------------------------------------ Entpacken

    private static InputStream openChunk(RandomAccessFile f, Setup s, DataEntry d) throws IOException {
        if (d.chunkEncrypted) throw new InnoException("Verschlüsselte (Passwort-)Installer werden nicht unterstützt");
        if (d.firstSlice != 0 || d.lastSlice != 0) {
            throw new InnoException("Daten liegen in externen setup-N.bin-Dateien (Slice "
                    + d.firstSlice + "); nicht unterstützt");
        }
        long start = s.dataOffset + d.chunkOffset;
        byte[] magic = new byte[4];
        f.seek(start);
        f.readFully(magic);
        if (!Arrays.equals(magic, new byte[] {'z', 'l', 'b', 0x1a})) throw new InnoException("Ungültige Chunk-Kennung");
        InputStream raw = new BufferedInputStream(new FileRange(f, start + 4, d.chunkSize), 1 << 16);
        int comp = d.chunkCompressed ? s.compression : C_STORED;
        switch (comp) {
            case C_STORED: return raw;
            case C_ZLIB: return new InflaterInputStream(raw);
            case C_LZMA1: return newLzma1(raw);
            case C_LZMA2: return newLzma2(raw);
            default: throw new InnoException("Nicht unterstützte Kompression (bzip2)");
        }
    }

    /** x86-CALL/JMP-Filter für Inno 5.3.9+ (inno_exe_decoder_5200 mit flip_high_byte). */
    private static final class CallFilter extends InputStream {
        private final InputStream in;
        private long offset = 0;
        private final byte[] pending = new byte[4];
        private int pendPos = 0, pendLen = 0;

        CallFilter(InputStream in) {
            this.in = in;
        }

        @Override
        public int read() throws IOException {
            if (pendPos < pendLen) return pending[pendPos++] & 0xff;
            int b = in.read();
            if (b < 0) return -1;
            offset++;
            if (b == 0xe8 || b == 0xe9) {
                long blockLeft = 0x10000 - ((offset - 1) % 0x10000);
                if (blockLeft >= 5) {
                    int n = 0;
                    while (n < 4) {
                        int c = in.read();
                        if (c < 0) break;
                        pending[n++] = (byte) c;
                    }
                    offset += n;
                    if (n == 4) {
                        int hi = pending[3] & 0xff;
                        if (hi == 0x00 || hi == 0xff) {
                            int addr = (int) (offset & 0xffffff);
                            int rel = (pending[0] & 0xff) | ((pending[1] & 0xff) << 8) | ((pending[2] & 0xff) << 16);
                            rel -= addr;
                            pending[0] = (byte) rel;
                            pending[1] = (byte) (rel >> 8);
                            pending[2] = (byte) (rel >> 16);
                            if ((rel & 0x800000) != 0) pending[3] = (byte) ~pending[3];
                        }
                    }
                    pendPos = 0;
                    pendLen = n;
                }
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (len == 0) return 0;
            int n = 0;
            while (n < len) {
                int c = read();
                if (c < 0) break;
                b[off + n++] = (byte) c;
            }
            return n == 0 ? -1 : n;
        }
    }

    private static final Comparator<Target> BY_OFFSET = new Comparator<Target>() {
        @Override
        public int compare(Target a, Target b) {
            if (a.data.fileOffset != b.data.fileOffset) return a.data.fileOffset < b.data.fileOffset ? -1 : 1;
            if (a.data.fileSize != b.data.fileSize) return a.data.fileSize < b.data.fileSize ? -1 : 1;
            return 0;
        }
    };

    private static void extractTargets(RandomAccessFile f, Setup s, List<Target> targets, File outDir, Options o,
                                       Progress progress) throws IOException {
        // Gesamtgröße (jede Datenquelle zählt einmal pro Ausgabedatei nicht; wir zählen gelesene Bytes)
        long total = 0;
        Map<String, List<Target>> chunks = new LinkedHashMap<String, List<Target>>();
        for (Target t : targets) {
            DataEntry d = t.data;
            String key = d.firstSlice + ":" + d.chunkOffset + ":" + d.chunkSize + ":" + d.chunkCompressed;
            List<Target> list = chunks.get(key);
            if (list == null) {
                list = new ArrayList<Target>();
                chunks.put(key, list);
            }
            list.add(t);
        }
        for (List<Target> list : chunks.values()) {
            Collections.sort(list, BY_OFFSET);
            long lastOff = -1, lastSize = -1;
            for (Target t : list) {
                if (t.data.fileOffset != lastOff || t.data.fileSize != lastSize) total += t.data.fileSize;
                lastOff = t.data.fileOffset;
                lastSize = t.data.fileSize;
            }
        }
        progress.onStart(targets.size(), total);

        String rootCanon = outDir.getCanonicalPath() + File.separator;
        long done = 0;
        byte[] buf = new byte[64 * 1024];

        for (List<Target> list : chunks.values()) {
            InputStream cs = null;
            long pos = 0;
            int i = 0;
            try {
                while (i < list.size()) {
                    // Gruppe mit identischem (Offset, Größe) -> eine Quelle, mehrere Ausgaben
                    Target first = list.get(i);
                    int j = i + 1;
                    while (j < list.size() && list.get(j).data.fileOffset == first.data.fileOffset
                            && list.get(j).data.fileSize == first.data.fileSize) j++;
                    DataEntry d = first.data;

                    if (cs == null || d.fileOffset < pos) {
                        if (cs != null) cs.close();
                        cs = openChunk(f, s, d);
                        pos = 0;
                    }
                    skipFully(cs, d.fileOffset - pos);
                    pos = d.fileOffset;

                    InputStream src = new LimitStream(cs, d.fileSize);
                    if (d.callInstructionOptimized) src = new CallFilter(src);

                    List<OutputStream> outs = new ArrayList<OutputStream>();
                    List<File> files = new ArrayList<File>();
                    try {
                        for (int k = i; k < j; k++) {
                            File target = new File(outDir, list.get(k).path.replace('/', File.separatorChar));
                            if (!target.getCanonicalPath().startsWith(rootCanon)) {
                                throw new InnoException("Unsicherer Pfad: " + list.get(k).path);
                            }
                            File parent = target.getParentFile();
                            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                                throw new InnoException("Kann Ordner nicht anlegen: " + parent);
                            }
                            outs.add(new BufferedOutputStream(new FileOutputStream(target), 1 << 16));
                            files.add(target);
                            progress.onFile(list.get(k).path);
                        }
                        MessageDigest md = null;
                        if (o.verifyChecksums && d.checksumAlgo != null) {
                            try {
                                md = MessageDigest.getInstance(d.checksumAlgo);
                            } catch (NoSuchAlgorithmException e) {
                                md = null;
                            }
                        }
                        long written = 0;
                        int n;
                        while ((n = src.read(buf, 0, buf.length)) > 0) {
                            if (md != null) md.update(buf, 0, n);
                            for (OutputStream os : outs) os.write(buf, 0, n);
                            written += n;
                            done += n;
                            progress.onBytes(done, total);
                            if (progress.isCancelled()) throw new CancellationException("Abgebrochen");
                        }
                        pos += d.fileSize;
                        if (written != d.fileSize) {
                            throw new InnoException("Datei unvollständig: " + first.path + " (" + written + " von "
                                    + d.fileSize + " Bytes)");
                        }
                        if (md != null && !Arrays.equals(md.digest(), d.checksum)) {
                            throw new InnoException("Prüfsumme stimmt nicht: " + first.path);
                        }
                    } finally {
                        for (OutputStream os : outs) {
                            try { os.close(); } catch (IOException ignore) { }
                        }
                    }
                    // Zeitstempel (FILETIME -> ms seit 1970)
                    long ms = (d.fileTime - 0x19DB1DED53E8000L) / 10000L;
                    if (ms > 0) {
                        for (File out : files) out.setLastModified(ms);
                    }
                    i = j;
                }
            } finally {
                if (cs != null) {
                    try { cs.close(); } catch (IOException ignore) { }
                }
            }
        }
    }

    private static void skipFully(InputStream in, long n) throws IOException {
        byte[] tmp = new byte[64 * 1024];
        while (n > 0) {
            int r = in.read(tmp, 0, (int) Math.min(tmp.length, n));
            if (r < 0) throw new EOFException("Chunk zu kurz");
            n -= r;
        }
    }

    // ------------------------------------------------------------- CLI (Test)

    /**
     * Test auf dem Desktop:
     *   java -cp .:xz.jar inno.InnoExtractor version setup.exe
     *   java -cp .:xz.jar inno.InnoExtractor list setup.exe [Sprache]
     *   java -cp .:xz.jar inno.InnoExtractor extract setup.exe Zielordner [Sprache]
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Aufruf: version|list|extract <setup.exe> [Zielordner] [Sprache]");
            System.exit(2);
        }
        File exe = new File(args[1]);
        Options o = new Options();
        if (args[0].equals("version")) {
            System.out.println(dataVersion(exe));
            System.out.println("Sprachen: " + languages(exe));
        } else if (args[0].equals("list")) {
            if (args.length > 2) o.language = args[2];
            for (Entry e : list(exe, o)) System.out.println(e);
        } else if (args[0].equals("extract")) {
            File out = new File(args[2]);
            if (args.length > 3) o.language = args[3];
            final long[] last = {0};
            extract(exe, out, o, new Progress() {
                @Override
                public void onBytes(long done, long total) {
                    long now = System.currentTimeMillis();
                    if (now - last[0] > 500 || done == total) {
                        last[0] = now;
                        System.err.printf("\r%d / %d MiB", done >> 20, total >> 20);
                    }
                }
            });
            System.err.println();
        }
    }
}
