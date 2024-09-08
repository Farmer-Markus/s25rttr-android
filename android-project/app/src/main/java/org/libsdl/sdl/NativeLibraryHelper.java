package org.libsdl.app;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class NativeLibraryHelper {

    private static final String TAG = "NativeLibraryHelper";

    /**
     * Creates the directory structure and copies specific native libraries to their respective directories.
     * @param context The application context to get the native library directory.
     * @return 1 if successful, 0 otherwise.
     */
    public static int createDirectory(Context context) {
        // Define the directory path
        File baseDir = new File(context.getFilesDir(), "lib/s25rttr/driver");
        File videoDir = new File(baseDir, "video");
        File audioDir = new File(baseDir, "audio");

        // Create the directories
        boolean baseSuccess = baseDir.exists()   || baseDir.mkdirs();
        boolean videoSuccess = videoDir.exists() || videoDir.mkdirs();
        boolean audioSuccess = audioDir.exists() || audioDir.mkdirs();

        if (!baseSuccess || !videoSuccess || !audioSuccess) {
            Log.e(TAG, "org.libsdl.app Failed to create directory structure.");
            return 0; // Failed to create directories
        }
        Log.e(TAG, "org.libsdl.app created dirs");

        // Get the native library directory
        File nativeLibraryDir = new File(context.getApplicationInfo().nativeLibraryDir);
        
        //System.setProperty("LD_LIBRARY_PATH", context.getApplicationInfo().nativeLibraryDir);
        //System.loadLibrary("GL");

        // Copy specific files to their respective directories
        boolean audioCopySuccess = copyFile(nativeLibraryDir, audioDir, "libaudioSDL.so");
        boolean videoCopySuccess = copyFile(nativeLibraryDir, videoDir, "libvideoSDL2.so");

        // Return success if both files were copied successfully
        return (audioCopySuccess && videoCopySuccess) ? 1 : 0;
    }

    /**
     * Copies a specific file from the source directory to the destination directory.
     * @param sourceDir The directory to copy files from.
     * @param destDir The directory to copy files to.
     * @param fileName The name of the file to copy.
     * @return true if the file was copied successfully, false otherwise.
     */
    private static boolean copyFile(File sourceDir, File destDir, String fileName) {
        File sourceFile = new File(sourceDir, fileName);
        File destFile = new File(destDir, fileName);

        if (!sourceFile.exists()) {
            Log.e(TAG, "Source file does not exist: " + sourceFile.getAbsolutePath());
            return false;
        }

        try (FileInputStream in = new FileInputStream(sourceFile);
             FileOutputStream out = new FileOutputStream(destFile)) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            Log.d(TAG, "File copied successfully: " + destFile.getAbsolutePath());
            return true;

        } catch (IOException e) {
            Log.e(TAG, "Error copying file: " + fileName, e);
            return false;
        }
    }
}

