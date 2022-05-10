package lordkbx.workshop.ereader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.flexbox.FlexboxLayout;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Comparator;

public class utils {
    public static final int TAG_BOOKCASE_TEXTVIEW = 66500;

    public static String trimStringByString(String text, String trimBy) {
        int beginIndex = 0;
        int endIndex = text.length();

        while (text.substring(beginIndex, endIndex).startsWith(trimBy)) {
            beginIndex += trimBy.length();
        }

        while (text.substring(beginIndex, endIndex).endsWith(trimBy)) {
            endIndex -= trimBy.length();
        }

        return text.substring(beginIndex, endIndex);
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static String getStringFromFile (String filePath) throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }

    public static Drawable decodeDrawable(Context context, String base64) {
        String[] Tbase64 = base64.split(",");
        base64 = Tbase64[Tbase64.length - 1];
        Drawable ret = null;
        Log.e("decodeDrawable", "");
        if (!base64.equals("")) {
            ByteArrayInputStream bais = new ByteArrayInputStream(
                    Base64.decode(base64.getBytes(), Base64.DEFAULT));
            ret = Drawable.createFromResourceStream(context.getResources(),
                    null, bais, null, null);
            try {
                bais.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return ret;
    }

    public static Drawable getRotateDrawable(final Drawable d, final float angle) {
        final Drawable[] arD = { d };
        return new LayerDrawable(arD) {
            @Override
            public void draw(final Canvas canvas) {
                canvas.save();
                canvas.rotate(angle, d.getBounds().width() / 2, d.getBounds().height() / 2);
                super.draw(canvas);
                canvas.restore();
            }
        };
    }

    private static int lastid = 24999;
    private static JSONObject checkboxes = new JSONObject();
    public static void resetCheckboxesArray(){ checkboxes = new JSONObject(); }
    public static JSONObject getCheckboxesArray(){ return checkboxes; }

    public static LinearLayout bookCase(MainDrawerActivity context, FlexboxLayout parent, String id, String title){ return bookCase(context, parent, id, title, false, false);}
    public static LinearLayout bookCase(MainDrawerActivity context, FlexboxLayout parent, String id, String title, boolean onPhone, boolean checkable){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        if(displayMetrics.widthPixels > displayMetrics.heightPixels){ width = displayMetrics.heightPixels; }
        int wi = (int) Math.round(width / 3) - 40;
        int hi = (int) Math.round(wi * 1.333);
        float multiplier = wi / 250.0f;
        Log.e("multiplier", ""+multiplier);
        LinearLayout ll0 = new LinearLayout(context);
        ll0.setOrientation(LinearLayout.VERTICAL);
        ll0.setBackgroundResource(R.drawable.ic_default_cover);
        LinearLayout.LayoutParams lp0 = new LinearLayout.LayoutParams(wi, hi);
        lp0.setMargins(5,5,5,5);
        ll0.setLayoutParams(lp0);
        lastid += 1;
        if(lastid > Integer.MAX_VALUE){lastid = 25000;}
        ll0.setId(lastid);
        ll0.setTooltipText(title);

        LinearLayout ll1 = new LinearLayout(context);
        ll1.setOrientation(LinearLayout.HORIZONTAL);
        int w70 = Math.round(70 * multiplier);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, w70);
        lp1.setMargins(0,0,0,0);
        ll1.setLayoutParams(lp1);
        Space sp1 = new Space(context);
        LinearLayout.LayoutParams slp1 = new LinearLayout.LayoutParams(wi - w70, w70);
        sp1.setLayoutParams(slp1);
        ll1.addView(sp1);
        if(onPhone){
            ImageView iv1 = new ImageView(context);
            iv1.setImageResource(R.drawable.ic_save);
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(w70, w70);
            ilp.setMargins(0,0,0,0);
            iv1.setLayoutParams(ilp);
            iv1.setForegroundGravity(Gravity.RIGHT);
            ll1.addView(iv1);
        }

        LinearLayout ll2 = new LinearLayout(context);
        ll2.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(lp0.width, hi - Math.round(150 * multiplier));
        lp2.setMargins(0,0,0,0);
        ll2.setLayoutParams(lp2);
        TextView tv = new TextView(context);
        tv.setMinimumWidth(wi);
        tv.setPadding(Math.round(15 * multiplier), Math.round(15 * multiplier), Math.round(15 * multiplier), Math.round(15 * multiplier));
        tv.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        tv.setText(title);
        tv.setTag(TAG_BOOKCASE_TEXTVIEW);
        tv.setTextSize(12*multiplier);
        ll2.addView(tv);

        LinearLayout ll3 = new LinearLayout(context);
        ll3.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(lp0.width, Math.round(80 * multiplier));
        lp3.setMargins(0,0,0,0);
        ll3.setLayoutParams(lp3);
        ll3.setPadding(0, 0, 0, 5);

        Button bt = new Button(context, null, R.style.Theme_EReader_VoidButton);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(Math.round(75 * multiplier), Math.round(75 * multiplier));
        blp.setMargins(0,0,0,0);
        bt.setLayoutParams(blp);
        bt.setPadding(0, 0, 0, 0);
        bt.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        Drawable dr = AppCompatResources.getDrawable(context, R.drawable.ic_more );
        dr = getRotateDrawable(dr, 90.0f);
        bt.setBackground(dr);
        lastid += 1;
        if(lastid > Integer.MAX_VALUE){lastid = 25000;}
        bt.setId(lastid);
        try{ checkboxes.put(""+bt.getId(), id); }
        catch (Exception err){}
        bt.setTag(bt.getId());
        bt.setTooltipText(""+parent.getId());
        bt.setText("");
        bt.setTextColor(context.getColor(R.color.transparent));

        bt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                try{
                    if(motionEvent.getAction() == MotionEvent.ACTION_UP){
                        Log.d("DEBUG_APP", "Click book button");
                        MotionEvent.PointerCoords cords = new MotionEvent.PointerCoords();
                        motionEvent.getPointerCoords(0, cords);
                        view.showContextMenu(cords.x, cords.y);
                        return true;
                    }
                }
                catch (Exception error){ Log.d("DEBUG_APP", "ERROR => "+error.getMessage()); }
                return false;
            }
        });
        context.registerForContextMenu(bt);
        ll3.addView(bt);

        if(checkable){
            Space sp2 = new Space(context);
            LinearLayout.LayoutParams slp2 = new LinearLayout.LayoutParams(lp0.width - Math.round(150 * multiplier), w70);
            sp2.setLayoutParams(slp2);
            ll3.addView(sp2);

            CheckBox cb = new CheckBox(context);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(Math.round(120 * multiplier), Math.round(75 * multiplier));
            clp.setMargins(0,0,0, 0);
            cb.setLayoutParams(clp);
            cb.setPadding(0,0,0,0);
            cb.setButtonDrawable(R.drawable.checkbox_selector);
            cb.setTextSize(0);
            lastid += 1;
            if(lastid > Integer.MAX_VALUE){lastid = 25000;}
            cb.setId(lastid);
            ll0.setLabelFor(cb.getId());
            try{ checkboxes.put(""+cb.getId(), id); }
            catch (Exception err){}
            ll3.addView(cb);
        }

        ll0.addView(ll1);
        ll0.addView(ll2);
        ll0.addView(ll3);

        return ll0;
    }
    public static LinearLayout replaceBookCaseCover(LinearLayout bookcase, String cover){
        if(bookcase == null){ return null; }
        if(cover == null && cover.trim().equals("")){ return bookcase; }
        Drawable drawable = utils.decodeDrawable(bookcase.getContext(), cover);
        if(drawable != null){
            bookcase.setBackground(utils.decodeDrawable(bookcase.getContext(), cover));
            ((TextView)bookcase.findViewWithTag(TAG_BOOKCASE_TEXTVIEW)).setText("");
        }
        return bookcase;
    }

    public static LinearLayout nobookCase(MainDrawerActivity context, FlexboxLayout parent, String text){
        LinearLayout ll0 = new LinearLayout(context);
        ll0.setOrientation(LinearLayout.HORIZONTAL);
        ll0.setGravity(Gravity.CENTER_HORIZONTAL);
        //ll0.setBackgroundResource(R.drawable.side_nav_bar);
        ll0.setWeightSum(0);

        ImageView im = new ImageView(context);
        im.setImageDrawable(context.getDrawable(R.drawable.ic_empty));
        im.setLayoutParams(new LinearLayout.LayoutParams(150, 150));

        TextView tv = new TextView(context);
        tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 150));
        tv.setPadding(5, 0, 0, 0);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER_VERTICAL);

        ll0.addView(im);
        ll0.addView(tv);
        return ll0;
    }
}

