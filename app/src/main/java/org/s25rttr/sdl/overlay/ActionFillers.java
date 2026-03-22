package org.s25rttr.sdl.overlay;

import android.content.Context;
import android.view.KeyEvent;

import org.s25rttr.sdl.R;
import org.s25rttr.sdl.utils.UiHelper;

import java.util.ArrayList;
import java.util.List;

// Fill Spinners to choose action in button config menu
public class ActionFillers {
    public static List<UiHelper.SpinnerItem> GetEventBehaviourItems(Context context) {
        final List<UiHelper.SpinnerItem> items = new ArrayList<>();

        items.add(new UiHelper.SpinnerItem(Config.ClickBehaviour.SEND_KEY, context.getString(R.string.overlay_config_action_keypress)));
        items.add(new UiHelper.SpinnerItem(Config.ClickBehaviour.SEND_MOUSE, context.getString(R.string.overlay_config_action_mouse)));
        items.add(new UiHelper.SpinnerItem(Config.ClickBehaviour.OVERLAY, context.getString(R.string.overlay_config_action_overlay)));
        items.add(new UiHelper.SpinnerItem(Config.ClickBehaviour.KEYBOARD_TOGGLE, context.getString(R.string.overlay_config_action_keyboard)));
        return items;
    }

    public static List<UiHelper.SpinnerItem> GetKeyItems(Context context) {
        final List<UiHelper.SpinnerItem> items = new ArrayList<>();

        for(char c = 'A'; c <= 'Z'; c++) {
            int code = KeyEvent.keyCodeFromString("KEYCODE_" + c);
            items.add(new UiHelper.SpinnerItem(code, String.valueOf(c)));
        }

        for(int i = 0; i <= 9; i++) {
            int code = KeyEvent.keyCodeFromString("KEYCODE_" + i);
            items.add(new UiHelper.SpinnerItem(code, String.valueOf(i)));
        }

        // Special
        items.add(new UiHelper.SpinnerItem(KeyEvent.KEYCODE_SPACE, context.getString(R.string.overlay_config_keyaction_space)));
        items.add(new UiHelper.SpinnerItem(KeyEvent.KEYCODE_ENTER, context.getString(R.string.overlay_config_keyaction_enter)));
        items.add(new UiHelper.SpinnerItem(KeyEvent.KEYCODE_ESCAPE, context.getString(R.string.overlay_config_keyaction_escape)));
        items.add(new UiHelper.SpinnerItem(KeyEvent.KEYCODE_TAB, context.getString(R.string.overlay_config_keyaction_tab)));
        items.add(new UiHelper.SpinnerItem(KeyEvent.KEYCODE_ALT_LEFT, context.getString(R.string.overlay_config_keyaction_alt)));
        items.add(new UiHelper.SpinnerItem(KeyEvent.KEYCODE_CTRL_LEFT, context.getString(R.string.overlay_config_keyaction_ctrl)));
        return items;
    }

    public static List<UiHelper.SpinnerItem> GetOverlayItems(Context context) {
        final List<UiHelper.SpinnerItem> items = new ArrayList<>();

        items.add(new UiHelper.SpinnerItem(Config.OverlayEvent.EDIT, context.getString(R.string.overlay_config_overlayaction_edit)));
        items.add(new UiHelper.SpinnerItem(Config.OverlayEvent.TOGGLE, context.getString(R.string.overlay_config_overlayaction_toggle)));
        return items;
    }

    public static List<UiHelper.SpinnerItem> GetMouseItems(Context context) {
        final List<UiHelper.SpinnerItem> items = new ArrayList<>();

        items.add(new UiHelper.SpinnerItem(1, context.getString(R.string.overlay_config_mouseaction_left)));
        items.add(new UiHelper.SpinnerItem(2, context.getString(R.string.overlay_config_mouseaction_right)));
        items.add(new UiHelper.SpinnerItem(3, context.getString(R.string.overlay_config_mouseaction_middle)));
        return items;
    }
}
