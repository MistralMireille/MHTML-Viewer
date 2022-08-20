package com.example.webpdf;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

public class BrowserActivity extends AppCompatActivity {

    WebView soleWebView;
    Toolbar toolbarMenu;
    EditText urlEditText;
    String preserveUrl;
    int localPageIndex;
    MenuItem menuSavePage;
    MenuItem menuRemoveElements;
    MenuItem menuNextPage;
    MenuItem menuPreviousPage;
    HashMap<String, String> localFileMap;
    ArrayList<String> filepaths;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_browser);

        final ArrayList<String> adCheckerStrings = new ArrayList<>();
        InputStream adServerRaw = getResources().openRawResource(R.raw.ad_server_query);
        BufferedReader reader = new BufferedReader(new InputStreamReader(adServerRaw));
        try {
            String line = reader.readLine();
            adCheckerStrings.add(line);
            while(line != null) {
                adCheckerStrings.add(line);
                line = reader.readLine();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }

        soleWebView = findViewById(R.id.soleWebView);
        toolbarMenu = findViewById(R.id.toolbarMenu);
        urlEditText = findViewById(R.id.urlEditText);
        preserveUrl = "";

        soleWebView.getSettings().setJavaScriptEnabled(true);
        soleWebView.getSettings().setDomStorageEnabled(true);
        soleWebView.getSettings().setDatabaseEnabled(true);
        soleWebView.getSettings().setLoadWithOverviewMode(true);
        soleWebView.getSettings().setUseWideViewPort(true);
        soleWebView.getSettings().setBuiltInZoomControls(true);
        soleWebView.getSettings().setDisplayZoomControls(false);
        soleWebView.getSettings().setAllowContentAccess(false);
        soleWebView.getSettings().setGeolocationEnabled(false);
        soleWebView.getSettings().setAllowFileAccess(false);
        soleWebView.getSettings().setUserAgentString(soleWebView.getSettings().getUserAgentString().replace("; wv", "")); // android studio developer pages 403 when they see it's a webview. It checks for wv to see if it's a webview, so I got rid of it.

        soleWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        soleWebView.setWebViewClient(new WebViewClient() {

            @Override
            public WebResourceResponse shouldInterceptRequest (final WebView view, WebResourceRequest request) {
                //If it's a local file, we don't have to check for ads.
                if(request.getUrl().getScheme().equals("file")) {
                    return super.shouldInterceptRequest(view, request);
                } else {
                    view.post(new Runnable() {
                        @Override
                        public void run() {
                            if(soleWebView.getSettings().getAllowFileAccess()) {
                                soleWebView.getSettings().setAllowFileAccess(false);
                            }
                        }
                    });
                }

                String url = request.getUrl() + "";
                boolean foundAd = false;
                for(String s : adCheckerStrings) {
                    if(s.startsWith("||") && request.getUrl().getHost().equals(s.substring(2))) {
                        foundAd = true;
                        break;
                    }else if(url.contains(s)) {
                        foundAd = true;
                        break;
                    }
                }
                if(foundAd) {
                    return new WebResourceResponse("text/plain", "utf8", new ByteArrayInputStream("".getBytes())); // return an empty response
                } else {
                    return super.shouldInterceptRequest(view, request);
                }
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                if(view.getUrl().startsWith("file:///")) {
                    menuSavePage.setVisible(false);
                    menuRemoveElements.setVisible(false);
                    urlEditText.setText(view.getTitle()); // File urls don't don't really need to be shown, especially since they're pretty hard to read. I think it's better to show the title so that the user can get some information about what local file they're on.
                } else {
                    view.getSettings().setAllowFileAccess(false); // might be unnecessary since shouldOverrideUrlLoading also sets it to false on non-local
                    menuSavePage.setVisible(true);
                    menuRemoveElements.setVisible(true);
                    menuNextPage.setVisible(false);
                    menuPreviousPage.setVisible(false);
                    urlEditText.setText(url);
                }
                super.doUpdateVisitedHistory(view, url, isReload);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String localLinkBehavior = getSharedPreferences("settings", MODE_PRIVATE).getString("defaultLocalLinkBehavior", "Ask User");
                if(view.getUrl().startsWith("file:///") && request.getUrl().getScheme().equals("cid") && request.getUrl().toString().endsWith("@mhtml.blink") && request.getUrl().getHost() == null) {
                    return super.shouldOverrideUrlLoading(view, request);
                } else if(view.getUrl().startsWith("file:///") && localFileMap.containsKey(request.getUrl().toString())) {
                    switch(localLinkBehavior) {
                        case "Ask User":
                            AlertDialog.Builder builder = new AlertDialog.Builder(BrowserActivity.this);
                            builder.setTitle("Local Version Found");
                            builder.setMessage("Found a local version of this link. Would you like to load that instead?");
                            builder.setPositiveButton("Load Local Version", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    localPageIndex = filepaths.indexOf(localFileMap.get(request.getUrl().toString()));
                                    view.getSettings().setAllowFileAccess(true);
                                    view.loadUrl("file:///" + fixCharacters(localFileMap.get(request.getUrl().toString())));
                                }
                            });
                            builder.setNegativeButton("Load Online Version", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    view.getSettings().setAllowFileAccess(false);
                                    view.loadUrl(request.getUrl().toString());
                                }
                            });
                            builder.show();
                            return true; // returns true stopping the original url from loading.
                        case "Local Only":
                            view.getSettings().setAllowFileAccess(true);
                            view.loadUrl("file:///" + fixCharacters(localFileMap.get(request.getUrl().toString())));
                            return true;
                        default:
                            return true;
                    }
                } else if(view.getUrl().startsWith("file:///")) {
                    if(localLinkBehavior.equals("Ask User")) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(BrowserActivity.this);
                        builder.setTitle("No Local Versions Found");
                        builder.setMessage("Do you want to load the online version? You won't be able to press back to go to the local version again.");
                        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                view.getSettings().setAllowFileAccess(false);
                                view.loadUrl(request.getUrl().toString());
                            }
                        });
                        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // do nothing
                            }
                        });
                        builder.show();
                    }
                    return true;
                } else {
                    view.getSettings().setAllowFileAccess(false);
                    return super.shouldOverrideUrlLoading(view, request);
                }
            }
        });

        setSupportActionBar(toolbarMenu);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        urlEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_GO) {
                    String potentialUrl = urlEditText.getText() + "";
                    if(Patterns.WEB_URL.matcher(potentialUrl).matches()) {
                        if(!potentialUrl.startsWith("https://")) {
                            potentialUrl = "https://" + potentialUrl;
                        }
                        soleWebView.loadUrl(potentialUrl);
                        urlEditText.clearFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(BrowserActivity.this.getCurrentFocus().getWindowToken(), 0);
                    } else {
                        soleWebView.loadUrl("https://duckduckgo.com/?q=" + potentialUrl);
                    }
                }
                return false;
            }
        });

        urlEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    preserveUrl = urlEditText.getText() + "";
                } else {
                    urlEditText.setText(preserveUrl);
                }
            }
        });

        Intent intent = getIntent();
        String address = intent.getStringExtra("address");
        urlEditText.setText(address);

        if(intent.hasExtra("local") && intent.getStringExtra("local").equals("true")) {
            soleWebView.getSettings().setAllowFileAccess(true);
            localPageIndex = getIntent().getIntExtra("position", -1);
            filepaths = (ArrayList<String>) getIntent().getSerializableExtra("filepaths");
        }

        if(intent.hasExtra("map")) {
            localFileMap = (HashMap<String, String>) intent.getSerializableExtra("map");
        }
        soleWebView.loadUrl(fixCharacters(address));
    }

    private String fixCharacters(String address) {
        Character[] specialCharacters = new Character[] {'%', '?', '#'};
        String[] replacements = new String[] {"%25", "%3f", "%23"};
        for(int i = 0; i < specialCharacters.length; i++) {
            if(address.contains(specialCharacters[i] + "")) {
                address = address.replace(specialCharacters[i] + "", replacements[i]);
            }
        }
        return address;
    }

    @Override
    public void onBackPressed() {
        if(toolbarMenu.getVisibility() == View.GONE) {
            toolbarMenu.setVisibility(View.VISIBLE);
        } else if (soleWebView.canGoBack()) {
            soleWebView.goBack();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.browser_menu, menu);

        menuSavePage = menu.findItem(R.id.menuSavePage);
        menuRemoveElements = menu.findItem(R.id.menuRemoveElements);
        menuNextPage = menu.findItem(R.id.menuNextPage);
        menuPreviousPage = menu.findItem(R.id.menuPreviousPage);
        if(getIntent().getStringExtra("local").equals("true")) {
            menuNextPage.setVisible(true);
            menuPreviousPage.setVisible(true);
        } else {
            menuSavePage.setVisible(true);
            menuRemoveElements.setVisible(true);
        }

        return super.onCreateOptionsMenu(menu);
    }

    private void saveWebArchiveAsDialog(final File directory, String invalidFileName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        builder.setView(input);
        builder.setTitle(invalidFileName + " already exists. Choose a different file name.");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(!input.getText().equals("")) {
                    String fileName = input.getText().toString();
                    fileName = Pattern.compile("(?i)\\.mhtm?l?$").matcher(fileName).replaceFirst(""); // get rid of file extension if there is one
                    fileName = fileName.replace("\n", "").replace("\r", "");
                    File filePath = new File(directory.getAbsolutePath() + "/" + fileName + ".mht");
                    if(!filePath.exists()) {
                        soleWebView.saveWebArchive(filePath.getAbsolutePath(), false, new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String value) {
                                if(value == null) {
                                    Toast.makeText(BrowserActivity.this, "Failed to save page.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(BrowserActivity.this, "Saved " + value, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } else {
                        saveWebArchiveAsDialog(directory, filePath.getName());
                    }
                }
            }
        });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // nothing
            }
        });
        builder.show();
    }

    private String getMhtmlUrlFromFile(File f) throws IOException {
        InputStream fileData = new FileInputStream(f);
        BufferedReader reader = new BufferedReader(new InputStreamReader(fileData));
        reader.readLine();
        String[] urlLine = reader.readLine().split(" ");
        if(urlLine[0].equals("Snapshot-Content-Location:")) {
            return urlLine[1];
        } else {
            for(int i = 0; i < 30; i++) {
                String backupLine = reader.readLine();
                if(backupLine.startsWith("Content-Location:")) {
                    return backupLine.split(" ")[1];
                }
            }
            return "Didn't find a url.";
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            case R.id.menuSavePage:
                String fileName = soleWebView.getTitle();
                fileName = fileName.replace("\n", "").replace("\r", "");
                String folderName = getSharedPreferences("settings", MODE_PRIVATE).getString("defaultSaveLocationFolder", Environment.getExternalStorageDirectory() + "/Download");
                File folderPath = new File(folderName);
                File filePath = new File(folderPath.getAbsolutePath() + "/" + fileName + ".mht");
                if(!filePath.exists()) {
                    soleWebView.saveWebArchive(filePath.getAbsolutePath(), false, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            if(value == null) {
                                Toast.makeText(BrowserActivity.this, "Failed to save page.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(BrowserActivity.this, "Saved " + value, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    saveWebArchiveAsDialog(folderPath, filePath.getName());
                }
                return true;
            case R.id.menuHide:
                toolbarMenu.setVisibility(View.GONE);
                return true;
            case R.id.menuRemoveElements:
                if(item.getTitle().equals("Remove Elements")) {
                    soleWebView.evaluateJavascript("function removeElementsFromPageWebPDF() { let e = window.event; e.stopPropagation; e.stopImmediatePropagation; e.preventDefault(); if(e.target.getAttribute('highlightedDelete') === null) { e.target.setAttribute('highlightedDelete', ''); e.target.style.border = 'solid blue 1px'; } else { e.target.remove(); } } document.addEventListener('click', removeElementsFromPageWebPDF, true);", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {

                        }
                    });
                    item.setTitle("Stop Removing Elements");
                } else {
                    soleWebView.evaluateJavascript("document.removeEventListener('click', removeElementsFromPageWebPDF, true);", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {

                        }
                    });
                    item.setTitle("Remove Elements");
                }
                return true;
            case R.id.menuOpenFileInBrowser:
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_VIEW);
                if(soleWebView.getUrl().startsWith("http")) {
                    sendIntent.setData(Uri.parse(soleWebView.getUrl()));
                    if(sendIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(sendIntent);
                    }
                } else if(soleWebView.getUrl().startsWith("file:///")) {
                    File localFile = new File(Uri.decode(soleWebView.getUrl().substring(8)));
                    if(localFile.exists()) {
                        try {
                            String urlString = getMhtmlUrlFromFile(localFile);
                            if(urlString.equals("Didn't find a url.")) {
                                Toast.makeText(BrowserActivity.this, urlString, Toast.LENGTH_LONG).show();
                            } else {
                                sendIntent.setData(Uri.parse(urlString));
                                if(sendIntent.resolveActivity(getPackageManager()) != null) {
                                    startActivity(sendIntent);
                                }
                            }
                        } catch(IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return true;
            case R.id.menuNextPage:
                if(localPageIndex != -1) {
                    changeLocalPage(localPageIndex + 1);
                }
                return true;
            case R.id.menuPreviousPage:
                if(localPageIndex != -1) {
                    changeLocalPage(localPageIndex - 1);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void changeLocalPage(int position) {
        ArrayList<String> orderedFileNames = (ArrayList<String>) getIntent().getSerializableExtra("filenames");
        if(orderedFileNames != null && position > -1 && position < orderedFileNames.size()) {
            int lastSlashIndex = soleWebView.getUrl().lastIndexOf("/");
            soleWebView.loadUrl(soleWebView.getUrl().substring(0, lastSlashIndex) + "/" + fixCharacters(orderedFileNames.get(position)));
            localPageIndex = position;
        } else {
            Toast.makeText(BrowserActivity.this, "This is the " + (position <= -1 ? "first" : "last") + " file.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        soleWebView.clearCache(true);
        soleWebView.clearFormData();
        soleWebView.clearHistory();
        soleWebView.clearSslPreferences();

        WebStorage.getInstance().deleteAllData();
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();

        super.onDestroy();
    }
}
