package org.s25rttr.sdl;

import android.content.Context;
import android.content.res.AssetManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.util.Log;

public class CopyAssets {
    private static final String TAG = "s25rttr";
    

    public static void copyAssetsToDataStorage(Context context, String destDir) throws IOException {
        AssetManager assetManager = context.getAssets();
        File destinationDir = new File(destDir, "share");
        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
            Log.e(TAG, "org.libsdl.app Copying assets... from " + assetManager + " to " + destinationDir);
            copyDirectory(assetManager, "share", destinationDir);
        }
        //copyDirectory(assetManager, "share", destinationDir);
    }

    private static void copyDirectory(AssetManager assetManager, String sourceDir, File destinationDir) throws IOException {
        String[] files = assetManager.list(sourceDir);
        if (files == null) {
            Log.e(TAG, "org.libsdl.app Files = NULL!");
            return;
        }

        for (String fileName : files) {
            String assetPath = sourceDir + "/" + fileName;
            String[] nestedFiles = assetManager.list(assetPath);

            File outFile = new File(destinationDir, fileName);
            if (nestedFiles != null && nestedFiles.length > 0) {
                if (!outFile.exists()) {
                    outFile.mkdirs();
                }
                copyDirectory(assetManager, assetPath, outFile);
            } else {
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

