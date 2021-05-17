package lordkbx.workshop.ereader.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowInsets;
import android.view.WindowMetrics;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.res.Resources;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import lordkbx.workshop.ereader.MainDrawerActivity;
import lordkbx.workshop.ereader.R;
import lordkbx.workshop.ereader.ReaderActivity;

public class HomeFragment extends Fragment {

    TableLayout eList;
    private static List<TableRow> rows = new ArrayList<TableRow>();
    View.OnClickListener btnListener;
    Intent reader;
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    private static File directory;
    private static View root;

    @Override
    public void onResume() {
        super.onResume();

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                parseFolder(directory.getAbsolutePath());
            }
        });
    }


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_home, container, false);

        reader = new Intent(getContext(), ReaderActivity.class);
        btnListener = new View.OnClickListener(){
            public void onClick(View v) {
                final List<String> data = (List<String>) v.getTag();
                Log.d("DEBUG_APP", data.toString());
                //v.setBackgroundColor(getResources().getColor(R.color.purple_200));

                if(data.get(2) == "dir"){
                    parseFolder(data.get(1));
                }
                else{
                    reader.putExtra(EXTRA_MESSAGE, data.get(1));
                    startActivity(reader);
                }

            }
        };

        FloatingActionButton btnImport = (FloatingActionButton) root.findViewById(R.id.floatingActionButton);
        btnImport.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                Log.d("DEBUG_APP", "IMPORT START");
                MainDrawerActivity activity = (MainDrawerActivity)getActivity();
                activity.importFile();
                parseFolder(getContext().getExternalMediaDirs()[0].getAbsolutePath());
            }
        });

        eList = (TableLayout) root.findViewById(R.id.home_table);
        TableRow tbr = (TableRow) root.findViewById(R.id.tbr);
        eList.removeView(tbr);

        File[] directorys = getContext().getExternalMediaDirs();
        for(File file : directorys){
            Log.d("DEBUG_APP", "dirs list >> "+file.getAbsolutePath());
        }
        directory = getContext().getExternalMediaDirs()[0];

        return root;
    }

    private void parseFolder(String path){
        eList = (TableLayout) root.findViewById(R.id.home_table);
        TableRow.LayoutParams params = new TableRow.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER | Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;

        Log.d("DEBUG_APP", "parseFolder(\""+path+"\")");
        File directory;
        File[] files = null;

        /*
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSION_EXTERNAL_STORAGE);
         */
        Log.d("DEBUG_APP", "PASS 1");
        try{
            directory = new File(path);
            if(directory.canRead()){
                files = directory.listFiles();
                int t = files.length;
                Log.d("DEBUG_APP", "length"+files.length);
            }
            else{
                int e = (int)0.000000000007;
            }
        }
        catch (Exception error){
            Toast toast=Toast.makeText(getContext(),"Folder access error",Toast.LENGTH_LONG);
            Log.d("DEBUG_APP", "Folder access error ("+path+")");
            //toast.setMargin(50,50);
            toast.show();
            return;
        }
        Log.d("DEBUG_APP", "PASS 2");

        TableRow row = null;
        int j = 0;
        while(j < rows.size()){
            eList.removeView(rows.get(j));
            j++;
        }
        rows.clear();

        Log.d("DEBUG_APP", "PASS 3");

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindow().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        double divider = 4.00;
        double margin = 30.00;
        double calc = (displayMetrics.widthPixels - (divider * margin)) / divider;
        Log.d("DEBUG_APP", "PASS 4, calc = "+calc);

        row = null;
        for (int i = 0; i < files.length; i++)
        {
            Log.d("DEBUG_APP", files[i].getName());
            if(i==0 || i % 4 == 0){
                row = new TableRow(getContext());
                row.setGravity(Gravity.CENTER_HORIZONTAL);
                row.setPadding(5, 0, 5, 0);
                rows.add(row);
                eList.addView(row);

                Log.d("DEBUG_APP", "PASS 5");
            }
            Button btn = new Button(getContext());
            btn.setMaxWidth((int)calc);
            btn.setMaxHeight((int)calc);
            btn.setWidth((int)calc);
            btn.setHeight((int)calc);
            btn.setPadding(5, 5,5, 5);
            btn.setPaddingRelative(0, 0, 0, 0);
            btn.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
            btn.setLayoutParams(params);

            btn.setId(btn.generateViewId());
            btn.setText(files[i].getName());
            List<String> data = new ArrayList<String>();
            data.add(files[i].getName());
            data.add(files[i].getAbsolutePath());
            data.add((files[i].isFile())?"file":"dir");
            btn.setTag(data);
            btn.setOnClickListener(btnListener);

            row.addView(btn);
            Log.d("DEBUG_APP", "PASS 6");
        }

    }
}