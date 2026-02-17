package org.s25rttr.sdl.data;

import android.app.Activity;
import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import org.s25rttr.sdl.SDLActivity;
import org.s25rttr.sdl.overlay.Config;

import java.util.List;

public class Actions {
    public static void SendKeyCode(int keyCode, ViewGroup view) {
        // First down, then up
        for(int action = 0; action < 1; action++) {
            KeyEvent event = new KeyEvent(
                    System.currentTimeMillis(),
                    System.currentTimeMillis(),
                    action,
                    keyCode,
                    0
            );

            view.dispatchKeyEvent(event);
        }
    }

    public static void SendMouseEvent(int button, Config.Pos pos) {
        // Down, then up
        for(int action = 0; action < 1; action++)
            SDLActivity.onNativeMouse(button, action, 0, 0, false);
    }

    public static void ChangeVisibility(Button button, List<Button> elements, boolean visible) {
        for(Button btn : elements)
            btn.setVisibility(visible ? View.VISIBLE : View.GONE);

        // Calling button needs to stay visible
        button.setVisibility(View.VISIBLE);
    }

    // https://stackoverflow.com/a/5617130
    public static void OpenKeyboard(Context context, ViewGroup view) {
        InputMethodManager manager = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        manager.toggleSoftInputFromWindow(view.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
    }
}
