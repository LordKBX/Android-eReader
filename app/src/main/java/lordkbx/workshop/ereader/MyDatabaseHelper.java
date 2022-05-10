package lordkbx.workshop.ereader;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.ErrorHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MyDatabaseHelper extends SQLiteOpenHelper {
    private static MyDatabaseHelper mInstance = null;

    private static final String DATABASE_NAME = "database.db";
    private static final int DATABASE_VERSION = 14;

    private Context mCxt;

    public static MyDatabaseHelper getInstance(Context ctx) {
        /**
         * use the application context as suggested by CommonsWare.
         * this will ensure that you dont accidentally leak an Activitys
         * context (see this article for more information:
         * http://developer.android.com/resources/articles/avoiding-memory-leaks.html)
         */
        if (mInstance == null) { mInstance = new MyDatabaseHelper(ctx.getApplicationContext()); }
        return mInstance;
    }

    public MyDatabaseHelper(Context context) {
        super( context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION,
                new DatabaseErrorHandler() {
                    @Override
                    public void onCorruption(SQLiteDatabase sqLiteDatabase) {
                        Log.e("BDD ERROR", "Corruption");
                    }
                });
        Log.e("CLASS", context.getClass().getName());
        mCxt = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try{
            // Script to create table.
            String script = "CREATE TABLE \"books\" (" +
                    " \"guid\" TEXT NOT NULL," +
                    " \"title\" TEXT NOT NULL," +
                    " \"authors\" TEXT," +
                    " \"series\" TEXT," +
                    " \"series_vol\" NUMERIC DEFAULT 0," +
                    " \"tags\" TEXT," +
                    " \"synopsis\" TEXT," +
                    " \"cover\" TEXT," +
                    " \"import_date\" NUMERIC DEFAULT 0," +
                    " \"last_update_date\" NUMERIC DEFAULT 0," +
                    " PRIMARY KEY(\"guid\")" +
                    ")";
            db.execSQL(script);

            script = "CREATE TABLE \"files\" (" +
                    " \"guid_file\" TEXT NOT NULL," +
                    " \"book_id\" TEXT NOT NULL," +
                    " \"size\" TEXT NOT NULL," +
                    " \"format\" TEXT NOT NULL," +
                    " \"link\" TEXT NOT NULL," +
                    " \"file_hash\" TEXT NOT NULL," +
                    " \"bookmark\" TEXT," +
                    " \"file_import_date\" NUMERIC DEFAULT 0," +
                    " \"file_last_update_date\" NUMERIC DEFAULT 0," +
                    " \"file_last_read_date\" NUMERIC DEFAULT 0," +
                    " \"editors\" TEXT," +
                    " \"lang\" TEXT," +
                    " \"publication_date\" NUMERIC DEFAULT 0," +
                    " PRIMARY KEY(\"guid_file\")" +
                    ")";
            db.execSQL(script);

            script = "CREATE TABLE \"favorites\" (" +
                    " \"fav_guid\" TEXT NOT NULL," +
                    " \"book_id\" TEXT NOT NULL," +
                    " PRIMARY KEY(\"fav_guid\")" +
                    ")";
            db.execSQL(script);

            script = "CREATE TABLE \"progessions\" (" +
                    " \"prog_guid\" TEXT NOT NULL," +
                    " \"book_id\" TEXT NOT NULL," +
                    " \"guid_file\" TEXT NOT NULL," +
                    " \"chapter\" TEXT NOT NULL," +
                    " \"zoom\" NUMERIC DEFAULT 1," +
                    " \"scrollY\" NUMERIC DEFAULT 0," +
                    " PRIMARY KEY(\"prog_guid\")" +
                    ")";
            db.execSQL(script);
        }
        catch (Exception err){ Log.e("BDD TABLE CREATE ERROR", err.getMessage());}
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Storage.deleteDirectoryContent(Storage.getAppStoragePath("books"));
        Purge(db);
    }

    public void insert(String table, String nullColumnHack, ContentValues values) {
        try{
            SQLiteDatabase database = this.getWritableDatabase();
            database.insert(table, nullColumnHack, values);
        }
        catch (Exception err){
            Log.e("ERROR", err.getMessage());
            err.printStackTrace();
        }
    }

    public void Purge() { Purge(this.getWritableDatabase());}
    public void Purge(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS books");
        db.execSQL("DROP TABLE IF EXISTS files");
        db.execSQL("DROP TABLE IF EXISTS favorites");
        db.execSQL("DROP TABLE IF EXISTS progessions");
        onCreate(db);
    }

    public ArrayMap<String, Integer> getTags(){
        ArrayMap<String, Integer> rez = new ArrayMap<String, Integer>();
        Cursor cur = this.getReadableDatabase().rawQuery("SELECT tags FROM books ORDER BY tags ASC", new String[]{});
        if (cur != null){
            cur.moveToFirst();
            boolean end = false;
            while(!end){
                if(cur.isLast()){end = true;}
                String[] tags = cur.getString(0).split(";");
                for(String tag : tags){
                    if(tag == null || tag.trim().equals("") || tag.trim().equals("null")){ continue; }
                    if(!rez.containsKey(tag.trim())){ rez.put(tag.trim(), 1); }
                    else{ rez.replace(tag.trim(), rez.get(tag.trim()) + 1); }
                }
                cur.moveToNext();
            }
        }

        return rez;
    }

    public List<JSONObject> select(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit){
        List<JSONObject> ret = null;

        Cursor cur = this.getReadableDatabase().query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
        if (cur != null){
            ret = new ArrayList<>();
            cur.moveToFirst();
            int line = 0;
            boolean end = false;
            while(!end){
                if(cur.isLast()){end = true;}
                JSONObject jsonBody = new JSONObject();
                for(int i=0; i<cur.getColumnCount(); i++){
                    try{ jsonBody.put(cur.getColumnName(i), cur.getString(i));}
                    catch (Exception err){}
                }
                ret.add(jsonBody);
                cur.moveToNext();
                line += 1;
            }
        }
        return ret;
    }

    public List<String> getBooksIds(){
        List<String> rez = new ArrayList<String>();

        String query = "SELECT guid FROM books ORDER BY guid ASC";
        Cursor cur = this.getReadableDatabase().rawQuery(query, new String[]{});
        if(cur == null || cur.getCount() == 0){ return rez; }
        else{
            cur.moveToFirst();
            boolean end = false;
            while(end == false && cur.getCount() != 0){
                if(cur.isLast()){end = true;}
                rez.add(cur.getString(cur.getColumnIndex("guid")));
                cur.moveToNext();
            }
            return rez;
        }
    }
    public List<JSONObject> getBooks(){ return getBooks(null, null, null); }
    public List<JSONObject> getBooks(String id){ return getBooks(id, null, null); }
    public List<JSONObject> getBooks(String id, String search){ return getBooks(id, search, null); }
    public List<JSONObject> getBooks(String id, String search, SEARCH_TYPE search_type){
        String queryStart = "SELECT guid,title,authors,series,series_vol,import_date,last_update_date,tags,synopsis,cover,files.*,fav_guid FROM books " +
                "LEFT JOIN files ON(files.book_id = books.guid)" +
                "LEFT JOIN favorites ON(files.book_id = favorites.book_id)";
        List<JSONObject> ret = null;
        Cursor cur = null;
        if(id == null){
            if(search == null){
                cur = this.getReadableDatabase().rawQuery(queryStart+" ORDER BY title ASC", new String[]{});
            }
            else{
                String query = "";
                String[] data = null;
                if(search_type == null || search_type == SEARCH_TYPE.ALL){
                    query=queryStart+" " +
                            "WHERE LOWER(books.title) LIKE ? " +
                            "OR LOWER(books.series) LIKE ? " +
                            "OR LOWER(books.authors) LIKE ? " +
                            "OR LOWER(books.tags) LIKE ? " +
                            "OR LOWER(files.format) LIKE ? " +
                            "OR LOWER(files.lang) LIKE ? " +
                            "OR LOWER(files.editors) LIKE ?";
                        data = new String[]{
                                "%"+search+"%", "%"+search+"%", "%"+search+"%", "%"+search+"%",
                                "%"+search+"%", "%"+search+"%", "%"+search+"%"
                            };
                }
                else{
                    data = new String[]{"%"+search+"%"};
                    query=queryStart+" WHERE LOWER(*) LIKE ? ";
                    if(search_type == SEARCH_TYPE.AUTHOR){ query = query.replace("*", "books.authors"); }
                    else if(search_type == SEARCH_TYPE.SERIE){ query = query.replace("*", "books.series"); }
                    else if(search_type == SEARCH_TYPE.TAG){ query = query.replace("*", "books.tags"); }
                    else if(search_type == SEARCH_TYPE.TITLE){ query = query.replace("*", "books.title"); }
                    else if(search_type == SEARCH_TYPE.FORMAT){ query = query.replace("*", "files.format"); }
                    else if(search_type == SEARCH_TYPE.LANG){ query = query.replace("*", "files.lang"); }
                    else if(search_type == SEARCH_TYPE.EDITOR){ query = query.replace("*", "files.editors"); }
                    else if(search_type == SEARCH_TYPE.FILE){ query = query.replace("*", "files.link"); }
                }

                cur = this.getReadableDatabase().rawQuery(query, data);
            }
        }
        else { cur = this.getReadableDatabase().rawQuery(queryStart+" WHERE books.guid = ?", new String[]{id}); }

        if (cur != null){
            ret = new ArrayList<>();
            cur.moveToFirst();
            boolean end = false;
            String lastBookId = "";
            while(!end && cur.getCount() != 0){
                if(cur.isLast()){end = true;}
                if(!lastBookId.equals(cur.getString(cur.getColumnIndex("guid")))){
                    JSONObject jsonBody = new JSONObject();
                    try{
                        jsonBody.put("guid", cur.getString(cur.getColumnIndex("guid")));
                        jsonBody.put("title", cur.getString(cur.getColumnIndex("title")));
                        jsonBody.put("authors", cur.getString(cur.getColumnIndex("authors")));
                        jsonBody.put("series", cur.getString(cur.getColumnIndex("series")));
                        jsonBody.put("series_vol", cur.getString(cur.getColumnIndex("series_vol")));
                        jsonBody.put("import_date", cur.getString(cur.getColumnIndex("import_date")));
                        jsonBody.put("last_update_date", cur.getString(cur.getColumnIndex("last_update_date")));
                        jsonBody.put("tags", cur.getString(cur.getColumnIndex("tags")));
                        jsonBody.put("synopsis", cur.getString(cur.getColumnIndex("synopsis")));
                        jsonBody.put("cover", cur.getString(cur.getColumnIndex("cover")));
                        jsonBody.put("files", new JSONArray());
                        jsonBody.put("fav_guid", cur.getString(cur.getColumnIndex("fav_guid")));
                    }
                    catch (Exception err){ Log.e("BDD", err.getMessage() ); }
                    ret.add(jsonBody);
                }
                try{
                    JSONObject jsonBodyFile = new JSONObject();
                    jsonBodyFile.put("guid", cur.getString(cur.getColumnIndex("guid_file")));
                    jsonBodyFile.put("size", cur.getString(cur.getColumnIndex("size")));
                    jsonBodyFile.put("format", cur.getString(cur.getColumnIndex("format")));
                    jsonBodyFile.put("link", cur.getString(cur.getColumnIndex("link")));
                    jsonBodyFile.put("editors", cur.getString(cur.getColumnIndex("editors")));
                    jsonBodyFile.put("publication_date", cur.getString(cur.getColumnIndex("publication_date")));
                    jsonBodyFile.put("lang", cur.getString(cur.getColumnIndex("lang")));
                    jsonBodyFile.put("import_date", cur.getString(cur.getColumnIndex("file_import_date")));
                    jsonBodyFile.put("last_update_date", cur.getString(cur.getColumnIndex("file_last_update_date")));
                    jsonBodyFile.put("last_read_date", cur.getString(cur.getColumnIndex("file_last_read_date")));
                    jsonBodyFile.put("file_hash", cur.getString(cur.getColumnIndex("file_hash")));
                    jsonBodyFile.put("bookmark", cur.getString(cur.getColumnIndex("bookmark")));

                    ((JSONArray)ret.get(ret.size()-1).get("files")).put(jsonBodyFile);
                }
                catch (Exception err){
                    Log.e("BDD", err.getMessage() );
                }

                lastBookId = cur.getString(cur.getColumnIndex("guid"));
                cur.moveToNext();
            }
        }
        return ret;
    }

    public boolean exec(String query){
        try { this.getWritableDatabase().execSQL(query); return false; }
        catch (Exception err){ return false; }
    }

    public void addFavorite(String book_id){
        String query = "INSERT INTO favorites(fav_guid,book_id) VALUES (?,?)";
        this.getWritableDatabase().execSQL(query, new String[]{ UUID.randomUUID().toString(), book_id });
    }

    public void delFavorite(String book_id){
        String query = "DELETE FROM favorites WHERE book_id = ?";
        this.getWritableDatabase().execSQL(query, new String[]{ book_id });
    }

    public String getFavorite(String book_id){
        String query = "SELECT fav_guid,book_id FROM favorites WHERE book_id = ?";
        Cursor cur = this.getReadableDatabase().rawQuery(query, new String[]{ book_id });
        if(cur == null || cur.getCount() == 0){ return null; }
        else{
            cur.moveToFirst();
            return cur.getString(cur.getColumnIndex("fav_guid"));
        }
    }

    public List<String> getFavorites(){
        List<String> favs = new ArrayList<String>();
        String query = "SELECT fav_guid,book_id FROM favorites GROUP BY book_id";
        Cursor cur = this.getReadableDatabase().rawQuery(query, new String[]{});
        if(cur == null || cur.getCount() == 0){ return null; }
        else{
            cur.moveToFirst();
            boolean end = false;
            while(end == false && cur.getCount() != 0){
                if(cur.isLast()){end = true;}
                favs.add(cur.getString(cur.getColumnIndex("book_id")));
                cur.moveToNext();
            }
            return favs;
        }
    }

    public List<String> getMostRecentFiles(){ return getMostRecentFiles(0); }
    public List<String> getMostRecentFiles(int limit){
        if(limit <= 0){ limit = 10; }
        if(limit > 50){ limit = 50; }
        List<String> favs = new ArrayList<String>();
        String query = "SELECT book_id, file_last_read_date FROM files WHERE file_last_read_date > 0 ORDER BY file_last_read_date DESC LIMIT "+limit;
        Cursor cur = this.getReadableDatabase().rawQuery(query, new String[]{});
        if(cur == null || cur.getCount() == 0){ return null; }
        else{
            cur.moveToFirst();
            boolean end = false;
            while(end == false && cur.getCount() != 0){
                if(cur.isLast()){end = true;}
                favs.add(cur.getString(cur.getColumnIndex("book_id")));
                cur.moveToNext();
            }
            return favs;
        }
    }

    public boolean newBook(String guid, String title, String authors, String series, double series_vol,
                           String tags, String synopsis, String cover, int import_date, int last_update_date,
                           //data book file
                           String guid_file, String size, String format, String link, String file_hash,
                           String bookmark, int file_import_date, int file_last_update_date, int file_last_read_date,
                           String editors, String lang, int publication_date){

        String query1 = "INSERT INTO \"books\" (\"guid\",\"title\",\"authors\",\"series\",\"series_vol\"," +
                "\"tags\",\"synopsis\",\"cover\",\"import_date\",\"last_update_date\") " +
                "VALUES (?,?,?,?,?,?,?,?,?,?)";

        //book_id
        String query2 = "INSERT INTO \"files\" (\"guid_file\", \"book_id\", \"size\", \"format\", \"link\", \"file_hash\"," +
                "\"bookmark\", \"file_import_date\", \"file_last_update_date\", \"file_last_read_date\"," +
                "\"editors\", \"lang\", \"publication_date\") " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            List<JSONObject> bl = getBooks(guid);
            if(bl == null || bl.size() == 0){
                this.getWritableDatabase().execSQL(query1,
                    new String[]{
                            guid, title, authors,
                            series, ""+series_vol,
                            tags, synopsis, cover,
                            ""+import_date, ""+last_update_date
                    });
                this.getWritableDatabase().execSQL(query2,
                    new String[]{
                            guid_file, guid, size, format, link, file_hash,
                            bookmark, ""+file_import_date, ""+file_last_update_date, ""+file_last_read_date,
                            editors, lang, ""+publication_date
                    });
            }
            else{
                JSONArray files = (JSONArray)bl.get(0).get("files");
                boolean present = false;
                for(int i=0; i<files.length(); i++){
                    if(((JSONObject)files.get(i)).get("guid").equals(guid_file)){present = true;}
                }
                if(present == false){
                    this.getWritableDatabase().execSQL(query2,
                        new String[]{
                                guid_file, guid, size, format, link, file_hash,
                                bookmark, ""+file_import_date, ""+file_last_update_date, ""+file_last_read_date,
                                editors, lang, ""+publication_date
                        });
                }
            }
            return true;
        }
        catch (Exception err){
            Log.e("newBook", err.getMessage());
            err.printStackTrace();
            return false;
        }
    }
    public boolean deleteBook(String bookID){
        List<JSONObject> list = getBooks(bookID);
        try{
            JSONArray files = list.get(0).getJSONArray("files");
            for(int i = 0; i < files.length(); i++){
                JSONObject file = files.getJSONObject(i);
                Storage.deleteFile(file.getString("link"));
            }
            SQLiteDatabase database = this.getWritableDatabase();
            database.execSQL("DELETE FROM files WHERE book_id = ?", new String[]{bookID});
            database.execSQL("DELETE FROM books WHERE guid = ?", new String[]{bookID});

            return true;
        }
        catch (Exception err){
            Log.e("eraaseBook", err.getMessage());
            err.printStackTrace();
        }
        return false;
    }

    public boolean updateBookTitle(String book_id, String title){
        Log.e("updateBook", "Title");
        String query = "UPDATE books SET title = ?, last_update_date = ? WHERE guid = ?";
        try{ this.getWritableDatabase().execSQL(query, new String[]{ title, ""+Integer.parseInt(""+(System.currentTimeMillis() / 1000)), book_id }); return true; }
        catch (Exception err){
            Log.e("ERROR", err.getMessage());
            err.printStackTrace();
            return false;
        }
    }
    public boolean updateBookAuthors(String book_id, String authors){
        Log.e("updateBook", "Authors");
        String query = "UPDATE books SET authors = ?, last_update_date = ? WHERE guid = ?";
        try{ this.getWritableDatabase().execSQL(query, new String[]{ authors, ""+Integer.parseInt(""+(System.currentTimeMillis() / 1000)), book_id }); return true; }
        catch (Exception err){
            Log.e("ERROR", err.getMessage());
            err.printStackTrace();
            return false;
        }
    }
    public boolean updateBookSeries(String book_id, String series_name, double series_vol){
        Log.e("updateBook", "Series");
        String query = "UPDATE books SET series = ?, series_vol = ?, last_update_date = ? WHERE guid = ?";
        try{ this.getWritableDatabase().execSQL(query, new String[]{ series_name, ""+series_vol, ""+Integer.parseInt(""+(System.currentTimeMillis() / 1000)), book_id }); return true; }
        catch (Exception err){
            Log.e("ERROR", err.getMessage());
            err.printStackTrace();
            return false;
        }
    }
    public boolean updateBookTags(String book_id, String[] tags){
        Log.e("updateBook", "Tags");
        String query = "UPDATE books SET tags = ?, last_update_date = ? WHERE guid = ?";
        try{ this.getWritableDatabase().execSQL(query, new String[]{ String.join(";", tags), ""+Integer.parseInt(""+(System.currentTimeMillis() / 1000)), book_id }); return true; }
        catch (Exception err){
            Log.e("ERROR", err.getMessage());
            err.printStackTrace();
            return false;
        }
    }
    public boolean updateBookSynopsis(String book_id, String synopsis){
        Log.e("updateBook", "Synopsis");
        String query = "UPDATE books SET synopsis = ?, last_update_date = ? WHERE guid = ?";
        try{ this.getWritableDatabase().execSQL(query, new String[]{ synopsis, ""+Integer.parseInt(""+(System.currentTimeMillis() / 1000)), book_id }); return true; }
        catch (Exception err){
            Log.e("ERROR", err.getMessage());
            err.printStackTrace();
            return false;
        }
    }
    public boolean updateBookCover(String book_id, String cover){
        Log.e("updateBook", "Cover");
        Log.e("Cover", ""+cover);
        String query = "UPDATE books SET cover = ?, last_update_date = ? WHERE guid = ?";
        try{ this.getWritableDatabase().execSQL(query, new String[]{ cover, ""+Integer.parseInt(""+(System.currentTimeMillis() / 1000)), book_id }); return true; }
        catch (Exception err){
            Log.e("ERROR", err.getMessage());
            err.printStackTrace();
            return false;
        }
    }

    public void eraseProgression(String bookID, String fileID) {
        try{
            SQLiteDatabase database = this.getWritableDatabase();
            database.execSQL("DELETE FROM progessions WHERE book_id = ? AND guid_file = ?", new String[]{bookID, fileID});
        }
        catch (Exception err){
            Log.e("ERROR", err.getMessage());
            err.printStackTrace();
        }
    }
    public void newProgression(String bookID, String fileID, int chapter, float zoom, int scrollY) {
        try{
            ContentValues values = new ContentValues();
            values.put("prog_guid", UUID.randomUUID().toString());
            values.put("book_id", bookID);
            values.put("guid_file", fileID);
            values.put("chapter", chapter);
            values.put("zoom", zoom);
            values.put("scrollY", scrollY);
            insert("progessions", null, values);
            try{
                SQLiteDatabase database = this.getWritableDatabase();
                database.execSQL(
                    "UPDATE files SET file_last_read_date = ? WHERE guid_file = ?",
                    new String[]{ ""+Integer.parseInt(""+(System.currentTimeMillis() / 1000)), fileID}
                );
            }
            catch (Exception err2){
                Log.e("ERROR", err2.getMessage());
                err2.printStackTrace();
            }
        }
        catch (Exception err){
            Log.e("ERROR", err.getMessage());
            err.printStackTrace();
        }
    }

}
