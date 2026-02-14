package org.s25rttr.sdl.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.s25rttr.sdl.R;

import java.util.Objects;

public class UiHelper {
    public static interface DialogCallback {
        void Callback();
    }

    // Simple dialog. Show error message etc. User needs to click OK
    public static void AlertDialog(Context context, String title, String message, DialogCallback okCallback) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(context.getString(R.string.dialog_ok), (dialog, which) -> {
                    dialog.dismiss();
                    if(okCallback != null)
                        okCallback.Callback();
                }).show();
    }

    // Simple question dialog. User must click YES or NO
    public static void QuestionDialog(Context context, String title, String message, DialogCallback yesCallback, DialogCallback noCallback) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton(context.getString(R.string.dialog_no), (dialog, which) -> {
                    dialog.dismiss();
                    if(noCallback != null)
                        noCallback.Callback();
                })
                .setPositiveButton(context.getString(R.string.dialog_yes), (dialog, which) -> {
                    dialog.dismiss();
                    if(yesCallback != null)
                        yesCallback.Callback();
                })
                .show();
    }

    // Imform dialog. User can click OK or Do not show again
    public static void InformDialog(Context context, String title, String message, DialogCallback okCallback, DialogCallback notAskCallback) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setNeutralButton(context.getString(R.string.dialog_not_show), (dialog, which) -> {
                    dialog.dismiss();
                    if(notAskCallback != null)
                        notAskCallback.Callback();
                })
                .setPositiveButton(context.getString(R.string.dialog_ok), (dialog, which) -> {
                    dialog.dismiss();
                    if(okCallback != null)
                        okCallback.Callback();
                })
                .show();
    }

    public static Dialog ManualDialog(Context context, String title, String message) {
        return ManualDialog(context, title, message, null);
    }

    public static Dialog ManualDialog(Context context, String title, String message, String additional) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.manual_dialog);
        dialog.setCancelable(false);

        ((TextView)dialog.findViewById(R.id.ManualTitleText)).setText(title);
        ((TextView)dialog.findViewById(R.id.ManualMessageText)).setText(message);
        if(additional != null)
            ((TextView)dialog.findViewById(R.id.ManualAdditionalText)).setText(additional);

        dialog.show();
        return dialog;
    }

    // calls finish() after clicking ok
    public static void FatalError(Context context, String message) {
        AlertDialog(
                context,
                context.getString(R.string.config_dialog_generic_error_title),
                message,
                ((Activity)context)::finish
        );
    }

    public static class SpinnerItem implements Comparable<SpinnerItem> {
        public int id;
        public String label;
        public String additional;


        @Override
        public int compareTo(SpinnerItem other) {
            return other.label.compareToIgnoreCase(this.label);
        }

        @Override
        @NonNull
        public String toString() { return label; }

        public SpinnerItem(int id, String label) {
            this.id = id;
            this.label = label;
            this.additional = "";
        }

        public SpinnerItem(int id, String label, String additional) {
            this.id = id;
            this.label = label;
            this.additional = additional;
        }

        public static boolean SelectItemById(Spinner spinner, int id) {
            ArrayAdapter<SpinnerItem> adapter = (ArrayAdapter<SpinnerItem>)spinner.getAdapter();
            int items = adapter.getCount();

            for(int item = 0; item < items; item++) {
                if(Objects.requireNonNull(adapter.getItem(item)).id == id) {
                    spinner.setSelection(item);
                    return true;
                }
            }

            return false;
        }
    }

    // Just so I don't have to define all functions even if I don't want to use them
    public abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
        @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
        @Override public void afterTextChanged(Editable editable) {}
    }



}
