package org.s25rttr.sdl;

import static android.widget.Toast.LENGTH_SHORT;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;

import org.s25rttr.sdl.data.Filesystem;
import org.s25rttr.sdl.data.Path;
import org.s25rttr.sdl.utils.Permissions;
import org.s25rttr.sdl.data.Settings;
import org.s25rttr.sdl.utils.RttrHelper;
import org.s25rttr.sdl.utils.UiHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/*
  Config activity.
  Allows user to set various settings, open logs
  pick folders
  TODO: Sometimes rttr's fps are going down to 4 and suddenly up again. Try debugging rttr (breakpoint in videodriver)
 */
public class GameConfigActivity extends Activity {
    // Activity result codes
    private static final int RTTR_DIR_PICKER_CODE = 0;
    private static final int GAME_DIR_PICKER_CODE = 1;
    private static final int PERMISSION_CODE = 2;
    private static final int UPDATER_CODE = 3;
    private static final int OVERLAY_CODE = 4;

    private Settings settings;


    public GameConfigActivity() {
        settings = new Settings();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    ()->{
                        if(!HandleBackPressed())
                            super.getOnBackInvokedDispatcher();
                    }
            );
        }

        settings.Load(this);
        InitUi();
        EnsurePermission();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Store current unsaved settings until going back to this activity
        outState.putSerializable("config_settings", settings);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        settings = (Settings)savedInstanceState.getSerializable("config_settings");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if(resultCode == RESULT_OK) {
            switch(requestCode) {
                case RTTR_DIR_PICKER_CODE:
                    HandleRttrDirPickerResult(resultData);
                    break;

                case GAME_DIR_PICKER_CODE:
                    HandleGameDirPickerResult(resultData);
                    break;

                case PERMISSION_CODE:
                    // Do... Nothing? No I don't think so :-/
                    break;

                case OVERLAY_CODE:
                    // Also not needed
                    break;

                case UPDATER_CODE:
                    // Setting could have been disabled
                    if(settings.EnableUpdater) {
                        settings.EnableUpdater = new Settings().Load(this).EnableUpdater;
                        ReloadUi();
                    }
                    break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if(!HandleBackPressed())
            super.onBackPressed();
    }

    private boolean HandleBackPressed() {
        if(SettingsChanged()) {
            // Open dialog "do you really want to discard changes" etc.
            UiHelper.QuestionDialog(
                    this,
                    getString(R.string.config_dialog_discard_changes_title),
                    getString(R.string.config_dialog_discard_changes_message),
                    ()->{
                        ExitDialog(this::finish);
                    },
                    null
            );
        } else
            ExitDialog(this::finish);

        return true;
    }

    private void HandleRttrDirPickerResult(Intent resultData) {
        if(resultData != null && EnsurePermission()) {
            Uri uri = resultData.getData();
            settings.RttrDirectory = Filesystem.UriToRealPath(uri);
            if(!Filesystem.IsPathWritable(settings.RttrDirectory)) {
                UiHelper.AlertDialog(
                        this,
                        getString(R.string.assets_dialog_dir_not_writable_title),
                        getString(R.string.assets_dialog_dir_not_writable_message),
                        null
                );
                return;
            }
            // Maybe an old installation folder was selected so we can use the old location
            if(settings.GameDirectory.isEmpty())
                settings.GameDirectory = RttrHelper.COMPAT_FindS2Installation(settings);
            ReloadUi();
            // Assets will be copied on game start
        }
    }

    private void HandleGameDirPickerResult(Intent resultData) {
        if(resultData != null && EnsurePermission()) {
            Uri uri = resultData.getData();
            settings.GameDirectory = Filesystem.UriToRealPath(uri);
            ReloadUi();
            if(!RttrHelper.CheckS2Files(settings)) {
                UiHelper.AlertDialog(
                        this,
                        getString(R.string.config_dialog_s2files_title),
                        getString(R.string.config_dialog_s2files_message),
                        null
                );
            }
        }
    }

    private void OpenFilePicker(int activityCode) {
        if(EnsurePermission()) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, activityCode);
        }
    }

    private UiHelper.SpinnerItem[] GetAvailableLogItems() {
        if(!Filesystem.IsPathWritable(settings.RttrDirectory))
            return null;
        Path logPath = RttrHelper.GetLogDir(settings);
        String[] logFiles = logPath.List();
        if(logFiles == null)
            return null;

        Arrays.sort(logFiles, Collections.reverseOrder());
        UiHelper.SpinnerItem[] items = new UiHelper.SpinnerItem[logFiles.length];

        for(int i = 0; i < logFiles.length; i++)
            items[i] = new UiHelper.SpinnerItem(i, logFiles[i]);

        return items;
    }

    private void OpenSelectedLog()
    {
        Spinner spinner = findViewById(R.id.LogSpinner);
        UiHelper.SpinnerItem item  = (UiHelper.SpinnerItem)spinner.getSelectedItem();

        if(item.label.isEmpty() || item.additional.equals("nolog")) {
            Toast.makeText(this, getString(R.string.config_toast_no_selected_log), LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, LogViewActivity.class);
        intent.putExtra("log_path", RttrHelper.GetLogDir(settings).Append(item.label));
        startActivity(intent);
    }

    // Set listeners for buttons & update ui
    private void InitUi() {
        setContentView(R.layout.game_config);

        Button button = findViewById(R.id.RttrDirPickButton);
        button.setOnClickListener(view -> {
            OpenFilePicker(RTTR_DIR_PICKER_CODE);
        });

        EditText et = findViewById(R.id.RttrDirEditText);
        et.addTextChangedListener(new UiHelper.SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable)
            {
                settings.RttrDirectory = editable.toString();
                CheckRttrDirUi();
            }
        });

        button = findViewById(R.id.GameDirPickButton);
        button.setOnClickListener(view -> {
            OpenFilePicker(GAME_DIR_PICKER_CODE);
        });

        et = findViewById(R.id.GameDirEditText);
        et.addTextChangedListener(new UiHelper.SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                settings.GameDirectory = editable.toString();
                CheckGameDirUi();
            }
        });

        et = findViewById(R.id.DefaultNameEditText);
        et.addTextChangedListener(new UiHelper.SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                settings.DefaultName = editable.toString();
            }
        });

        button = findViewById(R.id.LogOpenButton);
        button.setOnClickListener(view -> {
            OpenSelectedLog();
        });

        button = findViewById(R.id.LogDeleteButton);
        button.setOnClickListener(view -> {
            UiHelper.QuestionDialog(this, getString(R.string.config_log_delete_button), getString(R.string.config_dialog_log_delete_message), () -> {
                Path logDir = RttrHelper.GetLogDir(settings);
                if(logDir.Exists()) { // not really needed
                    Filesystem.DeleteDirectory(logDir);
                    ReloadUi();
                }

            }, null);
        });

        CheckBox checkBox = findViewById(R.id.EnableOverlayCheckbox);
        checkBox.setOnClickListener(view -> {
            settings.EnableOverlay = ((CheckBox)view).isChecked();
        });

        button = findViewById(R.id.OverlayEditButton);
        button.setOnClickListener(view -> {
            startActivityForResult(new Intent(this, OverlayConfigActivity.class), OVERLAY_CODE);
        });

        checkBox = findViewById(R.id.EnableUpdaterCheckbox);
        checkBox.setOnClickListener(view -> {
            settings.EnableUpdater = ((CheckBox)view).isChecked();
        });

        button = findViewById(R.id.UpdaterButton);
        button.setOnClickListener(view -> {
            startActivityForResult(new Intent(this, AssetManagerActivity.class), UPDATER_CODE);
        });

        button = findViewById(R.id.GameStartButton);
        button.setOnClickListener(view -> {
            if(SettingsChanged()) {
                settings.Save(this);
                Toast.makeText(this, getText(R.string.assets_toast_saved_settings), LENGTH_SHORT).show();
            }
            ExitDialog(()->{
                startActivity(new Intent(this, GameStartActivity.class));
                finish();
            });
        });

        // Initialize orientation spinner
        Spinner spinner = findViewById(R.id.OrientationSpinner);
        final UiHelper.SpinnerItem[] orientations = {
                new UiHelper.SpinnerItem(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, getString(R.string.config_orientation_dynamic_landscape)),
                new UiHelper.SpinnerItem(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT, getString(R.string.config_orientation_dynamic_portrait)),
                new UiHelper.SpinnerItem(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, getString(R.string.config_orientation_landscape)),
                new UiHelper.SpinnerItem(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, getString(R.string.config_orientation_portrait)),
                new UiHelper.SpinnerItem(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE, getString(R.string.config_orientation_reverse_landscape)),
                new UiHelper.SpinnerItem(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT, getString(R.string.config_orientation_reverse_portrait))
        };

        ArrayAdapter<UiHelper.SpinnerItem> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                orientations
        );
        spinner.setAdapter(adapter);
        UiHelper.SpinnerItem.SelectItemById(spinner, settings.Orientation);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                settings.Orientation = ((UiHelper.SpinnerItem)parent.getAdapter().getItem(position)).id;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                UiHelper.SpinnerItem.SelectItemById((Spinner)parent, settings.Orientation);
            }
        });
        // ~Initialize orientation spinner

        // Set saved values
        ReloadUi();
    }

    private void CheckRttrDirUi() {
        if(Filesystem.IsPathWritable(settings.RttrDirectory))
            ((EditText)findViewById(R.id.RttrDirEditText)).setBackgroundColor(Color.GREEN);
        else
            ((EditText)findViewById(R.id.RttrDirEditText)).setBackgroundColor(Color.RED);
    }

    private void CheckGameDirUi() {
        if(RttrHelper.CheckS2Files(settings))
            ((EditText)findViewById(R.id.GameDirEditText)).setBackgroundColor(Color.GREEN);
        else
            ((EditText)findViewById(R.id.GameDirEditText)).setBackgroundColor(Color.RED);
    }

    private void ReloadUi() {
        EditText editText = findViewById(R.id.RttrDirEditText);
        editText.setText(settings.RttrDirectory);
        CheckRttrDirUi();

        editText = findViewById(R.id.GameDirEditText);
        editText.setText(settings.GameDirectory);
        CheckGameDirUi();

        editText = findViewById(R.id.DefaultNameEditText);
        editText.setText(settings.DefaultName);

        Spinner spinner = findViewById(R.id.OrientationSpinner);
        UiHelper.SpinnerItem.SelectItemById(spinner, settings.Orientation);

        // Update log spinner
        spinner = findViewById(R.id.LogSpinner);

        UiHelper.SpinnerItem[] items = GetAvailableLogItems();
        if(items == null || items.length == 0) {
            items = new UiHelper.SpinnerItem[] {
                    new UiHelper.SpinnerItem(0, getString(R.string.config_spinner_logs_not_found), "nolog")
            };
        }

        ArrayAdapter<UiHelper.SpinnerItem> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                items
        );
        spinner.setAdapter(adapter);
        // ~Update log spinner

        CheckBox checkBox = findViewById(R.id.EnableOverlayCheckbox);
        checkBox.setChecked(settings.EnableOverlay);

        checkBox = findViewById(R.id.EnableUpdaterCheckbox);
        checkBox.setChecked(settings.EnableUpdater);
    }

    // Did settings change?
    private boolean SettingsChanged() {
        Settings temp = new Settings();
        temp.Load(this);
        return !settings.equals(temp);
    }

    // Request permission(false) if not already granted(true)
    private boolean EnsurePermission() {
        if(!Permissions.HasPermission(this)) {
            UiHelper.AlertDialog(
                    this,
                    getString(R.string.config_dialog_permission_title),
                    getString(R.string.config_dialog_permission_request),
                    ()->{
                        Permissions.RequestPermission(this, PERMISSION_CODE);
                    }
            );
            return false;
        }

        return true;
    }

    private void ExitDialog(UiHelper.DialogCallback okCallback) {
        if(settings.ShowExitDialog) {
            UiHelper.InformDialog(
                    this,
                    getString(R.string.config_dialog_info_title),
                    getString(R.string.config_dialog_exit_dialog),
                    okCallback,
                    ()->{ // Disable message for next time
                        // Directly save
                        Settings temp = new Settings().Load(this);
                        temp.ShowExitDialog = false;
                        temp.Save(this);

                        okCallback.Callback();
                    }
            );
        } else
            okCallback.Callback();
    }
}
