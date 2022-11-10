package lordkbx.workshop.ereader;

import android.Manifest;
import android.app.Activity;

import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.LocaleList;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import lordkbx.workshop.ereader.ui.MultiSpinner;
import lordkbx.workshop.ereader.ui.sync.SyncFragment;
import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubReader;

public class MainDrawerActivity extends AppCompatActivity {
    public static final int PERMISSION_EXTERNAL_STORAGE = 1;
    private NavController navController;
    private AppBarConfiguration mAppBarConfiguration;
    public MyDatabaseHelper dbh;
    public AlertDialog loadingDialog;
    private BottomAppBar bottomBar;
    private SyncFragment sync;
    private String dialogRetText;

    private ImageView BookInfoCover = null;
    private String BookInfoCoverHiden = null;
    private boolean BookInfoCoverChanged = false;
    private static final int coverMaxWidth = 600;
    private ArrayMap<String, Integer> currentTags = null;
    private String fragmentName = "";
    private List<JSONObject> syncBooksList = new ArrayList<JSONObject>();

    private static MainDrawerActivity mInstance = null;

    public static MainDrawerActivity getInstance() {
        /**
         * use the application context as suggested by CommonsWare.
         * this will ensure that you dont accidentally leak an Activitys
         * context (see this article for more information:
         * http://developer.android.com/resources/articles/avoiding-memory-leaks.html)
         */
        if (mInstance == null) { mInstance = new MainDrawerActivity(); }
        return mInstance;
    }

    public void setFragmentName(String name){ this.fragmentName = name; }
    public void setCurrentTags(ArrayMap<String, Integer> tags){ this.currentTags = tags; }
    public void setSyncBooksList(List<JSONObject> books){ this.syncBooksList = books; }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mInstance = this;
        dbh = new MyDatabaseHelper(this);
        LocaleList slo = getApplication().getResources().getConfiguration().getLocales();
        String[] alo = getApplication().getResources().getStringArray(R.array.lang_values);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String rec = sharedPreferences.getString("lang", "en");
        String end = "en";
        if(rec.equals("auto")){
            for(int i=0; i<slo.size(); i++){
                boolean broken = false;
                for(int j=0; j<alo.length; j++){
                    if(j == 0){ continue; }
                    if(slo.get(i).getLanguage().equals(alo[j])){ end = alo[j]; broken = true; break; }
                }
                if(broken == true){break;}
            }
        }
        else{ end = rec; }
        MainDrawerActivity.updateLanguage(this, end);

        super.onCreate(savedInstanceState);
        getWindow().setNavigationBarColor(getResources().getColor(R.color.statusBar, getTheme()));
        Storage.setContext(this.getApplicationContext());

        setContentView(R.layout.activity_main_drawer);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_home, R.id.nav_library, R.id.nav_settings, R.id.nav_sync).setDrawerLayout(drawer).build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        String tx_version = getResources().getString(R.string.nav_header_subtitle).replace("X", BuildConfig.VERSION_NAME);
        ((TextView)navigationView.getHeaderView(0).findViewById(R.id.header_version_label)).setText(tx_version);

        bottomBar = (BottomAppBar)findViewById(R.id.bottomAppBar);
        hideBottomBar();
        bottomBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Log.d("onMenuItemClick","");
                if(item.getItemId() == R.id.sync_menu_download){
                    Log.d("onMenuItemClick","R.id.sync_menu_download");
                    sync.DownloadSelection();

                }
                else if(item.getItemId() == R.id.sync_menu_upload){
                    Log.d("onMenuItemClick","R.id.sync_menu_upload");
                    sync.UploadSelection();
                }
                else if(item.getItemId() == R.id.sync_menu_refresh){
                    Log.d("onMenuItemClick","R.id.sync_menu_refresh");
                    sync.connect();
                }
                else{return false;}
                return true;
            }
        });
        dialogRetText = "";

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_EXTERNAL_STORAGE );

        ActivityCompat.requestPermissions( this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_EXTERNAL_STORAGE );
    }

    public void setSyncFragment(SyncFragment sf){ sync = sf; }

    public void showBottomBar(){
        try{
            bottomBar.setVisibility(View.VISIBLE);
            LinearLayout bottomAppBarMemoryLayout = (LinearLayout)findViewById(R.id.bottomAppBarMemoryLayout);
            bottomAppBarMemoryLayout.setVisibility(View.VISIBLE);
        } catch (Exception err){}
    }

    public void hideBottomBar(){
        try{
            bottomBar.setVisibility(View.GONE);
            LinearLayout bottomAppBarMemoryLayout = (LinearLayout)findViewById(R.id.bottomAppBarMemoryLayout);
            bottomAppBarMemoryLayout.setVisibility(View.GONE);
        } catch (Exception err){}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_drawer, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public void importFile(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        if(MimeTypeMap.getSingleton().hasMimeType("application/x-cbz")){
            String[] mimetypes = {
                    "application/epub+zip", "application/pdf", "application/x-cbz",
                    "application/vnd.comicbook+zip", "application/zip"
            };
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        }
        startActivityForResult(intent, 7);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        Log.e("onActivityResult, requestCode", ""+requestCode);
        Log.e("onActivityResult, resultCode", ""+resultCode);
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        Uri uri = null;
        if (requestCode == 7) {
            if(resultData == null){ return; }
            if(resultCode != Activity.RESULT_OK){ return; }
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            if (resultData != null) {
                uri = resultData.getData();
                Log.w("DEBUG_APP, import file", "Uri: " + uri.toString());
                try{
                    int i;
                    String sourceFileName = getFileNameByUri(uri);
                    String ofte = sourceFileName.substring(sourceFileName.lastIndexOf("."));
                    String fte = ofte.toUpperCase();
                    List<String> files_types = Arrays.asList("EPUB", "EPUB2", "EPUB3", "PDF", "CBZ");
                    if (!files_types.contains(fte.replace(".", "").toUpperCase())) {
                        Log.w("DEBUG_APP, import file", "Error fileName");
                        Log.w("DEBUG_APP, import file", "fte = "+fte);
                        Log.w("DEBUG_APP, import file", "files_types = "+files_types.toString());
                        return;
                    }
                    String fileName = sourceFileName.substring(0, sourceFileName.lastIndexOf(".")).replace(" ", "_").replaceAll("[^\\x21-\\x7F]", "*");
                    dialogRetText = fileName;
                    ;
                    Log.d("DEBUG_APP", "name is " + fileName);
                    String destinationFilename = Storage.getAppStoragePath("books")+"/"+dialogRetText;
                    
                    while(Storage.existFile(destinationFilename)){
                        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
                        builder.setCancelable(false);
                        builder.setTitle("File name already exist !");
                        EditText input = new EditText(this);
                        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                        input.setText(dialogRetText);
                        builder.setView(input);

                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialogRetText = input.getText().toString();
                                dialog.dismiss();
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        AlertDialog diag = builder.create();
                        diag.show();
                        diag.wait();
                        destinationFilename = Storage.getAppStoragePath("books")+"/"+dialogRetText+fte;
                    }
                    destinationFilename = Storage.getAppStoragePath("books")+"/"+dialogRetText+fte;
                    fileName = dialogRetText;
                    Log.d("DEBUG_APP", "end name is " + destinationFilename);

                    BufferedInputStream bis = null;
                    BufferedOutputStream bos = null;

                    try {
                        bis = new BufferedInputStream(getContentResolver().openInputStream(uri));
                        bos = new BufferedOutputStream(new FileOutputStream(destinationFilename, false));
                        byte[] buf = new byte[1024];
                        bis.read(buf);
                        do {
                            bos.write(buf);
                        } while(bis.read(buf) != -1);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    } finally {
                        try {
                            if (bis != null) bis.close();
                            if (bos != null) bos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    int time = Integer.parseInt(""+(System.currentTimeMillis() / 1000));
                    String lang = "";
                    String guid = UUID.randomUUID().toString();
                    String title = fileName;
                    String authors = "";
                    String cover = "";
                    String publisher = "";
                    String synopsis = "";
                    String tags = "";

                    String size = Storage.formatSize(new File(destinationFilename).length());
                    Log.w("DEBUG_APP", "file size = " + size);
                    String mimeType = Storage.getMimeType(sourceFileName);
                    Log.w("DEBUG_APP", "mimeType = " + ((mimeType == null)?"null":mimeType));
                    //"application/epub+zip", "application/pdf", "application/x-cbz"
                    String format = (mimeType == null)?"CBZ":(mimeType.equals("application/epub+zip"))?"EPUB":((mimeType.equals("application/pdf"))?"PDF":"CBZ");
                    if(!MimeTypeMap.getSingleton().hasMimeType("application/x-cbz") && mimeType == null){
                        if(format == "CBZ"){
                            if(!destinationFilename.toLowerCase().endsWith(".cbz")){
                                Storage.deleteFile(destinationFilename);
                                return;
                            }
                            if(!Storage.isZip(destinationFilename)){
                                Storage.deleteFile(destinationFilename);
                                return;
                            }
                        }
                    }
                    String hash = Storage.fileHash(destinationFilename);


                    if(format.equals("EPUB")){
                        try {
                            // Load Book from inputStream
                            Book book = (new EpubReader()).readEpub(new FileInputStream(destinationFilename));
                            for(Author au : book.getMetadata().getAuthors()){
                                if(!authors.equals("")){ authors += ";"; }
                                authors += au.getFirstname() + " " + au.getLastname();
                            }
                            for(String au : book.getMetadata().getPublishers()){
                                if(!publisher.equals("")){ publisher += ";"; }
                                publisher += au;
                            }
                            for(String au : book.getMetadata().getDescriptions()){
                                if(!synopsis.equals("")){ synopsis += "\n\n"; }
                                synopsis += au;
                            }
                            for(String au : book.getMetadata().getSubjects()){
                                if(!tags.equals("")){ tags += ";"; }
                                tags += au;
                            }

                            title = book.getTitle();
                            lang = book.getMetadata().getLanguage();
                            Resource img = book.getCoverImage();
                            byte[] dataImg = img.getData();
                            Bitmap bm = BitmapFactory.decodeByteArray(dataImg, 0, dataImg.length);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bm.compress(Bitmap.CompressFormat.JPEG, 100, baos); // bm is the bitmap object
                            dataImg = baos.toByteArray();
                            cover = Base64.encodeToString(dataImg, Base64.DEFAULT);
                        } catch (Exception e) {
                            Log.e("epublib", e.getMessage());
                        }
                    }
                    else if(format.equals("PDF")){
                        try {
                            // Load Book from inputStream
                            Log.d("DEBUG_APP", "test uri = "+uri);
                            PdfRenderer renderer = new PdfRenderer(getContentResolver().openFileDescriptor(uri, "r"));
                            Log.d("DEBUG_APP", "PdfRenderer loaded "+renderer.toString());

                            PdfRenderer.Page page = renderer.openPage(0);
                            Bitmap mBitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                            // say we render for showing on the screen
                            page.render(mBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos); // bm is the bitmap object

                            byte[] dataImg = baos.toByteArray();
                            cover = Base64.encodeToString(dataImg, Base64.DEFAULT);
                            baos.close();
                            page.close();
                            renderer.close();
                        } catch (Exception e) {
                            Log.e("epublib", e.getMessage());
                        }
                    }
                    else if(format.equals("CBZ")){
                        try {
                            ZipInputStream zis = new ZipInputStream(new FileInputStream(destinationFilename));
                            ZipEntry zipEntry = zis.getNextEntry();
                            while (zipEntry.isDirectory()) { zipEntry = zis.getNextEntry(); }
                            byte[] buffer = new byte[1024];
                            ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
                            int read;
                            while((read = zis.read()) != -1) {
                                baos1.write(read);
                            }
                            zis.closeEntry();
                            zis.close();

                            byte[] dataImg = baos1.toByteArray();
                            Bitmap bm = BitmapFactory.decodeByteArray(dataImg, 0, dataImg.length);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bm.compress(Bitmap.CompressFormat.JPEG, 100, baos); // bm is the bitmap object
                            dataImg = baos.toByteArray();
                            cover = Base64.encodeToString(dataImg, Base64.DEFAULT);
                        } catch (Exception e) {
                            Log.e("epublib", e.getMessage());
                        }
                    }


                    dbh.newBook(
                            guid, title, authors, "", 0.0,
                            tags.toString(), synopsis, cover,
                            time, time , UUID.randomUUID().toString(), size, format, destinationFilename,
                            hash, "", time, time, 0, publisher, lang, 0
                    );
                }
                catch (Exception error){
                    Log.e("DEBUG_APP", error.getMessage());
                    error.printStackTrace();
                }
            }
        }
        else {
            if(requestCode == 2404 && resultData != null && resultData.getData() != null){
                uri = resultData.getData();
                Log.d("DEBUG_APP, import file", "Uri: " + uri.toString());

                try{
                    String dest = uri.toString().replace("file://", "").replace(" ", "_").replaceAll("[^\\x21-\\x7F]", "*");
                    FileInputStream fi = new FileInputStream(new File(dest));
                    ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
                    int read;
                    while((read = fi.read()) != -1) { baos1.write(read); }
                    fi.close();

                    byte[] dataImg = baos1.toByteArray();
                    baos1.close();

                    InputStream is = new ByteArrayInputStream(dataImg);

                    String cover = Base64.encodeToString(dataImg, Base64.DEFAULT);
                    BookInfoCoverHiden = cover;
                    BookInfoCover.setImageDrawable(Drawable.createFromStream(is, null));
                    BookInfoCoverChanged = true;
                    Storage.deleteFile(uri.toString().replace("file://", ""));
                }
                catch (Exception err){

                }

            }
            try{
                Log.e("onActivityResult, Gson result", new Gson().toJson(resultData));
            }
            catch (Exception err){ err.printStackTrace(); }
            if(loadingDialog != null){
                try{ loadingDialog.dismiss(); } catch (Exception err){}
            }
        }
    }

    public String getFileNameByUri(Uri uri){
        String fileName = null;
        if (uri.getScheme().equals("file")) {
            fileName = uri.getLastPathSegment();
        } else {
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(uri, new String[]{
                        MediaStore.Images.ImageColumns.DISPLAY_NAME
                }, null, null, null);

                if (cursor != null && cursor.moveToFirst()) {
                    fileName = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME));
                }
            } finally {

                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return fileName;
    }

    public static void updateLanguage(Context ctx, String lang){
        Configuration cfg = new Configuration();
        if (!TextUtils.isEmpty(lang))
            cfg.locale = new Locale(lang);
        else
            cfg.locale = Locale.getDefault();

        ctx.getResources().updateConfiguration(cfg, null);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        String id = "";
        try{ id = utils.getCheckboxesArray().getString(""+(int)v.getTag()); } catch (Exception err){}
        List<JSONObject> books = dbh.getBooks(id);
        super.onCreateContextMenu(menu, v, menuInfo);
        String fav = "";
        try{ fav = books.get(0).getString("fav_guid"); } catch (Exception err){}
        Log.e("Button", "fav1 = "+fav);
        int idP = Integer.parseInt((String) v.getTooltipText());
        Log.e("Button", "idP = "+idP);
        Log.e("Button", "R.id.library_flex_layout = "+R.id.library_flex_layout);
        Log.e("Button", "R.id.home_flex_layout_favs = "+R.id.home_flex_layout_favs);
        Log.e("Button", "R.id.home_flex_layout_recents = "+R.id.home_flex_layout_recents);

        Log.e("FRAGMENT NAME", fragmentName);

        // add menu items
        menu.add((int)v.getTag(), v.getId(), 0, R.string.book_ctm_info);

        if(idP == R.id.library_flex_layout || idP == R.id.home_flex_layout_recents){
            if(fav == null || fav.equals("")){ menu.add((int)v.getTag(), v.getId(), 0, R.string.book_ctm_fav_add); }
            else{ menu.add((int)v.getTag(), v.getId(), 0, R.string.book_ctm_fav_del); }
        }
        if(idP == R.id.home_flex_layout_favs){
            menu.add((int)v.getTag(), v.getId(), 0, R.string.book_ctm_fav_del);
        }

        if(idP == R.id.library_flex_layout || idP == R.id.sync_flex_layout){
            if(books != null && books.size() > 0){
                menu.add((int)v.getTag(), v.getId(), 0, R.string.book_ctm_del);
            }
        }
    }

    // menu item select listener
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Log.e("Button", "id = "+item.getItemId());
        String id = "";
        try{ id = utils.getCheckboxesArray().getString(""+item.getGroupId()); } catch (Exception err){}
        Log.e("Button", "book id = "+id);
        Log.e("Button", "title = \""+item.getTitle()+"\"");
        Log.e("Button", "group = \""+item.getGroupId()+"\"");

        if(item.getTitle().equals(getResources().getString(R.string.book_ctm_fav_add))){ dbh.addFavorite(id); this.recreate(); }
        if(item.getTitle().equals(getResources().getString(R.string.book_ctm_fav_del))){ dbh.delFavorite(id); this.recreate(); }
        if(item.getTitle().equals(getResources().getString(R.string.book_ctm_del))){ dbh.deleteBook(id); this.recreate(); }
        if(item.getTitle().equals(getResources().getString(R.string.book_ctm_info))){
            if(!fragmentName.equals("Sync")){ this.bookInfo(id); }
            else{ this.bookInfo(id, true); }
        }

        return true;
    }

    private void bookInfo(String bookID ){ this.bookInfo(bookID, false); }
    private void bookInfo(String bookID, boolean fromSync){
        try {
            final List<JSONObject> books = new ArrayList<JSONObject>();
            if(!fromSync) {
                try{ books.addAll(dbh.getBooks(bookID)); } catch (Exception ignored){}
                currentTags = dbh.getTags();
            }
            else {
                try{
                    for(int i = 0; i < syncBooksList.size(); i++){
                        JSONObject ob = syncBooksList.get(i);
                        if(ob.getString("guid").equals(bookID)){ books.add(ob); }
                    }
                }
                catch (Exception ignored){}
                // currentTags = dbh.getTags();
            }
            if(books.size() == 0){ return; }
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setTitle(R.string.book_ctm_info);
            ConstraintLayout mView = (ConstraintLayout) getLayoutInflater().inflate(R.layout.layaout_book_info,null);
            builder.setView(mView);

            ((TextView)mView.findViewById(R.id.layout_book_info_label_title)).setText(getResources().getString(R.string.dialog_book_info_title));
            ((TextView)mView.findViewById(R.id.layout_book_info_label_authors)).setText(getResources().getString(R.string.dialog_book_info_authors));
            ((TextView)mView.findViewById(R.id.layout_book_info_label_series)).setText(getResources().getString(R.string.dialog_book_info_series));
            ((TextView)mView.findViewById(R.id.layout_book_info_label_tags)).setText(getResources().getString(R.string.dialog_book_info_tags));
            ((TextView)mView.findViewById(R.id.layout_book_info_label_synopsis)).setText(getResources().getString(R.string.dialog_book_info_synopsis));
            ((Button)mView.findViewById(R.id.layout_book_info_button_download_cover)).setText(getResources().getString(R.string.dialog_book_info_import_cover));

            EditText fieldTitle = (EditText)mView.findViewById(R.id.layout_book_info_input_title);
            EditText fieldAuthors = (EditText)mView.findViewById(R.id.layout_book_info_input_authors);
            EditText fieldSeriesName = (EditText)mView.findViewById(R.id.layout_book_info_input_series_name);

            List<String> ntags = new ArrayList<String>();
            Collections.addAll(ntags, (String[]) currentTags.keySet().toArray(new String[]{}));

            if(!fromSync){
                ((Button)mView.findViewById(R.id.layout_book_info_button_download_cover)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ImagePicker.with(MainDrawerActivity.getInstance())
                                .crop()	    			//Crop image(Optional), Check Customization for more option
                                .compress(1024)			//Final image size will be less than 1 MB(Optional)
                                .maxResultSize(600, 600)	//Final image resolution will be less than 1080 x 1080(Optional)
                                .start();

                        ((EditText)mView.findViewById(R.id.layout_book_info_cover_hidden)).setText("");
                    }
                });

                ((Button)mView.findViewById(R.id.layout_book_info_button_tags_clear)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ((EditText)mView.findViewById(R.id.layout_book_info_input_tags)).setText("");
                        ((MultiSpinner)mView.findViewById(R.id.layout_book_info_spinner_tags)).unselectAll();
                    }
                });

                ((MultiSpinner)mView.findViewById(R.id.layout_book_info_spinner_tags)).setItems(
                        ntags, books.get(0).getString("tags"),
                        new MultiSpinner.MultiSpinnerListener() {
                            @Override
                            public void onItemsSelected(boolean[] selected) {
                                String tags = "";
                                for(int i = 0; i < ntags.size(); i++){
                                    if(selected[i]){
                                        if(!tags.equals("")){ tags += "\n"; }
                                        tags += ntags.get(i);
                                    }
                                }
                                ((EditText)mView.findViewById(R.id.layout_book_info_input_tags)).setText(tags);
                            }
                        }
                );

                ((Button)mView.findViewById(R.id.layout_book_info_button_tags_select)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ((MultiSpinner)mView.findViewById(R.id.layout_book_info_spinner_tags)).performClick();
                    }
                });

                fieldTitle.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) { }
                    @Override
                    public void afterTextChanged(Editable editable) { }
                    @Override
                    public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                        StringBuilder textBuiler = new StringBuilder(charSequence);
                        if(textBuiler.indexOf("\n") >= 0){
                            String text = textBuiler.toString();
                            text = text.replace("\r", "").replace("\n", "");
                            fieldTitle.setText(text);
                            fieldTitle.setSelection(start);
                        }
                    }
                });

                fieldAuthors.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) { }
                    @Override
                    public void afterTextChanged(Editable editable) { }
                    @Override
                    public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                        StringBuilder textBuiler = new StringBuilder(charSequence);
                        String tx1 = textBuiler.toString()
                                .replace("\r", "")
                                .replaceAll("[ ]{2,}/gm", " ")
                                .replace("\n \n", "\n\n");
                        if(tx1.contains("\n\n")){
                            fieldAuthors.setText(tx1.replace("\n\n", "\n"));
                            try{ fieldAuthors.setSelection(start); } catch (Exception ignored){}
                        }
                    }
                });

                fieldSeriesName.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) { }
                    @Override
                    public void afterTextChanged(Editable editable) { }
                    @Override
                    public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                        StringBuilder textBuiler = new StringBuilder(charSequence);
                        if(textBuiler.indexOf("\n") >= 0){
                            String text = textBuiler.toString();
                            text = text.replace("\r", "").replace("\n", "");
                            fieldSeriesName.setText(text);
                            fieldSeriesName.setSelection(start);
                        }
                    }
                });
            }
            else{
                ((Button)mView.findViewById(R.id.layout_book_info_button_download_cover)).setVisibility(View.GONE);
                ((Button)mView.findViewById(R.id.layout_book_info_button_tags_clear)).setVisibility(View.GONE);
                ((Button)mView.findViewById(R.id.layout_book_info_button_tags_select)).setVisibility(View.GONE);

                ((EditText)mView.findViewById(R.id.layout_book_info_input_title)).setEnabled(false);
                ((EditText)mView.findViewById(R.id.layout_book_info_input_authors)).setEnabled(false);
                ((EditText)mView.findViewById(R.id.layout_book_info_input_series_name)).setEnabled(false);
                ((EditText)mView.findViewById(R.id.layout_book_info_input_series_number)).setEnabled(false);
                ((EditText)mView.findViewById(R.id.layout_book_info_input_tags)).setEnabled(false);
                ((EditText)mView.findViewById(R.id.layout_book_info_input_synopsis)).setEnabled(false);
            }

            BookInfoCover = ((ImageView)mView.findViewById(R.id.layout_book_info_cover));
            BookInfoCoverHiden = "";
            BookInfoCoverChanged = false;
            Drawable drawable = utils.decodeDrawable(this, books.get(0).getString("cover"));
            if(drawable != null){
                BookInfoCoverHiden = books.get(0).getString("cover");
                BookInfoCover.setImageDrawable(drawable);
            }

            fieldTitle.setText(books.get(0).getString("title"));
            fieldAuthors.setText(books.get(0).getString("authors").replace(";", "\n"));
            fieldSeriesName.setText(books.get(0).getString("series"));
            ((EditText)mView.findViewById(R.id.layout_book_info_input_series_number)).setText(books.get(0).getString("series_vol"));
            ((EditText)mView.findViewById(R.id.layout_book_info_input_tags)).setText(books.get(0).getString("tags").replace(";", "\n"));
            String syp = books.get(0).getString("synopsis");
            ((EditText)mView.findViewById(R.id.layout_book_info_input_synopsis)).setText((syp.equals("null"))?"":books.get(0).getString("synopsis"));

            if(!fromSync) {
                builder.setPositiveButton(getResources().getString(R.string.dialog_save), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String title = ((EditText) mView.findViewById(R.id.layout_book_info_input_title)).getText().toString();
                        String authorString = ((EditText) mView.findViewById(R.id.layout_book_info_input_authors)).getText().toString();
                        String series_name = ((EditText) mView.findViewById(R.id.layout_book_info_input_series_name)).getText().toString();
                        double series_vol = Double.parseDouble(((EditText) mView.findViewById(R.id.layout_book_info_input_series_number)).getText().toString());
                        String tagsString = ((EditText) mView.findViewById(R.id.layout_book_info_input_tags)).getText().toString();
                        String synopsis = ((EditText) mView.findViewById(R.id.layout_book_info_input_synopsis)).getText().toString();
                        String cover = BookInfoCoverHiden;

                        List<String> lsa = new ArrayList<String>();
                        for (String author : authorString.split("\n")) {
                            lsa.add(author.trim());
                        }
                        lsa.sort(new StringComparator());
                        String authors = String.join(";", lsa.toArray(new String[]{}));

                        List<String> lst = new ArrayList<String>();
                        for (String tag : tagsString.split("\n")) {
                            lst.add(tag.trim());
                        }
                        lst.sort(new StringComparator());
                        String[] tags = lst.toArray(new String[]{});

                        try {
                            if (!books.get(0).getString("title").equals(title)) {
                                dbh.updateBookTitle(bookID, title);
                            }
                            if (!books.get(0).getString("authors").equals(authors)) {
                                dbh.updateBookAuthors(bookID, authors);
                            }
                            if (!books.get(0).getString("series").equals(series_name) || Double.parseDouble(books.get(0).getString("series_vol")) != series_vol) {
                                dbh.updateBookSeries(bookID, series_name, series_vol);
                            }
                            if (!books.get(0).getString("tags").replace(";", "\n").equals(tagsString)) {
                                dbh.updateBookTags(bookID, tags);
                            }
                            if (!books.get(0).getString("synopsis").equals(synopsis)) {
                                dbh.updateBookSynopsis(bookID, synopsis);
                            }
                            if (BookInfoCoverChanged) {
                                dbh.updateBookCover(bookID, "data:image/jpeg;base64," + cover);
                            }
                        } catch (Exception err) {
                        }

                        dialog.dismiss();
                        BookInfoCoverHiden = "";
                        BookInfoCover = null;
                        recreate();
                    }
                });
                builder.setNegativeButton(getResources().getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
            }
            else{
                builder.setNegativeButton(getResources().getString(R.string.dialog_close), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
            }

            AlertDialog diagI = builder.create();
            diagI.show();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
