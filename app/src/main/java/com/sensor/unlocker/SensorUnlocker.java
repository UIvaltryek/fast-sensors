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
    
    // A silent background listener to force the Uncalibrated hardware to turn on
    private static final SensorEventListener silentListener = new SensorEventListener() {
        @Override public void onSensorChanged(android.hardware.SensorEvent event) {}
        @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.activision.callofduty.shooter")) return;

        XC_MethodHook delayHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.args[2] = 0; // Force SENSOR_DELAY_FASTEST

                Sensor sensor = (Sensor) param.args[1];
                
                // When Unity asks for the Calibrated Compass (Type 2) for the minimap
                if (sensor != null && sensor.getType() == 2) { 
                    SensorManager sm = (SensorManager) param.thisObject;
                    Sensor uncali = sm.getDefaultSensor(14); // 14 = Uncalibrated Compass
                    
                    if (uncali != null) {
                        // Secretly open the Uncalibrated 200Hz pipeline for our C++ module to steal
                        try {
                            sm.registerListener(silentListener, uncali, 0);
                        } catch (Throwable t) {}
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
