package lordkbx.workshop.ereader.ui.explorer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import android.Manifest;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


import lordkbx.workshop.ereader.R;
import lordkbx.workshop.ereader.reader.ReaderActivity;

public class ExplorerFragment extends Fragment {
    private static final int OPEN_DIRECTORY_REQUEST_CODE = 13;
    public static final int PERMISSION_EXTERNAL_STORAGE = 1;
    private static List<TableRow> rows = new ArrayList<TableRow>();
    TableLayout eList;
    View.OnClickListener btnListener;
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";

    Intent reader;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_explorer, container, false);

        reader = new Intent(getContext(), ReaderActivity.class);
        btnListener = new View.OnClickListener(){
            public void onClick(View v) {
                final List<String> data = (List<String>) v.getTag();
                Log.d("DEBUG_APP", data.toString());
                //v.setBackgroundColor(getResources().getColor(R.color.purple_200));

                if(data.get(2) == "dir"){
                    parseFolder(data.get(1), eList);
                }
                else{
                    reader.putExtra(EXTRA_MESSAGE, data.get(1));
                    startActivity(reader);
                }

            }
        };

        eList = (TableLayout) root.findViewById(R.id.home_table);
        TableRow tbr = (TableRow) root.findViewById(R.id.tbr);
        eList.removeView(tbr);

        String MemoryFolder = Environment.getExternalStorageDirectory().getAbsolutePath();
        //Environment.DIRECTORY_DOWNLOADS

        Map<String, File> dirs = new HashMap<String, File>();
        dirs.put("APP_DATA_DIR", inflater.getContext().getExternalMediaDirs()[0]);
        dirs.put("INTERN_DOWNLOADS", new File(MemoryFolder+"/Download"));
        dirs.put("INTERN_BOOKS", new File(MemoryFolder+"/Books"));
        dirs.put("INTERN_DOCUMENTS", new File(MemoryFolder+"/Documents"));
        dirs.put("INTERN_PICTURES", new File(MemoryFolder+"/Pictures"));

        ActivityCompat.requestPermissions(
                getActivity(),
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSION_EXTERNAL_STORAGE
        );

        ActivityCompat.requestPermissions(
                getActivity(),
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSION_EXTERNAL_STORAGE
        );

        /*
        File scan = new File("/storage");
        if(scan.isDirectory()){
            File[] files = scan.listFiles();
            for (int i = 0; i < files.length; i++)
            {
                Log.d("DEBUG_APP", files[i].getAbsolutePath());
            }
        }
        */
        //Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        //startActivityForResult(intent, OPEN_DIRECTORY_REQUEST_CODE);

        String path = MediaStore.Downloads.EXTERNAL_CONTENT_URI.getPath();
        Log.d("DEBUG_APP", path);
        //String path = "/storage/emulated/0/Download";
        //String path = MemoryFolder+"/"+Environment.DIRECTORY_DOWNLOADS;

        DocumentFile pickedDir = DocumentFile.fromFile(new File(path));
        for (DocumentFile file : pickedDir.listFiles()) {
            Log.d("DEBUG_APP", file.getName());
        }

        File directory = new File(path);
        //File directory = Environment.getStorageDirectory();
        Log.d("DEBUG_APP", directory.getAbsolutePath());
        try{
            if(directory.canRead()){
                File[] files = directory.listFiles();
                for (int i = 0; i < files.length; i++)
                {
                    Log.d("DEBUG_APP", files[i].getAbsolutePath());
                }

            }
            parseFolder(path, eList);
        }
        catch (Exception error){
            Toast toast=Toast.makeText(getContext(),"Folder access error",Toast.LENGTH_LONG);
            Log.d("DEBUG_APP", "Folder access error ("+path+"), "+error.getMessage());
            //toast.setMargin(50,50);
            toast.show();
        }

        return root;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,Intent resultData)
    {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == OPEN_DIRECTORY_REQUEST_CODE && resultCode == Activity.RESULT_OK)
        {
            if (resultData != null)
            {
                Uri uri = resultData.getData();
                Log.d("DEBUG_APP", "OPEN URI = "+uri);
            }
        }
    }

    private void parseFolder(String path, TableLayout eList){
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