package lordkbx.workshop.ereader;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.provider.ContactsContract;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.preference.PreferenceManager;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

public class Storage {
    private static Context context;
    public static void setContext(Context ctx){
        context = ctx;
    }
    private static String[] hashErrorCodes = {"Execution Error", "Unknown Algorithm", "File not found", "File access error", "Digest error"};

    public static String cleanFileName(String input){
        StringBuffer buffer = new StringBuffer();
        for (char c : input.toCharArray()) {
            if (Character.isJavaIdentifierPart(c)) {
                buffer.append(c);
            }
        }
        if (buffer.length() == 0) {
            buffer.append(System.currentTimeMillis());
        }
        return buffer.toString();
    }

    public static String getAppStoragePath(){
        try{
            return getAppStoragePath(!PreferenceManager.getDefaultSharedPreferences(context).getString("storage", "MEMORY").equals("MEMORY"));
        }
        catch (Exception err){ err.printStackTrace(); }
        return null;
    }
    public static String getAppStoragePath(boolean extern){
        File[] directorys = context.getExternalMediaDirs();
        if(directorys.length > 1 && extern){ return directorys[1].getAbsolutePath(); }
        else{ return directorys[0].getAbsolutePath(); }
    }
    public static String getAppStoragePath(String sub){
        try{
            return getAppStoragePath(!PreferenceManager.getDefaultSharedPreferences(context).getString("storage", "MEMORY").equals("MEMORY"), sub);
        }
        catch (Exception err){ err.printStackTrace(); }
        return null;
    }
    public static String getAppStoragePath(boolean extern, String sub){
        File[] directorys = context.getExternalMediaDirs();
        String dir = "";
        if(directorys.length > 1 && extern){ dir = directorys[1].getAbsolutePath(); }
        else{ dir = directorys[0].getAbsolutePath(); }
        if(sub != null && !sub.trim().equals("")){
            dir += "/" + sub;
            File file = new File(dir);
            if(!file.exists()){ file.mkdirs(); }
        }
        return dir;
    }

    public static String getAppCachePath(){
        return getAppCachePath(null);
    }
    public static String getAppCachePath(String sub){
        try{
            return getAppCachePath(!PreferenceManager.getDefaultSharedPreferences(context).getString("storage", "MEMORY").equals("MEMORY"), sub);
        }
        catch (Exception err){ err.printStackTrace(); }
        return null;
    }
    public static String getAppCachePath(boolean extern, String sub){
        File[] directorys = context.getExternalCacheDirs();
        String dir = "";
        if(directorys.length > 1 && extern == true){ dir = directorys[1].getAbsolutePath(); }
        else{ dir = directorys[0].getAbsolutePath(); }
        if(sub != null && !sub.trim().equals("")){
            dir += "/" + sub;
            File file = new File(dir);
            if(!file.exists()){ file.mkdirs(); }
        }
        return dir;
    }

    public static String getSDPath(){
        File[] directorys = context.getExternalMediaDirs();
        if(directorys.length > 1){ return "/storage/"+directorys[1].getAbsolutePath().split("/")[2]; }
        else{ return "/storage/emulated/0"; }
    }

    public static boolean externalMemoryAvailable() {
        if(android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
            File[] directorys = context.getExternalMediaDirs();
            if(directorys.length > 1){ return true; }
            else{ return false; }
        }
        else{ return false; }
    }

    public static long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        Log.d("getAvailableInternalMemorySize", formatSize(availableBlocks * blockSize));
        return availableBlocks * blockSize;
    }

    public static long getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        Log.d("getTotalInternalMemorySize", formatSize(totalBlocks * blockSize));
        return totalBlocks * blockSize;
    }

    public static long getAvailableExternalMemorySize() {
        if (externalMemoryAvailable()) {
            StatFs stat = new StatFs(getSDPath());
            long blockSize = stat.getBlockSizeLong();
            long availableBlocks = stat.getAvailableBlocksLong();
            Log.d("getAvailableExternalMemorySize", formatSize(availableBlocks * blockSize));
            return availableBlocks * blockSize;
        } else {
            return -1;
        }
    }

    public static long getTotalExternalMemorySize() {
        if (externalMemoryAvailable()) {
            StatFs stat = new StatFs(getSDPath());
            long blockSize = stat.getBlockSizeLong();
            long totalBlocks = stat.getBlockCountLong();
            Log.d("getTotalExternalMemorySize", formatSize(totalBlocks * blockSize));
            return totalBlocks * blockSize;
        } else {
            return -1;
        }
    }

    public static String formatSize(long size) {
        String suffix = null;

        if (size >= 1024) {
            suffix = "KB";
            size /= 1024;
            if (size >= 1024) {
                suffix = "MB";
                size /= 1024;
                if (size >= 1024) {
                    suffix = "GB";
                    size /= 1024;
                    if (size >= 1024) {
                        suffix = "TB";
                        size /= 1024;
                    }
                }
            }
        }

        StringBuilder resultBuffer = new StringBuilder(Long.toString(size));

        int commaOffset = resultBuffer.length() - 3;
        while (commaOffset > 0) {
            resultBuffer.insert(commaOffset, ',');
            commaOffset -= 3;
        }

        if (suffix != null) resultBuffer.append(suffix);
        return resultBuffer.toString();
    }

    public static String getMimeType(String url) {
        Path path = new File(url).toPath();
        try {
            return Files.probeContentType(path);
        } catch (IOException e) {
            return null;
        }
    }

    public static boolean deleteFile(String path){
        try{ File file = new File(path); return file.delete(); }catch (Exception err){}
        return false;
    }

    public static boolean existFile(String path){
        try{ File file = new File(path); return file.exists(); }catch (Exception err){}
        return false;
    }

    public static boolean writeTextFile(String path, String data){
        try{
            File file = new File(path);
            if(!file.exists()){ file.createNewFile(); }
            FileWriter myWriter = new FileWriter(path);
            myWriter.write(data);
            myWriter.close();
            return true;
        }
        catch (Exception err){}
        return false;
    }

    public static void deleteDirectoryContent(String path){
        File directory = new File(path);
        if(directory.canRead()){
            File[] files = directory.listFiles();
            for(File file : files){
                Log.d("file = ", file.getAbsolutePath());
                if(!file.isFile()){ deleteDirectoryContent(file.getAbsolutePath()); }
                file.delete();
            }
        }
    }

    public static String fileHash(String filePath){
        // availlable MD2, MD5, SHA-1, SHA-256, SHA-384, SHA-512
        String[] rez = centralHash(filePath, "SHA-1");
        if(rez[1] == ""){ return rez[0]; }
        else{ return null; }
    }

    private static String[] centralHash(String filePath, String algo){
        FileInputStream fis;
        String[] ret = {"",""};
        try{
            filePath = filePath.replace("file:///", "/").replace("file:", "");

            fis = new FileInputStream(filePath);
            MessageDigest md = MessageDigest.getInstance(algo);
            byte[] dataBytes = new byte[1024];
            int nread = 0;
            while ((nread = fis.read(dataBytes)) != -1){ md.update(dataBytes, 0, nread); };
            byte[] mdbytes = md.digest();
            //convert the byte to hex format method 1
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < mdbytes.length; i++) { sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1)); }
            ret[0] = sb.toString();
            System.out.println(sb.toString());
            return ret;
        }
        catch(Exception ex){
            Log.e("File Hash Error", ex.getMessage());
            ex.printStackTrace();
            if(ex instanceof FileNotFoundException){
                File f = new File(filePath);
                if(f.exists()){ret[0] = Integer.toString(3); ret[1] = hashErrorCodes[3];}
                else{ret[0] = Integer.toString(2); ret[1] = hashErrorCodes[2];}
                return ret;
            }
            else{System.out.println(ex); ret[0] = Integer.toString(4); ret[1] = hashErrorCodes[4]; return ret;}
        }
    }
}
