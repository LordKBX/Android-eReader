package lordkbx.workshop.ereader.ui.sync;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.ArrayList;
import java.util.List;

import lordkbx.workshop.ereader.MainDrawerActivity;
import lordkbx.workshop.ereader.Storage;
import lordkbx.workshop.ereader.R;
import lordkbx.workshop.ereader.SEARCH_TYPE;
import lordkbx.workshop.ereader.extendVolley.InputStreamRequest;
import lordkbx.workshop.ereader.extendVolley.NukeSSLCerts;
import lordkbx.workshop.ereader.utils;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.flexbox.FlexboxLayout;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

class APIClient{
    String host = null;
    int port = 33004;
    Message myMessage;
    SyncFragment frag;
    PipedReader reader;
    RequestQueue queue;
    String Host;
    String Port;

    public APIClient(SyncFragment parent, PipedReader pipedReader){
        frag = parent;
        reader = pipedReader;
        NukeSSLCerts.nuke();
        CookieManager manager = new CookieManager();
        CookieHandler.setDefault( manager  );
        queue = Volley.newRequestQueue(parent.getContext());
    }
    public void setHost(String host, String port){
        Host = host; Port = port;
    }

    public void Get(String url){
        this.Get(url, null);
    }
    public void Get(String url, CallBack callback){
        try{
            frag.log("GET URL => "+url);
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            frag.log("Response is: " + response);
                            try {if(callback != null){ callback.run(response, url); }}
                            catch (Exception err){}
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    frag.log("That didn't work!");
                    try {if(callback != null){ callback.error(error.getMessage()); }}
                    catch (Exception err){}
                }
            });

            queue.add(stringRequest);
        }
        catch (Exception err){ frag.log(err.getMessage()); }
    }
    public void Post(String url, String data, CallBack callback){
        /* "application/octet-stream" */
        try{
            frag.log("POST URL => "+url);
            StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        // response
                        frag.log("Response is: " + response);
                        try {if(callback != null){ callback.run(response, url); }}
                        catch (Exception err){
                            Log.e("API POST", ""+err.getMessage());
                            err.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        frag.log("Error: "+error.getMessage());
                        try {if(callback != null){ callback.error(""+error.getMessage()); }}
                        catch (Exception err){
                            Log.e("API POST", ""+err.getMessage());
                            err.printStackTrace();
                        }
                    }
                }
            ) {
                @Override
                public byte[] getBody() throws AuthFailureError {
                    try {
                        return data == null ? null : data.getBytes("utf-8");
                    } catch (UnsupportedEncodingException uee) {
                        return null;
                    }
                }
            };

            queue.add(postRequest);
        }
        catch (Exception err){ Log.e("API POST", err.getMessage()); }
    }

    public void Download(String url, String FileName, CallBack callback){
        InputStreamRequest request = new InputStreamRequest(Request.Method.GET, url,
            new Response.Listener<byte[]>() {
                @Override
                public void onResponse(byte[] response) {
                    // TODO handle the response
                    try {
                        if (response!=null) {
                            FileOutputStream outputStream;
                            String name=FileName;
                            FileOutputStream stream = new FileOutputStream(name);
                            stream.write(response);
                            stream.close();
                            try {if(callback != null){ callback.run("Download:OK", url+"<>"+FileName); }}
                            catch (Exception err){ err.printStackTrace(); callback.error(err.getMessage());}
                        }
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        Log.d("KEY_ERROR", "UNABLE TO DOWNLOAD FILE");
                        e.printStackTrace();
                        try {if(callback != null){ callback.error(e.getMessage()); }}
                        catch (Exception err){}
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // TODO handle the error
                    error.printStackTrace();
                }
            },null
        );
        queue.add(request);
    }

    public void Login(String user, String password){ Login(user, password, null); }
    public void Login(String user, String password, CallBack callback){
        try{
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("user", user);
            jsonBody.put("password", password);

            Post("https://" + Host + ":" + Port + "/login", jsonBody.toString(), callback);
        }
        catch (Exception err){ callback.error(err.getMessage()); }
    }

    public void getBooks(){ getBooks(null, null, null, null); }
    public void getBooks(CallBack callback){ getBooks(callback, null, null, null); }
    public void getBooks(CallBack callback, String id){ getBooks(callback, id, null, null); }
    public void getBooks(CallBack callback, String id, String search){ getBooks(callback, id, search, null); }
    public void getBooks(CallBack callback, String id, String search, SEARCH_TYPE search_type){
        try {
            if(callback == null){
                callback = new CallBack() {
                    @Override
                    public void run(String body, String header) {
                        frag.log(body);
                        try{
                            JSONObject json = (JSONObject) new JSONTokener(body).nextValue();
                        }
                        catch (Exception err){ err.printStackTrace(); }
                    }

                    @Override
                    public void error(String error) {
                        Log.e("ERROR", error);
                    }
                };
            }
            Get("https://" + Host + ":" + Port + "/list/books", callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void log(String msg){
        frag.log(msg);
    }
}

public class SyncFragment extends Fragment {
    View.OnClickListener btnListener;
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    private static File directory;
    private static View root;
    private static MainDrawerActivity parent;
    private static FlexboxLayout mainLayout;

    SyncFragment frag;
    PipedReader r;
    PipedWriter w;
    APIClient client;

    SharedPreferences sharedPreferences;
    String Host;
    String Port;
    String User;
    String Password;

    JSONObject dataBooks;
    JSONObject dataBooksFiles;
    JSONObject dataBooksCovers;
    JSONObject layoutList;
    JSONObject checkboxes;
    int lastid = 24999;

    int dowloadFileIndex = 0;
    int dowloadFileCount = 0;
    int dowloadFileErrors = 0;
    List<String> downloadList = new ArrayList<String>();
    List<JSONObject> downloadListFiles = new ArrayList<JSONObject>();
    AlertDialog downloadDialog = null;

    @Override
    public void onResume() {
        super.onResume();
        try{ ((MainDrawerActivity)this.getActivity()).showBottomBar(); } catch (Exception err){}
        updateMemorySize();

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    public void log(String message){ Log.d("DEBUG_APP", ""+message); }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_sync, container, false);
        parent = (MainDrawerActivity)getActivity();
        parent.setSyncFragment(this);
        frag = this;
        r = new PipedReader();
        w = new PipedWriter();
        try{ w.connect(r); } catch (IOException err){}
        super.onCreate(savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());

        btnListener = new View.OnClickListener(){
            public void onClick(View v) {
                final List<String> data = (List<String>) v.getTag();
                Log.d("DEBUG_APP", data.toString());
            }
        };
        mainLayout = (FlexboxLayout) root.findViewById(R.id.sync_flex_layout);

        Host = sharedPreferences.getString("server_address", "");
        Port = sharedPreferences.getString("server_port", "");
        User = sharedPreferences.getString("server_user", "");
        Password = sharedPreferences.getString("server_password", "");
        dataBooks = null;
        client = new APIClient(frag, r);
        client.setHost(Host, Port);
        connect();
        try{ ((MainDrawerActivity)this.getActivity()).showBottomBar(); } catch (Exception err){}
        updateMemorySize();
        return root;
    }

    public void updateMemorySize(){
        String memoryPref = sharedPreferences.getString("storage", "MEMORY");
        long ogt = 0, oga = 0; int gt = 0, ga = 0;
        if(memoryPref.equals("SD")){
            Log.d("memoryPref", memoryPref);
            if(Storage.externalMemoryAvailable()){
                ogt = Storage.getTotalExternalMemorySize();
                oga = Storage.getAvailableExternalMemorySize();
            }
            else{ memoryPref = "MEMORY"; sharedPreferences.edit().putString("storage", "MEMORY").commit(); }
        }
        if(memoryPref.equals("MEMORY")){
            ogt = Storage.getTotalInternalMemorySize();
            oga = Storage.getAvailableInternalMemorySize();
        }
        gt = (int)Math.round(ogt / 1024);
        ga = (int)Math.round(oga / 1024);
        ProgressBar pb = (ProgressBar)this.getActivity().findViewById(R.id.bottomAppBarMemoryProgressBar);
        pb.setMax(gt);
        pb.setProgress(gt-ga);
        TextView tv = (TextView) this.getActivity().findViewById(R.id.bottomAppBarMemoryValues);
        tv.setText(Storage.formatSize(ogt-oga)+" / "+ Storage.formatSize(ogt));
    }

    public void connect(){
        loadingScreen();
        this.log("LOG START");
        client.Login(User, Password,
            new CallBack() {
                @Override
                public void run(String body, String header) {
                    frag.log(body);
                    client.getBooks(new CallBack() {
                        @Override
                        public void run(String body, String header) {
                            mainLayout.removeAllViews();
                            try{
                                utils.resetCheckboxesArray();
                                checkboxes = new JSONObject();
                                dataBooks = (JSONObject) new JSONTokener(body).nextValue();
                                dataBooksFiles = new JSONObject();
                                dataBooksCovers = new JSONObject();
                                layoutList = new JSONObject();
                                Log.d("API, OBJECT DATA TYPE", dataBooks.get("Data").getClass().getName());
                                if(dataBooks.get("Data").getClass().getName().equals("org.json.JSONArray")){
                                    JSONArray list = (JSONArray) dataBooks.get("Data");

                                    dowloadFileIndex = 0;
                                    dowloadFileCount = 0;
                                    dowloadFileErrors = 0;
                                    downloadList = new ArrayList<String>();

                                    for(int i=0; i<list.length(); i++){
                                        JSONObject book = (JSONObject)list.get(i);
                                        LinearLayout book_case = utils.bookCase(
                                            parent,
                                            mainLayout,
                                            book.getString("guid"),
                                            book.getString("title"),
                                            false, true
                                        );
                                        layoutList.put(book.getString("guid"), book_case.getId());
                                        mainLayout.addView(book_case);
                                        String cacheFile = Storage.getAppCachePath("covers") + '/' + book.getString("guid");
                                        if(Storage.existFile(cacheFile)){
                                            File file = new File(cacheFile);
                                            String line = "", cover = "";
                                            BufferedReader br = new BufferedReader(new FileReader(file));
                                            while ((line = br.readLine()) != null) { cover += line; }
                                            br.close();
                                            utils.replaceBookCaseCover(book_case, cover);
                                            dataBooksCovers.put(book.getString("guid"), cover);
                                        }
                                        else{ downloadList.add(book.getString("guid")); }
                                    }
                                    loadCoverNext();
                                }
                                checkboxes = utils.getCheckboxesArray();
                            }
                            catch (Exception err){
                                errorScreen(err.getMessage());
                            }
                        }

                        @Override
                        public void error(String error) {
                            errorScreen(error);
                        }
                    });
                }
                @Override
                public void error(String error) {
                    errorScreen(error);
                }
            }
        );
    }

    public void loadingScreen(){
        mainLayout.removeAllViews();
        LinearLayout ll = new LinearLayout(this.getContext());
        ll.setMinimumWidth(mainLayout.getWidth());
        ll.setGravity(Gravity.CENTER_HORIZONTAL);
        ll.setOrientation(LinearLayout.HORIZONTAL);

        ProgressBar pb = new ProgressBar(this.getContext(), null, android.R.attr.progressBarStyleLarge);
        pb.measure(200, 200);

        TextView tv = new TextView(this.getContext());
        tv.setMinHeight(200);
        tv.setMaxHeight(200);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setText(R.string.sync_loading);

        ll.addView(pb);
        ll.addView(tv);
        mainLayout.addView(ll);
    }

    public void errorScreen(String message){
        Log.e("ERROR SCREEN", message);
        mainLayout.removeAllViews();
        LinearLayout ll = new LinearLayout(this.getContext());
        ll.setMinimumWidth(mainLayout.getWidth());
        ll.setMinimumHeight(mainLayout.getHeight());
        ll.setGravity(Gravity.CENTER_HORIZONTAL);
        ll.setOrientation(LinearLayout.HORIZONTAL);

        ImageView iv = new ImageView(this.getContext());
        iv.setImageResource(android.R.drawable.alert_light_frame);

        TextView tv = new TextView(this.getContext());
        tv.setMinHeight(200);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setText(message);

        ll.addView(iv);
        ll.addView(tv);
        mainLayout.addView(ll);
    }

    private void loadCoverNext(){
        if(getMemoryBooks() == null){ return; }
        if(downloadList.size() <= 0){ return; }
        if(dowloadFileIndex >= downloadList.size()){ return; }
        client.Get("https://" + Host + ":" + Port + "/book/cover/"+downloadList.get(dowloadFileIndex),
            new CallBack() {
                @Override
                public void run(String body, String header) {
                    try {
                        header = utils.trimStringByString(header, "/").trim();
                        String[] taburl = header.split("/");
                        String bookId = taburl[taburl.length - 1];
                        JSONObject data = (JSONObject) new JSONTokener(body).nextValue();
                        String cover = data.getString("Data");
                        dataBooksCovers.put(bookId, cover);
                        utils.replaceBookCaseCover(mainLayout.findViewById(layoutList.getInt(bookId)), cover);

                        FileWriter fw = new FileWriter(Storage.getAppCachePath("covers") + '/' + bookId);
                        fw.append(cover);
                        fw.close();
                    }
                    catch (Exception err){
                        err.printStackTrace();
                    }
                    loadCoverNext();
                }

                @Override
                public void error(String error) {
                    loadCoverNext();
                }
            }
        );

        dowloadFileIndex += 1;
    }

    public JSONArray getMemoryBooks(){
        if(dataBooks == null){ return null; }
        try{
            if(dataBooks.get("Data") == null){ return null; }
            return (JSONArray) dataBooks.get("Data");
        }
        catch (Exception err){}
        return null;
    }

    public JSONObject getMemoryBookInfo(String guid){
        if(guid == null){ return null; }
        if(guid.trim().equals("")){ return null; }
        if(dataBooks == null){ return null; }
        try{
            if(getMemoryBooks() == null){ return null; }
            JSONArray books = getMemoryBooks();
            for(int j=0; j<books.length(); j++){
                JSONObject bl = (JSONObject)books.get(j);
                if(bl.getString("guid").equals(guid)){ return bl; }
            }
        }
        catch (Exception err){}
        return null;
    }

    public String getMemoryBookInfo(String guid, String column){
        if(guid == null){ return null; }
        if(guid.trim().equals("")){ return null; }
        if(column == null){ return null; }
        if(column.trim().equals("")){ return null; }
        if(dataBooks == null){ return null; }
        try{
            if(getMemoryBooks() == null){ return null; }
            JSONArray books = getMemoryBooks();
            for(int j=0; j<books.length(); j++){
                JSONObject bl = (JSONObject)books.get(j);
                if(bl.getString("guid").equals(guid)){ return bl.getString(column); }
            }
        }
        catch (Exception err){}
        return null;
    }

    public JSONObject getMemoryBookFileInfo(String guid, String file_guid){
        if(guid == null){ return null; }
        if(guid.trim().equals("")){ return null; }
        if(file_guid == null){ return null; }
        if(file_guid.trim().equals("")){ return null; }
        if(dataBooksFiles == null){ return null; }
        try{
            if(dataBooksFiles.length() == 0){ return null; }
            Log.d("getMemoryBookFileInfo", "Pass 1");
            Log.d("getMemoryBookFileInfo file_guid", file_guid);
            JSONArray files = dataBooksFiles.getJSONArray(guid);
            Log.d("getMemoryBookFileInfo 2", new Gson().toJson(files));
            for(int j=0; j<files.length(); j++){
                if(files.getJSONObject(j).getString("guid").equals(file_guid)){ return files.getJSONObject(j); }
            }
        }
        catch (Exception err){
            err.printStackTrace();
            Log.d("getMemoryBookFileInfo", new Gson().toJson(dataBooksFiles));
        }
        return null;
    }

    public String getMemoryBookCover(String guid){
        if(guid == null){ return ""; }
        if(guid.trim().equals("")){ return ""; }
        if(dataBooksCovers == null){ return ""; }
        try{ return dataBooksCovers.getString(guid); }
        catch (Exception err){
            err.printStackTrace();
            Log.d("getMemoryBookCover", new Gson().toJson(dataBooksFiles));
        }
        return "";
    }

    public void DownloadSelection(){
        if(getMemoryBooks() == null){ return; }
        downloadList = new ArrayList<String>();
        downloadListFiles = new ArrayList<JSONObject>();
        dowloadFileIndex = 0;
        dowloadFileCount = 0;
        dowloadFileErrors = 0;
        int cp = mainLayout.getChildCount();
        for(int i = 0; i<cp; i++){
            int idbox = mainLayout.getChildAt(i).getLabelFor();
            Log.d("idbox", ""+idbox);
            CheckBox chb = (CheckBox) root.findViewById(idbox);
            if(chb.isChecked()){
                try{ downloadList.add(checkboxes.getString(""+idbox)); }
                catch (Exception err){}
            }
        }
        Log.d("downloadList", ""+new Gson().toJson(downloadList));
        if(downloadList.size() != 0){
            dowloadFileCount = downloadList.size();
            LayoutInflater inflater = requireActivity().getLayoutInflater();
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            //builder.setTitle(R.string.dialog_download_title);
            builder.setTitle(R.string.dialog_get_info_title);
            builder.setMessage("0 / "+downloadList.size());
            downloadDialog = builder.create();
            downloadDialog.show();
            downloadInfosNext();
        }
    }

    private void downloadInfosNext(){
        if(getMemoryBooks() == null){ return; }
        if(downloadList.size() <= 0){ return; }
        if(dowloadFileIndex >= downloadList.size()){
            dowloadFileIndex = 0;
            dowloadFileCount = downloadListFiles.size();
            downloadDialog.setTitle(R.string.dialog_download_title);
            downloadDialog.setMessage("0 / "+downloadList.size());
            downloadNext();
            return;
        }
        client.Get("https://" + Host + ":" + Port + "/book/files/"+getMemoryBookInfo(downloadList.get(dowloadFileIndex),"guid"),
            new CallBack() {
                @Override
                public void run(String body, String header) {
                    frag.log(body);
                    try{
                        header = utils.trimStringByString(header, "/").trim();
                        String[] taburl = header.split("/");
                        String bookId = taburl[taburl.length - 1];

                        JSONObject data = (JSONObject) new JSONTokener(body).nextValue();
                        JSONArray files = (JSONArray) data.get("Data");
                        dataBooksFiles.put(bookId, files);
                        downloadDialog.setMessage("" + dowloadFileIndex + " / "+dowloadFileCount);

                        for(int u=0; u<files.length(); u++){
                            JSONObject file = (JSONObject)files.get(u);
                            JSONObject book2 = getMemoryBookInfo(bookId);
                            if(book2 == null) { continue; }
                            String MemoryFolder = Storage.getAppStoragePath("books");
                            String fileName = MemoryFolder+"/"+Storage.cleanFileName(book2.getString("authors"))+"-"+Storage.cleanFileName(book2.getString("title"))+"."+file.getString("format").toLowerCase();
                            if(Storage.existFile(fileName)){ Storage.deleteFile(fileName); }

                            JSONObject fileInfo = new JSONObject();
                            fileInfo.put("book", bookId);
                            fileInfo.put("file", file.getString("guid"));
                            fileInfo.put("dest", fileName);
                            downloadListFiles.add(fileInfo);
                        }
                        downloadInfosNext();
                    }
                    catch (Exception err){
                        err.printStackTrace();
                        dowloadFileErrors += 1;
                        downloadInfosNext();
                    }

                }

                @Override
                public void error(String error) {
                    frag.log(error);
                    dowloadFileErrors += 1;
                    downloadInfosNext();
                }
            }
        );

        dowloadFileIndex += 1;
    }

    private void downloadNext(){
        if(getMemoryBooks() == null){ return; }
        if(downloadList.size() <= 0){ return; }
        JSONObject file = downloadListFiles.get(dowloadFileIndex);
        try{
            client.Download(
                "https://" + Host + ":" + Port + "/book/file/"+file.getString("book")+"/"+file.getString("file"),
                file.getString("dest"),
                new CallBack() {
                    @Override
                    public void run(String body, String header) {
                        dowloadFileIndex += 1;
                        downloadDialog.setMessage(""+ dowloadFileIndex +" / "+downloadList.size());
                        String[] tab = header.split("<>");
                        String[] taburl = tab[0].split("/");
                        String bookId = taburl[taburl.length - 2];
                        String fileId = taburl[taburl.length - 1];
                        if(body.equals("Download:OK")){
                            frag.log(body);
                            JSONObject book = getMemoryBookInfo(bookId);
                            try{
                                JSONObject file = getMemoryBookFileInfo(bookId, fileId);

                                parent.dbh.newBook(
                                        bookId,
                                        book.getString("title"),
                                        book.getString("authors"),
                                        book.getString("series"),
                                        book.getDouble("series_vol"),
                                        book.getString("tags"),
                                        (book.getString("synopsis").equals("null"))?"":book.getString("synopsis"),
                                        getMemoryBookCover(bookId),
                                        book.getInt("import_date"),
                                        book.getInt("last_update_date"),
                                        fileId,
                                        file.getString("size"),
                                        file.getString("format"),
                                        tab[1],
                                        file.getString("file_hash"),
                                        file.getString("bookmark"),
                                        file.getInt("import_date"),
                                        file.getInt("last_update_date"),
                                        0,
                                        file.getString("editors"),
                                        file.getString("lang"),
                                        file.getInt("publication_date")
                                );
                            }
                            catch (Exception err){ err.printStackTrace(); }
                        }
                        else{
                            dowloadFileErrors += 1;
                            Storage.deleteFile(tab[1]);
                        }
                        if(dowloadFileIndex >= downloadList.size()){
                            downloadDialog.dismiss();
                            if(dowloadFileErrors > 0){ Toast.makeText(frag.getContext(), getResources().getString(R.string.sync_message_download_end_error).replace("[X]", ""+ dowloadFileErrors), Toast.LENGTH_LONG).show(); }
                            else{ Toast.makeText(frag.getContext(), getResources().getString(R.string.sync_message_download_end_ok), Toast.LENGTH_LONG).show(); }
                        }
                        else{ downloadNext(); }
                    }
                    @Override
                    public void error(String error) {
                        frag.log(error);
                        dowloadFileIndex += 1;
                        dowloadFileErrors += 1;
                        if(dowloadFileIndex >= downloadList.size()){
                            downloadDialog.dismiss();
                            if(dowloadFileErrors > 0){ Toast.makeText(frag.getContext(), getResources().getString(R.string.sync_message_download_end_error).replace("[X]", ""+ dowloadFileErrors), Toast.LENGTH_LONG).show(); }
                            else{ Toast.makeText(frag.getContext(), getResources().getString(R.string.sync_message_download_end_ok), Toast.LENGTH_LONG).show(); }
                        }
                        else{ downloadNext(); }
                    }
                }
            );
        }
        catch (Exception err){
            dowloadFileIndex += 1;
            dowloadFileErrors += 1;
            if(dowloadFileIndex >= downloadList.size()){
                downloadDialog.dismiss();
                if(dowloadFileErrors > 0){ Toast.makeText(frag.getContext(), getResources().getString(R.string.sync_message_download_end_error).replace("[X]", ""+ dowloadFileErrors), Toast.LENGTH_LONG).show(); }
                else{ Toast.makeText(frag.getContext(), getResources().getString(R.string.sync_message_download_end_ok), Toast.LENGTH_LONG).show(); }
            }
            else{ downloadNext(); }
        }
    }


}

