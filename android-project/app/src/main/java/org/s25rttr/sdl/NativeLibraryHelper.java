package org.s25rttr.sdl;

import android.content.Context;
import android.os.Environment;
import android.system.Os;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class NativeLibraryHelper {

    private static final String TAG = "NativeLibraryHelper";

    public static int manageLibs(Context context) throws Exception {
	File baseDir = new File(context.getCacheDir(), "lib/s25rttr/driver");
	File videoDir = new File(baseDir, "video");
	File audioDir = new File(baseDir, "audio");

	boolean baseSuccess = baseDir.exists()   || baseDir.mkdirs();
	boolean videoSuccess = videoDir.exists() || videoDir.mkdirs();
	boolean audioSuccess = audioDir.exists() || audioDir.mkdirs();

	if (!baseSuccess || !videoSuccess || !audioSuccess) {
	    Log.e(TAG, "org.libsdl.app Failed to create directory structure.");
	    return 0;
	}
	Log.e(TAG, "org.libsdl.app created dirs");

	File nativeLibraryDir = new File(context.getApplicationInfo().nativeLibraryDir);

	boolean audioCopySuccess = copyFile(nativeLibraryDir, audioDir, "libaudioSDL.so");
	boolean videoCopySuccess = copyFile(nativeLibraryDir, videoDir, "libvideoSDL2.so");
	
	Os.setenv("libDir", baseDir.toString() + "/", true);
	
	return (audioCopySuccess && videoCopySuccess) ? 1 : 0;
    }

    private static boolean copyFile(File sourceDir, File destDir, String fileName) {
	File sourceFile = new File(sourceDir, fileName);
	File destFile = new File(destDir, fileName);

	if (!sourceFile.exists()) {
	    Log.e(TAG, "Source file does not exist: " + sourceFile.getAbsolutePath());
	    return false;
	} else if (destFile.exists()) {
	    Log.e(TAG, "Destination file already exist: " + destFile.getAbsolutePath());
	    return true;
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

