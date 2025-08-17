package org.s25rttr.sdl.utils;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;


public class Data {
    public String gameFolder;
    public String defaultName;
    public int orientation;

    @Override
    public boolean equals(Object o) {
        // Basic quals operation
        if(this == o) return true;
        if(!(o instanceof Data)) return false;

        // Looks if content is the same
        Data obj = (Data)o;
        return gameFolder.equals(obj.gameFolder) && defaultName.equals(obj.defaultName)
                && orientation == obj.orientation;
    }

    public void saveSettings(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("game_config", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("game_folder", gameFolder);
        editor.putString("default_name", defaultName);
        editor.putInt("orientation", orientation);
        editor.apply();
    }
    public void loadSettings(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("game_config", MODE_PRIVATE);
        gameFolder = preferences.getString("game_folder", "");
        defaultName = preferences.getString("default_name", "android");
        orientation = preferences.getInt("orientation", ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }
}