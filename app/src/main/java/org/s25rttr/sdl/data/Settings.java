package org.s25rttr.sdl.data;


import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;

/*
  Store & load all userset variables
  paths and username
 */
public class Settings implements Serializable
{
    public String RttrDirectory;
    public String GameDirectory;
    public String DefaultName;
    public int Orientation;

    public boolean ShowExitDialog;
    public boolean EnableUpdater;
    public long LastUpdated;


    @Override
    public boolean equals(Object obj)
    {
        if(this == obj) return true;
        if(!(obj instanceof Settings)) return false;

        Settings s = (Settings)obj;
        return RttrDirectory.equals(s.RttrDirectory) && GameDirectory.equals(s.GameDirectory)
                && DefaultName.equals(s.DefaultName) && Orientation == s.Orientation
                && ShowExitDialog == s.ShowExitDialog && EnableUpdater == s.EnableUpdater
                && LastUpdated == s.LastUpdated;
    }

    // Save current settings
    public Settings Save(Context context)
    {
        SharedPreferences pref = context.getSharedPreferences("settings", MODE_PRIVATE);
        SharedPreferences.Editor edit = pref.edit();

        edit.putString("rttr_directory", RttrDirectory);
        edit.putString("game_directory", GameDirectory);
        edit.putString("default_name", DefaultName);
        edit.putInt("orientation", Orientation);
        edit.putBoolean("show_exit_dialog", ShowExitDialog);
        edit.putBoolean("enable_updater", EnableUpdater);
        edit.putLong("last_updated", LastUpdated);

        edit.apply();
        return this;
    }

    // Load saved settings
    public Settings Load(Context context)
    {
        SharedPreferences pref = context.getSharedPreferences("settings", MODE_PRIVATE);

        RttrDirectory = pref.getString("rttr_directory", "");
        GameDirectory = pref.getString("game_directory", "");
        DefaultName = pref.getString("default_name", "android");
        Orientation = pref.getInt("orientation", ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        ShowExitDialog = pref.getBoolean("show_exit_dialog", true);
        EnableUpdater = pref.getBoolean("enable_updater", true);
        LastUpdated = pref.getLong("last_updated", 0);
        return this;
    }

    public static String COMPAT_GetOld(Context context)
    {
        Path oldConf = new Path(context.getFilesDir().toString()).Append("AppPathConfig.conf");
        if(!oldConf.Exists())
            return null;

        try(BufferedReader br = new BufferedReader(new FileReader(oldConf.toString())))
        {
            return br.readLine();
        } catch (Exception ignore)
        {
            return null;
        }
    }
}
