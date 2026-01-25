package org.s25rttr.sdl;

import static android.widget.Toast.LENGTH_SHORT;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import org.s25rttr.sdl.data.Path;
import org.s25rttr.sdl.utils.AssetHelper;
import org.s25rttr.sdl.data.Filesystem;
import org.s25rttr.sdl.data.Settings;
import org.s25rttr.sdl.utils.UiHelper;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AssetManagerActivity extends Activity
{
    private final Settings settings;
    private final List<String> toUpdate = new ArrayList<>();

    private boolean short_dialog;

    public AssetManagerActivity()
    {
        settings = new Settings();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        short_dialog = getIntent().getBooleanExtra("short_dialog", false);

        settings.Load(this);
        if(settings.RttrDirectory.isEmpty() || !Filesystem.IsPathWritable(settings.RttrDirectory))
        {
            // Ask to check settings -> open config
            UiHelper.AlertDialog(
                    this,
                    getString(R.string.assets_dialog_dir_not_writable_title),
                    getString(R.string.assets_dialog_dir_not_writable_message),
                    this::Failed
            );
            return;
        }

        Path assetDir = AssetHelper.GetExternalAssetDirPath(settings);
        if(!assetDir.Exists())
        {
            Dialog dialog = UiHelper.ManualDialog(
                    this,
                    getString(R.string.assets_dialog_copying_title),
                    ""
            );
            dialog.show();

            new Thread(()->{
                try
                {
                    CopyAssets(dialog.findViewById(R.id.ManualAdditionalText));
                } catch (IOException e) {
                    dialog.dismiss();
                    runOnUiThread(()->{
                        CopyError(e.toString());
                    });
                    return;
                }
                dialog.dismiss();
                Success();
            }).start();
            return;
        }

        Dialog dialog = UiHelper.ManualDialog(
                this,
                getString(R.string.assets_dialog_searching_title),
                ""
        );
        dialog.show();

        new Thread(()->{
            try {
                CheckDir(dialog.findViewById(R.id.ManualAdditionalText));
            } catch (IOException e) {
                dialog.dismiss();
                runOnUiThread(()->{
                    UpdateError(e.toString());
                });
                return;
            }

            runOnUiThread(this::UpdateReady);
            dialog.dismiss();;
        }).start();
    }

    // Are files the same?
    private boolean CompareFiles(AssetManager manager, String path) throws IOException
    {
        Path externalFile = AssetHelper.GetExternalAssetPath(settings, path);
        if(!externalFile.Exists())
            return false;
        FileInputStream fInStrm = new FileInputStream(externalFile.toString());
        InputStream inStrm = manager.open(path);
        String sha1 = Filesystem.FileGenSha256(inStrm);
        return Filesystem.FileGenSha256(fInStrm).equals(sha1);
    }

    private void CopyFile(AssetManager manager, String path) throws IOException
    {
        Path fullPath = AssetHelper.GetExternalAssetPath(settings, path);
        Path parentDir = fullPath.GetParent();
        if(!parentDir.Exists())
            parentDir.Mkdirs();
        FileOutputStream out = new FileOutputStream(fullPath.toString());
        InputStream in = manager.open(path);
        Filesystem.CopyFile(in, out);
    }

    public void CopyAssets(TextView status) throws IOException
    {
        CheckDir(getAssets(), "RTTR", true, status);
    }

    // Check for updates
    public void CheckDir(TextView status) throws IOException
    {
        CheckDir(getAssets(), "RTTR", false, status);
    }

    // Index files where update is needed or just copy all assets over(simpleCopy = true)
    private void CheckDir(AssetManager manager, String currPath, boolean simpleCopy, TextView status) throws IOException
    {
        runOnUiThread(()->{
            status.setText(currPath);
        });
        String[] dirContent = manager.list(currPath);
        if(dirContent == null)
            throw new IOException("Could not get Assets from " + currPath);

        // Must be a file
        if(dirContent.length == 0)
        {
            if(simpleCopy)
                CopyFile(manager, currPath);
            else
            {
                if(!CompareFiles(manager, currPath))
                    toUpdate.add(currPath);
            }
            return;
        }

        // If is folder
        for(String entry : dirContent)
            CheckDir(manager, currPath + "/" + entry, simpleCopy, status);
    }

    private void DisableUpdater()
    {
        UiHelper.QuestionDialog(
                this,
                getString(R.string.assets_dialog_updater_disable_title),
                getString(R.string.assets_dialog_updater_disable_message),
                ()->{
                    settings.EnableUpdater = false;
                    settings.Save(this);
                    Toast.makeText(this, getText(R.string.assets_toast_updater_disable), LENGTH_SHORT).show();
                    Success();
                },
                this::Success
        );
    }

    private void CopyError(String error)
    {
        UiHelper.AlertDialog(
                this,
                getString(R.string.assets_dialog_copy_failed_title),
                getString(R.string.assets_dialog_copy_failed_message, error),
                this::Failed
        );
    }

    private void UpdateError(String error)
    {
        UiHelper.AlertDialog(
                this,
                getString(R.string.assets_dialog_update_failed_title),
                getString(R.string.assets_dialog_update_failed_message, error),
                this::Failed
        );
    }

    private void UpdateFiles()
    {
        Dialog dialog = UiHelper.ManualDialog(
                this,
                getString(R.string.assets_dialog_updating_title),
                getString(R.string.assets_dialog_updating)
        );

        new Thread(()->{
            AssetManager assets = getAssets();
            for(String file : toUpdate)
            {
                runOnUiThread(()->{
                    ((TextView)dialog.findViewById(R.id.ManualAdditionalText)).setText(file);
                });

                Path tmpFile = AssetHelper.GetExternalAssetPath(settings, file);
                if(!(tmpFile = tmpFile.GetParent()).Exists() && !tmpFile.Mkdirs())
                {
                    dialog.dismiss();
                    runOnUiThread(()->{
                        UpdateError("Failed to create directory: " + AssetHelper.GetExternalAssetPath(settings, file).toString());
                    });
                    return;
                }

                try
                {
                    Filesystem.CopyFile(assets.open(file), new FileOutputStream(AssetHelper.GetExternalAssetPath(settings, file).toString()));
                } catch (IOException e)
                {
                    dialog.dismiss();
                    runOnUiThread(() -> {
                        UpdateError(e.toString());
                    });
                    return;
                }
            }

            dialog.dismiss();
            if(short_dialog)
            {
                runOnUiThread(()->{
                    Toast.makeText(this, getString(R.string.assets_dialog_update_done_title), LENGTH_SHORT).show();
                });
                Success();
            } else
            {
                runOnUiThread(() -> {
                    UiHelper.AlertDialog(
                            this,
                            getString(R.string.assets_dialog_update_done_title),
                            getString(R.string.assets_dialog_update_done_message),
                            this::Success
                    );
                });
            }
        }).start();
    }

    // Checked every file, now ready to update or dismiss
    private void UpdateReady()
    {
        if(toUpdate.isEmpty()) {
            if(short_dialog)
            {
                Toast.makeText(this, getString(R.string.assets_toast_no_updates), LENGTH_SHORT).show();
                Success();
            } else
            {
                UiHelper.AlertDialog(
                        this,
                        getString(R.string.assets_dialog_updated_title),
                        getString(R.string.assets_dialog_updated_message),
                        this::Success
                );
            }
        } else {
            UiHelper.QuestionDialog(
                    this,
                    getString(R.string.assets_dialog_update_ready_title),
                    getString(R.string.assets_dialog_update_message, toUpdate.size()),
                    this::UpdateFiles,
                    this::DisableUpdater
            );
        }
    }

    private void Success()
    {
        setResult(RESULT_OK);
        finish();
    }

    private void Failed()
    {
        setResult(RESULT_CANCELED);
        finish();
    }
}
