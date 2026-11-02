package org.s25rttr.sdl.gog;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.s25rttr.sdl.R;
import org.s25rttr.sdl.gog.GogModels.GogTokenResponse;

/**
 * GOG web login and installer download for The Settlers 2 Gold Edition.
 * <p>
 * Shows the official GOG login page in a WebView. After the user signs in, the OAuth authorization
 * code is captured from the redirect and exchanged for an access token. The Windows installer is then
 * downloaded into the app's external files directory. On success the activity finishes with
 * {@link Activity#RESULT_OK} and the installer path is returned as the result data string extra
 * {@link #EXTRA_INSTALLER_PATH}.
 */
public class GogLoginActivity extends Activity {

    public static final String EXTRA_INSTALLER_PATH = "installer_path";

    private static final String TAG = "RttrGog";
    private static final String REDIRECT_HOST = "embed.gog.com";
    private static final String INSTALLER_NAME = "settlers2_gog_setup.exe";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private WebView loginWebView;
    private LinearLayout progressLayout;
    private ProgressBar progressBar;
    private TextView statusText;
    private boolean loginHandled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showGogLogin();
    }

    private void showGogLogin() {
        loginWebView = new WebView(this);
        WebSettings settings = loginWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        loginWebView.setWebViewClient(new GogWebViewClient());
        setContentView(loginWebView);
        loginWebView.loadUrl(GogLogin.AUTH_URL);
    }

    @Override
    public void onBackPressed() {
        if (progressLayout == null && loginWebView != null && loginWebView.canGoBack()) {
            loginWebView.goBack();
            return;
        }
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
        if (loginWebView != null) {
            loginWebView.destroy();
        }
    }

    /**
     * Called when the login captured an OAuth authorization code. Exchanges it for a token and downloads.
     */
    private void onAuthCode(String code) {
        if (loginHandled) {
            return;
        }
        loginHandled = true;
        showProgress(getString(R.string.gog_logging_in), 0);

        executorService.execute(() -> {
            try {
                GogTokenResponse token = GogLogin.exchangeCode(code);
                startDownload(token.accessToken);
            } catch (Exception ex) {
                Log.e(TAG, "Login failed", ex);
                mainHandler.post(() -> onError(ex.getMessage()));
            }
        });
    }

    private void startDownload(String accessToken) {
        mainHandler.post(() -> showProgress(getString(R.string.gog_starting_download), 0));
        try {
            File target = new File(getExternalFilesDir(null), INSTALLER_NAME);
            new GogDownloader(accessToken).download(target,
                    (status, percent) -> mainHandler.post(() -> updateProgress(status, percent)));

            mainHandler.post(() -> {
                Toast.makeText(this, R.string.gog_download_complete, Toast.LENGTH_LONG).show();
                setResult(RESULT_OK, new Intent().putExtra(EXTRA_INSTALLER_PATH, target.getAbsolutePath()));
                finish();
            });
        } catch (Exception ex) {
            Log.e(TAG, "Download failed", ex);
            mainHandler.post(() -> onError(getString(R.string.gog_download_failed) + " " + ex.getMessage()));
        }
    }

    private void onError(String message) {
        // setContentView() below replaces the overlay, so only the reference needs resetting.
        progressLayout = null;
        loginHandled = false;
        showGogLogin();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Shows a full-screen progress overlay with a status text and a progress bar.
     */
    private void showProgress(String status, int percent) {
        if (progressLayout == null) {
            progressLayout = new LinearLayout(this);
            progressLayout.setOrientation(LinearLayout.VERTICAL);
            progressLayout.setGravity(Gravity.CENTER);
            progressLayout.setBackgroundColor(Color.parseColor("#1a1a2e"));

            progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progressBar.setMax(100);
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            barParams.leftMargin = dpToPx(32);
            barParams.rightMargin = dpToPx(32);
            progressLayout.addView(progressBar, barParams);

            statusText = new TextView(this);
            statusText.setTextSize(14);
            statusText.setGravity(Gravity.CENTER);
            statusText.setTextColor(Color.parseColor("#dddddd"));
            LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            statusParams.topMargin = dpToPx(16);
            progressLayout.addView(statusText, statusParams);

            addContentView(progressLayout, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        progressBar.setProgress(percent);
        statusText.setText(status);
    }

    private void updateProgress(String status, int percent) {
        if (progressLayout == null) {
            return;
        }
        progressBar.setProgress(percent);
        statusText.setText(status);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    /**
     * Captures the "code" query parameter from the GOG redirect URL.
     */
    private class GogWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return handleRedirect(request.getUrl().toString());
        }

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return handleRedirect(url);
        }

        private boolean handleRedirect(String url) {
            if (url == null || !url.startsWith("https://" + REDIRECT_HOST)) {
                return false;
            }
            String code = Uri.parse(url).getQueryParameter("code");
            if (code == null || code.isEmpty()) {
                return false;
            }
            onAuthCode(code);
            return true;
        }
    }
}
