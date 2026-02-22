package org.s25rttr.sdl;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
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
    private OverlayEditor oEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ViewGroup view = new FrameLayout(this);
        setContentView(view);

        oEditor = new OverlayEditor(this, view, false);
        oEditor.Load();
    }


    // oEditor.Save();
}
