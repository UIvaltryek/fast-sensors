package com.sensor.unlocker;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;

public class SensorUnlocker implements IXposedHookLoadPackage {
    
    // A silent background listener to keep the uncalibrated pipelines open
    private static final SensorEventListener silentListener = new SensorEventListener() {
        @Override public void onSensorChanged(android.hardware.SensorEvent event) {}
        @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // Universal Hook: Removed package specific restrictions completely. 
        // This will now intercept sensor requests system-wide.

        XC_MethodHook delayHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                // Force max hardware polling rate
                param.args[2] = 0; 

                Sensor sensor = (Sensor) param.args[1];
                
                if (sensor != null) {
                    SensorManager sm = (SensorManager) param.thisObject;
                    
                    // 1. If an app asks for Calibrated Accel (Type 1), wake up Uncalibrated Accel (Type 35)
                    if (sensor.getType() == 1) { 
                        Sensor uncaliAcc = sm.getDefaultSensor(35); 
                        if (uncaliAcc != null) {
                            try { sm.registerListener(silentListener, uncaliAcc, 0); } catch (Throwable t) {}
                        }
                    }
                    
                    // 2. If an app asks for Calibrated Mag (Type 2), wake up Uncalibrated Mag (Type 14)
                    if (sensor.getType() == 2) { 
                        Sensor uncaliMag = sm.getDefaultSensor(14); 
                        if (uncaliMag != null) {
                            try { sm.registerListener(silentListener, uncaliMag, 0); } catch (Throwable t) {}
                        }
                    }
                }
            }
        };

        // Hook the hidden internal implementation
        try {
            XposedHelpers.findAndHookMethod(
                "android.hardware.SystemSensorManager", 
                lpparam.classLoader, 
                "registerListenerImpl", 
                SensorEventListener.class, Sensor.class, int.class, Handler.class, int.class, int.class, 
                delayHook
            );
        } catch (Throwable t) {}

        // Blanket-hook all 4 public SensorManager overloads
        try {
            XposedHelpers.findAndHookMethod("android.hardware.SensorManager", lpparam.classLoader, "registerListener", SensorEventListener.class, Sensor.class, int.class, delayHook);
            XposedHelpers.findAndHookMethod("android.hardware.SensorManager", lpparam.classLoader, "registerListener", SensorEventListener.class, Sensor.class, int.class, Handler.class, delayHook);
            XposedHelpers.findAndHookMethod("android.hardware.SensorManager", lpparam.classLoader, "registerListener", SensorEventListener.class, Sensor.class, int.class, int.class, delayHook);
            XposedHelpers.findAndHookMethod("android.hardware.SensorManager", lpparam.classLoader, "registerListener", SensorEventListener.class, Sensor.class, int.class, int.class, Handler.class, delayHook);
        } catch (Throwable t) {}
    }
}
