package com.example.webpdf;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    Toolbar toolbarMenu;
    ListView mhtList;
    LinearLayout folderLayout;
    ImageButton folderBackButton;
    TextView noPermissionsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("folderUpdateNeeded", false).apply();

        toolbarMenu = findViewById(R.id.toolbarMenu);
        mhtList = findViewById(R.id.mhtList);
        folderLayout = findViewById(R.id.folderLayout);
        folderBackButton = findViewById(R.id.folderBackButton);
        noPermissionsTextView = findViewById(R.id.noPermissionsTextView);

        setSupportActionBar(toolbarMenu);

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            createFolders();
        }

        folderBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mhtList.setVisibility(View.GONE);
                ((ScrollView) folderLayout.getParent()).setVisibility(View.VISIBLE);
            }
        });

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if(Intent.ACTION_SEND.equals(action) && type != null) {
            if("text/plain".equals(type)) {
                Intent sendIntent = new Intent(this, BrowserActivity.class);
                sendIntent.putExtra("address", intent.getStringExtra(Intent.EXTRA_TEXT));
                sendIntent.putExtra("local", "false");
                startActivity(sendIntent);
            }
        } else if(Intent.ACTION_VIEW.equals(action) && type != null) {
            File f = new File(intent.getData().getPath().substring(14));
            if(f.exists()) {
                Intent sendIntent = new Intent(this, BrowserActivity.class);
                sendIntent.putExtra("address", "file:///" + f.getAbsolutePath());
                sendIntent.putExtra("local", "true");
                startActivity(sendIntent);
                finish();
            } else {
                Toast.makeText(this, "Could not find the file to open.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void createFolders() {
        folderLayout.removeAllViews();

        File defaultFolder = new File(getSharedPreferences("settings", MODE_PRIVATE).getString("defaultCrawlerFolder", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()));
        ArrayList<String> crawlerPaths = new ArrayList<>();
        addFilesCrawler(crawlerPaths, defaultFolder);
        HashSet<File> crawlerFolders = new HashSet<>(); // put in a hashset to ensure no duplicate folders

        for(String s : crawlerPaths) {
            crawlerFolders.add(new File(s).getParentFile());
        }

        ArrayList<File> crawlerFoldersArrayList = new ArrayList<>(); // put the hashset contents into an ArrayList so we can sort them
        for(File f : crawlerFolders) {
            crawlerFoldersArrayList.add(f);
        }
        crawlerFoldersArrayList.sort(new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        for(final File f : crawlerFoldersArrayList) {
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
                    ((ScrollView) folderLayout.getParent()).setVisibility(View.GONE);
                    populateListViewByDirectory(f.getAbsolutePath());
                    mhtList.setVisibility(View.VISIBLE);
                }
            });
            folderLayout.addView(b);

            View borderLine = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.height = (int) getResources().getDimension(R.dimen.lineBreakHeight);
            borderLine.setLayoutParams(params);
            borderLine.setBackgroundColor(ContextCompat.getColor(this, R.color.black));
            folderLayout.addView(borderLine);
        }
    }

    private void addFilesCrawler(ArrayList<String> al, File directory) {
        if(directory.exists() && directory.isDirectory()) {
            for(File f : directory.listFiles()) {
                if(f.isDirectory()) {
                    addFilesCrawler(al, f);
                } else if(f.getAbsolutePath().matches("(?i).*\\.mhtm?l?")) {
                    al.add(f.getAbsolutePath());
                }
            }
        }
    }

    private void addFilesFromFolder(ArrayList<String> al, File directory) {
        if(directory.exists() && directory.isDirectory()) {
            for(File f : directory.listFiles()) {
                if(f.getAbsolutePath().matches("(?i).*\\.mhtm?l?")) {
                    al.add(f.getAbsolutePath());
                }
            }
        }
    }

    private void alphaNumericSort(ArrayList<String> al) {
        // Get Longest Digit Sequence out of all Strings in al.
        int longestNumberSequence = 0;
        for(String s : al) {
            for(int i = 0; i < s.length(); i++) {
                if(Character.isDigit(s.charAt(i))) {
                    longestNumberSequence = 1;
                    int j = i;
                    while(j < s.length() && Character.isDigit(s.charAt(j))) {
                        longestNumberSequence += 1;
                        j += 1;
                    }
                }
            }
        }

        final int finalLongestNumberSequence = longestNumberSequence;

        al.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                String o1Numbers = "";
                ArrayList<Pair<Pair<Integer, Integer>, String>> o1Locations = new ArrayList<>();
                Matcher m = Pattern.compile("\\d+").matcher(o1);
                while(m.find()) {
                    String replacementDigit = m.group();
                    while(replacementDigit.length() < finalLongestNumberSequence) {
                        replacementDigit = "0" + replacementDigit;
                    }
                    o1Locations.add(new Pair<>(new Pair<>(m.start(), m.end()), replacementDigit));
                }
                int o1StringStart = 0;
                for(Pair p : o1Locations) {
                    int start = ((Pair<Integer, Integer>) p.first).first;
                    int end = ((Pair<Integer, Integer>) p.first).second;
                    String replace = (String) p.second;
                    o1Numbers += o1.substring(o1StringStart, start);
                    o1Numbers += replace;
                    o1StringStart = end;
                }
                o1Numbers += o1.substring(o1StringStart);

                String o2Numbers = "";
                ArrayList<Pair<Pair<Integer, Integer>, String>> o2Locations = new ArrayList<>();
                Matcher m2 = Pattern.compile("\\d+").matcher(o2);
                while(m2.find()) {
                    String replacementDigit = m2.group();
                    while(replacementDigit.length() < finalLongestNumberSequence) {
                        replacementDigit = "0" + replacementDigit;
                    }
                    o2Locations.add(new Pair<>(new Pair<>(m2.start(), m2.end()), replacementDigit));
                }
                int o2StringStart = 0;
                for(Pair p : o2Locations) {
                    int start = ((Pair<Integer, Integer>) p.first).first;
                    int end = ((Pair<Integer, Integer>) p.first).second;
                    String replace = (String) p.second;
                    o2Numbers += o2.substring(o2StringStart, start);
                    o2Numbers += replace;
                    o2StringStart = end;
                }
                o2Numbers += o2.substring(o2StringStart);

                return o1Numbers.compareTo(o2Numbers);
            }
       });
    }

    private void populateListViewByDirectory(final String directoryPath) {
        mhtList.setAdapter(null); // clear listview
        ArrayList<String> listValues = new ArrayList<>();
        addFilesFromFolder(listValues, new File(directoryPath));
        ArrayList<String> listTitles = new ArrayList<>();

        for(String s : listValues) {
            String[] splitString = s.split("/");
            listTitles.add(splitString[splitString.length - 1]);
        }
        alphaNumericSort(listTitles);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.list_entry, R.id.listEntryTitle, listTitles);

        mhtList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String path = (String) parent.getItemAtPosition(position);
                File f = new File(directoryPath + "/" + path);
                path = "file:///" + f.getAbsolutePath();

                Intent sendIntent = new Intent(MainActivity.this, BrowserActivity.class);
                sendIntent.putExtra("address", path);
                sendIntent.putExtra("local", "true");
                startActivity(sendIntent);
            }
        });
        mhtList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(final AdapterView<?> parent, View view, final int position, long id) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Fix Blob Images");
                builder.setMessage("Do you want to attempt to fix blob images? This will create a copy of the file.");

                builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String fileName = ((String) parent.getItemAtPosition(position));
                        String newFileName = fileName.replaceFirst("(?i)\\.mhtm?l?$", "") + " Blob Fix";
                        File filePath = new File(directoryPath + "/" + newFileName + ".mht");
                        if(!filePath.exists()) {
                            try {
                                String[] blobReplacements = new String[]{"b(=\\r?\\n)lob:(http)", "bl(=\\r?\\n)ob:(http)", "blo(=\\r?\\n)b:(http)", "blob(=\\r?\\n):(http)", "blob:(=\\r?\\nhtt)(p)", "blob:(h=\\r?\\ntt)(p)", "blob:(ht=\\r?\\ntt)(p)", "blob:(htt=\\r?\\n)(p)", "blob:(htt)(p)"};

                                InputStream fileData = new FileInputStream(new File(directoryPath + "/" + fileName));
                                BufferedReader reader = new BufferedReader(new InputStreamReader(fileData));
                                String line = reader.readLine();

                                boolean startOfFile = true;
                                boolean endOfFile = false;
                                String carryOverString = "";
                                while(!endOfFile) {

                                    StringBuilder tempString = new StringBuilder();
                                    for(int i = 0; i < 20; i++) {
                                        if(line != null) {
                                            if(i == 0 && !startOfFile) { // it doesn't add the carryOverString if we're at the start of the file
                                                tempString.append(carryOverString);
                                                tempString.append("\r\n");
                                            }
                                            tempString.append(line);
                                            tempString.append("\r\n");
                                            line = reader.readLine();
                                        } else {
                                            endOfFile = true;
                                        }
                                    }

                                    startOfFile = false;

                                    String replaceString = tempString.toString();
                                    for(String s : blobReplacements) {
                                        replaceString = replaceString.replaceAll(s, "$1$2");
                                    }
                                    if(!endOfFile) {
                                        replaceString = replaceString.substring(0, replaceString.lastIndexOf("\r\n"));
                                        carryOverString = replaceString.substring(replaceString.lastIndexOf("\r\n") + "\r\n".length());
                                        replaceString = replaceString.substring(0, replaceString.lastIndexOf("\r\n") + "\r\n".length()); // [19 + 20n, 20 + 20n] combination has been handled, so we remove the 20 + 20n line so we can use it for [20 + 20n, 21 + 20n]
                                    }
                                    BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true));
                                    writer.append(replaceString);
                                    writer.close();
                                }
                                populateListViewByDirectory(directoryPath);
                            } catch(IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "File with Blob Fix at the end already exists. Consider deleting it or renaming it.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
                builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing.
                    }
                });
                builder.show();

                return true;
            }
        });
        mhtList.setAdapter(adapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    createFolders();
                } else {
                    noPermissionsTextView.setVisibility(View.VISIBLE);
                }
                return;
            }
            default:
                Toast.makeText(this, "No exceptions with requestCode: " + requestCode, Toast.LENGTH_LONG).show();
                return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menuBrowserOpen:
                Intent sendIntent = new Intent(this, BrowserActivity.class);
                sendIntent.putExtra("address", "https://www.google.com/");
                sendIntent.putExtra("local", "false");
                startActivity(sendIntent);
                return true;
            case R.id.menuSettings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
               return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(getSharedPreferences("settings", MODE_PRIVATE).getBoolean("folderUpdateNeeded", false)) {
            getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("folderUpdateNeeded", false).apply();
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                createFolders();
            }
        }
    }
}
