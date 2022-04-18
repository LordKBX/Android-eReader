package lordkbx.workshop.ereader.ui.library;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.ArrayList;
import java.util.List;

import lordkbx.workshop.ereader.R;
import lordkbx.workshop.ereader.extendVolley.InputStreamRequest;
import lordkbx.workshop.ereader.extendVolley.NukeSSLCerts;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

interface CallBack {
    void run(String body, String header);
    void error(String error);
}

class APIClient{
    String host = null;
    int port = 33004;
    Message myMessage;
    View v= null;
    LibraryFragment frag;
    PipedReader reader;
    RequestQueue queue;

    public APIClient(View button, LibraryFragment parent, PipedReader pipedReader){
        v = button;
        frag = parent;
        reader = pipedReader;
        NukeSSLCerts.nuke();
        CookieManager manager = new CookieManager();
        CookieHandler.setDefault( manager  );
        queue = Volley.newRequestQueue(parent.getContext());

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
                            try {if(callback != null){ callback.run(response, ""); }}
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
                        try {if(callback != null){ callback.run(response, ""); }}
                        catch (Exception err){}
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        frag.log("Error: "+error.getMessage());
                        try {if(callback != null){ callback.error(error.getMessage()); }}
                        catch (Exception err){}
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
        catch (Exception err){ frag.log(err.getMessage()); }
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
                            Toast.makeText(frag.getContext(), "Download Complete", Toast.LENGTH_LONG).show();
                            try {if(callback != null){ callback.run("Download:OK", ""); }}
                            catch (Exception err){}
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

    private void log(String msg){
        v.post(new Runnable() {
            public void run() {
                frag.log(msg);
            }
        });
    }
}

public class LibraryFragment extends Fragment {
    private static List<TableRow> rows = new ArrayList<TableRow>();
    View.OnClickListener btnListener;
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    private static File directory;
    private static View root;

    LibraryFragment frag;
    PipedReader r;
    PipedWriter w;
    APIClient client;

    /* layout objects */
    ImageView eStatus;
    LinearLayout eScrollLog;
    Button ioButton;
    EditText ioHost;
    EditText ioPort;
    EditText ioUser;
    EditText ioPassword;

    Button ButtonList;
    Button ButtonGetBook;
    Button ButtonEcho;

    SharedPreferences sharedPreferences;

    @Override
    public void onResume() {
        super.onResume();

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_library, container, false);
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

        ioButton = (Button) root.findViewById(R.id.test_io_button);
        ioHost = (EditText) root.findViewById(R.id.test_io_host);
        ioPort = (EditText) root.findViewById(R.id.test_io_port);
        ioUser = (EditText) root.findViewById(R.id.test_io_user);
        ioPassword = (EditText) root.findViewById(R.id.test_io_password);

        ioHost.setText(sharedPreferences.getString("server_address", ""));
        ioPort.setText(sharedPreferences.getString("server_port", ""));
        ioUser.setText(sharedPreferences.getString("server_user", ""));
        ioPassword.setText(sharedPreferences.getString("server_password", ""));

        ButtonList = (Button) root.findViewById(R.id.ButtonSave);
        ButtonGetBook = (Button) root.findViewById(R.id.test_button_get_book);
        ButtonEcho = (Button) root.findViewById(R.id.test_button_echo);

        eScrollLog = (LinearLayout) root.findViewById(R.id.Test_layout_scroll_layout);
        this.log("LOG START");

        ioButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                Log.d("DEBUG_APP", "Click io button");
                try{
                    if(client == null){ client = new APIClient(v, frag, r); }
                    /*
                    client.Get("https://" + ioHost.getText() + ":" + ioPort.getText() + "/status", new CallBack() {
                        @Override
                        public void run(String body, String header) {
                            frag.log(body);
                        }
                    });
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("user", ioUser.getText().toString());
                    jsonBody.put("password", ioPassword.getText().toString());

                    client.Post("https://" + ioHost.getText() + ":" + ioPort.getText() + "/login", jsonBody.toString(),
                        new CallBack() {
                            @Override
                            public void run(String body, String header) {
                                frag.log(body);
                            }
                            @Override
                            public void error(String body) {
                                frag.log(body);
                            }
                        }
                    );
                    */

                    String MemoryFolder = Environment.getExternalStorageDirectory().getAbsolutePath();
                    client.Download("https://192.168.1.64/test_data/__________63387085_p0.jpg", MemoryFolder+"/Download"+"/__________63387085_p0.jpg",
                        new CallBack() {
                            @Override
                            public void run(String body, String header) {
                                frag.log(body);
                                frag.log(frag.getContext().getFilesDir().getAbsolutePath());
                            }
                            @Override
                            public void error(String body) {
                                frag.log(body);
                            }
                        }
                    );
                }
                catch (Exception error){ Log.d("DEBUG_APP", "ERROR => "+error.getMessage()); }
            }
        });

        ButtonList.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                Log.d("DEBUG_APP", "Click ButtonLogin");
                try{
                    w.write("SEND:LIST:ALL");
                }
                catch (Exception error){ Log.d("DEBUG_APP", "ERROR => "+error.getMessage()); }
            }
        });

        ButtonGetBook.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                Log.d("DEBUG_APP", "Click ButtonGetBook");
                try{
                    w.write("SEND:GET:0475049f-8e6b-11eb-9ee5-f828195ec2d4,05386ab5-8e6b-11eb-9c04-f828195ec2d4");
                }
                catch (Exception error){ Log.d("DEBUG_APP", "ERROR => "+error.getMessage()); }
            }
        });

        ButtonEcho.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                Log.d("DEBUG_APP", "Click ButtonEcho");
                try{
                    w.write("SEND:ECHO:MAMA");
                }
                catch (Exception error){ Log.d("DEBUG_APP", "ERROR => "+error.getMessage()); }
            }
        });

        return root;
    }

    public void log(String message){
        Log.d("DEBUG_APP", message);
        TextView tx = new TextView(getContext());
        tx.setText(message);
        eScrollLog.addView(tx);
    }
}