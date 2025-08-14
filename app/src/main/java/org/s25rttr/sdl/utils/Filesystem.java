package org.s25rttr.sdl.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class Filesystem {

    public static boolean pathIsWritable(String path) {
        try {
            File file = new File(path + "testFile.txt");
            if(file.exists()) {
                file.delete();
                if(file.exists()) {
                    throw new IOException("Cannot delete file");
                }
            }
            file.createNewFile();
            if(file.exists()) return true;

        } catch (IOException e) {
            return false;
        }
        return false;
    }

    public static boolean prepareDrivers(Context context, boolean overWrite) throws IOException {
        // Location of .so libraries including rttr audio/video driver
        File libDir = new File(context.getApplicationInfo().nativeLibraryDir);
        File cacheDir = context.getCacheDir();

        File videoDir = new File(cacheDir, "lib/s25rttr/driver/video");
        File audioDir = new File(cacheDir, "lib/s25rttr/driver/audio");

        File videoDest = new File(videoDir, "libvideoSDL2.so");
        File audioDest = new File(audioDir, "libaudioSDL.so");

        if(overWrite) {
            Files.deleteIfExists(videoDest.toPath());
            Files.deleteIfExists(audioDest.toPath());

        } else {
            if(videoDest.exists() && audioDest.exists()) return true;
        }

        // Ensures that destination dirs exist
        boolean videoSuccsess = videoDir.exists() || videoDest.mkdirs();
        boolean audioSuccsess = audioDir.exists() || audioDest.mkdirs();

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

    public static boolean copyAssets(Context context, AssetManager manager, String source, File destination) throws IOException {
        String[] files = manager.list(source);
        if(files == null) {
            Log.e("org.s25rttr.sdl", "Filesystem::copyAssets: Could not find any asset files!");
            Ui.alertDialog(context, "Gamedata error", "Could not find assets in apk", null);
            return false;
        }

        if(!destination.exists()) {
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
                copyAssets(context, manager, filePath, out);

            } else {
                InputStream inStrm = manager.open(filePath);
                OutputStream outStrm = new FileOutputStream(out);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while((bytesRead = inStrm.read(buffer)) != -1) {
                    outStrm.write(buffer, 0, bytesRead);
                }
            }
        }

        return true;
    }
}
