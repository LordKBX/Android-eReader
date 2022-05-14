package lordkbx.workshop.ereader.ui.library;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.widget.Toast;

import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import lordkbx.workshop.ereader.MainDrawerActivity;
import lordkbx.workshop.ereader.R;
import lordkbx.workshop.ereader.reader.ReaderActivity;
import lordkbx.workshop.ereader.Storage;
import lordkbx.workshop.ereader.utils;

public class LibraryFragment extends Fragment {

    TableLayout eList;
    private static List<TableRow> rows = new ArrayList<TableRow>();
    Intent reader;
    public static final String EXTRA_MESSAGE1 = "LibraryFragment.MESSAGE1";
    public static final String EXTRA_MESSAGE2 = "LibraryFragment.MESSAGE2";
    public static final String EXTRA_MESSAGE3 = "LibraryFragment.MESSAGE3";
    public static final String EXTRA_MESSAGE4 = "LibraryFragment.MESSAGE4";
    private static File directory;
    private static View root;
    private static MainDrawerActivity parent;
    private static FlexboxLayout mainLayout;

    public View.OnClickListener btnListener = new View.OnClickListener(){
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
                reader.putExtra(EXTRA_MESSAGE1, data.get(1));
                reader.putExtra(EXTRA_MESSAGE2, data.get(3));
                reader.putExtra(EXTRA_MESSAGE3, data.get(4));
                reader.putExtra(EXTRA_MESSAGE4, data.get(0));

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
        MainDrawerActivity activity = null;
        try{
            activity = (MainDrawerActivity)this.getActivity();
            activity.hideBottomBar();
            activity.setFragmentName("Library");
        }
        catch (Exception err){}

        if(activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mainLayout.removeAllViews();
                    List<JSONObject> books = parent.dbh.getBooks();
                    utils.resetCheckboxesArray();

                    if (books.size() == 0) {
                        mainLayout.addView(utils.nobookCase(parent, mainLayout, getResources().getString(R.string.library_label_no_files)));
                    } else {
                        for (JSONObject book : books) {
                            Log.w("Book", new Gson().toJson(book));
                            try {
                                JSONArray files = book.getJSONArray("files");
                                LinearLayout Bcase = utils.bookCase(
                                        parent,
                                        mainLayout,
                                        book.getString("guid"),
                                        book.getString("title"),
                                        false, true
                                );
                                utils.replaceBookCaseCover(Bcase, book.getString("cover"));
                                ((CheckBox) Bcase.findViewById(Bcase.getLabelFor())).setVisibility(View.GONE);

                                List<String> data = new ArrayList<String>();
                                data.add(book.getString("title") + "(" + files.getJSONObject(0).getString("format") + ")");
                                data.add(files.getJSONObject(0).getString("link"));
                                data.add("file");
                                data.add(book.getString("guid"));
                                data.add(((JSONObject) files.get(0)).getString("guid"));
                                Bcase.setTag(data);
                                Bcase.setOnClickListener(btnListener);

                                mainLayout.addView(Bcase);
                            } catch (Exception err) {
                            }
                        }
                    }
                }
            });
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_library, container, false);
        parent = (MainDrawerActivity)getActivity();
        super.onCreate(savedInstanceState);
        try{ ((MainDrawerActivity)this.getActivity()).hideBottomBar(); } catch (Exception err){}
        reader = new Intent(getContext(), ReaderActivity.class);

        mainLayout = (FlexboxLayout) root.findViewById(R.id.library_flex_layout);

        FloatingActionButton btnImport = (FloatingActionButton) root.findViewById(R.id.library_floatingActionButton);
        btnImport.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                Log.d("DEBUG_APP", "IMPORT START");
                MainDrawerActivity activity = (MainDrawerActivity)getActivity();
                activity.importFile();
            }
        });

        directory = new File(Storage.getAppStoragePath());

        return root;
    }

}