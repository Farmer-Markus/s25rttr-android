package org.s25rttr.sdl.data;


import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.io.File;

// Simple path class (no java Path for compatibility)
public class Path implements Parcelable {
    private String data;
    // data will never end with '/'

    protected Path(Parcel in) {
        data = in.readString();
    }

    public static final Creator<Path> CREATOR = new Creator<Path>() {
        @Override
        public Path createFromParcel(Parcel in) {
            return new Path(in);
        }

        @Override
        public Path[] newArray(int size) {
            return new Path[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(data);
    }

    public Path() {}
    public Path(@NonNull Path path) {
        this.data = path.data;
    }

    public Path(@NonNull String path) {
        data = CheckPart(path);
    }

    @Override
    @NonNull
    public String toString() {
        return data;
    }

    /**
     * Append string
     * @param child the path to append
     * @param child2 another path to append
     * @return <code>Appended path</code>
     */
    public Path Append(@NonNull String child, @NonNull String child2) {
        return Append(child).Append(child2);
    }

    /**
     * Append string
     * @param child the path to append
     * @return <code>Appended path</code>
     */
    public Path Append(@NonNull String child) {
        Path newPath = new Path(this);

        String path = CheckPart(child);
        if(newPath.data.isEmpty())
            newPath.data = path;
        else {
            if (path.startsWith("/"))
                newPath.data += path;
            else
                newPath.data += "/" + path;
        }

        return newPath;
    }

    /**
     * Append path
     * @param child the path to append
     * @return <code>Appended path</code>
     */
    public Path Append(@NonNull Path child) {
        return Append(child.data);
    }

    /**
     * Append path
     * @param child the path to append
     * @param child2 another path to append
     * @return <code>Appended path</code>
     */
    public Path Append(@NonNull Path child, @NonNull Path child2) {
        return Append(child.data).Append(child2.data);
    }

    /**
     * Get parent of path
     * @return <code>Path</code> of parent
     */
    public Path GetParent() {
        int i;
        if((i = data.lastIndexOf('/')) == -1)
            return this;

        return new Path(data.substring(0, i));
    }

    /**
     * Checks if file exists in filesystem
     * @return <code>true</code> if exists in filesystem,
     * <code>false</code> otherwise
     */
    public boolean Exists() {
        return new File(data).exists();
    }

    /**
     * Tests if path points to directory
     * @return <code>true</code> if path is a directory,
     * <code>false</code> otherwise
     */
    public boolean IsDirectory() { return new File(data).isDirectory(); }

    /**
     * Creates folder with all needed parents
     * @return <code>true</code> if folder was created,
     * <code>false</code> otherwise
     */
    public boolean Mkdirs() {
        return new File(data).mkdirs();
    }

    /**
     * Creates folder
     * @return <code>true</code> if folder was created,
     * <code>false</code> otherwise
     */
    public boolean Mkdir() {
        return new File(data).mkdir();
    }

    /**
     * Delete file or empty directory
     * @return <code>true</code> if file or folder was deleted,
     * <code>false</code> otherwise
     */
    public boolean Delete() { return new File(data).delete(); }

    /**
     * Get list of filenames in current directory
     * @return <code>Array of filenames</code> when pointing to directory,
     * <code>empty array</code> if not so,
     * <code>null</code> otherwise
     */
    public String[] List() { return new File(data).list(); }

    /**
     * Get name of destination (file or directory)
     * @return <code>destination name</code>
     */
    public String GetDestName() {
        int i = data.lastIndexOf('/');
        if(i == -1)
            return data;

        if(data.length() > i)
            return data.substring(i + 1);

        return "";
    }

    private static String CheckPart(String part) {
        StringBuilder out = new StringBuilder();
        boolean lastWasSlash = false;

        for(int i = 0; i < part.length(); i++) {
            char c = part.charAt(i);
            if(c == '/') {
                if(lastWasSlash) // Skip multiple slashes
                    continue;
                lastWasSlash = true;
            } else
                lastWasSlash = false;

            // Slash does not need to be verified
            /*if(!lastWasSlash && !IsCharValid(c))
                throw new Exception(c + " is not a valid path character");*/
            out.append(c);
        }

        if(out.toString().endsWith("/"))
            out.deleteCharAt(out.length() - 1);
        return out.toString();
    }

    /*private static boolean IsCharValid(char c)
    {
        // slash & numbers, Uppercase letters, normal letters
        return c > 0x2e && c < 0x3a || c > 0x40 && c < 0x5b || c > 0x60 && c < 0x7b;
    }*/
}
