package org.s25rttr.sdl.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.s25rttr.sdl.R;

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

    public static Dialog manualDialog(Context context, String title, String message) {
        return manualDialog(context, title, message, null);
    }

        public static Dialog manualDialog(Context context, String title, String message, String additional) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.manual_dialog);
        dialog.setCancelable(false);

        ((TextView) dialog.findViewById(R.id.titleText)).setText(title);
        ((TextView) dialog.findViewById(R.id.messageText)).setText(message);
        if(additional != null)
            ((TextView) dialog.findViewById(R.id.additionalText)).setText(additional);

        dialog.show();
        return dialog;
    }

    public static class SpinnerItem implements Comparable<SpinnerItem> {
        public int id;
        public String label;
        public String additional;

        public SpinnerItem(int id, String label) {
            this.id = id;
            this.label = label;
        }

        public SpinnerItem(int id, String label, String additional) {
            this.id = id;
            this.label = label;
            this.additional = additional;
        }

        @Override
        public int compareTo(SpinnerItem other) {
            return other.label.compareToIgnoreCase(this.label);
        }

        @Override
        public String toString() {
            return label;
        }

        public static boolean selectItemWithId(Spinner spinner, int id) {
            ArrayAdapter<SpinnerItem> adapter = (ArrayAdapter<SpinnerItem>)spinner.getAdapter();
            int items = adapter.getCount();

            for(int item = 0; item < items; item++) {
                if(adapter.getItem(item).id == id) {
                    spinner.setSelection(item);
                    return true;
                }
            }

            return false;
        }
    }
}
