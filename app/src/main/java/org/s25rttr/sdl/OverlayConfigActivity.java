package org.s25rttr.sdl;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import org.s25rttr.sdl.data.Settings;
import org.s25rttr.sdl.overlay.OverlayEditor;
import org.s25rttr.sdl.utils.UiHelper;


/*
    Click on background -> open popup menu https://www.geeksforgeeks.org/android/popup-menu-in-android-with-example/
    to save config, cancel, add button
    Click on Button -> open popup menu to delete button, configure button(which will open an ErrorDialog or something
    with all the settings, spinner, text, etc.)
    Also set callback to backButtonPressed to show dialog(save, continue)
 */

public class OverlayConfigActivity extends Activity {
    private static final String EDITOR_KEY = "editor";
    private OverlayEditor oEditor;
    private Settings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = (Settings)getIntent().getSerializableExtra(GameConfigActivity.SETTINGS_ID);
        if(settings == null)
            settings = new Settings().Load(this);

        RelativeLayout view = new RelativeLayout(this);
        setContentView(view);

        // Restore editor or load new one
        if(savedInstanceState == null || (oEditor = (OverlayEditor)savedInstanceState.getSerializable(EDITOR_KEY)) == null) {
            oEditor = new OverlayEditor(this, view, null, false, settings);
            oEditor.Load(true);
        } else
            oEditor.Restore(this, view);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(EDITOR_KEY, oEditor);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        oEditor = (OverlayEditor)savedInstanceState.getSerializable("editor");
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiHelper.SetFullscreen(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R && hasFocus)
            UiHelper.SetFullscreen(this);
    }
}
