package org.s25rttr.sdl.data;


import android.support.annotation.NonNull;

import java.io.File;

// Simple path class (no java Path for compatibility)
public class Path
{
    private String data;
    // data will never end with '/'

    public Path() {}
    public Path(@NonNull Path path)
    {
        this.data = path.data;
    }

    public Path(@NonNull String path)
    {
        data = CheckPart(path);
    }

    @Override
    @NonNull
    public String toString()
    {
        return data;
    }

    /**
     * Append string to existing path
     * @param child the path to append
     * @param child2 another path to append
     * @return <code>Path</code>
     */
    public Path Append(@NonNull String child, @NonNull String child2)
    {
        Append(child);
        Append(child2);
        return this;
    }

    /**
     * Append string to existing path
     * @param child the path to append
     * @return <code>Path</code>
     */
    public Path Append(@NonNull String child)
    {
        String path = CheckPart(child);
        if(data.isEmpty())
            data = path;
        else
        {
            if (path.startsWith("/"))
                data += path;
            else
                data += "/" + path;
        }

        return this;
    }

    /**
     * Append path to existing path
     * @param child the path to append
     * @return <code>Path</code>
     */
    public Path Append(@NonNull Path child)
    {
        return Append(child.data);
    }

    /**
     * Append path to existing path
     * @param child the path to append
     * @param child2 another path to append
     * @return <code>Path</code>
     */
    public Path Append(@NonNull Path child, @NonNull Path child2)
    {
        return Append(child.data).Append(child2.data);
    }

    /**
     * Get parent of path
     * @return <code>Path</code> of parent
     */
    public Path GetParent()
    {
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
    public boolean Exists()
    {
        return new File(data).exists();
    }

    /**
     * Creates folder with all needed parents
     * @return <code>true</code> if folder was created,
     * <code>false</code> otherwise
     */
    public boolean Mkdirs()
    {
        return new File(data).mkdirs();
    }

    /**
     * Creates folder
     * @return <code>true</code> if folder was created,
     * <code>false</code> otherwise
     */
    public boolean Mkdir()
    {
        return new File(data).mkdir();
    }

    private static String CheckPart(String part)
    {
        StringBuilder out = new StringBuilder();
        boolean lastWasSlash = false;

        for(int i = 0; i < part.length(); i++)
        {
            char c = part.charAt(i);
            if(c == '/')
            {
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
