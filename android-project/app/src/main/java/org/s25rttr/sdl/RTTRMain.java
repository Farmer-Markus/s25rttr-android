package org.s25rttr.sdl;

import org.libsdl.app.SDLActivity;

import android.os.Bundle;
import android.util.Log;


public class RTTRMain extends SDLActivity {
    private static final String TAG = "s25rttr";
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.e(TAG, "org.libsdl.app starting rttr");
    }
}
