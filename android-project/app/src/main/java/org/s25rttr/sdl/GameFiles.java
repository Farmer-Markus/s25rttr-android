package org.s25rttr.sdl;

import android.os.Bundle;
import android.util.Log;
import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.system.Os;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

public class GameFiles extends AppCompatActivity {
    private ActivityResultLauncher<Intent> manageStoragePermissionLauncher;
    private ActivityResultLauncher<String[]> requestPermissionLauncher;
    private ActivityResultLauncher<Uri> folderPickerLauncher;
    private static final String TAG = "s25rttr";

    public interface AlertDialogCallback {
        void onOkPressed();
    }

    private void openFolderPicker() {
        folderPickerLauncher.launch(null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.e(TAG, "org.libsdl.app GameFiles()");

        folderPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocumentTree(),
            uri -> {
                if (uri != null) {
                    String Path = uri.getPath();
                    Path = FileUtils.OutputFullPath(Path);
                    Toast.makeText(this, "Selected folder: " + Path, Toast.LENGTH_SHORT).show();
                    FileUtils.WriteConfig(FileUtils.getConfFile("AppPathConfig.conf", this), Path);
                    checkForFileDir();
                    startRTTR();
                } else {
                    startRTTR();
                }
            }
        );

        manageStoragePermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (checkStoragePermissions()) {
                    Log.e(TAG, "Permission granted");
                    handleFilesAccess();
                } else {
                    Log.e(TAG, "Permission denied");
                    requestStoragePermissions();
                }
            }
        );

        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                if (checkStoragePermissions()) {
                    Log.e(TAG, "Permission granted");
                    handleFilesAccess();
                } else {
                    Log.e(TAG, "Permission denied");
                    requestStoragePermissions();
                }
            }
        );

        if (checkStoragePermissions()) {
            Log.e(TAG, "Permissions already granted");
            handleFilesAccess();
        } else {
            requestStoragePermissions();
        }
    }
    
    @Override
    protected void onResume() {
    super.onResume();

      // Check if the storage permission is granted
      if (checkStoragePermissions()) {
          // Permission granted, proceed with your logic
          Log.e(TAG, "Permission granted after returning from settings");
          handleFilesAccess();
      } else {
          // Permission denied, show an error or request permission again
          Log.e(TAG, "Permission denied after returning from settings");
          requestStoragePermissions();
      }
  }

    private boolean checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { //SDK 34+
            int readImages = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES);
            int readVideo = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO);
            int readAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO);
            return readImages == PackageManager.PERMISSION_GRANTED &&
                   readVideo == PackageManager.PERMISSION_GRANTED &&
                   readAudio == PackageManager.PERMISSION_GRANTED;
        } else {
            int readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            int writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return readPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermissions() {
        Log.e(TAG, "Requesting storage permissions");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            showAlertDialog("Permission Required", "The app needs access to all files. Please allow this permission.",
                () -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        manageStoragePermissionLauncher.launch(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Error opening permissions screen", e);
                        openAppSettings();
                    }
                });
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { //SDK 34+
            requestPermissionLauncher.launch(new String[]{
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            });
        } else {
            requestPermissionLauncher.launch(new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            });
        }
    }

    private void openAppSettings() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          try {
              Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
              intent.setData(Uri.parse("package:" + getPackageName()));
              startActivity(intent);
          } catch (Exception e) {
              Log.e(TAG, "Error opening manage storage permission screen", e);
              openAppSettingsFallback();  //fallback to old settings screen if the intent is not available
          }
      } else {
          openAppSettingsFallback();  //for older versions, use the fallback method
      }
    }

    private void openAppSettingsFallback() {
      // Fallback method for older versions or if manage storage intent is not available
      Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
      intent.setData(Uri.parse("package:" + getPackageName()));
      startActivity(intent);
    }

    private void showAlertDialog(String title, String message, AlertDialogCallback callback) {
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("OK", (dialog, which) -> {
                dialog.dismiss();
                if (callback != null) {
                    callback.onOkPressed();
                }
            })
            .show();
    }

    private void handleFilesAccess() {
        if (!checkForFileDir()) {
            openFolderPicker();
        } else {
            startRTTR();
        }
    }

    private boolean checkForFileDir() {
        String Path = FileUtils.ReadConfig(FileUtils.getConfFile("AppPathConfig.conf", this));
        if (Path == null || Path.isEmpty()) return false;

        try {
            if (!FileUtils.testPath(Path)) return false;
            Os.setenv("HOME", Path, true);
            Os.setenv("USER", "android", true);
            CopyAssets.copyAssetsToDataStorage(this, Path);
            NativeLibraryHelper.manageLibs(this);
        } catch (Exception e) {
            e.printStackTrace();
            showAlertDialog("Error", "RTTR encountered an error!", this::finishAffinity);
            return false;
        }

        Log.e(TAG, "Check for file directory successful");
        return true;
    }

    private void startRTTR() {
        Intent intent = new Intent(this, RTTRMain.class);
        startActivity(intent);
        Log.e(TAG, "Starting RTTRMain");
        finish();
    }
}
