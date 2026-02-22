package org.s25rttr.sdl.overlay;

import static android.widget.Toast.LENGTH_SHORT;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.Toast;
import android.widget.Toolbar;

import org.s25rttr.sdl.R;

import java.io.IOException;

public class OverlayEditor extends Overlay {

    public OverlayEditor(Activity activity, ViewGroup view, boolean hidden) {
        super(activity, view, hidden);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void AttachListeners() {
        super.AttachListeners();
        overlay.setOnTouchListener((v, event) -> {
            if(event.getAction() == MotionEvent.ACTION_DOWN) {
                OpenGeneralMenu(v, event);
            }

            // Don't consume event otherwise the Overlay::onTouchListener won't track mouse position
            return false;
        });
    }

    @Override
    @SuppressWarnings("unused")
    @SuppressLint("ClickableViewAccessibility")
    protected boolean AddButtonBehaviour(final Button button, final Config config) {
        button.setOnTouchListener(new View.OnTouchListener() {
            boolean dragged;
            float diffX, diffY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dragged = false;
                        diffX = v.getX() - event.getRawX();
                        diffY = v.getY() - event.getRawY();
                        return true;

                    case MotionEvent.ACTION_UP:
                        if(!dragged) {
                            OpenButtonMenu((Button)v, config);
                            return true;
                        }
                        break;

                    case MotionEvent.ACTION_MOVE:
                        dragged = true;
                        v.setX(event.getRawX() + diffX);
                        v.setY(event.getRawY() + diffY);
                        return true;
                }
                return false;
            }
        });

        return true;
    }

    private void OpenButtonMenu(Button button, Config config) {

    }

    private void OpenGeneralMenu(View view, MotionEvent event) {
        PopupMenu menu = OpenPopupMenu(R.menu.overlay_bg_menu, activity, (ViewGroup)view, event.getX(), event.getY());

        menu.setOnMenuItemClickListener(item -> {
            // switch not possible here due to constant requirement
            int id = item.getItemId();
            if(id == R.id.AddButton) {
                CreateButton(overlay);
            } else if(id == R.id.SaveButtons) {
                // Save button configuration to file
            } else if(id == R.id.ResetButtons) {
                // Reload from ocnfig
            }
            return true;
        });
    }

    private PopupMenu OpenPopupMenu(final int menuID, Context context, ViewGroup view, float x, float y) {
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

    // Create a single button
    private void CreateButton(final FrameLayout layout) {
        Config config = new Config();
        config.Pos = new Config.Pos();
        config.Pos.x = mousePos.x;
        config.Pos.y = mousePos.y;
        config.Text = "Button " + configs.size();
        configs.add(config);
        buttons.addAll(CreateButtons(new ConfigList(config), layout));
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
