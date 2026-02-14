package org.s25rttr.sdl.utils;

import android.content.Context;
import android.content.pm.PackageManager;

import org.s25rttr.sdl.data.Path;
import org.s25rttr.sdl.data.Settings;

public class AssetHelper {
    public static Path GetExternalAssetDirPath(Settings settings) {
        return GetExternalAssetDirPath(settings, null);
    }

    public static Path GetExternalAssetDirPath(Settings settings, String child) {
        Path path = new Path(settings.RttrDirectory);
        if(child == null)
            return path.Append("share/s25rttr/RTTR");
        return path.Append("share/s25rttr/RTTR").Append(child);
    }

    public static Path GetInternalAssetDirPath(String child) {
        return GetInternalAssetDirPath().Append(child);
    }

    public static Path GetInternalAssetDirPath(Path child) {
        return GetInternalAssetDirPath().Append(child);
    }

    public static Path GetInternalAssetDirPath() {
        return new Path("RTTR");
    }

    /**
     * Inside of assets folder!
     * @return <code>path to asset file</code>
     */
    public static Path GetAssetHashFilePath() {
        return new Path("assetHashes");
    }

    /**
     * Call function only once. LastUpdated will get overwritten after first call
     * @param context
     * @param settings
     * @return <code>true</code> if app was recently updated
     */
    public static boolean AppUpdated(Context context, Settings settings) {
        try {
            long last = context.getPackageManager().getPackageInfo(context.getPackageName(), 0)
                    .lastUpdateTime;
            if(last != settings.LastUpdated) {
                settings.LastUpdated = last;

                // Save in case current settings won't get saved
                Settings s = new Settings().Load(context);
                s.LastUpdated = last;
                s.Save(context);
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            UiHelper.FatalError(context, e.toString());
        }
        return false;
    }
}
