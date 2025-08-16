package org.s25rttr.sdl;

import android.app.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.system.ErrnoException;
import android.system.Os;

import org.libsdl.app.SDL;
import org.libsdl.app.SDLActivity;
import org.s25rttr.sdl.utils.Data;
import org.s25rttr.sdl.utils.Filesystem;
import org.s25rttr.sdl.utils.Ui;

import java.io.IOException;

public class GameStartActivity extends Activity {
    private static final int SDL_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkValues();
    }

    protected void onResume() {
        super.onResume();

        checkValues();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        // Returned from sdl activity
        if(requestCode == SDL_CODE) {
            finishAffinity();
            System.exit(0);
        }
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
            // Set home dir to save Logs/configs
            Os.setenv("HOME", data.gameFolder, true);
            
            // Set default system user name
            Os.setenv("USER", data.defaultName, true);
            
            // Set the dir to search for game data
            Os.setenv("RTTR_PREFIX_DIR", data.gameFolder, true);
            
            // Set driver dir for rttr
            Os.setenv("RTTR_DRIVER_DIR", getCacheDir().toString() + "/driver", true);

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

        // For result to make the app start this activity when clicket on app overview instead of sdl activity
        startActivityForResult(new Intent(this, SDLActivity.class), SDL_CODE);
    }
}
