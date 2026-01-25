package org.s25rttr.sdl.utils;

import android.content.Context;
import android.support.annotation.Nullable;
import android.system.ErrnoException;
import android.system.Os;

import org.s25rttr.sdl.data.Path;
import org.s25rttr.sdl.data.Settings;

import java.io.File;

public class RttrHelper
{
    // S2 game files rttr is actually using
    private static final String[] S2Assets = {
            "DATA",
            "GFX/PICS",
            "GFX/PICS/MISSION",
            "DATA/MAPS",
            "DATA/MAPS2",
            "DATA/MAPS3",
            "DATA/MAPS4",
            "DATA/MBOB",
            "GFX/TEXTURES",
            "DATA/SOUNDDAT/SOUND.LST",
            "DATA/BOBS/BOAT.LST",
            "DATA/BOOT_Z.LST",
            "DATA/BOBS/CARRIER.BOB",
            "DATA/IO/IO.DAT",
            "DATA/BOBS/JOBS.BOB",
            "DATA/MIS0BOBS.LST",
            "DATA/MIS1BOBS.LST",
            "DATA/MIS2BOBS.LST",
            "DATA/MIS3BOBS.LST",
            "DATA/MIS4BOBS.LST",
            "DATA/MIS5BOBS.LST",
            "GFX/PALETTE/PAL5.BBM",
            "GFX/PALETTE/PAL6.BBM",
            "GFX/PALETTE/PAL7.BBM",
            "GFX/PALETTE/PALETTI0.BBM",
            "GFX/PALETTE/PALETTI1.BBM",
            "GFX/PALETTE/PALETTI8.BBM",
            "DATA/RESOURCE.DAT",
            "DATA/CBOB/ROM_BOBS.LST",
            "GFX/PICS/SETUP013.LBM",
            "GFX/PICS/SETUP015.LBM"
    };


    public static String GetDriverDir(Context context)
    {
        return context.getCacheDir().toString() + "/driver";
    }

    public static String GetLibDir(Context context)
    {
        return context.getApplicationInfo().nativeLibraryDir;
    }

    public static boolean PrepareDrivers(Context context)
    {
        File libDir = new File(GetLibDir(context));
        File drDir = new File(GetDriverDir(context));

        File vidSrc = new File(libDir, "libvideoSDL2.so");
        File audSrc = new File(libDir, "libaudioSDL.so");

        File vidDir = new File(drDir, "video");
        File vidDest = new File(vidDir, "libvideoSDL2.so");
        File audDir = new File(drDir, "audio");
        File audDest = new File(audDir, "libaudioSDL.so");

        // Always deleting prevents weird errors after app update
        vidDest.delete();
        audDest.delete();

        boolean vidSuccess = vidDir.exists() || vidDir.mkdirs();
        boolean audSuccess = audDir.exists() || audDir.mkdirs();

        if(!vidSuccess || !audSuccess)
            return false;

        try
        {
            Os.symlink(vidSrc.toString(), vidDest.toString());
            Os.symlink(audSrc.toString(), audDest.toString());
        } catch (ErrnoException ignore)
        {
            return false;
        }

        return vidDest.exists() && audDest.exists();
    }

    public static boolean CheckS2Files(Settings settings)
    {
        return CheckS2Files(settings.GameDirectory);
    }
    public static boolean CheckS2Files(String path)
    {
        if(!path.endsWith("/"))
            path += "/";

        for(String asset : S2Assets)
        {
            if(!new File(path + asset).exists())
                return false;
        }
        return true;
    }

    /**
     * Tries to find s2 installation at old location
     * @param settings
     * @return <code>S2 path</code> if found,
     * <code>null</code> otherwise
     */
    public static String COMPAT_FindS2Installation(Settings settings)
    {
        Path path = new Path(settings.RttrDirectory).Append("share/s25rttr/S2");
        if(path.Exists() && CheckS2Files(path.toString()))
            return path.toString();
        return null;
    }
}
