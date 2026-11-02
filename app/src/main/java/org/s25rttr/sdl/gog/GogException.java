package org.s25rttr.sdl.gog;

import java.io.IOException;

/**
 * Error during the GOG login or download. The message is shown to the user.
 */
public class GogException extends IOException {

    public GogException(String message) {
        super(message);
    }

    public GogException(String message, Throwable cause) {
        super(message, cause);
    }
}
