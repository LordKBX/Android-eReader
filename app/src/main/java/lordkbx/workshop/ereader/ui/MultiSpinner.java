package lordkbx.workshop.ereader.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class MultiSpinner extends androidx.appcompat.widget.AppCompatSpinner implements
        DialogInterface.OnMultiChoiceClickListener, DialogInterface.OnCancelListener {

    private MultiSpinner root;
    private List<String> items;
    private boolean[] selected;
    private String defaultText;
    private MultiSpinnerListener listener;
    private AlertDialog alert;

    public MultiSpinner(Context context) {
        super(context);
        root = this;
    }

    public MultiSpinner(Context arg0, AttributeSet arg1) {
        super(arg0, arg1);
        root = this;
    }

    public MultiSpinner(Context arg0, AttributeSet arg1, int arg2) {
        super(arg0, arg1, arg2);
        root = this;
    }

    @Override
    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
        if (isChecked)
            selected[which] = true;
        else
            selected[which] = false;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        // refresh text on spinner
        StringBuffer spinnerBuffer = new StringBuffer();
        boolean someSelected = false;
        for (int i = 0; i < items.size(); i++) {
            if (selected[i] == true) {
                spinnerBuffer.append(items.get(i));
                spinnerBuffer.append(", ");
                someSelected = true;
            }
        }
        String spinnerText;
        if (someSelected) {
            spinnerText = spinnerBuffer.toString();
            if (spinnerText.length() > 2)
                spinnerText = spinnerText.substring(0, spinnerText.length() - 2);
        } else {
            spinnerText = defaultText;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_spinner_item,
                new String[] { spinnerText });
        setAdapter(adapter);
        listener.onItemsSelected(selected);
    }

    @Override
    public boolean performClick() {
        try{ alert.dismiss(); } catch (Exception err){ }
        try{
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMultiChoiceItems(items.toArray(new CharSequence[items.size()]), selected, this);

            LinearLayout ll = new LinearLayout(this.getContext());
            ll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            ll.setWeightSum(2);
            EditText input = new EditText(this.getContext());
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            Button btn = new Button(this.getContext());
            btn.setText("Add");
            btn.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            ll.addView(btn);
            ll.addView(input);
            btn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    String tag = input.getText().toString();
                    input.setText("");
                    if(tag != null && !tag.trim().equals("")){
                        tag = tag.trim();
                        if(!items.contains(tag)){
                            items.add(tag);
                            boolean[] ar = new boolean[selected.length + 1];
                            for (int i = 0; i < selected.length; i++){ ar[i] = selected[i]; }
                            ar[selected.length] = true;
                            selected = ar;
                            root.performClick();
                        }
                    }
                }
            });

            builder.setView(ll);
            builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }
            );
            builder.setOnCancelListener(this);
            alert = builder.show();
        }
        catch (Exception err){ return false; }
        return true;
    }

    public void setItems(List<String> items, String allText,
                         MultiSpinnerListener listener) {
        this.items = items;
        this.defaultText = allText;
        this.listener = listener;

        // all selected by default
        selected = new boolean[items.size()];
        for (int i = 0; i < selected.length; i++){
            if(allText.contains(items.get(i))){ selected[i] = true; }
            else{ selected[i] = false; }
        }

        // all text on the spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_spinner_item, new String[] { allText });
        setAdapter(adapter);
    }

    public interface MultiSpinnerListener {
        public void onItemsSelected(boolean[] selected);
    }
}
