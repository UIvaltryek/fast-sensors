package com.sensor.unlocker;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.os.Handler;

public class SensorUnlocker implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // Target Call of Duty exclusively
        if (!lpparam.packageName.equals("com.activision.callofduty.shooter")) return;

        // The unified delay override logic (No logging)
        XC_MethodHook delayHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.args[2] = 0; // Force SENSOR_DELAY_FASTEST
            }
        };

        // 1. Hook the hidden internal implementation
        try {
            XposedHelpers.findAndHookMethod(
                "android.hardware.SystemSensorManager", 
                lpparam.classLoader, 
                "registerListenerImpl", 
                SensorEventListener.class, Sensor.class, int.class, Handler.class, int.class, int.class, 
                delayHook
            );
        } catch (Throwable t) {}

        // 2. Blanket-hook all 4 public SensorManager overloads
        try {
            XposedHelpers.findAndHookMethod("android.hardware.SensorManager", lpparam.classLoader, "registerListener", SensorEventListener.class, Sensor.class, int.class, delayHook);
            XposedHelpers.findAndHookMethod("android.hardware.SensorManager", lpparam.classLoader, "registerListener", SensorEventListener.class, Sensor.class, int.class, Handler.class, delayHook);
            XposedHelpers.findAndHookMethod("android.hardware.SensorManager", lpparam.classLoader, "registerListener", SensorEventListener.class, Sensor.class, int.class, int.class, delayHook);
            XposedHelpers.findAndHookMethod("android.hardware.SensorManager", lpparam.classLoader, "registerListener", SensorEventListener.class, Sensor.class, int.class, int.class, Handler.class, delayHook);
        } catch (Throwable t) {}
    }
}
