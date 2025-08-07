package org.s25rttr.sdl;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.system.ErrnoException;
import android.system.Os;

public class GameStartActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent config = new Intent(this, GameConfigActivity.class);
        startActivity(config);
        finish();

        try {
            Os.setenv("HOME", getFilesDir().getAbsolutePath(), true);
            Os.setenv("USER", "android", true);
        } catch (ErrnoException e) {
            throw new RuntimeException(e);
        }

        Intent intent = new Intent(this, org.libsdl.app.SDLActivity.class);
        startActivity(intent);
        finish();
    }
}
