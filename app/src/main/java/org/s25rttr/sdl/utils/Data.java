package org.s25rttr.sdl.utils;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

public class Data {
    public String gameFolder;
    public String defaultName;

    public void saveSettings(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("game_config", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("game_folder", gameFolder);
        editor.putString("default_name", defaultName);
        editor.apply();
    }
    public void loadSettings(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("game_config", MODE_PRIVATE);
        gameFolder = preferences.getString("game_folder", "");
        defaultName = preferences.getString("default_name", "android");
    }


    /* Example uri's
        sdcard: /tree/0000-0000:s25rttr
        intern: /tree/primary:S25rttr

        we need:
        /storage/0/ <folder> // Internal storage
        /storage/<????-????>/ <folder> // Sdcard
     */
    public String getRealPath(Uri uri) {
        String path = uri.getPath();
        if(path == null) {
            return new String("");
        }
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

        return path;
    }
}