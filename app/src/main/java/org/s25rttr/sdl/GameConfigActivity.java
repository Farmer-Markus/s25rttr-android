package org.s25rttr.sdl;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.window.OnBackInvokedDispatcher;

import org.s25rttr.sdl.utils.Filesystem;
import org.s25rttr.sdl.utils.Data;
import org.s25rttr.sdl.utils.Permission;
import org.s25rttr.sdl.utils.Ui;

public class GameConfigActivity extends Activity {
    private static final Data data = new Data();

    private boolean waitingForPermission = false;
    private boolean startedByShortcut = false;

    private static final int FILE_PICKER_CODE = 1;
    private static final int PERMISSION_CODE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.game_config_main);
        View view = findViewById(R.id.mainLayout);


        startedByShortcut = getIntent().getBooleanExtra("shortcut", false);

        if(Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    ()->{
                        if(!backPressed()) super.getOnBackInvokedDispatcher();
                    }
            );
        }

        // Loads saved data into the UI
        loadData(data);

        Button button = findViewById(R.id.launchGameButton);
        button = findViewById(R.id.folderPickButton);
        button.setOnClickListener(v -> {
            openFilePicker();
        });

        button = findViewById(R.id.launchGameButton);
        button.setOnClickListener(v -> {
            getData(data);
            data.saveSettings(this);

            if(startedByShortcut) {
                startActivity(new Intent(this, GameStartActivity.class));
            }
            finish();
        });


        if(!Permission.checkPermission(this))
            Permission.requestPermission(this, PERMISSION_CODE);
    }

    @Override
    public void onBackPressed() {
        if(!backPressed()) super.onBackPressed();
    }

    private boolean backPressed() {
        if(dataChanged()) {
            Ui.questionDialog(this, getString(R.string.config_question_discard_changes_title), getString(R.string.config_question_discard_changes_message), this::finish, null);
        } else {
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        // Returned from file picker
        if(requestCode == FILE_PICKER_CODE && resultCode == RESULT_OK) {
            Uri uri = null;
            if(resultData != null) {
                uri = resultData.getData();
                data.gameFolder =  Filesystem.getRealPath(uri);
                reloadUi();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // When returning from requesting permission
        if(waitingForPermission) {
            waitingForPermission = false;

            reloadUi();
            if(!Permission.checkPermission(this))
                Permission.requestPermission(this, PERMISSION_CODE);
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, FILE_PICKER_CODE);
    }

    private void loadData(Data data) {
        data.loadSettings(this);
        reloadUi();
    }

    private void getData(Data data) {
        EditText editText = findViewById(R.id.folderTextInput);
        data.gameFolder = editText.getText().toString();
        editText = findViewById(R.id.nameTextInput);
        data.defaultName = editText.getText().toString();

        Spinner spinner = findViewById(R.id.orientationInput);
        Ui.SpinnerItem item = (Ui.SpinnerItem)spinner.getAdapter().getItem(spinner.getSelectedItemPosition());
        data.orientation = item.id;
    }

    private boolean dataChanged() {
        Data newData = new Data();
        getData(newData);
        return !newData.equals(data);
    }

    private void reloadUi() {
        EditText editText = findViewById(R.id.folderTextInput);
        editText.setText(data.gameFolder);
        editText = findViewById(R.id.nameTextInput);
        editText.setText(data.defaultName);

        Spinner spinner = findViewById(R.id.orientationInput);
        Ui.SpinnerItem[] orientations = {
                new Ui.SpinnerItem(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, getString(R.string.config_orientation_dynamic_landscape)),
                new Ui.SpinnerItem(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT, getString(R.string.config_orientation_dynamic_portrait)),
                new Ui.SpinnerItem(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, getString(R.string.config_orientation_landscape)),
                new Ui.SpinnerItem(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, getString(R.string.config_orientation_portrait)),
                new Ui.SpinnerItem(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE, getString(R.string.config_orientation_reverse_landscape)),
                new Ui.SpinnerItem(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT, getString(R.string.config_orientation_reverse_portrait))
        };

        ArrayAdapter<Ui.SpinnerItem> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                orientations
        );

        spinner.setAdapter(adapter);
        Ui.SpinnerItem.selectItemWithId(spinner, data.orientation);
    }

}
