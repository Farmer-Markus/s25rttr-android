package org.s25rttr.sdl.overlay;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;

import org.s25rttr.sdl.data.Filesystem;
import org.s25rttr.sdl.data.Path;
import org.s25rttr.sdl.utils.UiHelper;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class Overlay {
    protected Path DEFAULT_CONFIG_PATH = new Path("overlay/buttons.bin");

    // SDLActivity variables
    protected Activity activity;
    protected ViewGroup view;
    protected final SurfaceView surface;

    protected final OverlayLayout overlay;
    protected ConfigList configs;
    protected List<Button> buttons;
    protected KeyboardView keyboardView;

    protected boolean hidden;


    public Overlay(final Activity activity, final ViewGroup view, SurfaceView surface, boolean hidden) {
        this.activity = activity;
        this.view = view;
        this.surface = surface;

        configs = new ConfigList();
        buttons = new ArrayList<>();

        overlay = new OverlayLayout(activity, surface);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        overlay.setLayoutParams(params);
        overlay.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        overlay.setFitsSystemWindows(false);
        view.addView(overlay);
        this.hidden = hidden;

        AttachListeners();
    }

    @SuppressLint("ClickableViewAccessibility")
    protected void AttachListeners() {
        overlay.setOnTouchListener((view, event) -> {
            // Hide keyboard if shown & clicked outside
            if(keyboardView != null && keyboardView.hasFocus())
                Actions.ToggleKeyboard(keyboardView, activity);

            return false;
        });
    }

    /**
     * Load config and buttons (Will overwrite all previous loaded buttons/configs)
     * @return <code>true</code> if config was loaded successfully,
     * <code>false</code> otherwise
     */
    public boolean Load() {
        boolean ret = true;
        configs.clear();
        buttons.clear();

        try {
            configs = LoadButtonSettings(DEFAULT_CONFIG_PATH);
        } catch (IOException | ClassNotFoundException e) {
            UiHelper.AlertDialog(activity, "Overlay error", e.toString(), null);
            ret = false;
        }

        buttons.addAll(CreateButtons(configs, overlay));
        return ret;
    }

    protected List<Button> CreateButtons(final ConfigList configs, final OverlayLayout layout) {
        return CreateButtons(configs, layout, 0);
    }

    protected List<Button> CreateButtons(final ConfigList configs, final OverlayLayout layout, final int start) {
        List<Button> buttons = new ArrayList<>();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        for(int i = start; i < configs.size(); i++) {
            Config cfg;
            if((cfg = configs.get(i)) == null)
                continue;

            Button btn = new Button(activity);
            layout.addView(btn);

            btn.getBackground().setAlpha(cfg.opacity);
            btn.setTextColor(btn.getTextColors().withAlpha(cfg.textOpacity));
            btn.setText(cfg.text);

            btn.setX(cfg.pos.x);
            btn.setY(cfg.pos.y);
            btn.setLayoutParams(params);

            AddButtonBehaviour(btn, i);
            buttons.add(btn);
        }

        return buttons;
    }

    protected boolean AddButtonBehaviour(final Button button, final int configID) {
        final Config config = configs.get(configID);
        switch(config.clickBehaviour.behaviour) {
            case Config.ClickBehaviour.SEND_KEY:
                button.setOnClickListener(view -> Actions.SendKeyCode(config.keyCode, this.surface));
                break;

            case Config.ClickBehaviour.SEND_MOUSE:
                    button.setOnClickListener(view -> Actions.SendMouseEvent(config.mouseEvent.event, overlay.GetMousePos()));
                break;

            case Config.ClickBehaviour.OVERLAY:
                if(config.overlayEvent.event == Config.OverlayEvent.TOGGLE) {
                    button.setOnClickListener(view -> {
                        hidden = !hidden;
                        Actions.ChangeVisibility((Button)view, buttons, hidden);
                    });
                } else if(config.overlayEvent.event == Config.OverlayEvent.EDIT)
                    button.setOnClickListener(view -> Actions.OpenOverlayEditor(activity));
                break;

            case Config.ClickBehaviour.KEYBOARD_TOGGLE:
                // We have a keyboard button
                if(keyboardView == null) {
                    keyboardView = new KeyboardView(surface);
                    overlay.addView(keyboardView);
                }

                button.setOnClickListener(view -> Actions.ToggleKeyboard(keyboardView, activity));
                break;

            default:
                return false;
        }

        return true;
    }

    // Save button configs to file
    protected void SaveButtonSettings(final ConfigList buttons, final Path file) throws IOException {
        Path storage = Filesystem.GetInternalStoragePath(activity).Append(file);
        final ConfigList finalButtons = new ConfigList();
        // Sort out null configs
        for(Config cfg : buttons)
            if(cfg != null) finalButtons.add(cfg);

        Path parent = storage.GetParent();
        if(!parent.Exists())
            parent.Mkdirs();

        if(!storage.Exists())
            storage.CreateNewFile();

        FileOutputStream fOut = new FileOutputStream(storage.toString());
        ObjectOutputStream oOut = new ObjectOutputStream(fOut);

        oOut.writeObject(finalButtons);
    }

    // Read button configs from file
    protected ConfigList LoadButtonSettings(Path file) throws IOException, ClassNotFoundException {
        Path storage = Filesystem.GetInternalStoragePath(activity).Append(file);

        FileInputStream fIn = new FileInputStream(storage.toString());
        ObjectInputStream oIn = new ObjectInputStream(fIn);

        Object obj = oIn.readObject();
        if(obj instanceof ConfigList)
            return (ConfigList)obj;

        return new ConfigList();
    }

    // Separate class needed to use with instanceof
    // https://stackoverflow.com/questions/10108122/how-to-instanceof-listmytype
    protected static class ConfigList extends ArrayList<Config> {
        protected Class<Config> type;

        public ConfigList() {}
        public ConfigList(Config config) {
            add(config);
        }

        public Class<Config> Type() {
            return type;
        }
    }
}
