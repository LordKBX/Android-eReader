package lordkbx.workshop.ereader.reader;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.collection.ArrayMap;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.content.Intent;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import lordkbx.workshop.ereader.MainDrawerActivity;
import lordkbx.workshop.ereader.R;
import lordkbx.workshop.ereader.Storage;
import lordkbx.workshop.ereader.night;
import lordkbx.workshop.ereader.ui.library.LibraryFragment;
import lordkbx.workshop.ereader.utils;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.epub.EpubReader;

public class ReaderActivity extends AppCompatActivity{
    private static final boolean AUTO_HIDE = true;
    private static final int AUTO_HIDE_DELAY_MILLIS = 50000;
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private final Handler mNightHandler = new Handler();
    public WebView mContentView;
    private View.OnTouchListener touchListener;
    private int SideTouchZoneWidth = 0;
    private LinearLayout mTOCLayout;
    private ScrollView mTOCView;
    private BottomNavigationView mBottomNavigationView;
    private Toolbar mToolbar;
    private boolean mVisible;
    private Runnable mShowPart2Runnable;
    private Runnable mHideRunnable;
    private Runnable mHidePart2Runnable;

    private String viewMode = "CBZ";
    private DisplayMetrics displayMetrics = new DisplayMetrics();
    private WebViewCallBackObject eBookObj;

    private List<String> contentList = new ArrayList<String>();

    private MotionEvent.PointerCoords oldPtc1 = null;
    private ScaleGestureDetector mScaleGestureDetector;
    private float mScaleFactor = 1.0f;
    private final Handler mHandler = new Handler();

    public MainDrawerActivity parent;

    private String bookId = "";
    private String fileId = "";

    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            try{
                MotionEvent.PointerCoords ptc1 = new MotionEvent.PointerCoords();

                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        motionEvent.getPointerCoords(0, ptc1); oldPtc1 = ptc1;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        onTouchEvent(motionEvent);
                        break;

                    case MotionEvent.ACTION_UP:
                        mHandler.removeCallbacksAndMessages(null);
                        motionEvent.getPointerCoords(0, ptc1);
                        if(ptc1.x > SideTouchZoneWidth && ptc1.x < mContentView.getWidth() - SideTouchZoneWidth){
                            if(Math.abs(oldPtc1.x - ptc1.x) <= 10 && Math.abs(oldPtc1.y - ptc1.y) <= 10) { toggle(); }
                            else{ view.performClick(); }
                        }
                        else{
                            //if(viewMode == "CBZ"){
                            if (AUTO_HIDE && mBottomNavigationView.getVisibility() == View.VISIBLE) { delayedHide(AUTO_HIDE_DELAY_MILLIS); }
                            for(int i=0; i<50; i++){ mContentView.zoomOut(); }
                            if(motionEvent.getX(0) <= SideTouchZoneWidth){
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
                    default:
                        return false;
                }
            }
            catch (Exception err){
                Log.e("Error", err.getMessage());
                err.printStackTrace();
            }
            return false;
        }
    };

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        final float minScale = 0.5f;
        final float maxScale = 5.0f;
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            mScaleFactor *= scaleGestureDetector.getScaleFactor();
            mScaleFactor = Math.max(minScale, Math.min(mScaleFactor, maxScale));
            Log.e("NEW SCALE", ""+mScaleFactor);

            mContentView.evaluateJavascript("setZoom("+mScaleFactor+");", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String s) {}
            });

//            int w = (int) (mContentView.getWidth() * mScaleFactor);
//            int h = (int) (mContentView.getHeight() * mScaleFactor);
//            w = Math.min(Math.round(mContentView.getWidth() * minScale), Math.max(80, w));
//            h = Math.min(Math.round(mContentView.getHeight() * minScale), Math.max(80, h));
//            mImageView.setLayoutParams(new LinearLayout.LayoutParams(w, h));

            return true;
        }
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) { return super.onScaleBegin(detector); }
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) { super.onScaleEnd(detector); }
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        mScaleGestureDetector.onTouchEvent(motionEvent);
        return true;
    }

    private void onCreateSubInterfaceInit(){
        try{
            getWindow().setNavigationBarColor(getResources().getColor(R.color.statusBar, getTheme()));
            setContentView(R.layout.activity_reader);
            parent = (MainDrawerActivity)getParent();
            mScaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

            displayMetrics = getResources().getDisplayMetrics();
            SideTouchZoneWidth = (int)(displayMetrics.widthPixels * 0.07);

            mVisible = true;
            //mToolbar = (Toolbar) findViewById(R.id.toolbar);
            mContentView = (WebView) findViewById(R.id.fullscreen_content);
            mBottomNavigationView = (BottomNavigationView) findViewById(R.id.bottomNavigationView);
            mTOCLayout = (LinearLayout) findViewById(R.id.table_of_content_layout);
            mTOCView = (ScrollView) findViewById(R.id.table_of_content_view);
            mTOCView.setVisibility(View.GONE);

            mHideRunnable = new HideRunner(this);
            mHidePart2Runnable = new HideRunner2(mContentView);
            mContentView.setWebViewClient(new WebViewClient(){
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    String url = request.getUrl().toString();
                    Log.e("DEBUG UrlLoading", url);
                    if(url.startsWith("http://") || url.startsWith("https://")){ return true; }

                    return !url.contains("#");
                }
            });

            mContentView.getSettings().setAllowFileAccess(true);
            mContentView.setOnTouchListener(mDelayHideTouchListener);
            mContentView.getSettings().setAllowFileAccess(true);
        }
        catch (Exception ex){}
    }

    @SuppressLint("WrongThread")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try{
            super.onCreate(savedInstanceState);
            this.onCreateSubInterfaceInit();
            Intent intent = getIntent();
            String filePath = intent.getStringExtra(LibraryFragment.EXTRA_MESSAGE1);

            eBookObj = new WebViewCallBackObject(this);
            String[] nameTable = new File(filePath).getName().split("-");
            setTitle(nameTable[nameTable.length - 1]);
            String guidC = intent.getStringExtra(LibraryFragment.EXTRA_MESSAGE2);
            String guidF = intent.getStringExtra(LibraryFragment.EXTRA_MESSAGE3);
            if(guidC == null || guidC.trim().equals("")){ guidC = nameTable[nameTable.length - 1].replace('.','_'); }
            if(guidF == null || guidF.trim().equals("")){ guidF = nameTable[nameTable.length - 1].replace('.','_'); }
            bookId = guidC;
            fileId = guidF;
            eBookObj.setBookId(bookId, fileId);
            List<String> files = null;

            File cache_directory = new File(  Storage.getAppCachePath("tmp_reader") + "/" + guidC);
            //Files.deleteRecursive(cache_directory);
            try{ cache_directory.mkdirs(); } catch (Exception err){}
            //Storage.deleteDirectoryContent(cache_directory.getAbsolutePath());
            String fileHash = Storage.fileHash(filePath);
            if(fileHash == null){ finish(); }
            fileHash = fileHash.trim();
            Log.e("filePath", filePath);
            Log.e("fileHash =", fileHash);
            FileSystem fs = null;

            boolean cached = false;
            Storage.existFile(cache_directory.getAbsolutePath() + "/HASH");
            if(Storage.existFile(cache_directory.getAbsolutePath() + "/HASH")){
                String storedHash = "";
                try{ storedHash = utils.getStringFromFile(cache_directory.getAbsolutePath() + "/HASH").trim(); }
                catch (Exception err){}
                Log.e("storedHash =", storedHash);
                if(storedHash.equals(fileHash)){ cached = true; }
                else{ Storage.deleteDirectoryContent(cache_directory.getAbsolutePath()); }
            }
            Log.e("cached =", (cached)?"true":"false");

            if(filePath.toLowerCase().endsWith(".pdf")){// create a new renderer
                try{
                    viewMode = "CBZ";
                    if(!cached){
                        File file = new File(filePath);
                        Uri uri = Uri.fromFile(file);
                        Log.d("DEBUG_APP", "test uri = "+uri);
                        PdfRenderer renderer = new PdfRenderer(getContentResolver().openFileDescriptor(uri, "r"));
                        Log.d("DEBUG_APP", "PdfRenderer loaded "+renderer.toString());

                        // let us just render all pages
                        final int pageCount = renderer.getPageCount();
                        Log.d("DEBUG_APP", "PdfRenderer cout "+pageCount);
                        for (int i = 0; i < pageCount; i++) {
                            String fileEnd = cache_directory.getAbsolutePath() + "/" + "page";
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
                            Bitmap mBitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                            // say we render for showing on the screen
                            page.render(mBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                            FileOutputStream fileOuputStream = new FileOutputStream(fileEnd);
                            // do stuff with the bitmap
                            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOuputStream);
                            fileOuputStream.close();
                            // close the page
                            page.close();
                        }

                        // close the renderer
                        renderer.close();
                    }
                }
                catch (FileNotFoundException error){ Log.d("DEBUG_APP", "File not found"); }
                catch (IOException error){ Log.d("DEBUG_APP", "File not found(IOException)"); }
                //catch (Exception error){ Log.d("DEBUG_APP", "parse error => "+error.getMessage()+" => "+error.getStackTrace().toString()); }
            }
            if(filePath.toLowerCase().endsWith(".cbz") || filePath.toLowerCase().endsWith(".epub") || filePath.toLowerCase().endsWith(".epub2") || filePath.toLowerCase().endsWith(".epub3")){
                if(filePath.toLowerCase().endsWith(".cbz")){ viewMode = "CBZ"; }
                else{ viewMode = "EPUB"; }
                if(!cached){
                    try{
                        Log.e("DEBUG_APP", "Before unzip");
                        Unzip.UnzipFile(filePath, cache_directory);
                        Log.e("DEBUG_APP", "after unzip");
                    }
                    catch (IOException error){
                        Log.d("DEBUG_APP", "UNZIP ERROR => "+error.getMessage());
                    }
                }
            }
            eBookObj.setViewMode(viewMode);
            Storage.writeTextFile(cache_directory.getAbsolutePath() + "/HASH", fileHash);

            if(viewMode.equals("CBZ")){
                String block = "<div class=\"block\"><img src=\"{URI}\"/></div>";
                InputStream template = getResources().openRawResource(R.raw.cbz);
                String page = Files.convertStreamToString(template);
                page = page.replace("{BACKCOLOR}", ((filePath.toLowerCase().endsWith(".pdf"))?"#ffffff":"#000000;"));

                try{
                    files = Files.parseFolder(cache_directory);
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
            else if(viewMode.equals("EPUB")) {
                String guid = guidC;
                String title = "";
                String subDir = "";
                JSONArray tocList = new JSONArray();
                String READER_CACHE_INFO_FILE = cache_directory.getAbsolutePath() + "/READER_CACHE_INFO.JSON";

                if (!cached || !Storage.existFile(READER_CACHE_INFO_FILE)) {
                    try {
                        // find InputStream for book
                        InputStream epubInputStream = new FileInputStream(filePath);
                        // Load Book from inputStream
                        Book book = (new EpubReader()).readEpub(epubInputStream);
                        title = book.getTitle();
                        setTitle(title);

                        // Log the book's authors
                        Log.i("epublib", "author(s): " + book.getMetadata().getAuthors());

                        // Log the book's title
                        Log.i("epublib", "title: " + book.getTitle());

                        List<Resource> lr = book.getContents();
                        List<TOCReference> lt = book.getTableOfContents().getTocReferences();
                        String dataS = new Gson().toJson(lr);

                        String[] tabOpfPath = book.getOpfResource().getHref().replace(cache_directory.getAbsolutePath(), "").split("/");
                        if (tabOpfPath.length > 1) {
                            for (String section : tabOpfPath) {
                                if (section.toLowerCase().endsWith(".opf")) {
                                    break;
                                }
                                if (!subDir.equals("")) {
                                    subDir += "/";
                                }
                                subDir += section;
                            }
                            subDir += "/";
                        }
                        Log.e("subDir = ", "" + subDir);

                        String contentDirectory = cache_directory.getAbsolutePath() + "/" + subDir;
                        for (int i = 0; i < lr.size(); i++) {
                            String[] linkTab = lr.get(i).getHref().split("#");
                            String data = Files.fileToString(contentDirectory + linkTab[0]);
                            //data = data.replace("<head>", "<head><base href=\"file://" + cache_directory.getAbsolutePath() + "/" + subDir + "\"/>");
                            boolean hidden = true;
                            boolean relative = (linkTab.length > 1)?true:false;
                            String anchor = (linkTab.length > 1)?linkTab[1]:"";
                            String title2 = lr.get(i).getTitle();
                            for (int j = 0; j < lt.size(); j++) {
                                if (lt.get(j).getResourceId() == lr.get(i).getId()) {
                                    hidden = false;
                                    title2 = lt.get(j).getTitle();
                                    break;
                                }
                            }
                            if(title2 == null){ title2 = ""; }
                            JSONObject jo = new JSONObject();
                            try {
                                jo.put("id", lr.get(i).getId());
                                jo.put("href", contentDirectory + linkTab[0]);
                                jo.put("title2", title2);
                                jo.put("hidden", hidden);
                                jo.put("relative", relative);
                                jo.put("anchor", anchor);
                            } catch (Exception err) { }
                            tocList.put(jo);
                            eBookObj.addToc(lr.get(i).getId(), contentDirectory + linkTab[0], title2, data, hidden, relative, anchor);
                        }
                    }
                    catch (IOException e) {
                        Log.e("epublib", e.getMessage());
                    }
                    JSONObject jo = new JSONObject();
                    try {
                        jo.put("guid", guid);
                        jo.put("title", title);
                        jo.put("tocList", tocList);

                        Storage.writeTextFile(READER_CACHE_INFO_FILE, jo.toString());
                    }
                    catch (Exception err) {
                    }
                }
                else{
                    String cache_data = utils.getStringFromFile(READER_CACHE_INFO_FILE);
                    Log.e("cache_data", ""+cache_data);
                    try{
                        JSONObject obj = new JSONObject(cache_data);
                        guid = obj.getString("guid");
                        title = obj.getString("title");
                        tocList = obj.getJSONArray("tocList");
                        setTitle(title);

                        for (int i = 0; i < tocList.length(); i++) {
                            //Log.e("TOC LINE", ""+i);
                            JSONObject line = tocList.getJSONObject(i);
                            String data = Files.fileToString(line.getString("href"));
                            //data = data.replace("<head>", "<head><base href=\"file://" + cache_directory.getAbsolutePath() + "/" + subDir + "\"/>");
                            eBookObj.addToc(line.getString("id"), line.getString("href"),
                                    line.getString("title2"), data, line.getBoolean("hidden"));
                        }
                    }
                    catch (Exception err){
                        Log.e("ERROR", "Unable to read "+READER_CACHE_INFO_FILE);
                        Log.e("ERROR", err.getMessage());
                        Storage.deleteFile(READER_CACHE_INFO_FILE);
                        finish();
                    }
                }

                //Log.d("DEBUG_APP", "page => " + page);
                mContentView.getSettings().setLoadWithOverviewMode(true);
                mContentView.getSettings().setJavaScriptEnabled(true);
                //mContentView.getSettings().setBuiltInZoomControls(true);
                //mContentView.getSettings().setDisplayZoomControls(false);
                //mContentView.getSettings().setSupportZoom(true);
                mContentView.addJavascriptInterface(eBookObj, "parentView");
                InputStream template = getResources().openRawResource(R.raw.epub);
                String page = Files.convertStreamToString(template);
                mContentView.loadDataWithBaseURL(
                        "file:///android_asset/", page ,
                        "text/html", "utf-8",null
                );
            }
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }

    @Override
    public void onUserLeaveHint (){
        super.onUserLeaveHint();
    }

    @Override
    public void onBackPressed (){
        super.onBackPressed();

        try{
            Intent intentClose = new Intent();
            intentClose.putExtra("Code", 10);
            setResult(RESULT_OK, intentClose);
            finish();
        }
        catch (Exception err){}
    }

    public void buildTOC(){
        int max = this.eBookObj.getTocSize();
        if(max <= 0){ return; }
        this.mTOCLayout.removeAllViews();
        int lastId = 0;
        int pos = eBookObj.getTocPosition();
        int id0 = 888888;

        this.mTOCLayout.clearDisappearingChildren();
        // find last available entry closer of the current position
        for(int i=0; i<max; i++){
            if(i<=pos && !this.eBookObj.getTocEntry(i).get("hidden").equals("true")){ lastId = i; }
        }
        for(int i=0; i<max; i++){
            Map<String, String> chapter = this.eBookObj.getTocEntry(i);
            if(chapter.get("hidden").equals("true")){ continue; }
            //Log.e("DEBUG", new Gson().toJson(chapter));
            String path = chapter.get("path").toLowerCase();
            String[] tab = path.split("/");
            String FileName = tab[tab.length - 1];
            int last = FileName.lastIndexOf(".");
            FileName = FileName.substring(0, last);
            Button btnx = new Button(this);
            btnx.setWidth(mTOCLayout.getWidth());
            btnx.setHeight(50);
            if(i == 0){ btnx.setId(id0); }
            if(chapter.get("title") != null){ btnx.setText(chapter.get("title")); }
            else{ btnx.setText(FileName); }
            if(i == lastId){
                btnx.setBackgroundColor(getResources().getColor(R.color.light_blue_900, getTheme()));
                btnx.setTextColor(getResources().getColor(R.color.white, getTheme()));
            }
            else{
                btnx.setBackgroundColor(getResources().getColor(R.color.black_overlay, getTheme()));
                btnx.setTextColor(getResources().getColor(R.color.white, getTheme()));
            }
            btnx.setTag(i);
            if(viewMode.equals("CBZ")) {
                btnx.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int pos = (int) view.getTag();
                        mContentView.evaluateJavascript(
                            "position=" + pos + "; switchToPosition();",
                            new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String s) { hide(); }
                            }
                        );
                    }
                });
            }
            else if(viewMode.equals("EPUB")){
                btnx.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int pos = (int) view.getTag();
                        eBookObj.setTocPosition(pos);
                        mContentView.evaluateJavascript(
                            "load_page();",
                            new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String s) { hide(); }
                            }
                        );
                    }
                });
            }

            this.mTOCLayout.addView(btnx);
        }

        mHideHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int lastId = 0;
                int btnHeight = 50;
                int pos = eBookObj.getTocPosition();
                // find last available entry closer of the current position
                for(int i=0; i<max; i++){ if(i<=pos && !eBookObj.getTocEntry(i).get("hidden").equals("true")){ lastId = i; } }
                // get TOC button entry standard height(in px)
                try{ btnHeight = ((Button)mTOCLayout.findViewById(id0)).getHeight(); } catch (Exception err){ Log.e("ERROR", "get btnHeight"); }
                // calculate estimated vertical position in scrollview (top of button)
                int npos = lastId * btnHeight;
                // test if position is too low for justifying scroll
                if(npos < Math.round(mTOCView.getHeight() * 0.75)){ npos = 0; }
                else{
                    // calculate the number of button the TOC layout could display at the same time
                    int dpos = Integer.parseInt(""+Math.round(Float.parseFloat(""+mTOCView.getHeight()) / Float.parseFloat(""+btnHeight)));
                    // calculate new scroll position with a margin of 4 button + 1 for compensation padding of the scroll view
                    npos = ((lastId - dpos + 5) * btnHeight);
                }
                // set scroll new position
                mTOCView.scrollTo(0, npos);
            }
        }, UI_ANIMATION_DELAY);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        mBottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id== R.id.reader_menu_negatif){
                    mContentView.evaluateJavascript("negation();", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {}
                    });
                }
                else if (id== R.id.reader_menu_toc){
                    buildTOC();
                    mTOCView.setVisibility(View.VISIBLE);
                }
                else if (id== R.id.reader_menu_zoom_reset){
                    mContentView.evaluateJavascript("setZoom(1.0);", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {}
                    });
                }
                return true;
            }
        });

        try{ ((MainDrawerActivity)this.getParent()).loadingDialog.dismiss(); } catch (Exception err){}

        if(night.isNight(this)){
            mNightHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mContentView.evaluateJavascript("negation();", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {}
                    });
                }
            }, UI_ANIMATION_DELAY);
        }

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    public void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    public void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mTOCView.setVisibility(View.GONE);
        //mToolbar.setVisibility(View.GONE);
        mBottomNavigationView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    public void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        //        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        );
        //mToolbar.setVisibility(View.VISIBLE);
        mBottomNavigationView.setVisibility(View.VISIBLE);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
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