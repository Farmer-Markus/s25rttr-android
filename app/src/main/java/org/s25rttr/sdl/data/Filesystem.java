package org.s25rttr.sdl.data;

import android.net.Uri;
import android.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Filesystem {
    /* Example uri's
        sdcard: /tree/0000-0000:s25rttr
        intern: /tree/primary:S25rttr

        we need:
        /storage/0/ <folder>            // Internal storage
        /storage/<????-????>/ <folder>  // Sdcard
     */
    // I really hate this function but I don't know how to do this properly. Do you know? Please let me know :D
    public static String UriToRealPath(Uri uri) {
        if(uri == null) return "";

        String path = uri.getPath();
        if(path == null) return "";

        int treePos = path.indexOf("/tree/");
        if(treePos >= 0) // Remove "/tree/"
            path = path.substring(treePos + 6);

        int colonPos = path.indexOf(":");
        if(colonPos < 0)
            return "/storage/" + path;

        String storageCode = path.substring(0, colonPos);
        String restPath = path.substring(colonPos + 1);
        if("primary".equalsIgnoreCase(storageCode))
            return "/storage/emulated/0/" + restPath;
        else
            return "/storage/" + storageCode + "/" + restPath;
    }

    // Create testfile to test write permission
    public static boolean IsPathWritable(String path) {
        if(!path.endsWith("/"))
            path += "/";
        path += "testfile";

        File file = new File(path);
        boolean success;
        try {
            success = file.createNewFile();
        } catch (IOException e) {
            return false;
        }

        return file.exists() && file.delete() && success;
    }

    public static void CopyFile(InputStream inStr, FileOutputStream outStr) throws IOException {
        byte[] buffer = new byte[1024];
        int read;

        while((read = inStr.read(buffer)) != -1) {
            outStr.write(buffer, 0, read);
        }
    }

    public static String[] ListFiles(Path path) {
        return new File(path.toString()).list();
    }

    public static String FileGenSha256(InputStream stream) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ignore) {
            throw new IOException("Failed to get SHA-256. Unknown algorithm!");
        }

        byte[] buffer = new byte[256];
        int read;

        while((read = stream.read(buffer)) != -1)
            digest.update(buffer, 0, read);

        return Base64.encodeToString(digest.digest(), Base64.DEFAULT);
    }

    public static String FileGenSha256(FileInputStream stream) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ignore) {
            throw new IOException("Failed to get SHA-256. Unknown algorithm!");
        }

        byte[] buffer = new byte[256];
        int read;

        while((read = stream.read(buffer)) != -1)
            digest.update(buffer, 0, read);

        return Base64.encodeToString(digest.digest(), Base64.NO_WRAP);
    }
}
