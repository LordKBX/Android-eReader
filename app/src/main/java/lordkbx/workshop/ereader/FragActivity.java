package lordkbx.workshop.ereader;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.fragment.app.FragmentActivity;

import lordkbx.workshop.ereader.ui.PreferencesFragment;

public class FragActivity extends FragmentActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, new PreferencesFragment()).commit();
            try{
                ActionBar bar = getActionBar();
                bar.show();
            }
            catch (Exception err){
                Log.e("ERROR", err.getMessage());
            }
        }
    }
}
