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
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;

import org.s25rttr.sdl.R;
import org.s25rttr.sdl.utils.UiHelper;

import java.io.IOException;
import java.io.Serializable;

public class OverlayEditor extends Overlay implements Serializable {
    private static final float CLICK_DISTANCE = 5.0f;

    public OverlayEditor(Activity activity, ViewGroup view, SurfaceView surface, boolean hidden) {
        super(activity, view, surface, hidden);
        TextView textView = new TextView(activity);
        textView.setText("Tab anywhere to create button\nPortrait");
        textView.setGravity(Gravity.CENTER);

        overlay.addView(textView);
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

    public void Restore(final Activity activity, final ViewGroup view) {
        this.activity = activity;
        this.view = view;
        ((ViewGroup)overlay.getParent()).removeView(overlay);
        view.addView(overlay);
        AttachListeners();

        // Redraw buttons to meet possible new display size(rotated)
        overlay.removeAllViews();
        buttons.clear();
        buttons.addAll(CreateButtons(configs, overlay));
    }

    private void OpenButtonMenu(final Button button, final int configID) {
        final Config config = configs.get(configID);
        if(config == null)
            return;
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(config.text);

        View view = activity.getLayoutInflater().inflate(R.layout.overlay_button_config, null);
        builder.setView(view);
        builder.setTitle(config.text);
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
                ActionFillers.GetEventBehaviourItems()
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
                config.opacity = ToOpacity(editable, config.opacity);
                button.getBackground().setAlpha(config.opacity);
            }
        });

        editText = view.findViewById(R.id.TextOpacityEdit);
        editText.setText(String.valueOf(config.textOpacity));
        editText.addTextChangedListener(new UiHelper.SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                config.textOpacity = ToOpacity(editable, config.textOpacity);
                button.setTextColor(button.getTextColors().withAlpha(config.textOpacity));
            }
        });

        Button btn = view.findViewById(R.id.CloseButton);
        btn.setOnClickListener(v -> {
            dialog.dismiss();
        });

        btn = view.findViewById(R.id.DeleteButton);
        btn.setOnClickListener(v -> {
            // Just set to null and clean up later
            configs.set(configID, null);
            buttons.set(configID, null);
            ((ViewGroup)button.getParent()).removeView(button);
            dialog.dismiss();
            // overlay.removeAllViews();
            // Reload buttons
            // CreateButtons(configs, overlay);
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
                        ActionFillers.GetKeyItems()
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
                        ActionFillers.GetMouseItems()
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
                        ActionFillers.GetOverlayItems()
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

    private int ToOpacity(final Editable editable, final int opacity) {
        if(editable.toString().isEmpty())
            return opacity;

        int newOpacity;
        try {
            newOpacity = Integer.parseInt(editable.toString());
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
                    SaveButtonSettings(configs, DEFAULT_CONFIG_PATH);
                } catch (IOException e) {
                    UiHelper.FatalError(activity, e.toString());
                }
            } else if(id == R.id.ResetButtons) {
                // Restart activity
                Intent intent = activity.getIntent();
                activity.finish();
                activity.startActivity(intent);
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
        config.text = "Button " + configs.size();
        int start = configs.size();
        configs.add(config);
        buttons.addAll(CreateButtons(configs, layout, start));
    }

    // Convert dp to pixels
    private int DpToPx(int dp) {
        float density = activity.getResources().getDisplayMetrics().density;
        return (int)(dp * density);
    }

    public void Save() throws IOException {
        SaveButtonSettings(configs, DEFAULT_CONFIG_PATH);
    }
}
