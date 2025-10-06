package org.s25rttr.sdl;

import android.content.pm.ActivityInfo;

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
    protected String getMainFunction() {
        return "main";
    }
}