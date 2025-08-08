package org.s25rttr.sdl;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.s25rttr.sdl.utils.Data;
import org.s25rttr.sdl.utils.Permission;

public class GameConfigActivity extends Activity {
    private static Data data = new Data();

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

        // Loads saved data into the UI
        loadData();

        Button button = findViewById(R.id.launchGameButton);
        button = findViewById(R.id.folderPickButton);
        button.setOnClickListener(v -> {
            openFilePicker();
        });

        button = findViewById(R.id.launchGameButton);
        button.setOnClickListener(v -> {
            saveData();
            if(startedByShortcut) {
                startActivity(new Intent(this, GameStartActivity.class));
            }
            finish();
        });


        if(!Permission.checkPermission(this))
            Permission.requestPermission(this, PERMISSION_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        // Returned from file picker
        if(requestCode == FILE_PICKER_CODE && resultCode == RESULT_OK) {
            Uri uri = null;
            if(resultData != null) {
                uri = resultData.getData();
                data.gameFolder = data.getRealPath(uri);
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

    private void loadData() {
        data.loadSettings(this);
        reloadUi();
    }

    private void saveData() {
        EditText editText = findViewById(R.id.folderTextInput);
        data.gameFolder = editText.getText().toString();
        editText = findViewById(R.id.nameTextInput);
        data.defaultName = editText.getText().toString();

        data.saveSettings(this);
    }

    private void reloadUi() {
        EditText editText = findViewById(R.id.folderTextInput);
        editText.setText(data.gameFolder);
        editText = findViewById(R.id.nameTextInput);
        editText.setText(data.defaultName);
    }

}
