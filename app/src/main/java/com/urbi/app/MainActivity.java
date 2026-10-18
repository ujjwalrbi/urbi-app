package com.urbi.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.speech.tts.TextToSpeech;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    WebView w;
    TextToSpeech tts;
    long lastBack = 0;
    int insT = 0, insB = 0;
    android.speech.SpeechRecognizer sr;
    android.media.MediaPlayer mp;
    @Override protected void onPause() { try { w.evaluateJavascript("window.bgStop&&bgStop()", null); w.onPause(); } catch (Exception e) { } super.onPause(); }
    @Override protected void onResume() { super.onResume(); try { w.onResume(); w.evaluateJavascript("window.bgStart&&bgStart()", null); pushIns(); } catch (Exception e) { } }
    @Override protected void onDestroy() { try { if (mp != null) mp.release(); if (sr != null) sr.destroy(); } catch (Exception e) { } super.onDestroy(); }
    void pushIns() { if (w != null) w.evaluateJavascript("window.setIns&&setIns(" + insT + "," + insB + ")", null); }

    class Bridge {
        @JavascriptInterface public void say(String s) { try { if (tts != null) tts.speak(s, TextToSpeech.QUEUE_FLUSH, null, "u"); } catch (Exception e) { } }
        @JavascriptInterface public void openApp(String p) { try { Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + p)); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); } catch (Exception e) { } }
        @JavascriptInterface public void startVoice() {
            runOnUiThread(new Runnable() { public void run() {
                try {
                    if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 7);
                        w.evaluateJavascript("onVoiceErr('Allow the microphone, then tap the orb again.')", null);
                        return;
                    }
                    if (!android.speech.SpeechRecognizer.isRecognitionAvailable(MainActivity.this)) {
                        w.evaluateJavascript("onVoiceErr('Speech service is not available on this phone.')", null);
                        return;
                    }
                    if (sr == null) {
                        sr = android.speech.SpeechRecognizer.createSpeechRecognizer(MainActivity.this);
                        sr.setRecognitionListener(new android.speech.RecognitionListener() {
                            public void onReadyForSpeech(Bundle b) { }
                            public void onBeginningOfSpeech() { }
                            public void onRmsChanged(float v) { }
                            public void onBufferReceived(byte[] b) { }
                            public void onEndOfSpeech() { }
                            public void onError(int e) { w.evaluateJavascript("onVoiceErr('Could not hear that. Try again.')", null); }
                            public void onResults(Bundle b) {
                                java.util.ArrayList<String> l = b.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                                String t = (l != null && l.size() > 0) ? l.get(0) : "";
                                w.evaluateJavascript("onVoice(" + JSONObject.quote(t) + ")", null);
                            }
                            public void onPartialResults(Bundle b) { }
                            public void onEvent(int t, Bundle b) { }
                        });
                    }
                    Intent i = new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    i.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    i.putExtra(android.speech.RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
                    i.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "en-IN");
                    sr.startListening(i);
                } catch (Exception e) { w.evaluateJavascript("onVoiceErr('Voice could not start.')", null); }
            } });
        }
        @JavascriptInterface public String songs() {
            try {
                final String pm2 = Build.VERSION.SDK_INT >= 33 ? android.Manifest.permission.READ_MEDIA_AUDIO : android.Manifest.permission.READ_EXTERNAL_STORAGE;
                if (checkSelfPermission(pm2) != PackageManager.PERMISSION_GRANTED) {
                    runOnUiThread(new Runnable() { public void run() { requestPermissions(new String[]{pm2}, 8); } });
                    return "NOPERM";
                }
                JSONArray r = new JSONArray();
                android.database.Cursor c = getContentResolver().query(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{android.provider.MediaStore.Audio.Media._ID, android.provider.MediaStore.Audio.Media.TITLE, android.provider.MediaStore.Audio.Media.ARTIST}, android.provider.MediaStore.Audio.Media.IS_MUSIC + "!=0", null, android.provider.MediaStore.Audio.Media.TITLE + " ASC");
                if (c != null) {
                    int n = 0;
                    while (c.moveToNext() && n < 500) {
                        JSONObject o = new JSONObject();
                        o.put("id", c.getLong(0));
                        o.put("t", c.getString(1));
                        o.put("a", c.getString(2));
                        r.put(o);
                        n++;
                    }
                    c.close();
                }
                return r.toString();
            } catch (Exception e) { return "[]"; }
        }
        @JavascriptInterface public void play(final long id) {
            runOnUiThread(new Runnable() { public void run() {
                try {
                    if (mp != null) mp.release();
                    mp = new android.media.MediaPlayer();
                    mp.setDataSource(MainActivity.this, android.content.ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id));
                    mp.prepare();
                    mp.start();
                    mp.setOnCompletionListener(new android.media.MediaPlayer.OnCompletionListener() { public void onCompletion(android.media.MediaPlayer m) { w.evaluateJavascript("nxt()", null); } });
                } catch (Exception e) { }
            } });
        }
        @JavascriptInterface public boolean toggle() {
            try {
                if (mp == null) return false;
                if (mp.isPlaying()) { mp.pause(); return false; }
                mp.start();
                return true;
            } catch (Exception e) { return false; }
        }
        @JavascriptInterface public String openByName(String q) {
            try {
                PackageManager pm = getPackageManager();
                Intent main = new Intent(Intent.ACTION_MAIN);
                main.addCategory(Intent.CATEGORY_LAUNCHER);
                String best = null, bestPkg = null;
                int bs = 0;
                q = q.toLowerCase().replace(" ", "").trim();
                for (android.content.pm.ResolveInfo ri : pm.queryIntentActivities(main, 0)) {
                    String l = ri.loadLabel(pm).toString();
                    String ll = l.toLowerCase().replace(" ", "");
                    int sc = 0;
                    if (ll.equals(q)) sc = 4; else if (ll.startsWith(q)) sc = 3; else if (ll.contains(q)) sc = 2; else if (q.contains(ll) && ll.length() > 2) sc = 1;
                    if (sc > bs) { bs = sc; best = l; bestPkg = ri.activityInfo.packageName; }
                }
                if (bestPkg == null) return "";
                Intent li = pm.getLaunchIntentForPackage(bestPkg);
                if (li == null) return "";
                li.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(li);
                return best;
            } catch (Exception e) { return ""; }
        }
        @JavascriptInterface public String apps() {
            JSONArray r = new JSONArray();
            try {
                PackageManager pm = getPackageManager();
                for (PackageInfo p : pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)) {
                    ApplicationInfo a = p.applicationInfo;
                    if (a == null) continue;
                    if ((a.flags & ApplicationInfo.FLAG_SYSTEM) != 0 && (a.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) continue;
                    JSONObject o = new JSONObject();
                    o.put("n", pm.getApplicationLabel(a).toString());
                    o.put("p", p.packageName);
                    String ins = pm.getInstallerPackageName(p.packageName);
                    o.put("i", ins == null ? "" : ins);
                    JSONArray pr = new JSONArray();
                    if (p.requestedPermissions != null) for (String s : p.requestedPermissions) pr.put(s);
                    o.put("perms", pr);
                    r.put(o);
                }
            } catch (Exception e) { }
            return r.toString();
        }
        @JavascriptInterface public void vib(int ms) { try { ((android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(ms); } catch (Exception e) { } }
        @JavascriptInterface
        public String info() {
            JSONObject o = new JSONObject();
            try {
                Intent b = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (b != null) {
                    int lv = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int sc = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                    o.put("bat", sc > 0 ? lv * 100 / sc : -1);
                    o.put("temp", b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0);
                    o.put("volt", b.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0));
                    o.put("status", b.getIntExtra(BatteryManager.EXTRA_STATUS, 0));
                    o.put("health", b.getIntExtra(BatteryManager.EXTRA_HEALTH, 0));
                    if (Build.VERSION.SDK_INT >= 34) o.put("cycles", b.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1));
                }
                long tt = 0, ff = 0;
                if (Build.VERSION.SDK_INT >= 26) {
                    try {
                        android.app.usage.StorageStatsManager sq = (android.app.usage.StorageStatsManager) getSystemService(Context.STORAGE_STATS_SERVICE);
                        tt = sq.getTotalBytes(android.os.storage.StorageManager.UUID_DEFAULT);
                        ff = sq.getFreeBytes(android.os.storage.StorageManager.UUID_DEFAULT);
                    } catch (Exception e) { }
                }
                if (tt <= 0) {
                    StatFs s = new StatFs(Environment.getDataDirectory().getPath());
                    tt = s.getTotalBytes();
                    ff = s.getAvailableBytes();
                }
                o.put("stoT", tt);
                o.put("stoF", ff);
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                o.put("ramT", mi.totalMem);
                o.put("ramF", mi.availMem);
                o.put("model", Build.MANUFACTURER + " " + Build.MODEL);
                o.put("andr", Build.VERSION.RELEASE);
                BatteryManager bm = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
                o.put("cur", bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW));
                o.put("chg", bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER));
            } catch (Exception e) { }
            return o.toString();
        }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 28) { android.view.WindowManager.LayoutParams lp = getWindow().getAttributes(); lp.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES; getWindow().setAttributes(lp); }
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        if (Build.VERSION.SDK_INT >= 29) getWindow().setNavigationBarContrastEnforced(false);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        w = new WebView(this);
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() { public void onInit(int st) { if (st == TextToSpeech.SUCCESS) {
            int lr = tts.setLanguage(new java.util.Locale("en", "IN"));
            if (lr == TextToSpeech.LANG_MISSING_DATA || lr == TextToSpeech.LANG_NOT_SUPPORTED) tts.setLanguage(java.util.Locale.getDefault());
            tts.setSpeechRate(0.92f);
            tts.setPitch(1.05f);
            try {
                android.speech.tts.Voice pick = null;
                for (android.speech.tts.Voice v : tts.getVoices()) {
                    if (v.getLocale().getLanguage().equals("en") && "IN".equals(v.getLocale().getCountry()) && !v.isNetworkConnectionRequired()) {
                        if (pick == null || v.getQuality() > pick.getQuality()) pick = v;
                    }
                }
                if (pick != null) tts.setVoice(pick);
            } catch (Exception e) { }
        } } });
        w.setBackgroundColor(Color.parseColor("#020a02"));
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        w.addJavascriptInterface(new Bridge(), "URP");
        w.setOnApplyWindowInsetsListener(new android.view.View.OnApplyWindowInsetsListener() {
            public android.view.WindowInsets onApplyWindowInsets(android.view.View v, android.view.WindowInsets in) {
                float d = getResources().getDisplayMetrics().density;
                int t = in.getSystemWindowInsetTop(), b = in.getSystemWindowInsetBottom();
                if (Build.VERSION.SDK_INT >= 28 && in.getDisplayCutout() != null) t = Math.max(t, in.getDisplayCutout().getSafeInsetTop());
                insT = (int) (t / d); insB = (int) (b / d);
                pushIns();
                return in;
            }
        });
        w.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView v, String u) { pushIns(); }
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if ("file".equals(u.getScheme())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception e) { }
                return true;
            }
        });
        setContentView(w);
        w.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        w.evaluateJavascript("(window.goBack?goBack():false)", new android.webkit.ValueCallback<String>() {
            public void onReceiveValue(String v) {
                if ("true".equals(v)) return;
                long n = System.currentTimeMillis();
                if (n - lastBack < 2000) finish();
                else { lastBack = n; android.widget.Toast.makeText(MainActivity.this, "Press back again to exit", android.widget.Toast.LENGTH_SHORT).show(); }
            }
        });
    }
}
