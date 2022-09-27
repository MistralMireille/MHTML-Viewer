package com.example.webpdf;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {

    LinearLayout settingsDefaultCrawlerFolder;
    LinearLayout settingsDefaultSaveLocationFolder;
    LinearLayout settingsSortMethod;
    LinearLayout settingsLocalLinkBehavior;
    Toolbar toolbarMenu;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        toolbarMenu = findViewById(R.id.toolbarMenu);
        settingsDefaultCrawlerFolder = findViewById(R.id.settingsDefaultCrawlerFolder);
        settingsDefaultSaveLocationFolder = findViewById(R.id.settingsDefaultSaveLocationFolder);
        settingsSortMethod = findViewById(R.id.settingsSortMethod);
        settingsLocalLinkBehavior = findViewById(R.id.settingsLocalLinkBehavior);

        setSupportActionBar(toolbarMenu);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ((TextView) settingsDefaultCrawlerFolder.getChildAt(1)).setText(getSharedPreferences("settings", MODE_PRIVATE).getString("defaultCrawlerFolder", "/storage/emulated/0/Download"));
        settingsDefaultCrawlerFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFileBrowserDialog(getSharedPreferences("settings", MODE_PRIVATE).getString("defaultCrawlerFolder", "/storage/emulated/0/Download"), new FileBrowserResultFunctions() {
                    @Override
                    public void confirmFunction(String resultingPath) {
                        File directory  = new File(resultingPath);
                        if(directory.exists()) {
                            getSharedPreferences("settings", MODE_PRIVATE).edit().putString("defaultCrawlerFolder", directory.getAbsolutePath()).apply();
                            ((TextView) settingsDefaultCrawlerFolder.getChildAt(1)).setText(directory.getAbsolutePath());
                        } else {
                            Toast.makeText(SettingsActivity.this, "The folder does not exist.", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void cancelFunction() {
                        // do nothing
                    }
                });
            }
        });

        ((TextView) settingsDefaultSaveLocationFolder.getChildAt(1)).setText(getSharedPreferences("settings", MODE_PRIVATE).getString("defaultSaveLocationFolder", "/storage/emulated/0/Download"));
        settingsDefaultSaveLocationFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFileBrowserDialog(getSharedPreferences("settings", MODE_PRIVATE).getString("defaultSaveLocationFolder", "/storage/emulated/0/Download"), new FileBrowserResultFunctions() {
                    @Override
                    public void confirmFunction(String resultingPath) {
                        File directory  = new File(resultingPath);
                        if(directory.exists()) {
                            getSharedPreferences("settings", MODE_PRIVATE).edit().putString("defaultSaveLocationFolder", directory.getAbsolutePath()).apply();
                            ((TextView) settingsDefaultSaveLocationFolder.getChildAt(1)).setText(directory.getAbsolutePath());
                        } else {
                            Toast.makeText(SettingsActivity.this, "The folder does not exist.", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void cancelFunction() {
                        // do nothing
                    }
                });
            }
        });

        ((TextView) settingsSortMethod.getChildAt(1)).setText(getSharedPreferences("settings", MODE_PRIVATE).getString("settingsSortMethod", "alphanumeric case insensitive"));
        settingsSortMethod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                final RadioGroup input = new RadioGroup(SettingsActivity.this);
                String[] sortingMethods = new String[]{"alphanumeric", "alphabetic", "alphanumeric case insensitive", "alphabetic case insensitive", "numeric"};
                for(String s : sortingMethods) {
                    RadioButton option = new RadioButton(SettingsActivity.this);
                    option.setText(s);
                    input.addView(option);
                }
                builder.setView(input);
                builder.setTitle("Sort");
                builder.setMessage("Select what method to sort files with. Folders are always sorted alphabetically.");

                builder.setPositiveButton("CONFIRM", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RadioButton selectedButton = input.findViewById(input.getCheckedRadioButtonId());
                        if(selectedButton != null) {
                            getSharedPreferences("settings", MODE_PRIVATE).edit().putString("settingsSortMethod", selectedButton.getText().toString()).apply();
                            ((TextView) settingsSortMethod.getChildAt(1)).setText(selectedButton.getText());
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

        ((TextView) settingsLocalLinkBehavior.getChildAt(1)).setText(getSharedPreferences("settings", MODE_PRIVATE).getString("defaultLocalLinkBehavior", "Ask User"));
        settingsLocalLinkBehavior.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                final RadioGroup input = new RadioGroup(SettingsActivity.this);
                String[] sortingMethods = new String[]{"Ask User", "Local Only", "Do Nothing"};
                for(String s : sortingMethods) {
                    RadioButton option = new RadioButton(SettingsActivity.this);
                    option.setText(s);
                    input.addView(option);
                }
                builder.setView(input);
                builder.setTitle("How to handle links in local files:");
                builder.setMessage("Determines the behavior of the browser when a user clicks a link in a local file.");

                builder.setPositiveButton("CONFIRM", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RadioButton selectedButton = input.findViewById(input.getCheckedRadioButtonId());
                        if(selectedButton != null) {
                            getSharedPreferences("settings", MODE_PRIVATE).edit().putString("defaultLocalLinkBehavior", selectedButton.getText().toString()).apply();
                            ((TextView) settingsLocalLinkBehavior.getChildAt(1)).setText(selectedButton.getText());
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

    interface FileBrowserResultFunctions {
        void confirmFunction(String resultingPath);
        void cancelFunction();
    }

    /**
     * Given a startingDirectoryPath, opens a dialog that allows the user to browser the folders in the file system. When the user clicks OK to end
     * the dialog, the method onConfirm.confirmFunction(String) will be called where the String parameter is the value of the EditText at the top.
     * If the user clicks cancel the method onConfirm.cancelFunction will be called.
     * @param startingDirectoryPath - The path to open the file browser dialog at.
     * @param onConfirm - A FileBrowserResultFunctions object that determines what happens when the alertdialog is confirmed or cancelled.
     */
    public void showFileBrowserDialog(String startingDirectoryPath, FileBrowserResultFunctions onConfirm) {
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);

        LinearLayout fileBrowserLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.file_browser, null);
        populateFileBrowser(fileBrowserLayout, startingDirectoryPath);
        ImageButton folderBackButton = fileBrowserLayout.findViewById(R.id.fileBrowserBackButton);
        folderBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentPath = ((EditText) fileBrowserLayout.findViewById(R.id.fileBrowserPathEditText)).getText().toString();

                File subFolder = new File(currentPath.substring(0, currentPath.lastIndexOf("/")));
                if(subFolder.canRead()) {
                    populateFileBrowser(fileBrowserLayout, subFolder.getAbsolutePath());
                }
            }
        });
        builder.setView(fileBrowserLayout);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String resultingPath = ((EditText) fileBrowserLayout.findViewById(R.id.fileBrowserPathEditText)).getText().toString();
                onConfirm.confirmFunction(resultingPath);
            }
        });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onConfirm.cancelFunction();
            }
        });
        builder.show();
    }

    public void populateFileBrowser(LinearLayout fileBrowserLayout, String path) {
        LinearLayout folderList = fileBrowserLayout.findViewById(R.id.fileBrowserLinearLayout);
        folderList.removeAllViews();

        EditText pathEditText = fileBrowserLayout.findViewById(R.id.fileBrowserPathEditText);
        pathEditText.setText(path);
        pathEditText.setSelection(pathEditText.getText().length());

        for(File f : listFolders(path)) {
            Button b = new Button(this);
            b.setText(f.getName());
            b.isClickable();
            b.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            b.setBackground(Drawable.createFromPath("@android:color/transparent"));
            b.setGravity(Gravity.LEFT|Gravity.CENTER_HORIZONTAL);
            b.setPadding(10, b.getPaddingTop(), b.getPaddingRight(), b.getPaddingBottom());
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    populateFileBrowser(fileBrowserLayout, f.getAbsolutePath());
                }
            });
            folderList.addView(b);
        }
    }

    public ArrayList<File> listFolders(String path) {
        ArrayList<File> allFolders = new ArrayList<>();

        File directory = new File(path);
        if(directory.exists() && directory.isDirectory()) {
            for(File f : directory.listFiles()) {
                if(f.isDirectory()) {
                    allFolders.add(f);
                }
            }
        }
        return allFolders;
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
