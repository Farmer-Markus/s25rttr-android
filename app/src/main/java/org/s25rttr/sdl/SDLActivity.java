package org.s25rttr.sdl;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;

import org.s25rttr.sdl.data.Settings;
import org.s25rttr.sdl.overlay.Overlay;
import org.s25rttr.sdl.utils.UiHelper;

public class SDLActivity extends org.libsdl.app.SDLActivity {
    private Overlay overlay;
    private Settings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = new Settings().Load(this);
        if(settings.EnableOverlay) {
            overlay = new Overlay(this, mLayout, mSurface, false, settings);
            overlay.Load();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mFullscreenModeActive)
            UiHelper.SetFullscreen(this);
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
}
