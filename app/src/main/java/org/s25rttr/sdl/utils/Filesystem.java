package org.s25rttr.sdl.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Log;
import android.widget.TextView;

import org.s25rttr.sdl.TextViewActivity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Filesystem {

    /* Example uri's
        sdcard: /tree/0000-0000:s25rttr
        intern: /tree/primary:S25rttr

        we need:
        /storage/0/ <folder>            // Internal storage
        /storage/<????-????>/ <folder>  // Sdcard
     */
    public static String getRealPath(Uri uri) {
        if(uri == null) return "";

        String path = uri.getPath();
        if(path == null) return "";

        int tree = path.indexOf("/tree/");
        if(tree >= 0) {
            // removed "/tree/"
            path = path.substring(tree + 6);
        }

        int colon = path.indexOf(":");
        if(colon < 0) {
            return "/storage/" + path;
        }

        String storageCode = path.substring(0, colon);
        String restPath = path.substring(colon + 1);
        if("primary".equalsIgnoreCase(storageCode)) {
            return "/storage/emulated/0/" + restPath;
        } else {
            return "/storage/" + storageCode + "/" + restPath;
        }

        /*
        // removed "/tree/"
        path = path.substring(6);

        String storageCode = "";
        int pathOffset = path.length();

        int i = 0;
        for(; i < pathOffset; i++) {
            char c = path.charAt(i);
            if(c == ':') break;

            storageCode += c;
        }
        // Set offset to folder begin
        pathOffset = i + 1;

        // If internal storage
        if(storageCode.equals("primary")) {
            path = "/storage/emulated/0/" + path.substring(pathOffset);

        } else {
            // Apply sdcard code |
            path = "/storage/" + storageCode + "/" + path.substring(pathOffset);
        }

        return path;*/
    }

    public static boolean pathIsWritable(String path) {
        try {
            File file = new File(path, "testFile.txt");
            if(file.exists()) {
                if(!file.delete() || file.exists()) {
                    throw new IOException("Cannot delete file");
                }
            }

            if(file.createNewFile() && file.exists()) {
                return file.delete();
            }

        } catch (IOException e) {
            return false;
        }
        return false;
    }

    public static boolean prepareDrivers(Context context, boolean overWrite) throws IOException {
        // Location of .so libraries including rttr audio/video driver
        File libDir = new File(context.getApplicationInfo().nativeLibraryDir);
        File cacheDir = context.getCacheDir();

        File videoDir = new File(cacheDir, "driver/video");
        File audioDir = new File(cacheDir, "driver/audio");

        File videoDest = new File(videoDir, "libvideoSDL2.so");
        File audioDest = new File(audioDir, "libaudioSDL.so");

        if(overWrite) {
            Files.deleteIfExists(videoDest.toPath());
            Files.deleteIfExists(audioDest.toPath());

        } else {
            if(videoDest.exists() && audioDest.exists()) return true;
        }

        // Ensures that destination dirs exist
        boolean videoSuccsess = videoDir.exists() || videoDir.mkdirs();
        boolean audioSuccsess = audioDir.exists() || audioDir.mkdirs();

        if(!videoSuccsess || !audioSuccsess) {
            Log.e("org.s25rttr.sdl", "Filesystem::prepareDrivers: Failed to create video/audio driver cache directory.");
            return false;
        }

        /*try {
            Files.copy(libDir.toPath().resolve("libvideoSDL2.so"), videoDest.toPath());
            Files.copy(libDir.toPath().resolve("libaudioSDL.so"), audioDest.toPath());
        } catch (IOException e) {
            Log.e("org.s25rttr.sdl", "Filesystem::prepareDrivers: Failed to copy driver libraries.");
            Log.e("org.s25rttr.sdl", e.toString());
            return false;
        }*/

        Files.createSymbolicLink(videoDest.toPath(), libDir.toPath().resolve("libvideoSDL2.so"));
        Files.createSymbolicLink(audioDest.toPath(), libDir.toPath().resolve("libaudioSDL.so"));

        if(!videoDest.exists() || !audioDest.exists()) throw new IOException("Audio/Video driver link does not exist!");

        return true;
    }

    public static boolean copyAssets(Activity activity, AssetManager manager, String source, File destination) throws IOException {
        return copyAssets(activity, manager, source, destination, null);
    }

    public static boolean copyAssets(Activity activity, AssetManager manager, String source, File destination, TextView status) throws IOException {
        String[] files = manager.list(source);
        if(files == null) {
            Log.e("org.s25rttr.sdl", "Filesystem::copyAssets: Could not find any asset files!");
            Ui.alertDialog(activity, "Gamedata error", "Could not find assets in apk", null);
            return false;
        }

        if(!destination.exists()) {
            if(status != null) {
                activity.runOnUiThread(() -> {
                    status.setText(destination.getPath());
                });
            }

            if(!destination.mkdirs()) {
                Log.e("org.s25rttr.sdl", "Filesystem::copyAssets: Failed to create directories");
                return false;
            }
        }

        for(String fileName : files) {
            String filePath = source + "/" + fileName;
            File out = new File(destination, fileName);

            // If is folder
            String[] subFiles = manager.list(filePath);
            if(subFiles != null && subFiles.length > 0) {
                copyAssets(activity, manager, filePath, out, status);

            } else {
                if(status != null) {
                    activity.runOnUiThread(() -> {
                        status.setText(out.getPath());
                    });
                }

                InputStream inStrm = manager.open(filePath);
                OutputStream outStrm = Files.newOutputStream(out.toPath());
                byte[] buffer = new byte[1024];
                int bytesRead;
                while((bytesRead = inStrm.read(buffer)) != -1) {
                    outStrm.write(buffer, 0, bytesRead);
                }
            }
        }

        return true;
    }

    public static boolean deleteDirectory(File dir) {
        if(!dir.exists())
            return true;

        String[] files = dir.list();
        for(String fileName : files) {
            File file = new File(dir, fileName);
            if(file.isDirectory())
                if(!deleteDirectory(new File(file, fileName)))
                    return false;

            if(!file.delete())
                return false;
        }

        return true;
    }

    public static void openTextFile(Context context, String path) {
        Intent intent = new Intent(context, TextViewActivity.class);
        intent.putExtra("path", path);
        context.startActivity(intent);
    }

    public static String fileGetLine(RandomAccessFile raf, long offset) throws IOException {
        raf.seek(offset);
        return new String(raf.readLine().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
    }

    public static List<Long> fileGetLineOffsets(RandomAccessFile raf, long offset, long lines) throws IOException {
        List<Long> offsets = new ArrayList<>();
        raf.seek(offset);
        long currOffset = offset;

        while(raf.readLine() != null) {
            offsets.add(currOffset);
            if(lines > 0 && offsets.size() >= lines)
                break;
            currOffset = raf.getFilePointer();
        }

        return offsets;
    }
}
