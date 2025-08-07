package org.s25rttr.sdl.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

public class Permission {

    public boolean checkPermission(Context context) {
        // Older than MANAGE_EXTERNAL_STORAGE permission
        if(Build.VERSION.SDK_INT < 30) {

        } else {
            return Environment.isExternalStorageManager();
        }

        return false;
    }
}
