package lordkbx.workshop.ereader.reader;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.channels.FileLockInterruptionException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Files {
    public static List<String> parseFolder(File dir){
        List<String> ret = new ArrayList<String>();

        if(dir.isDirectory()){
            try{
                File[] files = dir.listFiles();
                for (int i = 0; i < files.length; i++) {
                    if(files[i].isDirectory()){
                        List<String> re = parseFolder(files[i]);
                        for (int j = 0; j < re.size(); j++) {
                            ret.add(re.get(j));
                        }
                    }
                    else{
                        ret.add(files[i].getAbsolutePath());
                    }
                }
            }
            catch (Exception error){
                Log.d("DEBUG_APP", "parseFolder error => " + error.getMessage());
            }
        }

        return ret;
    }

    // Method to encode a string value using `UTF-8` encoding scheme
    public static String encodePath(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("%2F", "/");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex.getCause());
        }
    }

    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);
        fileOrDirectory.delete();
    }

    public static String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try { while ((line = reader.readLine()) != null) { sb.append(line).append('\n'); } }
        catch (IOException e) { e.printStackTrace(); }
        finally { try { is.close(); } catch (IOException e) { e.printStackTrace(); } }
        return sb.toString();
    }

    public static String fileToString(String path) {
        try{ FileInputStream fi = new FileInputStream(path); return convertStreamToString(fi); }
        catch (Exception err){ return null; }
    }
}
