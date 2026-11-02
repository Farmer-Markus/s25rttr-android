package org.s25rttr.sdl.gog;

import com.google.gson.annotations.SerializedName;

/**
 * JSON models for the GOG API.
 */
public class GogModels {

    /** Response of GET https://auth.gog.com/token */
    public static class GogTokenResponse {
        @SerializedName("access_token")
        public String accessToken;
        @SerializedName("refresh_token")
        public String refreshToken;
        @SerializedName("expires_in")
        public long expiresIn;
        @SerializedName("token_type")
        public String tokenType;
        @SerializedName("error")
        public String error;
        @SerializedName("error_description")
        public String errorDescription;
    }

    /** Response of GET https://embed.gog.com/account/getFilteredProducts */
    public static class GogOwnedProducts {
        @SerializedName("totalPages")
        public int totalPages;
        @SerializedName("products")
        public GogProductSummary[] products;
    }

    /** A product entry in the owned products list. */
    public static class GogProductSummary {
        @SerializedName("id")
        public long id;
        @SerializedName("title")
        public String title;
        @SerializedName("slug")
        public String slug;
    }

    /** Response of GET https://api.gog.com/products/{id}?expand=downloads */
    public static class GogProduct {
        @SerializedName("id")
        public long id;
        @SerializedName("title")
        public String title;
        @SerializedName("downloads")
        public GogDownloads downloads;
    }

    /** The downloads of a product. */
    public static class GogDownloads {
        @SerializedName("installers")
        public GogInstaller[] installers;
    }

    /** An installer entry of a product. */
    public static class GogInstaller {
        @SerializedName("name")
        public String name;
        @SerializedName("os")
        public String os;
        @SerializedName("language")
        public String language;
        @SerializedName("files")
        public GogDownloadFile[] files;
    }

    /** A single file of an installer entry. */
    public static class GogDownloadFile {
        @SerializedName("id")
        public String id;
        @SerializedName("size")
        public long size;
        @SerializedName("downlink")
        public String downlink;
    }

    /** Response of the downlink URL of a product file. */
    public static class GogDownlinkResponse {
        @SerializedName("downlink")
        public String downlink;
        @SerializedName("url")
        public String url;
    }
}
