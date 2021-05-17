package lordkbx.workshop.ereader.ui.test;

import android.content.Intent;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.lang.Integer;

import java.net.Socket;
import java.util.ListIterator;
import java.util.stream.IntStream;
import java.util.zip.CRC32;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import lordkbx.workshop.ereader.R;
import lordkbx.workshop.ereader.ReaderActivity;

class TCPRunner implements Runnable{
    Socket socket = null;
    OutputStream socketOutputStream = null;
    DataInputStream socketInputStream = null;
    String host = null;
    int port = 33004;
    Message myMessage;
    View v= null;
    TestFragment frag;
    PipedReader reader;
    String SPublicKey = null;
    PublicKey publicKey = null;
    PrivateKey privateKey = null;

    public TCPRunner(View button, TestFragment parent, PipedReader pipedReader){
        v = button;
        frag = parent;
        reader = pipedReader;

        try{
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            KeyPair pair = keyGen.generateKeyPair();
            this.privateKey = pair.getPrivate();
            this.publicKey = pair.getPublic();}
        catch (NoSuchAlgorithmException err){ }
    }

    @Override
    public void run() {
        try {
            this.updateStatus(0);
            socket = new Socket(
                frag.ioHost.getText().toString(),
                Integer.parseInt(frag.ioPort.getText().toString())
            );
            socket.setKeepAlive(true);
            socketOutputStream = socket.getOutputStream();
            socketInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
            OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                    "SHA-256", "MGF1",
                    new MGF1ParameterSpec("SHA-256"),
                    PSource.PSpecified.DEFAULT
            );

            String keypem  = "-----BEGIN RSA PRIVATE KEY-----\n" +
                    Base64.getEncoder().encodeToString(privateKey.getEncoded()) +
                    "\n-----END RSA PRIVATE KEY-----\n";
            this.send("ANNOUNCE:"+keypem, false);
            if(!socket.isClosed()){ this.updateStatus(1); }
            while (!socket.isClosed()){
                if(socketInputStream.available() > 0){
                    int buffer_length = 512;
                    int currentBytesRead = 0;
                    byte[] messageByte = new byte[buffer_length];
                    StringBuilder lineB = new StringBuilder();
                    List<Byte> cht = new ArrayList<Byte>();
                    boolean end = false;
                    while(!end){
                        currentBytesRead = socketInputStream.read(messageByte);
                        if(currentBytesRead <= 0){ end = true; }
                        else{
                            lineB.append(new String(messageByte, 0, currentBytesRead, StandardCharsets.UTF_8));
                            if(currentBytesRead < buffer_length){ end = true; }
                        }
                    }
                    String[] lines = lineB.toString().split("\r\r");
                    for (String line: lines) {
                        Log.d("DEBUG_APP", "MSG <= " + line);
                        this.log("MSG <= " + line);
                        try{
                            String command = line.substring(0, line.indexOf(':'));
                            String parameters = line.substring(line.indexOf(':')+1);
                            if(command.equals("KEY")){
                                if(parameters.equals("OK")){
                                    this.send(
                                        "LOGIN:{\"user\": \"{U}\", \"password\": \"{P}\"}"
                                                .replace("{U}", frag.ioUser.getText().toString())
                                                .replace("{P}", frag.ioPassword.getText().toString())
                                    );
                                }
                                else{}
                            }
                            else if(command.equals("LOGIN")){
                                if(parameters.equals("OK")){
                                    this.updateStatus(2);
                                }
                                else{
                                    socket.close();
                                    break;
                                }
                            }
                            else {
                                String token = "";
                                String data = "";
                                if(privateKey != null){
                                    String controlE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
                                    StringBuilder sub_keyBuilder = new StringBuilder();
                                    String[] tab = parameters.split(":");

                                    cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams );
                                    Log.d("DEBUG_APP", "B64 token => " + tab[0]);
                                    byte[] pdata = Base64.getDecoder().decode(tab[0].replace("\n", ""));
                                    token = new String(cipher.doFinal(pdata));
                                    Log.d("DEBUG_APP", "token => " + token);
                                    long token_crc = this.crc_token(token);
                                    int alphabetSize = controlE.length();
                                    for(int i=0; i<alphabetSize; i++){
                                        long pos = i + token_crc;
                                        while(pos >= alphabetSize){ pos -= alphabetSize; }
                                        sub_keyBuilder.append(controlE.charAt((int)pos));
                                    }
                                    String sub_key = sub_keyBuilder.toString();
                                    Log.d("DEBUG_APP", "sub_key => " + sub_key);

                                    /*
                                    for letter in pre_text:
                                        if letter.lower() in controlE:
                                            result += sub_key[controlE.find(letter.lower())]
                                        else:
                                            result += letter
                                    */

                                    StringBuilder result = new StringBuilder();
                                    int max = tab[1].length();
                                    for(int i=0; i<max; i++){
                                        char letter = tab[1].charAt(i);
                                        result.append(controlE.charAt(sub_key.indexOf(letter)));
                                    }
                                    data = new String(Base64.getDecoder().decode(result.toString()), StandardCharsets.UTF_8);
                                }
                                else{
                                    data = parameters;
                                }
                                Log.d("DEBUG_APP", "data => " + data);
                                if(command.equals("ECHO")){
                                    this.log("data => " + data);
                                }
                                else if(command.equals("LIST")){
                                    int index = data.indexOf(':');
                                    String searchParam = data.substring(0, index);
                                    String tjs = data.substring(index+1);
                                    Log.d("DEBUG_APP", "data => " + tjs);
                                    JsonElement json = JsonParser.parseString(tjs);
                                    for (JsonElement elm: json.getAsJsonArray()) {
                                        Log.d("DEBUG_APP", "obj => " + elm.toString());
                                    }
                                }
                            }
                        }
                        catch (javax.crypto.IllegalBlockSizeException err){
                            Log.d("DEBUG_APP", "IllegalBlockSizeException => " + err.getMessage());
                        }
                        catch (javax.crypto.BadPaddingException err){
                            Log.d("DEBUG_APP", "BadPaddingException => " + err.getMessage());
                        }
                        catch (IllegalArgumentException err){
                            Log.d("DEBUG_APP", "IllegalArgumentException => " + err.getMessage());
                        }
                        catch (Exception err){
                            Log.d("DEBUG_APP", "Exception => " + err.getMessage());
                        }
                    }
                }
                if(reader.ready()){
                    Log.d("DEBUG_APP", "msg on PipedReader");
                    StringBuilder data = new StringBuilder();
                    while(reader.ready()){
                        int i = reader.read();
                        data.append((char) i);
                    }
                    String msg = data.toString();
                    String command = msg.substring(0, msg.indexOf(':'));
                    String parameters = msg.substring(msg.indexOf(':')+1);
                    Log.d("DEBUG_APP", "PipedReader => " + msg.toString());
                    Log.d("DEBUG_APP", command);
                    Log.d("DEBUG_APP", parameters);
                    if(command.equals("SEND")){
                        this.send(parameters);
                    }
                }
                try {Thread.sleep(300); }
                catch (InterruptedException err){}
            }
            this.updateStatus(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch (Exception err){
            this.log("err => " + err.getMessage());
            Log.d("DEBUG_APP", "Exception => " + err.getMessage());
        }
    }

    @Override
    public void finalize(){
        try {socket.close();} catch (IOException err){}
        socket = null;
    }

    public int crc_token(String token){
        int crc = 0;
        String[] tab = new String[]{"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
        List<String> table = new ArrayList<String>();
        Collections.addAll(table, tab);
        int size = token.length();
        for(int i=0; i<size; i++){
            crc += table.indexOf(""+token.charAt(i));
        }
        return (int)(crc / size);
    }

    public void send(String msg){ send(msg, true); }
    public void send(String msg, boolean encrypt){
        Log.d("DEBUG_APP", "SEND => "+msg);
        this.log("SEND => " + msg);
        try {
            if(encrypt){
                try {
                    Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
                    OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                            "SHA-256", "MGF1",
                            new MGF1ParameterSpec("SHA-256"),
                            PSource.PSpecified.DEFAULT
                    );
                    cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams);
                    byte[] pdata = Base64.getEncoder().encode(cipher.doFinal(msg.getBytes()));
                    msg = new String(pdata);
                    this.log("SEND CRYPTED => " + msg);
                    Log.d("DEBUG_APP", "SEND CRYPTED => " + msg);
                }
                catch (javax.crypto.IllegalBlockSizeException err){
                    this.log("IllegalBlockSizeException (crypt)=> " + err.getMessage());
                    Log.d("DEBUG_APP", "IllegalBlockSizeException => " + err.getMessage());
                }
                catch (javax.crypto.BadPaddingException err){
                    this.log("BadPaddingException => " + err.getMessage());
                    Log.d("DEBUG_APP", "BadPaddingException (crypt)=> " + err.getMessage());
                }
                catch (IllegalArgumentException err){
                    this.log("IllegalArgumentException => " + err.getMessage());
                    Log.d("DEBUG_APP", "IllegalArgumentException (crypt)=> " + err.getMessage());
                }
                catch (Exception err){
                    this.log("err => " + err.getMessage());
                    Log.d("DEBUG_APP", "Exception (crypt)=> " + err.getMessage());
                }
            }
            msg += "\r\r";
            socketOutputStream.write(msg.getBytes("UTF-8"));
            socketOutputStream.flush();
        }
        catch (UnsupportedEncodingException err){}
        catch (IOException err){}
    }

    private void updateStatus(int status){
        v.post(new Runnable() {
            public void run() {
                frag.changeIoDisplayStatus(status);
            }
        });
    }

    private void log(String msg){
        v.post(new Runnable() {
            public void run() {
                frag.log(msg);
            }
        });
    }
}

public class TestFragment extends Fragment {
    private static List<TableRow> rows = new ArrayList<TableRow>();
    View.OnClickListener btnListener;
    Intent reader;
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    private static File directory;
    private static View root;

    private Thread backgroundThread = null;
    TestFragment frag;
    PipedReader r;
    PipedWriter w;

    /* layout objects */
    ImageView eStatus;
    LinearLayout eScrollLog;
    Button ioButton;
    EditText ioHost;
    EditText ioPort;
    EditText ioUser;
    EditText ioPassword;
    ImageView ioStatus;

    Button ButtonList;
    Button ButtonGetBook;
    Button ButtonEcho;

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
        root = inflater.inflate(R.layout.fragment_test, container, false);
        frag = this;
        r = new PipedReader();
        w = new PipedWriter();
        try{ w.connect(r); } catch (IOException err){}

        reader = new Intent(getContext(), ReaderActivity.class);
        btnListener = new View.OnClickListener(){
            public void onClick(View v) {
                final List<String> data = (List<String>) v.getTag();
                Log.d("DEBUG_APP", data.toString());


            }
        };

        ioStatus = (ImageView) root.findViewById(R.id.test_io_status);
        ioButton = (Button) root.findViewById(R.id.test_io_button);
        ioHost = (EditText) root.findViewById(R.id.test_io_host);
        ioPort = (EditText) root.findViewById(R.id.test_io_port);
        ioUser = (EditText) root.findViewById(R.id.test_io_user);
        ioPassword = (EditText) root.findViewById(R.id.test_io_password);

        ButtonList = (Button) root.findViewById(R.id.test_button_list);
        ButtonGetBook = (Button) root.findViewById(R.id.test_button_get_book);
        ButtonEcho = (Button) root.findViewById(R.id.test_button_echo);

        eScrollLog = (LinearLayout) root.findViewById(R.id.Test_layout_scroll_layout);
        this.log("LOG START");

        ioButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                Log.d("DEBUG_APP", "Click io button");
                try{
                    if(backgroundThread != null){ return; }
                    backgroundThread = new Thread(new TCPRunner(v, frag, r));
                    backgroundThread.start();
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
                    w.write("SEND:GET:0475049f-8e6b-11eb-9ee5-f828195ec2d4");
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
        TextView tx = new TextView(getContext());
        tx.setText(message);
        eScrollLog.addView(tx);
    }

    public void changeIoDisplayStatus(){ this.changeIoDisplayStatus(0); }
    public void changeIoDisplayStatus(int active){
        if(active == 0){ this.ioStatus.setImageResource(R.drawable.ic_status_close); }
        else if(active == 1){ this.ioStatus.setImageResource(R.drawable.ic_status_wait); }
        else{ this.ioStatus.setImageResource(R.drawable.ic_status_open); }
        this.ioStatus.refreshDrawableState();
    }
}