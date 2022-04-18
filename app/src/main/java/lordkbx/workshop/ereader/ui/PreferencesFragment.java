package lordkbx.workshop.ereader.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import lordkbx.workshop.ereader.MainDrawerActivity;
import lordkbx.workshop.ereader.R;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.jakewharton.processphoenix.ProcessPhoenix;

import java.util.Locale;

public class PreferencesFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        onSharedPreferenceChanged(null, "");
    }

    @Override
    public void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d("PREF CHANGE, KEY =>", key);
        if(key.equals("lang")){
            Log.d("PREF CHANGE, VAL =>", sharedPreferences.getString(key, "en"));

            AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
            builder.setCancelable(true);
            builder.setTitle(getString(R.string.restart_title));
            builder.setMessage(getString(R.string.restart_message));
            builder.setPositiveButton(getString(R.string.restart_btn_ok),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MainDrawerActivity.updateLanguage(builder.getContext(), sharedPreferences.getString(key, "en"));
                            ProcessPhoenix.triggerRebirth(builder.getContext());
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }
}