package org.s25rttr.sdl.overlay;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import org.s25rttr.sdl.data.Actions;
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
    protected final Path BUTTON_CONFIG_FILE = new Path("overlay/buttons.bin");
    protected final Activity parent;
    protected final ViewGroup parentView;

    protected Config.Pos lastEventPos = new Config.Pos();


    protected FrameLayout oLayout;
    // List of all button configs
    protected ButtonConfList configList;
    // Cache with every button
    protected List<Button> buttonList = new ArrayList<>();

    // If elements are currently hidden
    protected boolean hidden = false;


    public Overlay(final Activity parent, final ViewGroup parentView) {
        this.parent = parent;
        this.parentView = parentView;

        oLayout = new FrameLayout(parent);

        oLayout.setOnTouchListener((view, event) -> {
            lastEventPos.x = event.getX();
            lastEventPos.y = event.getY();

            // Don't consume event, just listen
            return false;
        });
    }

    public boolean LoadElements() {
        return LoadButtons();
    }

    // Load config with all corresponding buttons
    protected boolean LoadButtons() {
        try {
            configList = LoadButtonSettings();
        } catch (IOException | ClassNotFoundException e) {
            UiHelper.AlertDialog(parent, "Failed to load overlay", e.toString(), null);
            return false;
        }

        buttonList.clear();
        oLayout = new FrameLayout(parent);
        parentView.addView(oLayout);

        // Set btn layouts, styles, texts etc.
        for(Config cfg : configList) {
            Button btn = new Button(parent);
            oLayout.addView(btn);

            btn.setText(cfg.Text);

            btn.setX(cfg.Pos.x);
            btn.setY(cfg.Pos.y);

            // Set onClick callback
            SetButtonBehaviour(btn, cfg);

            buttonList.add(btn);
        }

        return buttonList.size() == configList.size();
    }

    protected void SetButtonBehaviour(Button button, Config btnConfig) {
        switch(btnConfig.clickBehaviour) {
            case SEND_KEY:
                button.setOnClickListener(view -> Actions.SendKeyCode(btnConfig.keyCode, parentView));
                break;

            case SEND_MOUSE:
                button.setOnClickListener(view -> Actions.SendMouseEvent(btnConfig.mouseEvent.Value(), lastEventPos));
                break;

            case OVERLAY_TOGGLE:
                button.setOnClickListener(view -> Actions.ChangeVisibility((Button)view, buttonList, !hidden));
                break;

            case KEYBOARD_TOGGLE:
                button.setOnClickListener(view -> Actions.OpenKeyboard(parent, parentView));
                break;
        }
    }

    public Overlay Show() {
        for(Button btn : buttonList)
            btn.setVisibility(View.VISIBLE);
        return this;
    }

    public Overlay Hide() {
        for(Button btn : buttonList)
            btn.setVisibility(View.GONE);
        return this;
    }

    // Save button configs to file
    protected void SaveButtonSettings(ButtonConfList buttons) throws IOException {
        Path storage = Filesystem.GetInternalStoragePath(parent).Append(BUTTON_CONFIG_FILE);

        FileOutputStream fOut = new FileOutputStream(storage.toString());
        ObjectOutputStream oOut = new ObjectOutputStream(fOut);

        oOut.writeObject(buttons);
    }

    // Read button configs from file
    protected ButtonConfList LoadButtonSettings() throws IOException, ClassNotFoundException {
        Path storage = Filesystem.GetInternalStoragePath(parent).Append(BUTTON_CONFIG_FILE);

        FileInputStream fIn = new FileInputStream(storage.toString());
        ObjectInputStream oIn = new ObjectInputStream(fIn);

        Object obj = oIn.readObject();
        if(obj instanceof ButtonConfList)
            return (ButtonConfList)obj;

        return new ButtonConfList();
        // throw new ClassNotFoundException("Read class is not an instance of Class<ButtonList>");
    }

    // Convert dp to pixels
    protected int DpToPx(int dp) {
        float density = parent.getResources().getDisplayMetrics().density;
        return (int)(dp * density);
    }

    // Separate class needed to use with instanceof
    // https://stackoverflow.com/questions/10108122/how-to-instanceof-listmytype
    protected static class ButtonConfList extends ArrayList<Config> {
        protected Class<Config> type;

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