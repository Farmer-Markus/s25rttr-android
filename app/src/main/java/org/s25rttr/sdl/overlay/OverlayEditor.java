package org.s25rttr.sdl.overlay;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.Editable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;

import org.s25rttr.sdl.R;
import org.s25rttr.sdl.data.Settings;
import org.s25rttr.sdl.utils.UiHelper;

import java.io.IOException;
import java.io.Serializable;

public class OverlayEditor extends Overlay implements Serializable {
    private static final float CLICK_DISTANCE = 5.0f;
    private final TextView buttonInfoView;

    public OverlayEditor(Activity activity, ViewGroup view, SurfaceView surface, boolean hidden, Settings settings) {
        super(activity, view, surface, hidden, settings);

        activity.setRequestedOrientation(settings.Orientation);

        buttonInfoView = new TextView(activity);
        buttonInfoView.setText(activity.getString(R.string.overlay_confg_addbutton_info) + "\n" +
                activity.getString(R.string.overlay_config_orientation_info));
        buttonInfoView.setGravity(Gravity.CENTER);
        overlay.addView(buttonInfoView);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void AttachListeners() {
        overlay.setOnTouchListener(new View.OnTouchListener() {
            final Config.Pos startPos = new Config.Pos();

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startPos.x = event.getRawX();
                        startPos.y = event.getRawY();
                        break;

                    case MotionEvent.ACTION_UP:
                        if(startPos.InRange(new Config.Pos(event.getRawX(), event.getRawY()), CLICK_DISTANCE))
                            OpenGeneralMenu(v, event);
                        break;
                }

                // Don't consume event
                return false;
            }
        });
    }

    @Override
    @SuppressWarnings("unused")
    @SuppressLint("ClickableViewAccessibility")
    protected boolean AddButtonBehaviour(final Button button, final int configID) {
        button.setOnTouchListener(new View.OnTouchListener() {
            final Config.Pos startPos = new Config.Pos();
            float diffX, diffY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startPos.x = event.getRawX();
                        startPos.y = event.getRawY();
                        diffX = v.getX() - startPos.x;
                        diffY = v.getY() - startPos.y;
                        v.bringToFront();
                        return true;

                    case MotionEvent.ACTION_UP:
                        if(startPos.InRange(new Config.Pos(event.getRawX(), event.getRawY()), CLICK_DISTANCE)) {
                            OpenButtonMenu((Button)v, configID);
                            return true;
                        } else {
                            ButtonSavePos((Button)v, configs.get(configID));
                        }
                        break;

                    case MotionEvent.ACTION_MOVE:
                        v.setX(event.getRawX() + diffX);
                        v.setY(event.getRawY() + diffY);
                        return true;
                }
                return false;
            }
        });

        return true;
    }

    private void ButtonSavePos(final Button button, final Config config) {
        config.pos.x = button.getX();
        config.pos.y = button.getY();

        /*DisplayMetrics metrics = GetDisplayMetrics();

        // Store ralative coordinates
        Config.Pos pos = config.pos;
        pos.x = button.getX() / metrics.widthPixels;
        pos.y = button.getY() / metrics.heightPixels;

        pos.gravity.clear();

        if(pos.x > 0.5f) {
            pos.gravity.add(Config.Pos.Gravity.RIGHT);
            pos.x = 1.0f - pos.x;
        } else
            pos.gravity.add(Config.Pos.Gravity.LEFT);

        if(pos.y > 0.5f) {
            pos.gravity.add(Config.Pos.Gravity.BOTTOM);
            pos.y = 1.0f - pos.y;
        } else
            pos.gravity.add(Config.Pos.Gravity.TOP);

        Config.Pos pos = config.pos;
        pos.x = button.getX() / overlay.getWidth();
        pos.y = button.getY() / overlay.getHeight();*/
    }

    // Reload stuff
    public void Restore(final Activity activity, final ViewGroup view) {
        this.activity = activity;
        this.view = view;

        activity.setRequestedOrientation(settings.Orientation);

        ViewGroup vgParent = (ViewGroup)overlay.getParent();
        if(vgParent != null)
            vgParent.removeView(overlay);
        view.addView(overlay);
        AttachListeners();

        overlay.removeAllViews();

        // Redraw info text
        OverlayLayout olParent = (OverlayLayout)buttonInfoView.getParent();
        if(olParent != null)
            olParent.removeView(buttonInfoView);
        overlay.addView(buttonInfoView);

        // Redraw buttons to meet possible new display size(rotated)
        buttons.clear();
        buttons.addAll(CreateButtons(configs, overlay));
    }

    private void OpenButtonMenu(final Button button, final int configID) {
        final Config config = configs.get(configID);

        if(config.size.w < 1)
            config.size.w = button.getWidth();
        if(config.size.h < 1)
            config.size.h = button.getHeight();

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(config.text);

        View view = activity.getLayoutInflater().inflate(R.layout.overlay_button_config, null);
        builder.setView(view);
        builder.setTitle(config.text);

        builder.setPositiveButton(activity.getString(R.string.dialog_ok), (dialog, btn) -> {
            dialog.dismiss();
        });

        builder.setNeutralButton(activity.getString(R.string.overlay_dialog_remove), (dialog, btn) -> {
            // Just set to null and clean up later
            configs.set(configID, null);
            buttons.set(configID, null);
            ((ViewGroup)button.getParent()).removeView(button);
            dialog.dismiss();
        });

        final AlertDialog dialog = builder.show();

        EditText editText = view.findViewById(R.id.ButtonNameEdit);
        editText.setText(config.text);
        editText.addTextChangedListener(new UiHelper.SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                config.text = editable.toString();
                dialog.setTitle(config.text);
                button.setText(config.text);
            }
        });

        // Action spinner
        Spinner spinner = view.findViewById(R.id.ActionSpinner);
        ArrayAdapter<UiHelper.SpinnerItem> adapter = new ArrayAdapter<>(
                activity,
                android.R.layout.simple_spinner_dropdown_item,
                ActionFillers.GetEventBehaviourItems(activity)
        );
        spinner.setAdapter(adapter);
        UiHelper.SpinnerItem.SelectItemById(spinner, config.clickBehaviour.behaviour);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                SetSubActions((int)id, view.findViewById(R.id.SubActionSpinner), config);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                UiHelper.SpinnerItem.SelectItemById((Spinner)parent, Config.ClickBehaviour.SEND_KEY);
            }
        });

        // SubActionSpinner's values are set dynamically during item selection of ActionSpinner above

        editText = view.findViewById(R.id.OpacityEdit);
        editText.setText(String.valueOf(config.opacity));
        editText.addTextChangedListener(new UiHelper.SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                config.opacity = EditToOpacity(editable, config.opacity);
                button.getBackground().setAlpha(config.opacity);
            }
        });

        editText = view.findViewById(R.id.TextOpacityEdit);
        editText.setText(String.valueOf(config.textOpacity));
        editText.addTextChangedListener(new UiHelper.SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                config.textOpacity = EditToOpacity(editable, config.textOpacity);
                button.setTextColor(button.getTextColors().withAlpha(config.textOpacity));
            }
        });

        editText = view.findViewById(R.id.WidthEdit);
        editText.setText(String.valueOf(config.size.w));
        editText.addTextChangedListener(new UiHelper.SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                String st = editable.toString();
                if(!st.isEmpty()) {
                    config.size = EditToSize(editable, -1, config.size.h);
                    button.setWidth(config.size.w);
                }
            }
        });

        editText = view.findViewById(R.id.HeightEdit);
        editText.setText(String.valueOf(config.size.h));
        editText.addTextChangedListener(new UiHelper.SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                String st = editable.toString();
                if(!st.isEmpty()) {
                    config.size = EditToSize(editable, config.size.w, -1);
                    button.setHeight(config.size.h);
                }
            }
        });

        CheckBox checkBox = view.findViewById(R.id.IgnoreHideCheckbox);
        checkBox.setChecked(config.ignoreHide);
        checkBox.setOnClickListener(v -> {
            config.ignoreHide = ((CheckBox)v).isChecked();
        });
    }

    private void SetSubActions(final int action, final Spinner spinner, final Config config) {
        config.clickBehaviour.behaviour = action;

        switch(action) {
            case Config.ClickBehaviour.SEND_KEY: {
                // Get all keycodes
                ArrayAdapter<UiHelper.SpinnerItem> adapter = new ArrayAdapter<>(
                        activity,
                        android.R.layout.simple_spinner_dropdown_item,
                        ActionFillers.GetKeyItems(activity)
                );

                spinner.setAdapter(adapter);
                UiHelper.SpinnerItem.SelectItemById(spinner, config.keyCode);
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        UiHelper.SpinnerItem item = (UiHelper.SpinnerItem) parent.getSelectedItem();
                        config.keyCode = item.id;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        parent.setSelection(0);
                    }
                });
                break;
            }

            case Config.ClickBehaviour.SEND_MOUSE: {
                ArrayAdapter<UiHelper.SpinnerItem> adapter = new ArrayAdapter<>(
                        activity,
                        android.R.layout.simple_spinner_dropdown_item,
                        ActionFillers.GetMouseItems(activity)
                );
                spinner.setAdapter(adapter);
                UiHelper.SpinnerItem.SelectItemById(spinner, config.mouseEvent.event);
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        UiHelper.SpinnerItem item = (UiHelper.SpinnerItem)parent.getSelectedItem();
                        config.mouseEvent.event = item.id;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        parent.setSelection(0);
                    }
                });
                break;
            }

            case Config.ClickBehaviour.OVERLAY: {
                ArrayAdapter<UiHelper.SpinnerItem> adapter = new ArrayAdapter<>(
                        activity,
                        android.R.layout.simple_spinner_dropdown_item,
                        ActionFillers.GetOverlayItems(activity)
                );
                spinner.setAdapter(adapter);
                UiHelper.SpinnerItem.SelectItemById(spinner, config.overlayEvent.event);
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        UiHelper.SpinnerItem item = (UiHelper.SpinnerItem)parent.getSelectedItem();
                        config.overlayEvent.event = item.id;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        parent.setSelection(0);
                    }
                });
                break;
            }

            case Config.ClickBehaviour.KEYBOARD_TOGGLE:
                // There is just open/close keyboard. no further settings
                spinner.setAdapter(null);
                break;
        }
    }

    private Config.Size EditToSize(final Editable editable, final int w, final int h) {
        String str = editable.toString();
        if(str.isEmpty())
            return new Config.Size(w, h);

        Config.Size newSize = new Config.Size(w, h);
        if(w == -1) {
            try {
                newSize.w = Integer.parseUnsignedInt(str);
            } catch (NumberFormatException ignore) {
                newSize.w = 10;
            }
        } else if(h == -1) {
            try {
                newSize.h = Integer.parseUnsignedInt(str);
            } catch (NumberFormatException ignore) {
                newSize.h = 10;
            }
        }

        return newSize;
    }

    private int EditToOpacity(final Editable editable, final int opacity) {
        if(editable.toString().isEmpty())
            return opacity;

        int newOpacity;
        try {
            newOpacity = Integer.parseUnsignedInt(editable.toString());
        } catch (NumberFormatException e) {
            return 255;
        }

        if(newOpacity > 255)
            return 255;
        return Math.max(newOpacity, 0);
    }

    private void OpenGeneralMenu(final View view, final MotionEvent event) {
        PopupMenu menu = OpenPopupMenu(R.menu.overlay_bg_menu, activity, (ViewGroup)view, event.getX(), event.getY());

        menu.setOnMenuItemClickListener(item -> {
            // switch not possible here due to constant requirement
            int id = item.getItemId();
            if(id == R.id.AddButton) {
                CreateButton(overlay);
            } else if(id == R.id.SaveButtons) {
                // Save button configuration to file
                try {
                    SaveButtonSettings(configs, GetSaveFileFromRotation());
                } catch (IOException e) {
                    UiHelper.FatalError(activity, e.toString());
                }
            } else if(id == R.id.ResetButtons) {
                // Restart activity
                Intent intent = activity.getIntent();
                activity.finish();
                activity.startActivity(intent);
            } else if(id == R.id.ClearButtons) {
                configs.clear();
                buttons.clear();
                Restore(activity, this.view);
            } else if(id == R.id.BackButton) {
                activity.finish();
            }
            return true;
        });
    }

    private PopupMenu OpenPopupMenu(final int menuID, final Context context, final ViewGroup view, final float x, final float y) {
        View tempView = new View(context);
        tempView.setBackgroundColor(Color.TRANSPARENT);
        tempView.setX(x);
        tempView.setY(y);
        tempView.setLayoutParams(new ViewGroup.LayoutParams(1, 1));
        view.addView(tempView);

        PopupMenu menu = new PopupMenu(context, tempView);
        menu.getMenuInflater().inflate(menuID, menu.getMenu());
        menu.show();

        return menu;
    }

    // Create a single button at mouse position
    private void CreateButton(final OverlayLayout layout) {
        Config config = new Config();
        Config.Pos mousePos = layout.GetMousePos();
        config.pos.x = mousePos.x;
        config.pos.y = mousePos.y;
        config.size = new Config.Size();
        int start = configs.size();
        config.text = "Button " + start;

        configs.add(config);
        buttons.addAll(CreateButtons(configs, layout, start));
        // Get default size the button was created with
        config.size.w = buttons.get(start).getWidth();
        config.size.h = buttons.get(start).getHeight();
    }

    // Convert dp to pixels
    private int DpToPx(int dp) {
        float density = activity.getResources().getDisplayMetrics().density;
        return (int)(dp * density);
    }
}
