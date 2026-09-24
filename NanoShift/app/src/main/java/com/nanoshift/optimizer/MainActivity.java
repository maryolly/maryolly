package com.nanoshift.optimizer;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Locale;

public class MainActivity extends Activity {
    private PerformanceEngine engine;
    private TextView score, status, details;
    private ProgressBar bar;
    private Button toggle;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        engine = new PerformanceEngine(this);
        getWindow().setStatusBarColor(Color.rgb(10,13,18));
        getWindow().setNavigationBarColor(Color.rgb(10,13,18));
        build();
        refresh();
    }

    private void build() {
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20),dp(18),dp(20),dp(28));
        root.setBackgroundColor(Color.rgb(10,13,18));
        sv.addView(root);

        TextView brand = txt("NANOSHIFT",13,Color.rgb(124,247,178),Typeface.BOLD);
        root.addView(brand);
        root.addView(txt("2nm-style performance profile",28,Color.WHITE,Typeface.BOLD), lp(-1,-2,0,8,0,8));
        root.addView(txt("Uses Android-exposed performance hints and device telemetry. It cannot change the physical semiconductor process node.",14,Color.rgb(168,176,191),Typeface.NORMAL), lp(-1,-2,0,0,0,18));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18),dp(18),dp(18),dp(18));
        card.setBackgroundColor(Color.rgb(22,27,36));
        score = txt("--",44,Color.WHITE,Typeface.BOLD);
        card.addView(score);
        card.addView(txt("NanoScore / 100",14,Color.rgb(168,176,191),Typeface.BOLD));
        bar = new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        card.addView(bar,lp(-1,dp(8),0,12,0,10));
        status = txt("Profile inactive",14,Color.rgb(168,176,191),Typeface.BOLD);
        card.addView(status);
        root.addView(card,lp(-1,-2,0,0,0,14));

        toggle = button("ACTIVATE NANOSHIFT PROFILE");
        toggle.setOnClickListener(v -> {
            if (engine.isActive()) {
                engine.deactivate(this);
            } else {
                engine.activate(this);
            }
            refresh();
        });
        root.addView(toggle,lp(-1,dp(52),0,0,0,18));

        details = txt("",15,Color.WHITE,Typeface.NORMAL);
        root.addView(details);
        root.addView(txt("This app stays within Android user-space controls. It does not rewrite governors, voltage tables, firmware, bootloaders or silicon.",12,Color.rgb(137,148,166),Typeface.NORMAL),lp(-1,-2,0,18,0,0));
        setContentView(sv);
    }

    private void refresh() {
        PerformanceEngine.Snapshot s = engine.snapshot();
        score.setText(String.valueOf(s.score));
        bar.setProgress(s.score);
        toggle.setText(engine.isActive() ? "DEACTIVATE NANOSHIFT PROFILE" : "ACTIVATE NANOSHIFT PROFILE");
        status.setText(engine.isActive() ? "Profile active · sustained-performance hint requested" : "Profile inactive");
        status.setTextColor(engine.isActive() ? Color.rgb(124,247,178) : Color.rgb(168,176,191));
        details.setText(String.format(Locale.US,
            "SoC: %s %s\nCPU: %d logical cores · max observed: %s\nRAM: %.1f GB free / %.1f GB total\nThermal: %s · headroom %.2f",
            s.socManufacturer,s.socModel,s.cores,PerformanceEngine.formatFreq(s.maxFreqKhz),
            s.availRam/1073741824.0,s.totalRam/1073741824.0,
            PerformanceEngine.thermalLabel(s.thermalStatus),s.thermalHeadroom));
    }

    private TextView txt(String s,float size,int c,int style) {
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(c); t.setTypeface(Typeface.DEFAULT,style); return t;
    }
    private Button button(String s) {
        Button b=new Button(this); b.setText(s); b.setTextColor(Color.rgb(10,13,18)); b.setTextSize(13); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return b;
    }
    private LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h); p.setMargins(dp(l),dp(t),dp(r),dp(b)); return p;
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
