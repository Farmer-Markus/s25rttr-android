package org.s25rttr.sdl;

import android.content.pm.ActivityInfo;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class SDLActivity extends org.libsdl.app.SDLActivity {

    // Overrides SDL's internal orientation system to force custom user orientations
    /*@Override
    public void setOrientationBis(int w, int h, boolean resizable, String hint) {
        mSingleton.setRequestedOrientation(getIntent().getIntExtra("rotation", ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE));
    }*/

    // Above approach would cause issues when changing to windowed mode
    @Override
    public void setOrientationBis(int w, int h, boolean resizable, String hint) {
        super.setOrientationBis(w, h, resizable, hint);
        mSingleton.setRequestedOrientation(getIntent().getIntExtra("rotation", ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE));
    }

    @Override
    protected String[] getLibraries() {
        return new String[] {
                "SDL2",
                "SDL2_mixer",
                "s25client"
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mFullscreenModeActive)
            setFullScreen();
    }

    private void setFullScreen() {
        Window window = getWindow();
        int flags = View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.INVISIBLE;
        window.getDecorView().setSystemUiVisibility(flags);
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
    }
}