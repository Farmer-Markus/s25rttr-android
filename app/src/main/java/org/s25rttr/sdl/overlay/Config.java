package org.s25rttr.sdl.overlay;


import android.support.annotation.NonNull;

import java.io.Serializable;

// Store user defined buttons and locations
public class Config implements Serializable {
    public String text;
    public int opacity = 255;
    public int textOpacity = 255;
    public Pos pos = new Pos();

    public ClickBehaviour clickBehaviour = new ClickBehaviour();
    public int keyCode = -1;
    public MouseEvent mouseEvent = new MouseEvent();
    public OverlayEvent overlayEvent = new OverlayEvent();


    public static class Pos implements Serializable, Cloneable {
        public float x;
        public float y;

        public Pos() {}
        public Pos(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        @NonNull
        public Pos clone() {
            try {
                Pos clone = (Pos) super.clone();
                clone.x = x;
                clone.y = y;
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }

    public static class ClickBehaviour implements Serializable {
        public static final int SEND_KEY = 0;
        public static final int SEND_MOUSE = 1;
        public static final int OVERLAY = 2;
        public static final int KEYBOARD_TOGGLE = 3;

        public int behaviour;
        public ClickBehaviour(final int behaviour) { this.behaviour = behaviour; }
        public ClickBehaviour() { this.behaviour = SEND_KEY; }
    }

    public static class MouseEvent implements Serializable {
        public static final int LEFT_BUTTON = 1;
        public static final int MIDDLE_BUTTON = 2;
        public static final int RIGHT_BUTTON = 3;

        public int event;
        public MouseEvent(final int event) { this.event = event; }
        public MouseEvent() { this.event = LEFT_BUTTON; }
    }

    public static class OverlayEvent implements Serializable {
        public static final int TOGGLE = 0;
        public static final int EDIT = 1;

        public int event;
        public OverlayEvent(final int event) { this.event = event; }
        public OverlayEvent() { event = TOGGLE; }
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(!(obj instanceof Config)) return false;

        Config c = (Config)obj;
        return text.equals(c.text) && pos.equals(c.pos) && opacity == c.opacity
                && textOpacity == c.textOpacity && clickBehaviour.behaviour == c.clickBehaviour.behaviour
                && keyCode == c.keyCode && mouseEvent.event == c.mouseEvent.event
                && overlayEvent.event == c.overlayEvent.event;
    }
}
