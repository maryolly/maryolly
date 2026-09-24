package com.nanoshift.optimizer;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Locale;

public final class PerformanceEngine {
    public static final class Snapshot {
        public final String socManufacturer,socModel;
        public final int cores,thermalStatus,score;
        public final long maxFreqKhz,totalRam,availRam;
        public final float thermalHeadroom;
        Snapshot(String a,String b,int c,long d,long e,long f,int g,float h,int i){
            socManufacturer=a;socModel=b;cores=c;maxFreqKhz=d;totalRam=e;availRam=f;thermalStatus=g;thermalHeadroom=h;score=i;
        }
    }

    private final Context context;
    private boolean active;
    public PerformanceEngine(Context c){context=c.getApplicationContext();}
    public boolean isActive(){return active;}

    public boolean activate(Activity a){
        PowerManager pm=a.getSystemService(PowerManager.class);
        boolean supported=Build.VERSION.SDK_INT>=24 && pm!=null && pm.isSustainedPerformanceModeSupported();
        if(supported){a.getWindow().setSustainedPerformanceMode(true);active=true;}
        a.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        return supported;
    }
    public void deactivate(Activity a){
        if(Build.VERSION.SDK_INT>=24)a.getWindow().setSustainedPerformanceMode(false);
        a.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        active=false;
    }

    public Snapshot snapshot(){
        String maker=Build.VERSION.SDK_INT>=31?safe(Build.SOC_MANUFACTURER):"Unavailable";
        String model=Build.VERSION.SDK_INT>=31?safe(Build.SOC_MODEL):"Unavailable";
        int cores=Runtime.getRuntime().availableProcessors();
        long freq=readMax();
        ActivityManager am=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi=new ActivityManager.MemoryInfo(); am.getMemoryInfo(mi);
        PowerManager pm=context.getSystemService(PowerManager.class);
        int thermal=Build.VERSION.SDK_INT>=29?pm.getCurrentThermalStatus():PowerManager.THERMAL_STATUS_NONE;
        float headroom=1f;
        if(Build.VERSION.SDK_INT>=30)try{headroom=pm.getThermalHeadroom(0);}catch(Throwable ignored){}
        float cpu=Math.min(55f,Math.max(10f,cores*3.5f)+(freq>0?Math.min(15f,freq/250000f):0));
        float ram=mi.totalMem>0?15f+Math.min(15f,(mi.availMem/(float)mi.totalMem)*15f):20f;
        float th=Math.max(0,Math.min(20,headroom*20));
        int s=Math.max(0,Math.min(100,Math.round(cpu+ram+th+(active?5:0))));
        return new Snapshot(maker,model,cores,freq,mi.totalMem,mi.availMem,thermal,headroom,s);
    }

    private long readMax(){
        long max=0; File root=new File("/sys/devices/system/cpu");
        File[] ds=root.listFiles((d,n)->n.matches("cpu\\d+"));
        if(ds==null)return 0;
        for(File d:ds){long v=read(new File(d,"cpufreq/cpuinfo_max_freq"));if(v>max)max=v;}
        return max;
    }
    private long read(File f){
        try(BufferedReader r=new BufferedReader(new FileReader(f))){return Long.parseLong(r.readLine().trim());}
        catch(Exception e){return 0;}
    }
    private static String safe(String s){return s==null||s.trim().isEmpty()?"Unknown":s.trim();}
    public static String formatFreq(long k){return k<=0?"Unavailable":String.format(Locale.US,"%.2f GHz",k/1000000.0);}
    public static String thermalLabel(int s){
        switch(s){
            case PowerManager.THERMAL_STATUS_LIGHT:return "Light";
            case PowerManager.THERMAL_STATUS_MODERATE:return "Moderate";
            case PowerManager.THERMAL_STATUS_SEVERE:return "Severe";
            case PowerManager.THERMAL_STATUS_CRITICAL:return "Critical";
            case PowerManager.THERMAL_STATUS_EMERGENCY:return "Emergency";
            case PowerManager.THERMAL_STATUS_SHUTDOWN:return "Shutdown risk";
            default:return "Nominal";
        }
    }
}
