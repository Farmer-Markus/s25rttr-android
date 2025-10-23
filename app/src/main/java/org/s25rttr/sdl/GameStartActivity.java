package org.s25rttr.sdl;

import android.app.Activity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.system.ErrnoException;
import android.system.Os;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import org.s25rttr.sdl.utils.Data;
import org.s25rttr.sdl.utils.Filesystem;
import org.s25rttr.sdl.utils.Ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

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
                    ()->{startActivity(new Intent(this, GameConfigActivity.class));
            });
            return;
        }

        if(!Files.exists(Paths.get(data.gameFolder).resolve("share"))) {
            Ui.questionDialog(this, "Error! Assets not found", getString(R.string.alert_assets_missing), ()->{

                Dialog dialog = Ui.manualDialog(this, getString(R.string.config_dialog_copying_title),
                        getString(R.string.config_dialog_copying_message));

                new Thread(()->{
                    try {
                        Filesystem.copyAssets(this, getAssets(), "share", new File(data.gameFolder + "/share/s25rttr"),
                                dialog.findViewById(R.id.additionalText));
                    } catch (IOException e) {
                        dialog.dismiss();
                        throw new RuntimeException(e);
                    }
                    dialog.dismiss();
                    checkValues();
                }).start();
            }, this::finish);

            return;
        }

        try {
            // Set home dir to save Logs/configs
            Os.setenv("HOME", data.gameFolder, true);
            
            // Set default system user name
            Os.setenv("USER", data.defaultName, true);

            Os.setenv("LANG", Locale.getDefault().toString(), true);
            
            // Set the dir to search for game data
            Os.setenv("RTTR_PREFIX_DIR", data.gameFolder, true);
            
            // Set driver dir for rttr
            Os.setenv("RTTR_DRIVER_DIR", getCacheDir().toString() + "/driver", true);

            Os.setenv("RTTR_RTTR_DIR", data.gameFolder + "/share/s25rttr/RTTR", true);
            Os.setenv("RTTR_GAME_DIR", data.gameFolder + "/share/s25rttr/S2", true);

        } catch (ErrnoException e) {
            throw new RuntimeException(e);
        }

        try {
            Filesystem.prepareDrivers(this, true);

        } catch (IOException e) {
            Ui.alertDialog(this, "Filesystem error", "Failed to create symlinks in app cache: " + e,
                    this::finish);
            return;
        }

        Intent intent = new Intent(this, SDLActivity.class);
        intent.putExtra("rotation", data.orientation);

        // For result to make the app start this activity when clicked on app overview instead of sdl activity
        startActivityForResult(intent, SDL_CODE);
    }
}
