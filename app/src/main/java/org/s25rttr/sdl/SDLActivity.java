package org.s25rttr.sdl;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

public class SDLActivity extends org.libsdl.app.SDLActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SetFullscreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SetFullscreen();
    }

    @Override
    public void setOrientationBis(int w, int h, boolean resizable, String hint) {
        super.setOrientationBis(w, h, resizable, hint);
        mSingleton.setRequestedOrientation(getIntent().getIntExtra("orientation", ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE));
    }

    @Override
    protected String[] getLibraries() {
        return new String[] {
                "SDL2",
                "SDL2_mixer",
                "videoSDL2",
                "audioSDL",
                "s25client"
        };
    }

    private void SetFullscreen() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if(controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
    }
}
