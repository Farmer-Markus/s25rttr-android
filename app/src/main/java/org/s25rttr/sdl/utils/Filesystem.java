package org.s25rttr.sdl.utils;

import java.io.File;
import java.io.IOException;

public class Filesystem {

    public static boolean pathIsWritable(String path) {
        try {
            File file = new File(path + "testFile.txt");
            if(file.exists()) {
                file.delete();
                if(file.exists()) {
                    throw new IOException("Cannot delete file");
                }
            }
            file.createNewFile();
            if(file.exists()) return true;

        } catch (IOException e) {
            return false;
        }
        return false;
    }
}
