package org.s25rttr.sdl;

import android.content.pm.ActivityInfo;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class SDLActivity extends org.libsdl.app.SDLActivity {
    private int windowFlags =
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE;// | View.INVISIBLE;

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
            tryEnableFullscreen(5);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if(mFullscreenModeActive)
            tryEnableFullscreen(5);
    }

    public void tryEnableFullscreen(final int maxTries) {
        tryEnableFullscreen(0, maxTries);
    }

    private void tryEnableFullscreen(final int currTry, final int maxTries) {
        final View view = getWindow().getDecorView();
        view.postDelayed(()->{
            view.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );

            // Did it work?
            int flags = view.getSystemUiVisibility();
            boolean finished = (flags & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0;
            
            if(!finished && currTry < maxTries) {
                // start another instance
                tryEnableFullscreen(currTry + 1, maxTries);
            }
            
        }, 500);
    }
}