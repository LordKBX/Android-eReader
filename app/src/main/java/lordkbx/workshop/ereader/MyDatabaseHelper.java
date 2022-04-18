package lordkbx.workshop.ereader;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MyDatabaseHelper extends SQLiteOpenHelper {

    public MyDatabaseHelper(Context context) {
        super(context, "database", null, 2);
        /*
        String script = "INSERT INTO \"books\" (\"guid\",\"title\",\"authors\",\"series\",\"series_vol\",\"tags\",\"synopsis\",\"cover\",\"import_date\",\"last_update_date\") VALUES ('0475049f-8e6b-11eb-9ee5-f828195ec2d4','Rosario_Vampire S1 T01','Akihisa IKEDA','Rosario Vampire',0,NULL,NULL,'',1616787586.46821,0)";
        getWritableDatabase().execSQL(script);
        script = "INSERT INTO \"books\" (\"guid\",\"title\",\"authors\",\"series\",\"series_vol\",\"tags\",\"synopsis\",\"cover\",\"import_date\",\"last_update_date\") VALUES ('c4786e76-4838-43b9-9978-928d16a17e8b','Knight''s and Magic VOL01','Amazake No Hisago','Knight''s and Magic',0,NULL,'The story begins when Tsubasa Kurata, a software engineer and hardcore mecha otaku from Japan dies in a car accident. He is later reborn in the fantastical Fremmevilla Kingdom, a medieval world where giant, powerful mechs called Silhouette Knights are used to fight against horrifying creatures called Demon Beasts." +
                "" +
                "Here, Tsubasa is reborn as Ernesti ''Eru'' Echavalier, the son of a noble family. Blessed with exceptional magical abilities and the memories of his past life, he enrolls at the Royal Laihiala Academy, an elite magic school where the pilots of the Silhouette Knights called Knight Runners are being trained to battle threats both from within and outside the kingdom. He later teams up with Adeltrud Olter and her twin brother, Archid with the goal of piloting a Silhouette Knight of his own creation, something that has been unheard of for centuries.','',1616787657.76645,0)";
        getWritableDatabase().execSQL(script);
        newBook("AA", "mytitle", "the infamous author", "serie ?", 0.0,
                "my;tags", "", "", 0, 0,
                "BB", "0.3MB", "EPUB", "lien fichier", "hash",
                "", 0, 0, 0, "EDITOR SAN", "FR BECAUSE!", 0);
        */

        String json = new Gson().toJson(getBooks().get(2));
        Log.d("BDD BOOKS OUT", json);
        try{
            Log.d("BDD BOOKS OUT", getBooks().get(2).getString("title"));}
        catch (Exception err){}
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
                    " \"cover\" TEXT NOT NULL," +
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

            script = "CREATE TABLE \"versions\" (" +
                    " \"name\" TEXT NOT NULL," +
                    " \"version\" NUMERIC DEFAULT 1," +
                    " PRIMARY KEY(\"name\")" +
                    ")";
            db.execSQL(script);

            script = "CREATE TABLE \"favorites\" (" +
                    " \"fav_guid\" TEXT NOT NULL," +
                    " \"book_id\" TEXT NOT NULL," +
                    " \"guid_file\" TEXT NOT NULL," +
                    " PRIMARY KEY(\"fav_guid\")" +
                    ")";
            db.execSQL(script);
        }
        catch (Exception err){ Log.e("BDD TABLE CREATE ERROR", err.getMessage());}
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS books");
        db.execSQL("DROP TABLE IF EXISTS filess");
        db.execSQL("DROP TABLE IF EXISTS versions");
        db.execSQL("DROP TABLE IF EXISTS favorites");
        onCreate(db);
    }

    public List<JSONObject> select(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit){
        List<JSONObject> ret = null;

        Cursor cur = this.getReadableDatabase().query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
        if (cur != null){
            ret = new ArrayList<>();
            cur.moveToFirst();
            int line = 0;
            boolean end = false;
            while(end == false){
                if(cur.isLast()){end = true;}
                JSONObject jsonBody = new JSONObject();
                for(int i=0; i<cur.getColumnCount(); i++){
                    try{
                        jsonBody.put(cur.getColumnName(i), cur.getString(i));
                        Log.d("BDD READ, Line "+line+", col "+i, cur.getString(i));}
                    catch (Exception err){}
                }
                ret.add(jsonBody);
                cur.moveToNext();
                line += 1;
            }
        }
        return ret;
    }

    public List<JSONObject> getBooks(){ return getBooks(null, null, null); }
    public List<JSONObject> getBooks(String id){ return getBooks(id, null, null); }
    public List<JSONObject> getBooks(String id, String search){ return getBooks(id, search, null); }
    public List<JSONObject> getBooks(String id, String search, SEARCH_TYPE search_type){
        List<JSONObject> ret = null;
        Cursor cur = null;
        if(id == null){
            if(search == null){
                cur = this.getReadableDatabase().rawQuery("SELECT * FROM books LEFT JOIN files ON(files.book_id = books.guid) ORDER BY title ASC", new String[]{});
            }
            else{
                String query = "";
                String[] data = null;
                if(search_type == null || search_type == SEARCH_TYPE.ALL){
                    query="SELECT * FROM books JOIN files ON(files.book_id = books.guid) " +
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
                    query="SELECT * FROM books JOIN files ON(files.book_id = books.guid) WHERE LOWER(*) LIKE ? ";
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
        else{
            cur = this.getReadableDatabase().rawQuery(
                "SELECT * FROM books LEFT JOIN files ON(files.book_id = books.guid) WHERE books.guid = ?",
                new String[]{id}
            );
        }
        if (cur != null){
            ret = new ArrayList<>();
            cur.moveToFirst();
            boolean end = false;
            String lastBookId = "";
            while(end == false){
                if(cur.isLast()){end = true;}
                if(lastBookId != cur.getString(cur.getColumnIndex("guid"))){
                    JSONObject jsonBody = new JSONObject();
                    try{
                        jsonBody.put("guid", cur.getString(cur.getColumnIndex("guid")));
                        jsonBody.put("title", cur.getString(cur.getColumnIndex("title")));
                        jsonBody.put("authors", cur.getString(cur.getColumnIndex("authors")));
                        jsonBody.put("series_vol", cur.getString(cur.getColumnIndex("series_vol")));
                        jsonBody.put("import_date", cur.getString(cur.getColumnIndex("import_date")));
                        jsonBody.put("last_update_date", cur.getString(cur.getColumnIndex("last_update_date")));
                        jsonBody.put("tags", cur.getString(cur.getColumnIndex("tags")));
                        jsonBody.put("synopsis", cur.getString(cur.getColumnIndex("synopsis")));
                        jsonBody.put("cover", cur.getString(cur.getColumnIndex("cover")));
                        jsonBody.put("files", new JSONArray());
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
            return false;
        }
        catch (Exception err){ return false; }
    }
}
