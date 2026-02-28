package org.s25rttr.sdl.overlay;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.widget.FrameLayout;

public class OverlayLayout extends FrameLayout {
    private Config.Pos mousePos;
    private final SurfaceView surface;

    public OverlayLayout(@NonNull Context context, SurfaceView surface) {
        super(context);
        mousePos = new Config.Pos(0 ,0);
        this.surface = surface;
    }

    public OverlayLayout(@NonNull Context context, @Nullable AttributeSet attrs, SurfaceView surface) {
        super(context, attrs);
        this.surface = surface;
    }

    public OverlayLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, SurfaceView surface) {
        super(context, attrs, defStyleAttr);
        this.surface = surface;
    }

    public OverlayLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes, SurfaceView surface) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.surface = surface;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // If no button was clicked save new mouse/touch coords
        boolean ret = super.dispatchTouchEvent(ev);
        if(!ret) {
            mousePos.x = ev.getX();
            mousePos.y = ev.getY();

            // Pass event to game (SDLSurface)
            if(surface != null) {
                MotionEvent evCopy = MotionEvent.obtain(ev);
                surface.dispatchTouchEvent(evCopy);
                evCopy.recycle();
            }
        }
        // Consume event to get further up and move events
        return true;
    }

    public Config.Pos GetMousePos() {
        return mousePos;
    }
}
