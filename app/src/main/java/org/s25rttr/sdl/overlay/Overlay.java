package org.s25rttr.sdl.overlay;

import android.app.Activity;
import android.view.View;

public class Overlay {
    private final Activity parent;
    private final View parentView;

    public Overlay(final Activity parent, final View parentView) {
        this.parent = parent;
        this.parentView = parentView;
    }

    public Overlay Show() {
        // Go through every layout component and show()
        return this;
    }

    public Overlay Hide() {
        // Go through every layout component and hide()
        return this;
    }

    private int DpToPx(int dp) {
        float density = parent.getResources().getDisplayMetrics().density;
        return (int)(dp * density);
    }
}

/*
Implement customizer to dynamicly let user add/remove buttons.
Overwrite touch functions to let user drag buttons around to specific positions.
Store positions/text/keyboard-key inside overlay.Config somehow
 */