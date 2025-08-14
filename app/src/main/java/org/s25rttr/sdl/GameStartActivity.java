package org.s25rttr.sdl;

import android.app.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.system.ErrnoException;
import android.system.Os;

import org.libsdl.app.SDLActivity;
import org.s25rttr.sdl.utils.Data;
import org.s25rttr.sdl.utils.Filesystem;
import org.s25rttr.sdl.utils.Ui;

import java.io.IOException;

public class GameStartActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkValues();
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

            Ui.alertDialog(this, "Filesystem error", getString(R.string.alert_file_write_error),
                    ()->{startActivity(new Intent(this, GameConfigActivity.class));});
            return;
        }

        try {
            Os.setenv("HOME", data.gameFolder, true);
            Os.setenv("USER", data.defaultName, true);
            Os.setenv("libDir", getCacheDir().toString() + "/lib/s25rttr/driver/", true);

        } catch (ErrnoException e) {
            throw new RuntimeException(e);
        }

        try {
            if (!Filesystem.prepareDrivers(this, true)) {
                Ui.alertDialog(this, "Filesystem error", "Failed to create symlinks in app cache.",
                        this::finish);
                return;
            }
        } catch (IOException e) {
            Ui.alertDialog(this, "Filesystem error", "Failed to create symlinks in app cache: " + e,
                    this::finish);
            return;
        }

        startActivity(new Intent(this, SDLActivity.class));
        finish();
    }
}
