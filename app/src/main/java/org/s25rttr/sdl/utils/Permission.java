package org.s25rttr.sdl.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

public class Permission {

    public static boolean checkPermission(Context context) {
        // Older than MANAGE_EXTERNAL_STORAGE permission
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();

        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            int readPrm = context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            int writePrm = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return readPrm == 0 && writePrm == 0; // 0 = Permission granded
        } else {
            // Should always be granted
            return true;
        }
    }

    // Returns true when user actively needs to do something to get permissions
    public static boolean requestPermission(Context context, int permissionCode) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // MANAGE STORAGE
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent);
            } catch (Exception e) {
                Log.e("org.s25rttr.sdl", "Failed to request MANAGE_ALL_FILES permission");
                return true;
            }

            return true;

        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // READ/WRITE EXTERNAL STORAGE
            ActivityCompat.requestPermissions((Activity)context, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, permissionCode);
            return true;

        } else {}

        return false;
    }


}
