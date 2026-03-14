package org.s25rttr.sdl.overlay;

import android.text.InputType;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

public class KeyboardView extends View {
    private final SurfaceView surface; // The sdl surface

    public KeyboardView(SurfaceView surface) {
        super(surface.getContext());
        this.surface = surface;
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        outAttrs.imeOptions = EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI;

        KeyboardView kbv = this;
        return new BaseInputConnection(this, false) {
            // Send final enter and close keyboard
            @Override
            public boolean performEditorAction(int actionCode) {
                if(actionCode == EditorInfo.IME_ACTION_DONE) {
                    sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                    sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
                    Actions.ToggleKeyboard(kbv, surface.getContext());
                    return true;
                }
                return super.performEditorAction(actionCode);
            }

            // Triggers when typing text
            @Override
            public boolean commitText(CharSequence text, int newCurPosition) {
                // Hope this doesn't fail for some reason
                KeyCharacterMap map = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
                KeyEvent[] events = map.getEvents(text.toString().toCharArray());
                if(events != null) {
                    for(KeyEvent ev : events)
                        sendKeyEvent(ev);
                }
                return true;
            }

            // Triggers on backspace
            @Override
            public boolean deleteSurroundingText(int beforeLength, int afterLength) {
                sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
                return true;
            }

            @Override
            public boolean sendKeyEvent(KeyEvent ev) {
                surface.dispatchKeyEvent(ev);
                return true;
            }
        };
    }
}
