package org.s25rttr.sdl;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SetFullscreen();

        ViewGroup view = new FrameLayout(this);
        setContentView(view);

        // Restore editor or load new one
        if(savedInstanceState == null || (oEditor = (OverlayEditor)savedInstanceState.getSerializable(EDITOR_KEY)) == null) {
            oEditor = new OverlayEditor(this, view, false);
            oEditor.Load();
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
        SetFullscreen();
    }

    private void SetFullscreen() {
        getWindow().getDecorView().setSystemUiVisibility(ViewGroup.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | ViewGroup.SYSTEM_UI_FLAG_FULLSCREEN | ViewGroup.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }


    // oEditor.Save();
}
