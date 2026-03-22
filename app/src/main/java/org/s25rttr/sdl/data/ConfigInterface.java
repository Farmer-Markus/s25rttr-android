package org.s25rttr.sdl.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * My approach on writing/reading a conf file.
 * Does not support nested configs, only groups!
 * 1 conf per line
 */
public class ConfigInterface {
    private HashMap<String, Group> data;
    private Group lastGroup;

    public ConfigInterface() {
        data = new HashMap<>();
    }

    /**
     * Load config from given file
     * @param file The path to the config file
     * @return <code>true</code> when successfully loaded config, <code>false</code> otherwise
     * @throws Exception On BufferedReader errors
     */
    public boolean LoadFromPath(Path file) throws Exception {
        if(file == null || !file.Exists()) return false;

        try(BufferedReader br = new BufferedReader(new FileReader(file.toString()))) {
            Group currCfg = null;
            String currLine;

            for(long lNum = 0; (currLine = br.readLine()) != null; lNum++) {
                if(currLine.length() <= 2) continue;

                int fc = currLine.indexOf('[');
                int lc = currLine.indexOf(']');
                if(fc != -1) {
                    if(lc > 0) {
                        // Found [ and ] -> create new sub config
                        currCfg = new Group();
                        data.put(currLine.substring(fc + 1, lc), currCfg);
                        continue;
                    }

                    throw new Exception("Found '[' without closing ']' at line " + lNum);
                }

                int sep = currLine.indexOf(':');
                if(sep == -1) continue; // Skip line if not found
                if(currCfg == null)
                    throw new Exception("Found data without group at line " + lNum);

                if(sep == 0)
                    throw new Exception("Found ':' without key value at line " + lNum);
                if(currLine.length() - sep - 1 == 0)
                    throw new Exception("Found key and ':' without value at line " + lNum);

                String key = currLine.substring(0, sep);
                if(currCfg.get(key) != null)
                    throw new Exception("Found already parsed key at line " + lNum);

                String var = currLine.substring(sep + 1);
                currCfg.put(key, var);
            }
        }

        return true;
    }

    /**
     * Saves config to given file. File will be overwritten or created.
     * @param file The Path to the file
     * @return <code>true</code> when written successfully, <code>false</code> otherwise
     * @throws Exception On BufferedWriter errors
     */
    public boolean SaveToPath(Path file) throws Exception {
        if(file == null) return false;
        if(file.Exists())
            new FileWriter(file.toString(), false).close(); // Overwrite file
        else
            file.CreateNewFile();

        try(BufferedWriter bw = new BufferedWriter(new FileWriter(file.toString()))) {
            for(Map.Entry<String, Group> entry : data.entrySet()) {
                if(entry.getValue().isEmpty()) continue;
                bw.write("[" + entry.getKey() + "]\n"); // Write group

                // Now write each entry
                WriteEntries(bw, entry.getValue());

                bw.write("\n"); // Space at end of group (just to look fancy :D)
            }
        }

        return true;
    }

    // Write all entries of config/group to file
    private void WriteEntries(final BufferedWriter bw, final Group cfg) throws Exception {
        for(Map.Entry<String,String> entry : cfg.entrySet()) {
            bw.write(entry.getKey() + ":" + entry.getValue() + "\n");
        }
    }

    private Group GetOrSetDefault(final String group) {
        Group g = data.get(group);
        if(g == null)
            data.put(group, g = new Group());
        return g;
    }

    /**
     * Get all key of all groups in config
     * @return all keys
     */
    public Set<String> GetGroupKeys() {
        return data.keySet();
    }

    /**
     * Set value to key
     * @param group The group to set key
     * @param key The key to store the value with
     * @param value The value to store
     */
    public void Put(final String group, final String key, final String value) {
        lastGroup = GetOrSetDefault(group);
        lastGroup.put(key, value);
    }

    /**
     * Set value to key
     * @param group The group to set key
     * @param key The key to store the value with
     * @param value The value to store
     */
    public void PutString(final String group, final String key, final String value) {
        Put(group, key, value);
    }

    /**
     * Set value to key
     * @param group The group to set key
     * @param key The key to store the value with
     * @param value The value to store
     */
    public void PutInt(final String group, final String key, final int value) {
        Put(group, key, String.valueOf(value));
    }

    /**
     * Set value to key
     * @param group The group to set key
     * @param key The key to store the value with
     * @param value The value to store
     */
    public void PutFloat(final String group, final String key, final float value) {
        Put(group, key, String.valueOf(value));
    }

    /**
     * Set value to key
     * @param group The group to set key
     * @param key The key to store the value with
     * @param value The value to store
     */
    public void PutBoolean(final String group, final String key, final boolean value) {
        Put(group, key, String.valueOf(value));
    }

    /**
     * Get value from key
     * @param group The group to use the key
     * @param key The key to get the value from
     * @return <code>value</code> Stored under the key or <code>null</code> if key not found
     */
    public String Get(final String group, final String key) {
        lastGroup = GetOrSetDefault(group);
        return lastGroup.get(key);
    }

    /**
     * Get value from group and key
     * @param group The group to use the key
     * @param key The key to get the value from
     * @param def The default value to return if key is not found
     * @return <code>value</code> or given <code>def</code> value if not found
     */
    public String GetString(final String group, final String key, final String def) {
        String val = Get(group, key);
        return val == null ? def : val;
    }

    /**
     * Get value from group and key
     * @param group The group to use the key
     * @param key The key to get the value from
     * @param def The default value to return if key is not found
     * @return <code>value</code> or given <code>def</code> value if not found
     */
    public int GetInt(final String group, final String key, final int def) {
        String val = Get(group, key);
        return val == null ? def : Integer.parseInt(val);
    }

    /**
     * Get value from group and key
     * @param group The group to use the key
     * @param key The key to get the value from
     * @param def The default value to return if key is not found
     * @return <code>value</code> or given <code>def</code> value if not found
     */
    public float GetFloat(final String group, final String key, final float def) {
        String val = Get(group, key);
        return val == null ? def : Float.parseFloat(val);
    }

    /**
     * Get value from group and key
     * @param group The group to use the key
     * @param key The key to get the value from
     * @param def The default value to return if key is not found
     * @return <code>value</code> or given <code>def</code> value if not found
     */
    public boolean GetBoolean(final String group, final String key, final boolean def) {
        String val = Get(group, key);
        return val == null ? def : Boolean.parseBoolean(val);
    }

    private static class Group extends HashMap<String, String> {
        public Group() {
            super();
        }
    }
}
