package org.s25rttr.sdl.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

public class Permissions
{

    // Check if permissions are granted
    public static boolean HasPermission(Context context)
    {
        // Newer permission system
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            return Environment.isExternalStorageManager();

        // Old permission system
        int readPrm = context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        int writePrm = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return readPrm == 0 && writePrm == 0;
    }

    public static boolean RequestPermission(Context context, int activityCode)
    {
        // Newer permission system
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        {
            try
            {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent);
            } catch (Exception e) {
                Log.e("org.s25rttr.sdl", "Failed to request MANAGE_ALL_FILES permission: %s", e);
                return false;
            }
            return true;
        }

        // Old permission system
        ActivityCompat.requestPermissions((Activity)context, new String[] {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, activityCode);
        return true;
    }
}
