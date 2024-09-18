package org.s25rttr.sdl;

import org.s25rttr.sdl.RTTRMain;
import org.s25rttr.sdl.FileUtils;
import org.s25rttr.sdl.CopyAssets;
import org.s25rttr.sdl.NativeLibraryHelper;

import java.io.IOException;

import android.os.Bundle;
import android.util.Log;
import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.app.AlertDialog;
import android.widget.Toast;
import android.net.Uri;
import android.os.Environment;
import android.system.Os;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;


public class GameFiles extends AppCompatActivity {
    private ActivityResultLauncher<String[]> requestPermissionLauncher;
    private ActivityResultLauncher<Uri> folderPickerLauncher;
    private static final String TAG = "s25rttr";
    public interface AlertDialogCallback { void onOkPressed(); }
    private void openFolderPicker() { folderPickerLauncher.launch(null); }
    
 
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
                    
                    FileUtils.WriteConfig(FileUtils.getConfFile(this), Path);
                    checkForFileDir();
                    startRTTR();
                    
                } else
                    startRTTR();
            }
        );

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean readPermissionGranted = result.get(Manifest.permission.READ_EXTERNAL_STORAGE);
                    Boolean writePermissionGranted = result.get(Manifest.permission.WRITE_EXTERNAL_STORAGE);

                    if (readPermissionGranted != null && readPermissionGranted && writePermissionGranted != null && writePermissionGranted) {
                        Log.e(TAG, "org.libsdl.app GameFiles() permission granted");
                        
                        if (!checkForFileDir())
                            openFolderPicker();
                        else
                            startRTTR();
                        
                    } else {
                        Log.e(TAG, "org.libsdl.app GameFiles() permission denied");
                        String dialogTitel = "Permission Error";
                        String dialogMessage = "This App need the 'Files' permission to work. Please go to settings and grant permissions.";
                        showAlertDialog(dialogTitel, dialogMessage, new AlertDialogCallback() {
            		    @Override
            		    public void onOkPressed() {
                	        finishAffinity(); //exit whole app
            		    }
        		});
                    }
        });

        if (checkStoragePermissions()) {
            Log.e(TAG, "org.libsdl.app GameFiles() permissions already granted");
            
            if (!checkForFileDir())
                openFolderPicker();
            else
                startRTTR();

        } else {
            requestStoragePermissions();
            Log.e(TAG, "org.libsdl.app GameFiles() end of request");
        }
    }
    
    private boolean checkStoragePermissions() {
        int readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return readPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermissions() {
        Log.e(TAG, "org.libsdl.app GameFiles() request permission");
        
        String dialogTitel = "Permission";
        String dialogMessage = "The app needs the following permission to read/write to the folder picked by the user.";
        showAlertDialog(dialogTitel, dialogMessage, new AlertDialogCallback() {
            @Override
            public void onOkPressed() {
                requestPermissionLauncher.launch(new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
                });
            }
        });
    }
    
    private void showAlertDialog(String title, String message, AlertDialogCallback callback) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    
    builder.setTitle(title)
           .setMessage(message)
           .setCancelable(false)
           .setPositiveButton("OK", (dialog, which) -> {
               dialog.dismiss();
               if (callback != null) {
                   callback.onOkPressed();
               }
           });

    AlertDialog dialog = builder.create();
    dialog.show();
    }
    
    private boolean checkForFileDir() {
        String Path = FileUtils.ReadConfig(FileUtils.getConfFile(this));
        if (Path.isEmpty() || Path == null)
            return false;
        
        try {
            if(!FileUtils.testPath(Path))
                return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        
        try {
            Os.setenv("HOME", Path, true);
        } catch (Exception e) {
            e.printStackTrace();
            
            String dialogTitel = "Error";
            String dialogMessage = "RTTR crashed while setting HOME variable to picked folder!";
            showAlertDialog(dialogTitel, dialogMessage, new AlertDialogCallback() {
                @Override
                public void onOkPressed() {
                    finishAffinity(); //exit whole app
            	}
       	    });
        }
        
        try {
            CopyAssets.copyAssetsToDataStorage(this, Path);
        } catch (Exception e) {
            e.printStackTrace();
            
            String dialogTitel = "Error";
            String dialogMessage = "RTTR crashed while trying to copy asset files into picked folder!";
            showAlertDialog(dialogTitel, dialogMessage, new AlertDialogCallback() {
                @Override
                public void onOkPressed() {
                    finishAffinity(); //exit whole app
            	}
       	    });
        }
        
        try {
	    NativeLibraryHelper.manageLibs(this);
	} catch (Exception e) {
	    e.printStackTrace();
	    
	    String dialogTitel = "Error";
            String dialogMessage = "RTTR crashed while setting libDir (library Directory) variable to app cache!";
            showAlertDialog(dialogTitel, dialogMessage, new AlertDialogCallback() {
                @Override
                public void onOkPressed() {
                    finishAffinity(); //exit whole app
            	}
       	    });
	}
        
        Log.e(TAG, "org.libsdl.app checkfilesdir returning true");
        return true;
    }
    
    private void startRTTR() {
        Intent intent = new Intent(this, RTTRMain.class);
        //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        Log.e(TAG, "org.libsdl.app checkfilesdir Finished RTTRMain");
        finishAffinity();
    }
}
