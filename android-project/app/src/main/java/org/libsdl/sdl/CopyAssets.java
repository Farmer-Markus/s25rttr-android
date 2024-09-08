package org.libsdl.app;

import android.content.Context;
import android.content.res.AssetManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CopyAssets {

    public static void copyAssetsToInternalStorage(Context context) throws IOException {
        AssetManager assetManager = context.getAssets();
        // Define the destination directory as "share" inside the internal storage
        File destinationDir = new File(context.getFilesDir(), "share");
        // Create the "share" directory if it does not exist
        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
        }
        // Start copying assets
        copyDirectory(assetManager, "share", destinationDir);
    }

    private static void copyDirectory(AssetManager assetManager, String sourceDir, File destinationDir) throws IOException {
        String[] files = assetManager.list(sourceDir);
        if (files == null) {
            return; // No files or directories in the source directory
        }

        for (String fileName : files) {
            String assetPath = sourceDir + "/" + fileName;
            String[] nestedFiles = assetManager.list(assetPath);

            File outFile = new File(destinationDir, fileName);
            if (nestedFiles != null && nestedFiles.length > 0) {
                // It's a directory, recursively copy its contents
                if (!outFile.exists()) {
                    outFile.mkdirs();
                }
                copyDirectory(assetManager, assetPath, outFile);
            } else {
                // It's a file, copy its content
                try (InputStream in = assetManager.open(assetPath);
                     OutputStream out = new FileOutputStream(outFile)) {
                    copyFile(in, out);
                }
            }
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
    }
}

