package org.s25rttr.sdl.overlay;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import org.s25rttr.sdl.SDLActivity;

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

    public static void SendMouseEvent(final int button, final Config.Pos pos) {
        SDLActivity.onNativeMouse(button, 0, pos.x, pos.y, false);
        SDLActivity.onNativeMouse(0, 1, pos.x, pos.y, false);
    }

    public static void ChangeVisibility(Button button, List<Button> elements, boolean visible) {
        for(Button btn : elements) {
            if(btn == null) continue;
            btn.setVisibility(visible ? View.VISIBLE : View.GONE);
        }

        // Calling button needs to stay visible
        button.setVisibility(View.VISIBLE);
    }

    // https://stackoverflow.com/a/5617130
    public static void OpenKeyboard(Runnable runnable) {
        if(runnable != null) {
            Handler handler = new Handler();
            handler.post(runnable);
        }


        /*EditText editText = new EditText(context);
        view.addView(editText);
        editText.setHint("Enter text");
        editText.setOnEditorActionListener((v, actionID, keyEvent) -> {
            // User finished keyboard input -> Send keys to sdl
            // Handler.post(new Runnable())

            return true;
        });

        InputMethodManager manager = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        //manager.toggleSoftInputFromWindow(view.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
        manager.showSoftInput(view, InputMethodManager.SHOW_FORCED);*/


    }

    public static void OpenOverlayEditor(Context context) {
        context.startActivity(new Intent(context, OverlayEditor.class));
    }
}
