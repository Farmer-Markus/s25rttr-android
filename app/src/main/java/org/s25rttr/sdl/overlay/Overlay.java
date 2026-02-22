package org.s25rttr.sdl.overlay;

import android.app.Activity;
import android.view.ViewGroup;
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
    protected final Activity activity;
    protected final ViewGroup view;
    protected final FrameLayout overlay;

    protected Path DEFAULT_CONFIG_PATH = new Path("overlay/buttons.bin");
    protected ConfigList configs;
    protected List<Button> buttons;

    protected Config.Pos mousePos;
    protected boolean hidden;


    public Overlay(final Activity activity, final ViewGroup view, boolean hidden) {
        this.activity = activity;
        this.view = view;

        configs = new ConfigList();
        buttons = new ArrayList<>();
        mousePos = new Config.Pos();

        overlay = new FrameLayout(activity);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        overlay.setLayoutParams(params);
        overlay.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        view.addView(overlay);
        this.hidden = hidden;

        AttachListeners();
    }

    protected void AttachListeners() {
        // Listener to keep track of mouse/touch position used for button mouseClick actions
        view.setOnTouchListener((view, event) -> {
            mousePos.x = event.getX();
            mousePos.y = event.getY();

            // Don't consume event, just listen
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

    protected List<Button> CreateButtons(final ConfigList configs, final FrameLayout layout) {
        List<Button> buttons = new ArrayList<>();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        for(Config cfg : configs) {
            Button btn = new Button(activity);
            layout.addView(btn);

            btn.setText(cfg.Text);
            btn.setX(cfg.Pos.x);
            btn.setY(cfg.Pos.y);
            btn.setLayoutParams(params);

            AddButtonBehaviour(btn, cfg);
            buttons.add(btn);
        }

        return buttons;
    }

    protected boolean AddButtonBehaviour(final Button button, final Config config) {
        switch(config.clickBehaviour) {
            case SEND_KEY:
                button.setOnClickListener(view -> Actions.SendKeyCode(config.keyCode, this.view));
                break;

            case SEND_MOUSE:
                button.setOnClickListener(view -> Actions.SendMouseEvent(config.mouseEvent.Value(), mousePos));
                break;

            case OVERLAY_TOGGLE:
                button.setOnClickListener(view -> {
                    hidden = !hidden;
                    Actions.ChangeVisibility((Button)view, buttons, hidden);
                });
                break;

            case KEYBOARD_TOGGLE:
                button.setOnClickListener(view -> Actions.OpenKeyboard(activity, this.view));
                break;

            default:
                return false;
        }

        return true;
    }

    // Save button configs to file
    protected void SaveButtonSettings(ConfigList buttons, Path file) throws IOException {
        Path storage = Filesystem.GetInternalStoragePath(activity).Append(file);

        FileOutputStream fOut = new FileOutputStream(storage.toString());
        ObjectOutputStream oOut = new ObjectOutputStream(fOut);

        oOut.writeObject(buttons);
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
        // throw new ClassNotFoundException("Read class is not an instance of Class<ButtonList>");
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

/*
Implement customizer to dynamically let user add/remove buttons.
Overwrite touch functions to let user drag buttons around to specific positions.
Store positions/text/keyboard-key inside overlay.Config somehow
 */
