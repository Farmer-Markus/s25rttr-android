package org.s25rttr.sdl;

import org.libsdl.app.SDLActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;


public class FileUtils extends AppCompatActivity {
    private static final String TAG = "s25rttr";

 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.e(TAG, "org.libsdl.app FileUtils");
    }
    
    public static String OutputFullPath(String Path) {
	int colonIndex = Path.indexOf(":");
	String afterColon = Path.substring(colonIndex + 1).trim();
	String beforeColon = Path.substring(0, colonIndex).trim();

	String FullStoragePath = null;

	if (afterColon.isEmpty()) {
	    Log.e(TAG, "org.libsdl.app afterClonon is empty!");

	} else if (beforeColon.isEmpty()) {
	    Log.e(TAG, "org.libsdl.app beforeClonon is empty!");

	} else if (beforeColon.contains("/tree/primary")) {
	    Log.e(TAG, "org.libsdl.app Selected directory URI: /storage/emulated/0/" + afterColon);
	    FullStoragePath = ("/storage/emulated/0/" + afterColon);
	    return FullStoragePath;

	} else if (beforeColon.contains("/tree/")) {
	    int startIndex = Path.indexOf("/") + 6;
	    int endIndex = Path.lastIndexOf(":");
	    String sdcardID = Path.substring(startIndex, endIndex);
	    Log.e(TAG, "org.libsdl.app Selected directory URI: /storage/" + sdcardID + "/" + afterColon);
	    FullStoragePath = ("/storage/" + sdcardID + "/" + afterColon);
	    return FullStoragePath;
	}
	return "";
    }
    
    public static void WriteConfig(File ConfFile, String ToWrite) {
	    Log.e(TAG, "org.libsdl.app writing to Config File...");
	    try(FileWriter fileWriter = new FileWriter(ConfFile)) {
		fileWriter.write(ToWrite);
		fileWriter.close();
	    } catch (IOException e) {
		Log.e(TAG, "org.libsdl.app Failed writing to Config File: An exception occurred!");
	    }
    }
    
    public static String ReadConfig(File ConfFile) {
	Log.e(TAG, "org.libsdl.app Reading config...");
	try(BufferedReader br = new BufferedReader(new FileReader(ConfFile))) {
	    String line = br.readLine();
	    if (line == null)
		return "";
	    Log.e(TAG, "org.libsdl.app path in Config file is: " + line);
	    return line;
	} catch (IOException e) {
	    return "";
	}
    }
    
    public static File getConfFile(String ConfFileName, Context context) {
        String InternalStoragePath = context.getFilesDir() + "/";
        File ConfFile = new File(InternalStoragePath, ConfFileName);
        
        return ConfFile;
    }
    
    public static boolean testPath(String Path) throws IOException {
        File PathDir = new File(Path);
        if (!PathDir.exists())
            return false;
        
        File testFile = new File(Path, "testFile");
        Log.e(TAG, "org.libsdl.app trying to create testfile in: " + Path + " | " + testFile);
        if (testFile.createNewFile()) {
            Log.e(TAG, "org.libsdl.app testfile success");
            testFile.delete();
            return true;
        }
        return false;
    }
    
    /*
    public static boolean checkIfUpdated(Context context) throws NameNotFoundException {
        File ConfFile = getConfFile("AppLastUpdate", context);
        if(!ConfFile.exists()) {
            WriteConfig(ConfFile, Long.toString(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).firstInstallTime)); //thanks https://stackoverflow.com/questions/26352881/detect-if-new-install-or-updated-version-android-app
        } else {
            String latestUpdateFromFile = ReadConfig(ConfFile);
            if(latestUpdateFromFile == "")
                return false;
            
            String latestUpdate = Long.toString(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).lastUpdateTime);
            
             Log.e(TAG, "org.libsdl.app Latest update is: " + latestUpdate + " update from file is: " + latestUpdateFromFile);
            
            if(latestUpdateFromFile.equals(latestUpdate)) {
               Log.e(TAG, "org.libsdl.app App was NOT updated");
               return false;
            } else {
                Log.e(TAG, "org.libsdl.app App was recently updated");
                WriteConfig(ConfFile, latestUpdate); //.toString);
                return true;
            }
        }
        return false;
    }
    */
}
