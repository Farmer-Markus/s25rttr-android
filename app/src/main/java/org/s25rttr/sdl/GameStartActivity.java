package org.s25rttr.sdl;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.system.ErrnoException;
import android.system.Os;

import org.s25rttr.sdl.data.Path;
import org.s25rttr.sdl.utils.AssetHelper;
import org.s25rttr.sdl.data.Filesystem;
import org.s25rttr.sdl.data.Settings;
import org.s25rttr.sdl.utils.RttrHelper;
import org.s25rttr.sdl.utils.UiHelper;

import java.util.Locale;

public class GameStartActivity extends Activity
{
    private boolean asset_init = false;

    private static final int SDL_ACTIVITY_CODE = 0;
    private static final int UPDATER_CODE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if(savedInstanceState != null)
            asset_init = savedInstanceState.getBoolean("asset_init");
        PrepareGame();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        // Store current unsaved settings until going back to this activity
        if(asset_init) outState.putBoolean("asset_init", true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData)
    {
        switch(requestCode)
        {
            case SDL_ACTIVITY_CODE:
                Exit();
                break;

            case UPDATER_CODE:
                if(resultCode == RESULT_OK)
                    PrepareGame();
                else
                {
                    startActivity(new Intent(this, GameConfigActivity.class));
                    finish();
                }
                break;
        }
    }

    private void PrepareGame()
    {
        Settings s = new Settings();
        s.Load(this);

        if(!Filesystem.IsPathWritable(s.RttrDirectory) || s.RttrDirectory.isEmpty())
        {
            if(s.RttrDirectory.isEmpty() && (s.RttrDirectory = Settings.COMPAT_GetOld(this)) != null)
            {
                s.GameDirectory = RttrHelper.COMPAT_FindS2Installation(s);
                s.Save(this);
                PrepareGame();
                return;
            }


            UiHelper.AlertDialog(
                    this,
                    getString(R.string.game_dialog_missing_config_title),
                    getString(R.string.game_dialog_missing_config_message),
                    ()->{
                        startActivity(new Intent(this, GameConfigActivity.class));
                        finish();
                    }
            );
            return;
        }

        Path assetDir = AssetHelper.GetExternalAssetDirPath(s);
        if(!assetDir.Exists())
        {
            startActivityForResult(new Intent(this, AssetManagerActivity.class), UPDATER_CODE);
            asset_init = true;
            return;
        }

        // Don't need updater if assets were just copied
        if(!asset_init && s.EnableUpdater && AssetHelper.AppUpdated(this, s))
        {
            Intent intent = new Intent(this, AssetManagerActivity.class);
            intent.putExtra("short_dialog", true);
            startActivityForResult(intent, UPDATER_CODE);
            return;
        }

        if(!RttrHelper.CheckS2Files(s))
        {
            UiHelper.AlertDialog(
                    this,
                    getString(R.string.config_dialog_s2files_title),
                    getString(R.string.config_dialog_s2files_message),
                    ()->{
                        startActivity(new Intent(this, GameConfigActivity.class));
                        finish();
                    }
            );
            return;
        }

        if(!RttrHelper.PrepareDrivers(this))
        {
            UiHelper.AlertDialog(
                    this,
                        getString(R.string.game_dialog_driver_failed_title),
                        getString(R.string.game_dialog_driver_failed_message),
                        this::Exit
            );
            return;
        }

        try
        {
            // Linux env vars
            Os.setenv("HOME", s.RttrDirectory, true);
            Os.setenv("USER", s.DefaultName, true);
            Os.setenv("LANG", Locale.getDefault().toString(), true);

            // RTTR specific env vars
            Os.setenv("RTTR_PREFIX_DIR", s.RttrDirectory, true);
            Os.setenv("RTTR_DRIVER_DIR", RttrHelper.GetDriverDir(this), true);
            Os.setenv("RTTR_RTTR_DIR", AssetHelper.GetExternalAssetDirPath(s).toString(), true);
            Os.setenv("RTTR_GAME_DIR", s.GameDirectory, true);

        } catch (ErrnoException e)
        {
            UiHelper.AlertDialog(
                    this,
                    "Critical error",
                    "Failed to set essential environment variables. If your running in an emulator ensure you are using the right ABI!" + e.toString(),
                    this::Exit
            );
            return;
        }

        // Finally start RTTR
        StartGame(s.Orientation);
    }

    private void StartGame(int orientation)
    {
        Intent intent = new Intent(this, SDLActivity.class);
        intent.putExtra("orientation", orientation);

        // Make sure this activity gets started instead of directly the SDLActivity
        // (in app overview when app was soft closed)
        startActivityForResult(intent, SDL_ACTIVITY_CODE);
    }

    private void Exit()
    {
        finishAffinity();
        System.exit(0);
    }
}
