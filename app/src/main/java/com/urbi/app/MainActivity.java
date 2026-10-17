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
import org.json.JSONObject;

public class MainActivity extends Activity {
    WebView w;

    class Bridge {
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
                StatFs s = new StatFs(Environment.getDataDirectory().getPath());
                o.put("stoT", s.getTotalBytes());
                o.put("stoF", s.getAvailableBytes());
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                o.put("ramT", mi.totalMem);
                o.put("ramF", mi.availMem);
                o.put("model", Build.MANUFACTURER + " " + Build.MODEL);
                o.put("andr", Build.VERSION.RELEASE);
            } catch (Exception e) { }
            return o.toString();
        }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        w = new WebView(this);
        w.setBackgroundColor(Color.parseColor("#020a02"));
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        w.addJavascriptInterface(new Bridge(), "URP");
        w.setWebViewClient(new WebViewClient() {
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
        if (w.canGoBack()) w.goBack(); else super.onBackPressed();
    }
}
