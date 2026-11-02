package org.s25rttr.sdl.gog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.s25rttr.sdl.gog.GogModels.GogDownlinkResponse;
import org.s25rttr.sdl.gog.GogModels.GogInstaller;
import org.s25rttr.sdl.gog.GogModels.GogOwnedProducts;
import org.s25rttr.sdl.gog.GogModels.GogProduct;
import org.s25rttr.sdl.gog.GogModels.GogProductSummary;

/**
 * Downloads the installer of "The Settlers 2 Gold Edition" from GOG. Requires the user to own the
 * game on their GOG account.
 */
public class GogDownloader {

    /** Callback for download progress. */
    public interface ProgressCallback {
        void onProgress(String status, int percent);
    }

    private static final Gson GSON = new Gson();

    private final String accessToken;

    public GogDownloader(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Runs the download flow and stores the Windows installer in the given file.
     *
     * @return the file the installer was written to
     */
    public File download(File targetFile, ProgressCallback progress) throws IOException {
        progress.onProgress("Checking ownership...", 10);
        GogProductSummary settlers = findOwnedSettlers2();
        if (settlers == null) {
            throw new GogException("You do not own The Settlers 2 Gold Edition on this account.");
        }

        progress.onProgress("Getting download info...", 15);
        GogProduct product = getProduct(settlers.id);
        GogInstaller installer = pickInstaller(product);
        if (installer == null) {
            throw new GogException("No Windows installer found for this game.");
        }

        progress.onProgress("Preparing download...", 20);
        String downloadUrl = getDownlink(installer.files[0].downlink);

        downloadFile(downloadUrl, targetFile, progress, installer.name);
        return targetFile;
    }

    /**
     * Searches the owned games for The Settlers 2 by title or slug.
     */
    private GogProductSummary findOwnedSettlers2() throws IOException {
        int page = 1;
        while (true) {
            GogOwnedProducts owned = getOwnedProducts(page);
            if (owned == null || owned.products == null || owned.products.length == 0) {
                return null;
            }
            for (GogProductSummary product : owned.products) {
                if (isSettlers2(product)) {
                    return product;
                }
            }
            if (page >= owned.totalPages) {
                return null;
            }
            page++;
        }
    }

    static boolean isSettlers2(GogProductSummary product) {
        if (product.title != null) {
            String title = product.title.toLowerCase(Locale.ROOT);
            if (title.contains("settlers 2") || title.contains("siedler 2")) {
                return true;
            }
        }
        if (product.slug != null) {
            String slug = product.slug.toLowerCase(Locale.ROOT);
            if (slug.contains("settlers_2") || slug.contains("siedler_2")) {
                return true;
            }
        }
        return false;
    }

    private GogOwnedProducts getOwnedProducts(int page) throws IOException {
        HttpURLConnection connection = GogLogin.openConnection("GET",
                GogLogin.EMBED_URL + "/account/getFilteredProducts?filter=owned&mediaType=1&page=" + page
                        + "&productsPerPage=100",
                accessToken);
        return getJson(connection, GogOwnedProducts.class, "Could not load the GOG library");
    }

    private GogProduct getProduct(long productId) throws IOException {
        HttpURLConnection connection = GogLogin.openApiConnection("GET",
                "/products/" + productId + "?expand=downloads", accessToken);
        return getJson(connection, GogProduct.class, "Could not load the game details");
    }

    private String getDownlink(String downlinkUrl) throws IOException {
        if (downlinkUrl == null || downlinkUrl.isEmpty()) {
            throw new GogException("No download link available.");
        }
        HttpURLConnection connection = GogLogin.openConnection("GET", downlinkUrl, accessToken);
        GogDownlinkResponse response = getJson(connection, GogDownlinkResponse.class, "Could not generate a download link");
        String url = response.downlink != null ? response.downlink : response.url;
        if (url == null || url.isEmpty()) {
            throw new GogException("No download link received.");
        }
        return url;
    }

    private static <T> T getJson(HttpURLConnection connection, Class<T> type, String errorPrefix) throws IOException {
        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        String body = stream != null ? new String(GogLogin.readAllBytes(stream), StandardCharsets.UTF_8) : "";
        if (code < 200 || code >= 300) {
            throw new GogException(errorPrefix + " (HTTP " + code + ").");
        }
        try {
            T result = GSON.fromJson(body, type);
            if (result == null) {
                throw new GogException(errorPrefix + ": empty response.");
            }
            return result;
        } catch (JsonSyntaxException ex) {
            throw new GogException(errorPrefix + ": invalid response.", ex);
        }
    }

    /**
     * Picks the Windows installer, preferring the device language.
     */
    static GogInstaller pickInstaller(GogProduct product) {
        if (product == null || product.downloads == null || product.downloads.installers == null) {
            return null;
        }
        String deviceLanguage = Locale.getDefault().getLanguage();
        GogInstaller fallback = null;
        for (GogInstaller installer : product.downloads.installers) {
            if (!"windows".equals(installer.os) || installer.files == null || installer.files.length == 0) {
                continue;
            }
            if (fallback == null) {
                fallback = installer;
            }
            if (installer.language != null && installer.language.equalsIgnoreCase(deviceLanguage)) {
                return installer;
            }
        }
        return fallback;
    }

    /**
     * Downloads a file from the given URL to the target file, reporting progress.
     */
    private static void downloadFile(String url, File target, ProgressCallback progress, String name) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(60000);
        connection.setReadTimeout(60000);
        connection.setInstanceFollowRedirects(true);

        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new GogException("Download failed (HTTP " + code + ").");
        }

        long total = connection.getContentLength();
        try (InputStream in = connection.getInputStream(); OutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            long downloaded = 0;
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
                downloaded += n;
                if (total > 0) {
                    int percent = 20 + (int) (70.0 * downloaded / total);
                    progress.onProgress("Downloading " + name + "...", percent);
                }
            }
        }
    }
}
