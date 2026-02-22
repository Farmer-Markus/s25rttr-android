package org.s25rttr.sdl.overlay;


import java.io.Serializable;

// Store user defined buttons and locations
public class Config implements Serializable {
    public String Text = null;
    public Pos Pos = null;

    public ClickBehaviour clickBehaviour = null;
    public int keyCode = -1;
    public MouseEvent mouseEvent = null;



    public static class Pos {
        float x;
        float y;
    }

    public enum ClickBehaviour {
        // Send keyboard key
        SEND_KEY,
        // Send mouse input
        SEND_MOUSE,
        // Hide/Show overlay(keep this button visible!!!!!)
        OVERLAY_TOGGLE,
        // Show/Hide android keyboard
        KEYBOARD_TOGGLE
    }

    public enum MouseEvent {
        LEFT_BUTTON(1),
        MIDDLE_BUTTON(2),
        RIGHT_BUTTON(3);

        private final int value;

        MouseEvent(final int value) {
            this.value = value;
        }

        public int Value() {
            return value;
        }
    }
}
