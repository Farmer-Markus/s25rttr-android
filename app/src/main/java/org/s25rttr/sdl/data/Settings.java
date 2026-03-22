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
public class Settings implements Serializable {
    // Defaults
    private static final String DEF_PATHS = "";
    private static final String DEF_NAME = "android";
    private static final int DEF_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
    private static final boolean DEF_FEATURES = true;

    public static final boolean DEF_GL_VSYNC = false;
    public static final int DEF_GL_BATCH = 0;
    public static final int DEF_GL_FB = 0;
    public static final int GL_BATCH_MAX = 100;
    public static final int GL_FB_MAX = 3;

    // Save names
    private static final String ID_SETTINGS = "settings";
    private static final String ID_RTTR_DIR = "rttr_directory";
    private static final String ID_GAME_DIR = "game_directory";
    private static final String ID_DEF_NAME = "default_name";
    private static final String ID_ORIENTATION = "orientation";
    private static final String ID_SHOW_EXIT_DIALOG = "show_exit_dialog";
    private static final String ID_ENABLE_OVERLAY = "enable_overlay";
    private static final String ID_ENABLE_UPDATER = "enable_updater";
    private static final String ID_LAST_UPDATED = "last_updated";
    private static final String ID_FEATURE_SHOWN = "feature_shown";

    // Current settings
    public String RttrDirectory;
    public String GameDirectory;
    public String DefaultName;
    public int Orientation;

    public boolean ShowExitDialog;
    public boolean EnableOverlay;
    public boolean EnableUpdater;
    public long LastUpdated;

    public boolean FeatureShown;

    // Experimental settings
    public boolean GlVsync;
    public int GlBatch;
    public int GlFb;


    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(!(obj instanceof Settings)) return false;

        Settings s = (Settings)obj;
        return RttrDirectory.equals(s.RttrDirectory) && GameDirectory.equals(s.GameDirectory)
                && DefaultName.equals(s.DefaultName) && Orientation == s.Orientation
                && ShowExitDialog == s.ShowExitDialog && EnableOverlay == s.EnableOverlay
                && EnableUpdater == s.EnableUpdater && LastUpdated == s.LastUpdated
                && FeatureShown == s.FeatureShown
                && GlVsync == s.GlVsync && GlBatch == s.GlBatch && GlFb == s.GlFb;
    }

    // Save current settings
    public Settings Save(Context context) {
        SharedPreferences pref = context.getSharedPreferences(ID_SETTINGS, MODE_PRIVATE);
        SharedPreferences.Editor edit = pref.edit();

        edit.putString(ID_RTTR_DIR, RttrDirectory);
        edit.putString(ID_GAME_DIR, GameDirectory);
        edit.putString(ID_DEF_NAME, DefaultName);
        edit.putInt(ID_ORIENTATION, Orientation);
        edit.putBoolean(ID_SHOW_EXIT_DIALOG, ShowExitDialog);
        edit.putBoolean(ID_ENABLE_OVERLAY, EnableOverlay);
        edit.putBoolean(ID_ENABLE_UPDATER, EnableUpdater);
        edit.putLong(ID_LAST_UPDATED, LastUpdated);
        edit.putBoolean(ID_FEATURE_SHOWN, FeatureShown);

        edit.putBoolean("gl_vsync", GlVsync);
        edit.putInt("gl_batch", GlBatch);
        edit.putInt("gl_fb", GlFb);

        edit.apply();
        return this;
    }

    // Load saved settings
    public Settings Load(Context context) {
        SharedPreferences pref = context.getSharedPreferences(ID_SETTINGS, MODE_PRIVATE);

        RttrDirectory = pref.getString(ID_RTTR_DIR, DEF_PATHS);
        GameDirectory = pref.getString(ID_GAME_DIR, DEF_PATHS);
        DefaultName = pref.getString(ID_DEF_NAME, DEF_NAME);
        Orientation = pref.getInt(ID_ORIENTATION, DEF_ORIENTATION);
        ShowExitDialog = pref.getBoolean(ID_SHOW_EXIT_DIALOG, DEF_FEATURES);
        EnableOverlay = pref.getBoolean(ID_ENABLE_OVERLAY, DEF_FEATURES);
        EnableUpdater = pref.getBoolean(ID_ENABLE_UPDATER, DEF_FEATURES);
        LastUpdated = pref.getLong(ID_LAST_UPDATED, 0);
        FeatureShown = pref.getBoolean(ID_FEATURE_SHOWN, false);

        GlVsync = pref.getBoolean("gl_vsync", DEF_GL_VSYNC);
        GlBatch = pref.getInt("gl_batch", DEF_GL_BATCH);
        GlFb = pref.getInt("gl_fb", DEF_GL_FB);
        return this;
    }

    // Read config file of very old installation
    public static String COMPAT_GetOld(Context context) {
        Path oldConf = new Path(context.getFilesDir().toString()).Append("AppPathConfig.conf");
        if(!oldConf.Exists())
            return null;

        try(BufferedReader br = new BufferedReader(new FileReader(oldConf.toString()))) {
            return br.readLine();
        } catch (Exception ignore) {
            return null;
        }
    }
}
