package org.s25rttr.sdl;

import android.app.Activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.system.ErrnoException;
import android.system.Os;

import org.libsdl.app.SDLActivity;
import org.s25rttr.sdl.utils.Data;
import org.s25rttr.sdl.utils.Filesystem;

public class GameStartActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkValues();

        /*
        try {
            Os.setenv("HOME", getFilesDir().getAbsolutePath(), true);
            Os.setenv("USER", "android", true);
        } catch (ErrnoException e) {
            throw new RuntimeException(e);
        }

        Intent intent = new Intent(this, org.libsdl.app.SDLActivity.class);
        startActivity(intent);
        finish();*/
    }

    protected void onResume() {
        super.onResume();

        checkValues();
    }

    private void checkValues() {
        Data data = new Data();
        data.loadSettings(this);
        if (data.gameFolder.isEmpty()) {
            startActivity(new Intent(this, GameConfigActivity.class));
            return;
        }
        if (!Filesystem.pathIsWritable(data.gameFolder)) {

            alertDialog("Filesystem error", getString(R.string.alert_file_write_error),
                    ()->{startActivity(new Intent(this, GameConfigActivity.class));});
            return;
        }

        try {
            Os.setenv("HOME", data.gameFolder, true);
            Os.setenv("USER", data.defaultName, true);

        } catch (ErrnoException e) {
            throw new RuntimeException(e);
        }

        startActivity(new Intent(this, SDLActivity.class));
        finish();
    }


    private void alertDialog(String title, String message, alertCallback callback) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    if(callback != null) {
                        callback.onOkPressed();
                    }
                }).show();
    }

    private interface alertCallback {
        void onOkPressed();
    }
}