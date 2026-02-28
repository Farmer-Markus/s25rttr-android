package org.s25rttr.sdl.overlay;

import android.view.KeyEvent;

import org.s25rttr.sdl.utils.UiHelper;

import java.util.ArrayList;
import java.util.List;

// Fill Spinners to choose action in button config menu
public class ActionFillers {
    public static List<UiHelper.SpinnerItem> GetEventBehaviourItems() {
        final List<UiHelper.SpinnerItem> items = new ArrayList<>();

        items.add(new UiHelper.SpinnerItem(Config.ClickBehaviour.SEND_KEY, "Key"));
        items.add(new UiHelper.SpinnerItem(Config.ClickBehaviour.SEND_MOUSE, "Mouse"));
        items.add(new UiHelper.SpinnerItem(Config.ClickBehaviour.OVERLAY, "Visibility"));
        items.add(new UiHelper.SpinnerItem(Config.ClickBehaviour.KEYBOARD_TOGGLE, "Keyboard"));
        return items;
    }

    public static List<UiHelper.SpinnerItem> GetKeyItems() {
        final List<UiHelper.SpinnerItem> items = new ArrayList<>();

        for(char c = 'A'; c < 'Z'; c++) {
            int code = KeyEvent.keyCodeFromString("KEYCODE_" + c);
            items.add(new UiHelper.SpinnerItem(code, String.valueOf(c)));
        }

        for(int i = 0; i < 9; i++) {
            int code = KeyEvent.keyCodeFromString("KEYCODE_" + i);
            items.add(new UiHelper.SpinnerItem(code, String.valueOf(i)));
        }

        // Special
        items.add(new UiHelper.SpinnerItem(KeyEvent.KEYCODE_SPACE, "SPACE"));
        items.add(new UiHelper.SpinnerItem(KeyEvent.KEYCODE_ENTER, "ENTER"));
        items.add(new UiHelper.SpinnerItem(KeyEvent.KEYCODE_ESCAPE, "ESCAPE"));
        items.add(new UiHelper.SpinnerItem(KeyEvent.KEYCODE_TAB, "TAB"));
        items.add(new UiHelper.SpinnerItem(KeyEvent.KEYCODE_ALT_LEFT, "ALT"));
        items.add(new UiHelper.SpinnerItem(KeyEvent.KEYCODE_CTRL_LEFT, "CTRL"));
        return items;
    }

    public static List<UiHelper.SpinnerItem> GetOverlayItems() {
        final List<UiHelper.SpinnerItem> items = new ArrayList<>();

        items.add(new UiHelper.SpinnerItem(Config.OverlayEvent.EDIT, "EDIT"));
        items.add(new UiHelper.SpinnerItem(Config.OverlayEvent.TOGGLE, "TOGGLE"));
        return items;
    }

    public static List<UiHelper.SpinnerItem> GetMouseItems() {
        final List<UiHelper.SpinnerItem> items = new ArrayList<>();

        items.add(new UiHelper.SpinnerItem(1, "LEFT"));
        items.add(new UiHelper.SpinnerItem(2, "RIGHT"));
        items.add(new UiHelper.SpinnerItem(3, "MIDDLE"));
        return items;
    }
}
