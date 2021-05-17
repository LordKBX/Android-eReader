package lordkbx.workshop.ereader;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;

import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.FileOutputStream;

public class MainDrawerActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_drawer);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_settings)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
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
        String[] mimetypes = {"application/epub+zip", "application/pdf", "application/x-cbz"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        startActivityForResult(intent, 7);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == 7 && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                Log.d("DEBUG_APP", "Uri: " + uri.toString());
                try{
                    int i;
                    String sourceFilename= uri.getPath();
                    String fileName = getFileNameByUri(uri);
                    Log.d("DEBUG_APP", "name is " + fileName);
                    String destinationFilename = getExternalMediaDirs()[0].getAbsolutePath()+"/"+fileName;

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
                    } finally {
                        try {
                            if (bis != null) bis.close();
                            if (bos != null) bos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                catch (Exception error){
                    Log.d("DEBUG_APP", error.getMessage());
                }
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
}