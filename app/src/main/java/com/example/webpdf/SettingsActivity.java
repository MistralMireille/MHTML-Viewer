package com.example.webpdf;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {

    LinearLayout settingsDefaultCrawlerFolder;
    LinearLayout settingsDefaultSaveLocationFolder;
    Toolbar toolbarMenu;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        toolbarMenu = findViewById(R.id.toolbarMenu);
        settingsDefaultCrawlerFolder = findViewById(R.id.settingsDefaultCrawlerFolder);
        settingsDefaultSaveLocationFolder = findViewById(R.id.settingsDefaultSaveLocationFolder);

        setSupportActionBar(toolbarMenu);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ((TextView) settingsDefaultCrawlerFolder.getChildAt(1)).setText(getSharedPreferences("settings", MODE_PRIVATE).getString("defaultCrawlerFolder", "/storage/emulated/0/Download"));
        settingsDefaultCrawlerFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                final EditText input = new EditText(SettingsActivity.this);
                builder.setView(input);
                builder.setTitle("Type the path of the folder:");

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        File directory = new File(input.getText().toString());
                        if(directory.exists()) {
                            getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("folderUpdateNeeded", true).apply();
                            getSharedPreferences("settings", MODE_PRIVATE).edit().putString("defaultCrawlerFolder", directory.getAbsolutePath()).apply();
                            ((TextView) settingsDefaultCrawlerFolder.getChildAt(1)).setText(directory.getAbsolutePath());
                        } else {
                            Toast.makeText(SettingsActivity.this, "The folder does not exist.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
                builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing.
                    }
                });
                builder.show();
            }
        });

        ((TextView) settingsDefaultSaveLocationFolder.getChildAt(1)).setText(getSharedPreferences("settings", MODE_PRIVATE).getString("defaultSaveLocationFolder", "/storage/emulated/0/Download"));
        settingsDefaultSaveLocationFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                final EditText input = new EditText(SettingsActivity.this);
                builder.setView(input);
                builder.setTitle("Type the path of the folder:");

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        File directory = new File(input.getText().toString());
                        if(directory.exists()) {
                            getSharedPreferences("settings", MODE_PRIVATE).edit().putString("defaultSaveLocationFolder", directory.getAbsolutePath()).apply();
                            ((TextView) settingsDefaultSaveLocationFolder.getChildAt(1)).setText(directory.getAbsolutePath());
                        } else {
                            Toast.makeText(SettingsActivity.this, "The folder does not exist.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
                builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing.
                    }
                });
                builder.show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
