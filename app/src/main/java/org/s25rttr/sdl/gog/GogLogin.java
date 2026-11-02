package org.s25rttr.sdl.gog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.s25rttr.sdl.gog.GogModels.GogTokenResponse;

/**
 * Login against the GOG API. Uses the public OAuth2 API that the GOG Galaxy client uses (also used
 * by open-source tools like lgogdownloader).
 */
public class GogLogin {

    // Public OAuth2 client credentials of the GOG Galaxy client.
    public static final String CLIENT_ID = "46899977096215655";
    public static final String CLIENT_SECRET = "9d85c43b1482497dbbce61f6e4aa173a433796eeae2ca8c5f6129f2dc4de46d9";

    // Redirect target of the web login. The authorization code arrives as a "code" query parameter.
    public static final String REDIRECT_URI = "https://embed.gog.com/on_login_success?origin=client";

    public static final String AUTH_URL = "https://auth.gog.com/auth?client_id=" + CLIENT_ID
            + "&redirect_uri=" + urlEncode(REDIRECT_URI) + "&response_type=code&layout=galaxy";
    public static final String TOKEN_URL = "https://auth.gog.com/token";
    public static final String API_URL = "https://api.gog.com";
    public static final String EMBED_URL = "https://embed.gog.com";

    private static final Gson GSON = new Gson();

    private GogLogin() {
    }

    /**
     * Exchanges an authorization code (captured from the web login redirect) for an access token.
     */
    public static GogTokenResponse exchangeCode(String code) throws IOException {
        String query = "grant_type=authorization_code"
                + "&client_id=" + CLIENT_ID
                + "&client_secret=" + CLIENT_SECRET
                + "&code=" + urlEncode(code)
                + "&redirect_uri=" + urlEncode(REDIRECT_URI);
        return getToken(query);
    }

    private static GogTokenResponse getToken(String query) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(TOKEN_URL + "?" + query).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(60000);
        connection.setReadTimeout(60000);
        connection.setRequestProperty("Accept", "application/json");

        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        String responseBody = stream != null ? new String(readAllBytes(stream), StandardCharsets.UTF_8) : "";

        GogTokenResponse response;
        try {
            response = GSON.fromJson(responseBody, GogTokenResponse.class);
        } catch (JsonSyntaxException ex) {
            throw new GogException("Login failed (HTTP " + code + ").", ex);
        }
        if (code >= 200 && code < 300 && response != null && response.accessToken != null && !response.accessToken.isEmpty()) {
            return response;
        }

        String message = response != null && response.errorDescription != null
                ? response.errorDescription
                : "Login failed (HTTP " + code + ").";
        throw new GogException(message);
    }

    /**
     * Opens a connection to the GOG API with the given access token.
     */
    static HttpURLConnection openApiConnection(String method, String path, String accessToken) throws IOException {
        return openConnection(method, API_URL + path, accessToken);
    }

    static HttpURLConnection openConnection(String method, String url, String accessToken) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(60000);
        connection.setReadTimeout(60000);
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setRequestProperty("Accept", "application/json");
        return connection;
    }

    static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException("UTF-8 is not supported.", ex);
        }
    }

    static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }
}
