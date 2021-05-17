package lordkbx.workshop.ereader;

import android.annotation.SuppressLint;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.content.Intent;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


import lordkbx.workshop.ereader.ui.home.HomeFragment;

class ReaderActivityWebViewCallBackObject {
    private ReaderActivity parent = null;
    private Dictionary<Integer, Map<String, String>> tableOfContent = new Hashtable<Integer, Map<String, String>>();
    private int tableOfContentSize = 0;
    private int tableOfContentPosition = 0;
    private Map<String, Object> infos = new HashMap<String, Object>();

    public ReaderActivityWebViewCallBackObject(ReaderActivity parent){
        this.parent = parent;
    }

    public void clearToc(){
        for(int i=this.tableOfContentSize-1; i>=0; i--){ this.tableOfContent.remove(i); }
        this.tableOfContentSize = 0;
        this.tableOfContentPosition = 0;
    }
    public void addToc(String id, String path, String title, String data){
        Map<String, String> map = new HashMap<String, String>();
        map.put("id", id);
        map.put("path", path);
        map.put("title", title);
        map.put("data", data);
        this.tableOfContent.put(this.tableOfContentSize, map);
        this.tableOfContentSize += 1;
    }
    public int getTocSize(){ return this.tableOfContentSize; }
    public int getTocPosition(){ return this.tableOfContentPosition; }
    public void setTocPosition(int position){
        if(position < 0 || position >= this.tableOfContentSize){ return; }
        this.tableOfContentPosition = position;
    }
    public Map<String, String> getTocEntry(){ return this.getTocEntry(this.tableOfContentPosition); }
    public Map<String, String> getTocEntry(int position){ return this.tableOfContent.get(position); }

    @JavascriptInterface
    public String getData() { return (String)this.tableOfContent.get(this.tableOfContentPosition).get("data"); }
    public String getData(int pos) { return (String)this.tableOfContent.get(pos).get("data"); }

    @JavascriptInterface
    public String getUrl() { return (String)this.tableOfContent.get(this.tableOfContentPosition).get("path"); }
    public String getUrl(int pos) { return (String)this.tableOfContent.get(pos).get("path"); }

    public void setInfo(String param, Object value){ this.infos.put(param,value); }

    @JavascriptInterface
    public String getInfo(String param) { return (String)this.infos.get(param); }

    @JavascriptInterface
    public String getAllInfo() {
        GsonBuilder gsonMapBuilder = new GsonBuilder();
        Gson gsonObject = gsonMapBuilder.create();
        return gsonObject.toJson(this.infos);
    }

    @JavascriptInterface
    public String toString() { return "ReaderActivityWebViewCallBackObject"; }

    @JavascriptInterface
    public void event(String msg) {
        Log.d("DEBUG_APP", "Reader Activity WebView CallBackO with msg = "+msg);
        if(msg.equals("next")){
            if(this.tableOfContentPosition + 1 <= this.tableOfContentSize){
                this.tableOfContentPosition = this.tableOfContentPosition + 1;
            }
        }
        else if(msg.equals("previous")){
            if(this.tableOfContentPosition > 0){
                this.tableOfContentPosition = this.tableOfContentPosition - 1;
            }
        }
    }
}

public class ReaderActivity extends AppCompatActivity{
    private static final boolean AUTO_HIDE = true;
    private static final int AUTO_HIDE_DELAY_MILLIS = 50000;
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private WebView mContentView;
    private View.OnTouchListener touchListener;

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private LinearLayout mControlsView;
    private LinearLayout mTOCLayout;
    private ScrollView mTOCView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private String viewMode = "CBZ";
    private DisplayMetrics displayMetrics = new DisplayMetrics();
    private ReaderActivityWebViewCallBackObject eBookObj;

    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (AUTO_HIDE) {
                        delayedHide(AUTO_HIDE_DELAY_MILLIS);
                    }

                    view.getDisplay().getRealMetrics(displayMetrics);
                    int side = (int)(displayMetrics.widthPixels * 0.2);

                    if(motionEvent.getX(0) > side && motionEvent.getX(0) < mContentView.getWidth() - side){ view.performClick(); }
                    else{
                        //if(viewMode == "CBZ"){
                            hide();
                            for(int i=0; i<50; i++){ mContentView.zoomOut(); }
                            if(motionEvent.getX(0) <= side){
                                mContentView.evaluateJavascript("toLeft();", new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(String s) {}
                                });
                            }
                            else{
                                mContentView.evaluateJavascript("toRight();", new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(String s) {}
                                });
                            }
                        //}
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    String a = "";
                    break;
                default:
                    break;
            }
            return false;
        }
    };

    @SuppressLint("WrongThread")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        mVisible = true;
        mControlsView = (LinearLayout) findViewById(R.id.fullscreen_content_controls);
        mTOCLayout = (LinearLayout) findViewById(R.id.table_of_content_layout);
        mTOCView = (ScrollView) findViewById(R.id.table_of_content_view);
        mTOCView.setVisibility(View.GONE);

        eBookObj = new ReaderActivityWebViewCallBackObject(this);

        mContentView = (WebView) findViewById(R.id.fullscreen_content);

        mContentView.getSettings().setAllowFileAccess(true);
        mContentView.setOnTouchListener(mDelayHideTouchListener);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        Intent intent = getIntent();
        String message = intent.getStringExtra(HomeFragment.EXTRA_MESSAGE);
        Log.d("DEBUG_APP", message);

        setTitle(new File(message).getName());

        mContentView.getSettings().setAllowFileAccess(true);

        //clear cache dir
        File directory = new File(getDataDir().getAbsolutePath() + "/tmp_reader");
        deleteRecursive(directory);
        directory.mkdirs();
        List<String> files = null;

        if(message.toLowerCase().endsWith(".pdf")){// create a new renderer
            try{
                viewMode = "CBZ";
                File file = new File(message);
                Uri uri = Uri.fromFile(file);
                Log.d("DEBUG_APP", "test uri = "+uri);
                PdfRenderer renderer = new PdfRenderer(getContentResolver().openFileDescriptor(uri, "r"));
                Log.d("DEBUG_APP", "PdfRenderer loaded "+renderer.toString());

                // let us just render all pages
                final int pageCount = renderer.getPageCount();
                Log.d("DEBUG_APP", "PdfRenderer cout "+pageCount);
                for (int i = 0; i < pageCount; i++) {
                    String fileEnd = directory.getAbsolutePath() + "/" + "page";
                    if(i < 1000){
                        if(i < 100){
                            if(i < 10){ fileEnd = fileEnd + "000" + i; }
                            else{ fileEnd = fileEnd + "00" + i; }
                        }
                        else{ fileEnd = fileEnd + "0" + i; }
                    }
                    else{ fileEnd = fileEnd + "" + i; }
                    fileEnd = fileEnd + ".png";
                    PdfRenderer.Page page = renderer.openPage(i);
                    android.graphics.Bitmap mBitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                    // say we render for showing on the screen
                    page.render(mBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    FileOutputStream fileOuputStream = new FileOutputStream(fileEnd);
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOuputStream);
                    fileOuputStream.close();

                    // do stuff with the bitmap

                    // close the page
                    page.close();
                }

                // close the renderer
                renderer.close();
/*
                List<String> files = parseFolder(directory);
                for (int i = 0; i < files.size(); i++) {
                    Log.d("DEBUG_APP", files.get(i));
                }
 */
            }
            catch (FileNotFoundException error){ Log.d("DEBUG_APP", "File not found"); }
            catch (IOException error){ Log.d("DEBUG_APP", "File not found(IOException)"); }
            //catch (Exception error){ Log.d("DEBUG_APP", "parse error => "+error.getMessage()+" => "+error.getStackTrace().toString()); }
        }
        if(message.toLowerCase().endsWith(".cbz") || message.toLowerCase().endsWith(".epub") || message.toLowerCase().endsWith(".epub2") || message.toLowerCase().endsWith(".epub3")){
            if(message.toLowerCase().endsWith(".cbz")){ viewMode = "CBZ"; }
            else{ viewMode = "EPUB"; }

            try{
                Log.d("DEBUG_APP", "Before unzip");
                UnzipFile(message, directory);
                Log.d("DEBUG_APP", "after unzip");
            }
            catch (IOException error){
                Log.d("DEBUG_APP", "UNZIP ERROR => "+error.getMessage());
            }
        }

        if(viewMode.equals("CBZ")){
            String block = "<div class=\"block\"><img src=\"{URI}\"/></div>";
            InputStream template = getResources().openRawResource(R.raw.cbz);
            String page = convertStreamToString(template);
            page = page.replace("{BACKCOLOR}", ((message.toLowerCase().endsWith(".pdf"))?"#ffffff":"#000000;"));

            try{
                files = parseFolder(directory);
                int cout = 0;
                for (int i = 0; i < files.size(); i++) {
                    String path = files.get(i).toLowerCase();
                    if(path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg")){
                        Uri ur = Uri.fromFile(new File(files.get(i)));
                        Log.d("DEBUG_APP", ""+ur);
                        page = page.replace("{BLOCK}", block.replace("{URI}", ""+ur) + "{BLOCK}");
                        eBookObj.addToc("page_"+cout, path, "Page "+cout, "");
                        cout += 1;
                    }
                }
                page = page.replace("{PAGES}", ""+cout);
                page = page.replace("{BLOCK}", "");
            }
            catch (Exception error){
                Log.d("DEBUG_APP", "error CBZ File => " + error.getMessage());
            }

            //Log.d("DEBUG_APP", "page => " + page);
            mContentView.getSettings().setLoadWithOverviewMode(true);
            mContentView.getSettings().setJavaScriptEnabled(true);
            mContentView.getSettings().setBuiltInZoomControls(true);
            mContentView.getSettings().setDisplayZoomControls(false);
            mContentView.getSettings().setSupportZoom(true);
            mContentView.addJavascriptInterface(eBookObj, "parentView");
            mContentView.loadDataWithBaseURL(
                    "file:///android_asset/", page ,
                    "text/html", "utf-8",null
            );
        }
        else if(viewMode.equals("EPUB")){
            eBookObj.setInfo("lang", "");
            eBookObj.setInfo("guid", "");
            eBookObj.setInfo("title", "");
            eBookObj.setInfo("authors", "");
            eBookObj.setInfo("cover_path", "");
            String lang = "";
            String guid = "";
            String title = "";
            String authors = "";
            String cover_path = "";
            List<String> tags = new ArrayList<String>();

            try{
                files = parseFolder(directory);

                int mimeIndex = files.indexOf(directory.getAbsolutePath() + "/mimetype");
                if(mimeIndex == -1){ alertDialog("ERROR", "Invalid file format (1)"); return; }
                FileInputStream mimefile = new FileInputStream(directory.getAbsolutePath() + "/mimetype");
                String mimeContent = convertStreamToString(mimefile).trim();
                Log.d("DEBUG_APP", "mimeContent => '" + mimeContent + "'");
                if(!mimeContent.equals("application/epub+zip")){ alertDialog("ERROR", "Invalid file format (2)"); return; }

                int metaIndex = files.indexOf(directory.getAbsolutePath() + "/META-INF/container.xml");
                if(metaIndex == -1){ alertDialog("ERROR", "File entry point not found"); return; }
                FileInputStream indexfile = new FileInputStream(directory.getAbsolutePath() + "/META-INF/container.xml");

                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = builderFactory.newDocumentBuilder();
                Document indexDoc = docBuilder.parse(indexfile);
                String metadataFilePath = indexDoc.getElementsByTagName("rootfile").item(0).getAttributes().getNamedItem("full-path").getNodeValue();

                FileInputStream metafile = new FileInputStream(directory.getAbsolutePath() + "/" + metadataFilePath);
                Document mydoc = docBuilder.parse(metafile);

                try { lang = mydoc.getElementsByTagName("dc:language").item(0).getChildNodes().item(0).getNodeValue(); } catch (Exception error){}
                try { guid = mydoc.getElementsByTagName("dc:identifier").item(0).getChildNodes().item(0).getNodeValue(); } catch (Exception error){}
                try { title = mydoc.getElementsByTagName("dc:title").item(0).getChildNodes().item(0).getNodeValue(); } catch (Exception error){}
                try { authors = mydoc.getElementsByTagName("dc:creator").item(0).getChildNodes().item(0).getNodeValue(); } catch (Exception error){}
                try {
                    NodeList nodes = mydoc.getElementsByTagName("dc:subject");
                    for(int i=0; i<nodes.getLength(); i++){ tags.add(nodes.item(i).getChildNodes().item(0).getNodeValue()); }
                } catch (Exception error){}

                Log.d("DEBUG_APP", "lang => " + lang);
                Log.d("DEBUG_APP", "guid => " + guid);
                Log.d("DEBUG_APP", "title => " + title);
                Log.d("DEBUG_APP", "authors => " + authors);
                Log.d("DEBUG_APP", "tags => " + tags);

                String cover_id ="";
                NodeList metas = mydoc.getElementsByTagName("meta");
                for(int i=0; i<metas.getLength(); i++){
                    NamedNodeMap l = metas.item(i).getAttributes();
                    if(l.getNamedItem("cover") != null){
                        cover_id = l.getNamedItem("cover").getNodeValue();
                    }
                }
                Log.d("DEBUG_APP", "cover_id => " + cover_id);

                NodeList items = mydoc.getElementsByTagName("item");
                Map<String, String> chapters = new HashMap<String, String>();
                for(int i=0; i<items.getLength(); i++){
                    Node item = items.item(i);
                    NamedNodeMap l = item.getAttributes();
                    String mtype = l.getNamedItem("media-type").getNodeValue();
                    if(mtype.equals("image/jpeg") || mtype.equals("image/png")){
                        if(cover_path.equals("")){
                            if(!cover_id.equals("")){
                                if(l.getNamedItem("id").getNodeValue().equals(cover_id))
                                    { cover_path = l.getNamedItem("href").getNodeValue(); }
                            }
                            else{ cover_path = l.getNamedItem("href").getNodeValue(); }
                        }
                    }
                    else if(mtype.equals("application/xhtml+xml") || mtype.equals("text/html")){
                        chapters.put(l.getNamedItem("id").getNodeValue(), l.getNamedItem("href").getNodeValue());
                    }
                    else if(mtype.equals("application/x-dtbncx+xml")){// fichier .ncx Table of Content(if exist)
                        chapters.put(l.getNamedItem("id").getNodeValue(), l.getNamedItem("href").getNodeValue());
                    }

                }
                eBookObj.setInfo("lang", lang);
                eBookObj.setInfo("guid", guid);
                eBookObj.setInfo("title", title);
                eBookObj.setInfo("authors", authors);
                eBookObj.setInfo("cover_path", cover_path);
                eBookObj.setInfo("tags", tags);

                Log.d("DEBUG_APP", "eBookObj.infos => " + eBookObj.getAllInfo());

                Node spine = mydoc.getElementsByTagName("spine").item(0);
                //if(spine.getAttributes().getNamedItem("toc") == null){
                if(true){
                    NodeList itemrefs = mydoc.getElementsByTagName("itemref");
                    for(int i=0; i<itemrefs.getLength(); i++){
                        String id = itemrefs.item(i).getAttributes().getNamedItem("idref").getNodeValue();
                        Log.d("DEBUG_APP", "chapter id => " + id);
                        String path = directory.getAbsolutePath() + "/" + chapters.get(id);

                        FileInputStream chapterfile = new FileInputStream(path);

                        DocumentBuilderFactory chapterFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder chapterBuilder = chapterFactory.newDocumentBuilder();
                        Document chapterDoc = chapterBuilder.parse(chapterfile);
                        chapterfile.close();

                        String chapterTitle = null;
                        try{ chapterTitle = chapterDoc.getElementsByTagName("title").item(0).getChildNodes().item(0).getNodeValue(); }
                        catch (Exception err){}

                        chapterfile = new FileInputStream(path);
                        String data = this.convertStreamToString(chapterfile);
                        chapterfile.close();

                        eBookObj.addToc(id, path, chapterTitle, data);
                    }
                }
                else{
                    String tocId = spine.getAttributes().getNamedItem("toc").getNodeValue();
                }

            }
            catch (Exception error){
                Log.d("DEBUG_APP", "error EPUB => " + error.getMessage());
                Log.d("DEBUG_APP", "error EPUB => " + error.getStackTrace().toString());
            }

            //Log.d("DEBUG_APP", "page => " + page);
            mContentView.getSettings().setLoadWithOverviewMode(true);
            mContentView.getSettings().setJavaScriptEnabled(true);
            //mContentView.getSettings().setBuiltInZoomControls(true);
            //mContentView.getSettings().setDisplayZoomControls(false);
            //mContentView.getSettings().setSupportZoom(true);
            mContentView.addJavascriptInterface(eBookObj, "parentView");
            InputStream template = getResources().openRawResource(R.raw.epub);
            String page = convertStreamToString(template);
            mContentView.loadDataWithBaseURL(
                    "file:///android_asset/", page ,
                    "text/html", "utf-8",null
            );
        }

        mControlsView.removeView(findViewById(R.id.dummy_button));

        Button btn = new Button(this);
        btn.setWidth(100);
        btn.setHeight(mControlsView.getHeight());
        btn.setText("Contrast");
        btn.setBackgroundColor(getResources().getColor(R.color.black_overlay, getTheme()));
        btn.setTextColor(getResources().getColor(R.color.white, getTheme()));
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mContentView.evaluateJavascript("negation();", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String s) {}
                });
            }
        });
        mControlsView.addView(btn);


        Button btn2 = new Button(this);
        btn2.setWidth(100);
        btn2.setHeight(mControlsView.getHeight());
        btn2.setText("Table of content");
        btn2.setBackgroundColor(getResources().getColor(R.color.black_overlay, getTheme()));
        btn2.setTextColor(getResources().getColor(R.color.white, getTheme()));
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buildTOC();
                //GONE
                //VISIBLE
                mControlsView.setVisibility(View.GONE);
                mTOCView.setVisibility(View.VISIBLE);
            }
        });
        mControlsView.addView(btn2);

    }

    public void buildTOC(){
        int max = this.eBookObj.getTocSize();
        if(max <= 0){ return; }
        this.mTOCLayout.removeAllViews();

        this.mTOCLayout.clearDisappearingChildren();
        for(int i=0; i<max; i++){
            Map<String, String> chapter = this.eBookObj.getTocEntry(i);

            String path = chapter.get("path").toLowerCase();
            String[] tab = path.split("/");
            String FileName = tab[tab.length - 1];
            int last = FileName.lastIndexOf(".");
            FileName = FileName.substring(0, last);
            Button btnx = new Button(this);
            btnx.setWidth(mTOCLayout.getWidth());
            btnx.setHeight(50);
            if(chapter.get("title") != null){ btnx.setText(chapter.get("title")); }
            else{ btnx.setText(FileName); }
            btnx.setBackgroundColor(getResources().getColor(R.color.black_overlay, getTheme()));
            btnx.setTextColor(getResources().getColor(R.color.white, getTheme()));
            btnx.setTag(i);
            if(viewMode.equals("CBZ")) {
                btnx.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int pos = (int) view.getTag();
                        mContentView.evaluateJavascript("position=" + pos + "; switchToPosition(); ''+position;", new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String s) {
                                hide();
                            }
                        });
                    }
                });
            }
            else if(viewMode.equals("EPUB")){
                btnx.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int pos = (int) view.getTag();
                        eBookObj.setTocPosition(pos);
                        mContentView.evaluateJavascript("load_page();", new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String s) {
                                hide();
                            }
                        });
                    }
                });
            }

            this.mTOCLayout.addView(btnx);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mTOCView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    public static void UnzipFile(String fileZip, File destDir) throws IOException {
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(destDir, zipEntry);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                //Log.d("DEBUG_APP", "Extract file " + newFile);
                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

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
    private static String encodePath(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("%2F", "/");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex.getCause());
        }
    }

    private static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);
        fileOrDirectory.delete();
    }

    private String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    private void alertDialog(String title, String msg){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setMessage(msg);
        alertDialogBuilder.setPositiveButton("OK ", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }});

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}