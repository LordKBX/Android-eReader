package lordkbx.workshop.ereader.reader;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

import lordkbx.workshop.ereader.MainDrawerActivity;
import lordkbx.workshop.ereader.MyDatabaseHelper;

public class WebViewCallBackObject {
    private ReaderActivity parent;
    private Dictionary<Integer, Map<String, String>> tableOfContent = new Hashtable<Integer, Map<String, String>>();
    private int tableOfContentSize = 0;
    private int tableOfContentPosition = 0;
    private Map<String, Object> infos = new HashMap<String, Object>();
    private boolean firstLoad = true;
    private String bookId = "";
    private String fileId = "";
    private String viewMode = "";
    private float zoom = 1.0f;
    private int scroll = 0;
    private MyDatabaseHelper dbh;

    public WebViewCallBackObject(ReaderActivity parent) {
        this.parent = parent;
        dbh = MyDatabaseHelper.getInstance(parent);
    }

    public void setBookId(String book_id, String file_id){
        bookId = book_id;
        fileId = file_id;
    }

    public void setViewMode(String mode){
        viewMode = mode;
    }

    public void clearToc() {
        for (int i = this.tableOfContentSize - 1; i >= 0; i--) {
            this.tableOfContent.remove(i);
        }
        this.tableOfContentSize = 0;
        this.tableOfContentPosition = 0;
    }

    public void addToc(String id, String path, String title, String data) {
        addToc(id, path, title, data, false);
    }

    public void addToc(String id, String path, String title, String data, boolean hidden) {
        addToc(id, path, title, data, hidden, false, "");
    }
    public void addToc(String id, String path, String title, String data, boolean hidden, boolean relative, String anchor) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("id", id);
        map.put("path", path);
        map.put("title", title);
        map.put("data", data);
        map.put("hidden", hidden ? "true" : "false");
        map.put("relative", relative ? "true" : "false");
        map.put("anchor", anchor);
        this.tableOfContent.put(this.tableOfContentSize, map);
        this.tableOfContentSize += 1;
    }

    public int getTocSize() {
        return this.tableOfContentSize;
    }

    public int getTocPosition() {
        return this.tableOfContentPosition;
    }
    @JavascriptInterface
    public void setTocPosition(int position) {
        if (position < 0 || position >= this.tableOfContentSize) { return; }
        this.tableOfContentPosition = position;
    }

    public Map<String, String> getTocEntry() {
        return this.getTocEntry(this.tableOfContentPosition);
    }

    public Map<String, String> getTocEntry(int position) {
        return this.tableOfContent.get(position);
    }

    @JavascriptInterface
    public String getData() {
        try {
            if(firstLoad){ try{ recordPosition(); firstLoad = false; } catch (Exception error){} }
            return (String) this.tableOfContent.get(this.tableOfContentPosition).get("data");
        } catch (Exception err) {
            err.printStackTrace();
            Log.e("DEBUG", new Gson().toJson(this.tableOfContent));
            return "{}";
        }
    }
    @JavascriptInterface
    public void saveScroll(int scrollT) { scroll = scrollT; }
    @JavascriptInterface
    public void saveZoom(float zoomT) { zoom = zoomT; }
    @JavascriptInterface
    public float getScroll() { return scroll; }
    @JavascriptInterface
    public float getZoom() { return zoom; }

    @JavascriptInterface
    public String recordPosition() {
        dbh.eraseProgression(bookId, fileId);
        float izoom = zoom;
        int iscroll= scroll;
        if(viewMode.equals("CBZ")) { izoom = 1.0f; iscroll = 0; }
        dbh.newProgression(bookId, fileId, getTocPosition(), izoom, iscroll);
        //loadPositionRecord(false);
        return "null";
    }

    public String getData(int pos) {
        return (String) this.tableOfContent.get(pos).get("data");
    }

    private void loadPositionRecord(boolean apply){
        Cursor cur = null;
        cur = dbh.getReadableDatabase().rawQuery("SELECT chapter,zoom,scrollY FROM progessions WHERE book_id = ? AND guid_file = ?", new String[]{bookId, fileId});
        //cur = dbh.getReadableDatabase().rawQuery("SELECT chapter,zoom,scrollY FROM progessions LIMIT 1", null);
        if(cur != null){
            Log.e("loadPositionRecord", "Record found");
            cur.moveToFirst();
            int chapter = Integer.parseInt(cur.getString(cur.getColumnIndex("chapter")));
            String zoomC = cur.getString(cur.getColumnIndex("zoom"));
            String scrollY = cur.getString(cur.getColumnIndex("scrollY"));
            try { zoom = Float.parseFloat(zoomC); scroll = Integer.parseInt(scrollY); }
            catch (Exception err){ zoom = 1.0f; scroll = 0; err.printStackTrace(); }
            if(zoom < 0.5f){ zoom = 1.0f; }
            if(zoom > 5.0f){ zoom = 5.0f; }
            Log.e("DATA POSITION, CHAPTER", ""+chapter);
            Log.e("DATA POSITION, ZOOM", ""+zoom);
            Log.e("DATA POSITION, scrollY", ""+scrollY);
            if(viewMode.equals("EPUB")){
                if(apply)setTocPosition(chapter);
            }
        }
        else{
            Log.e("loadPositionRecord", "Record NOT found");
            if(apply)setTocPosition(0);
        }
    }

    @JavascriptInterface
    public String getUrl() {
        try {
            if(firstLoad){
                loadPositionRecord(true);
            }
            return (String) this.tableOfContent.get(this.tableOfContentPosition).get("path");
        } catch (Exception err) {
            return "";
        }
    }

    @JavascriptInterface
    public void logError(String msg) {
        Log.e("JS ERROR", msg);
    }

    public String getUrl(int pos) {
        return (String) this.tableOfContent.get(pos).get("path");
    }

    public void setInfo(String param, Object value) {
        this.infos.put(param, value);
    }

    @JavascriptInterface
    public String getInfo(String param) {
        return (String) this.infos.get(param);
    }

    @JavascriptInterface
    public String getAllInfo() {
        GsonBuilder gsonMapBuilder = new GsonBuilder();
        Gson gsonObject = gsonMapBuilder.create();
        return gsonObject.toJson(this.infos);
    }

    @JavascriptInterface
    public String toString() {
        return "ReaderActivityWebViewCallBackObject";
    }

    @JavascriptInterface
    public void event(String msg) {
        Log.d("DEBUG_APP", "Reader Activity WebView CallBackO with msg = " + msg);
        if (msg.equals("next")) {
            if (this.tableOfContentPosition + 1 <= this.tableOfContentSize) {
                this.tableOfContentPosition = this.tableOfContentPosition + 1;
            }
        } else if (msg.equals("previous")) {
            if (this.tableOfContentPosition > 0) {
                this.tableOfContentPosition = this.tableOfContentPosition - 1;
            }
        }
    }
}
