package lordkbx.workshop.ereader.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

import lordkbx.workshop.ereader.MainDrawerActivity;
import lordkbx.workshop.ereader.R;
import lordkbx.workshop.ereader.Storage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.jakewharton.processphoenix.ProcessPhoenix;

import java.io.File;
import java.util.Locale;

public class PreferencesFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static MainDrawerActivity parent;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        parent = (MainDrawerActivity)getActivity();
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        onSharedPreferenceChanged(null, "");
        setHasOptionsMenu(true);
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
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.settings_menu, menu);
        menu.getItem(0).setOnMenuItemClickListener(item -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
            builder.setCancelable(false);
            builder.setTitle(getString(R.string.settings_reset));
            builder.setMessage(getString(R.string.settings_reset_confirm));
            builder.setPositiveButton(getString(R.string.settings_reset_btn_ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.clear();
                        editor.apply();
                        PreferenceManager.setDefaultValues(getActivity(), R.xml.root_preferences, true);
                        getPreferenceScreen().removeAll();
                        onCreatePreferences(null,null); //or onCreate(null) in your code
                        dialog.dismiss();
                    }
                });
            builder.setNegativeButton(getString(R.string.settings_reset_btn_cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        });
        menu.getItem(1).setOnMenuItemClickListener(item -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
            builder.setCancelable(false);
            builder.setTitle(getString(R.string.settings_rebase));
            builder.setMessage(getString(R.string.settings_rebase_confirm));
            builder.setPositiveButton(getString(R.string.settings_reset_btn_ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Storage.deleteDirectoryContent(Storage.getAppStoragePath("books"));

                        parent.dbh.Purge();
                        dialog.dismiss();
                    }
                });
            builder.setNegativeButton(getString(R.string.settings_reset_btn_cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        });
        menu.getItem(2).setOnMenuItemClickListener(item -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
            builder.setCancelable(false);
            builder.setTitle(getString(R.string.settings_clean_cache));
            builder.setMessage(getString(R.string.settings_clean_cache_confirm));
            builder.setPositiveButton(getString(R.string.settings_reset_btn_ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Storage.deleteDirectoryContent(Storage.getAppCachePath());
                        dialog.dismiss();
                    }
                });
            builder.setNegativeButton(getString(R.string.settings_reset_btn_cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        });
    }



    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d("PREF CHANGE, KEY =>", ""+key);
        if(key == null){ return; }
        if(key.equals("lang")){
            Log.d("PREF CHANGE, VAL =>", sharedPreferences.getString(key, "en"));

            AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
            builder.setCancelable(false);
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