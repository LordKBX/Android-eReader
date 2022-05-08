package lordkbx.workshop.ereader.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.flexbox.FlexboxLayout;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import lordkbx.workshop.ereader.MainDrawerActivity;
import lordkbx.workshop.ereader.R;
import lordkbx.workshop.ereader.reader.ReaderActivity;
import lordkbx.workshop.ereader.Storage;
import lordkbx.workshop.ereader.ui.library.LibraryFragment;
import lordkbx.workshop.ereader.utils;

public class HomeFragment extends Fragment {

    TableLayout eList;
    private static List<TableRow> rows = new ArrayList<TableRow>();
    Intent reader;
    private static File directory;
    private static View root;
    private static MainDrawerActivity parent;
    private static FlexboxLayout favoritesLayout;
    private static FlexboxLayout recentsLayout;
    private LibraryFragment lif;

    View.OnClickListener btnListener = new View.OnClickListener(){
        public void onClick(View v) {
            final List<String> data = (List<String>) v.getTag();
            Log.d("DEBUG_APP", data.toString());
            //v.setBackgroundColor(getResources().getColor(R.color.purple_200));
            try{
                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(v.getContext());
                builder.setView(R.layout.processing);
                builder.setCancelable(false);
                parent.loadingDialog = builder.create();
                parent.loadingDialog.show();
                reader.putExtra(lif.EXTRA_MESSAGE1, data.get(1));
                reader.putExtra(lif.EXTRA_MESSAGE2, data.get(3));
                reader.putExtra(lif.EXTRA_MESSAGE3, data.get(4));
                reader.putExtra(lif.EXTRA_MESSAGE4, parent.getIntent());

                Handler mHandler=new Handler();
                mHandler.postDelayed(new Runnable() {
                    public void run(){
                        try{ parent.loadingDialog.dismiss(); }
                        catch (Exception err){}
                    }
                }, 30000);
            }
            catch (Exception err){
                err.printStackTrace();
            }
            startActivityForResult(reader, 10);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        try{ ((MainDrawerActivity)this.getActivity()).hideBottomBar(); } catch (Exception err){}

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                favoritesLayout.removeAllViews();
                recentsLayout.removeAllViews();
                List<JSONObject> books = parent.dbh.getBooks();
                List<String> favoredBooks = parent.dbh.getFavorites();
                List<String> recentBooks = parent.dbh.getMostRecentFiles();
                utils.resetCheckboxesArray();

                int cptFavs = 0;
                int cptRecents = 0;
                if(favoredBooks != null && favoredBooks.size() > 0){
                    for(JSONObject book : books){
                        Log.w("Book", new Gson().toJson(book));
                        try{
                            if(!favoredBooks.contains(book.getString("guid"))){ continue; }
                            cptFavs += 1;
                            JSONArray files = book.getJSONArray("files");
                            LinearLayout Bcase = utils.bookCase(
                                    parent,
                                    favoritesLayout,
                                    book.getString("guid"),
                                    book.getString("title"),
                                    false, true
                            );
                            utils.replaceBookCaseCover(Bcase, book.getString("cover"));
                            ((CheckBox)Bcase.findViewById(Bcase.getLabelFor())).setVisibility(View.GONE);

                            List<String> data = new ArrayList<String>();
                            data.add(book.getString("title") + "(" + files.getJSONObject(0).getString("format") + ")");
                            data.add(files.getJSONObject(0).getString("link"));
                            data.add("file");
                            data.add(book.getString("guid"));
                            data.add(((JSONObject)files.get(0)).getString("guid"));
                            Bcase.setTag(data);
                            Bcase.setOnClickListener(btnListener);

                            favoritesLayout.addView(Bcase);
                        }
                        catch (Exception err){}
                    }
                }
                if(cptFavs == 0){
                    favoritesLayout.addView(utils.nobookCase(parent, favoritesLayout, getResources().getString(R.string.home_label_no_files)));
                }

                if(recentBooks != null && recentBooks.size() > 0){
                    for(String bookID : recentBooks){
                        for(JSONObject book : books){
                            Log.w("Book", new Gson().toJson(book));
                            try{
                                if(!bookID.equals(book.getString("guid"))){ continue; }
                                cptRecents += 1;
                                JSONArray files = book.getJSONArray("files");
                                LinearLayout Bcase = utils.bookCase(
                                        parent,
                                        recentsLayout,
                                        book.getString("guid"),
                                        book.getString("title"),
                                        false, true
                                );
                                utils.replaceBookCaseCover(Bcase, book.getString("cover"));
                                ((CheckBox)Bcase.findViewById(Bcase.getLabelFor())).setVisibility(View.GONE);

                                List<String> data = new ArrayList<String>();
                                data.add(book.getString("title") + "(" + files.getJSONObject(0).getString("format") + ")");
                                data.add(files.getJSONObject(0).getString("link"));
                                data.add("file");
                                data.add(book.getString("guid"));
                                data.add(((JSONObject)files.get(0)).getString("guid"));
                                Bcase.setTag(data);
                                Bcase.setOnClickListener(btnListener);

                                recentsLayout.addView(Bcase);
                            }
                            catch (Exception err){}
                        }

                    }
                }
                if(cptRecents == 0){
                    recentsLayout.addView(utils.nobookCase(parent, recentsLayout, getResources().getString(R.string.home_label_no_files)));
                }

            }
        });
    }


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_home, container, false);
        parent = (MainDrawerActivity)getActivity();
        super.onCreate(savedInstanceState);
        lif = new LibraryFragment();
        try{ ((MainDrawerActivity)this.getActivity()).hideBottomBar(); } catch (Exception err){}

        reader = new Intent(getContext(), ReaderActivity.class);
        favoritesLayout = (FlexboxLayout) root.findViewById(R.id.home_flex_layout_favs);
        recentsLayout = (FlexboxLayout) root.findViewById(R.id.home_flex_layout_recents);

        directory = new File(Storage.getAppStoragePath());

        return root;
    }

}