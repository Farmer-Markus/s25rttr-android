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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class AssetManagerActivity extends Activity {
    private final Settings settings = new Settings();;
    private final List<String> toUpdate = new ArrayList<>();

    private boolean short_dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        short_dialog = getIntent().getBooleanExtra("short_dialog", false);
        settings.Load(this);

        // string! if the string is empty, not the folder itself
        if(settings.RttrDirectory.isEmpty() || !Filesystem.IsPathWritable(settings.RttrDirectory)) {
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
        if(!assetDir.Exists()) {
            Dialog dialog = UiHelper.ManualDialog(
                    this,
                    getString(R.string.assets_dialog_copying_title),
                    ""
            );
            dialog.show();

            new Thread(()->{
                try {
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


        // Check for updates

        Dialog dialog = UiHelper.ManualDialog(
                this,
                getString(R.string.assets_dialog_searching_title),
                ""
        );
        dialog.show();

        new Thread(()->{
            try {
                CheckAssets(dialog.findViewById(R.id.ManualAdditionalText));
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

    private void CopyFile(AssetManager manager, String path) throws IOException {
        Path fullPath = AssetHelper.GetExternalAssetDirPath(settings, path);
        Path parentDir = fullPath.GetParent();
        if(!parentDir.Exists())
            parentDir.Mkdirs();
        FileOutputStream out = new FileOutputStream(fullPath.toString());
        InputStream in = manager.open(AssetHelper.GetInternalAssetDirPath(path).toString());
        Filesystem.CopyFile(in, out);
    }

    public void CopyAssets(TextView status) throws IOException {
        CheckAssets(getAssets(), status, true);
    }

    private boolean ReadHashFileList(List<String> hashes, List<String> paths, AssetManager manager) throws IOException {
        // hashes, pathsHolds = hashvals & paths read from file (created at compile time with gradle)
        InputStream hashInStream = manager.open(AssetHelper.GetAssetHashFilePath().toString());
        BufferedReader hashReader = new BufferedReader(new InputStreamReader(hashInStream));

        String line;
        while ((line = hashReader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Index of space between hash & path
            int sIndex = line.indexOf(' ');
            if (sIndex > 0) {
                hashes.add(line.substring(0, sIndex));
                paths.add(line.substring(sIndex + 1));
            }
        }

        return !hashes.isEmpty() && !paths.isEmpty() && hashes.size() == paths.size();
    }

    private String GetFileHash(Path filePath) throws IOException {
        FileInputStream in = new FileInputStream(filePath.toString());
        return Filesystem.FileGenSha256(in);
    }

    private void CheckAssets(TextView status) throws IOException {
        CheckAssets(getAssets(), status, false);
    }

    private void CheckAssets(AssetManager manager, TextView status, boolean copy) throws IOException {
        List<String> FileHashes = new ArrayList<>();
        List<String> FilePaths = new ArrayList<>();
        // Read file paths & hashes
        if(!ReadHashFileList(FileHashes, FilePaths, manager))
            throw new IOException("Failed to read hashes from file! Amound of hashes is not the same as of paths");

        Path assetDir = AssetHelper.GetExternalAssetDirPath(settings);

        for(int i = 0; i < FileHashes.size() ; i++) {
            String relativePath = FilePaths.get(i);
            Path fullPath = assetDir.Append(relativePath);
            runOnUiThread(()-> status.setText(relativePath));

            if(!fullPath.Exists()) {
                if(copy)
                    CopyFile(manager, relativePath);
                else
                    toUpdate.add(relativePath);
                continue;
            }

            // Compare hashes
            if (!FileHashes.get(i).equals(GetFileHash(fullPath)))
                toUpdate.add(relativePath);
        }
    }

    // Check for updates
    /*public void CheckDir(TextView status) throws IOException {
        CheckDir(getAssets(), "RTTR", false, status);
    }

    // Index files where update is needed or just copy all assets over(simpleCopy = true)
    private void CheckDir(AssetManager manager, String currPath, boolean simpleCopy, TextView status) throws IOException {
        runOnUiThread(()->{
            status.setText(currPath);
        });
        String[] dirContent = manager.list(currPath);
        if(dirContent == null)
            throw new IOException("Could not get Assets from " + currPath);

        // Must be a file
        if(dirContent.length == 0) {
            if(simpleCopy)
                CopyFile(manager, currPath);
            else {
                if(!CompareFiles(manager, currPath))
                    toUpdate.add(currPath);
            }
            return;
        }

        // If is folder
        for(String entry : dirContent)
            CheckDir(manager, currPath + "/" + entry, simpleCopy, status);
    }*/

    private void DisableUpdater() {
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

    private void CopyError(String error) {
        UiHelper.AlertDialog(
                this,
                getString(R.string.assets_dialog_copy_failed_title),
                getString(R.string.assets_dialog_copy_failed_message, error),
                this::Failed
        );
    }

    private void UpdateError(String error) {
        UiHelper.AlertDialog(
                this,
                getString(R.string.assets_dialog_update_failed_title),
                getString(R.string.assets_dialog_update_failed_message, error),
                this::Failed
        );
    }

    private void UpdateFiles() {
        Dialog dialog = UiHelper.ManualDialog(
                this,
                getString(R.string.assets_dialog_updating_title),
                getString(R.string.assets_dialog_updating)
        );

        new Thread(()->{
            AssetManager assets = getAssets();
            for(String file : toUpdate) {
                runOnUiThread(()->{
                    ((TextView)dialog.findViewById(R.id.ManualAdditionalText)).setText(file);
                });

                Path tmpFile = AssetHelper.GetExternalAssetDirPath(settings, file);
                if(!(tmpFile = tmpFile.GetParent()).Exists() && !tmpFile.Mkdirs()) {
                    dialog.dismiss();
                    runOnUiThread(()->{
                        UpdateError("Failed to create directory: " + AssetHelper.GetExternalAssetDirPath(settings, file).toString());
                    });
                    return;
                }

                try {
                    CopyFile(assets, file);
                } catch (IOException e) {
                    dialog.dismiss();
                    runOnUiThread(() -> {
                        UpdateError(e.toString());
                    });
                    return;
                }
            }

            runOnUiThread(() -> AssetHelper.SaveAppUpdated(this, settings));

            dialog.dismiss();
            if(short_dialog) {
                runOnUiThread(()->{
                    Toast.makeText(this, getString(R.string.assets_dialog_update_done_title), LENGTH_SHORT).show();
                });
                Success();
            } else {
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
    private void UpdateReady() {
        if(toUpdate.isEmpty()) {
            if(short_dialog) {
                Toast.makeText(this, getString(R.string.assets_toast_no_updates), LENGTH_SHORT).show();
                AssetHelper.SaveAppUpdated(this, settings);
                Success();
            } else {
                UiHelper.AlertDialog(
                        this,
                        getString(R.string.assets_dialog_updated_title),
                        getString(R.string.assets_dialog_updated_message),
                        () -> {
                            runOnUiThread(() -> AssetHelper.SaveAppUpdated(this, settings));
                            Success();
                        }
                );
            }
        } else {
            UiHelper.QuestionDialog(
                    this,
                    getString(R.string.assets_dialog_update_ready_title),
                    getString(R.string.assets_dialog_update_message, toUpdate.size()),
                    this::UpdateFiles,
                    ()->{
                        // Only ask to disable if currently enabled
                        if(settings.EnableUpdater)
                            DisableUpdater();
                        else
                            Success();
                    }
            );
        }
    }

    private void Success() {
        setResult(RESULT_OK);
        finish();
    }

    private void Failed() {
        setResult(RESULT_CANCELED);
        finish();
    }
}
