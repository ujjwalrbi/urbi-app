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
    void pushIns() { if (w != null) w.evaluateJavascript("window.setIns&&setIns(" + insT + "," + insB + ")", null); }

    class Bridge {
        @JavascriptInterface public void say(String s) { try { if (tts != null) tts.speak(s, TextToSpeech.QUEUE_FLUSH, null, "u"); } catch (Exception e) { } }
        @JavascriptInterface public void openApp(String p) { try { Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + p)); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); } catch (Exception e) { } }
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
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() { public void onInit(int st) { if (st == TextToSpeech.SUCCESS) tts.setLanguage(java.util.Locale.getDefault()); } });
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
