package org.s25rttr.sdl.overlay;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import org.s25rttr.sdl.OverlayConfigActivity;
import org.s25rttr.sdl.SDLActivity;

import java.util.List;

public class Actions {
    public static void SendKeyCode(int keyCode, SurfaceView surface) {
        // First down, then up
        for(int action = 0; action < 2; action++) {
            KeyEvent event = new KeyEvent(
                    System.currentTimeMillis(),
                    System.currentTimeMillis(),
                    action,
                    keyCode,
                    0
            );

            surface.dispatchKeyEvent(event);
        }
    }

    public static void SendMouseEvent(final int button, final Config.Pos pos) {
        SDLActivity.onNativeMouse(button, 0, pos.x, pos.y, false);
        SDLActivity.onNativeMouse(0, 1, pos.x, pos.y, false);
    }

    public static void ChangeVisibility(Button button, List<Button> buttons, Overlay.ConfigList configs, boolean visible) {
        for(int i = 0; i < buttons.size(); i++) {
            Button btn = buttons.get(i);
            Config cfg = configs.get(i);

            if(btn != null) {
                // Calling button needs to stay visible
                if(cfg == null || cfg.ignoreHide || button == btn)
                    continue;
                btn.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        }
    }

    public static void ToggleKeyboard(KeyboardView view, Context context) {
        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);

        if(view.hasFocus()) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            view.clearFocus();
        } else if(view.requestFocus())
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    public static void OpenOverlayEditor(Activity activity, int code) {
        activity.startActivityForResult(new Intent(activity, OverlayConfigActivity.class), code);
    }
}
