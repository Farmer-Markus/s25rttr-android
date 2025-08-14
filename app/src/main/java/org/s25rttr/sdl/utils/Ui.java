package org.s25rttr.sdl.utils;

import android.app.AlertDialog;
import android.content.Context;

public class Ui {
    public static interface alertCallback {
        void onOkPressed();
    }

    public static void alertDialog(Context context, String title, String message, alertCallback callback) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    if(callback != null) {
                        callback.onOkPressed();
                    }
                }).show();
    }

    public static interface yesCallback {
        void onYesPressed();
    }

    public static interface noCallback {
        void onNoPressed();
    }

    public static void questionDialog(Context context, String title, String message, yesCallback yesCallback, noCallback noCallback) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setNegativeButton("NO", (dialog, which)->{
                    dialog.dismiss();
                    if(noCallback != null) {
                        noCallback.onNoPressed();
                    }
                })
                .setPositiveButton("YES", (dialog, which)->{
                    dialog.dismiss();
                    if(yesCallback != null) {
                        yesCallback.onYesPressed();
                    }
                }).show();
    }
}
