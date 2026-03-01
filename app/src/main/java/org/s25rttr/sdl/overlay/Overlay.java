package org.s25rttr.sdl.overlay;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.SurfaceView;
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
    protected Path DEFAULT_CONFIG_PATH = new Path("overlay/buttons.bin");

    // SDLActivity variables
    protected Activity activity;
    protected ViewGroup view;
    //protected final SurfaceView surface;

    protected final OverlayLayout overlay;
    protected ConfigList configs;
    protected List<Button> buttons;
    protected SoftKeyBoardInterface softKeyBoard;

    protected boolean hidden;


    public Overlay(final Activity activity, final ViewGroup view, SurfaceView surface, boolean hidden) {
        this.activity = activity;
        this.view = view;

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
        // Listener to keep track of mouse/touch position used for button mouseClick actions
        /*overlay.setOnTouchListener((view, event) -> {
            // Pass event down to sdl
            MotionEvent evCopy = MotionEvent.obtain(event);
            view.onTouchEvent(evCopy);
            evCopy.recycle();

            // Consume event so we get down, move and up events
            return true;
        });*/
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

    public void SetSoftKeyboardInterface(final SoftKeyBoardInterface softKeyBoardInterface) {
        this.softKeyBoard = softKeyBoardInterface;
    }

    /*public void SetMousePosInterface(final MousePosInterface mousePosInterface) {
        this.mousePosInterface = mousePosInterface;
    }*/

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
                button.setOnClickListener(view -> Actions.SendKeyCode(config.keyCode, this.view));
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
                if(softKeyBoard != null) // I don't really know what these vars are doing
                    button.setOnClickListener(view -> softKeyBoard.ShowTextInput(0, 0, 500, 500));
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
        // throw new ClassNotFoundException("Read class is not an instance of Class<ButtonList>");
    }


    @FunctionalInterface
    public interface SoftKeyBoardInterface {
        boolean ShowTextInput(int x, int y, int w, int h);
    }

    /*@FunctionalInterface
    public interface MousePosInterface {
        Config.Pos GetMousePos();
    }*/

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
