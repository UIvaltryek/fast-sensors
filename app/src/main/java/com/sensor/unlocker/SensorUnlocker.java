package com.sensor.unlocker;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
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

        XposedBridge.log("SensorUnlocker: CODM detected. Deploying hooks...");

        // The unified delay override logic
        XC_MethodHook delayHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                int originalDelay = (int) param.args[2];
                param.args[2] = 0; // Force SENSOR_DELAY_FASTEST
                XposedBridge.log("SensorUnlocker: Hijacked delay from " + originalDelay + " to 0");
            }
        };

        // 1. Hook the hidden internal implementation (The absolute lowest level in Java)
        try {
            XposedHelpers.findAndHookMethod(
                "android.hardware.SystemSensorManager", 
                lpparam.classLoader, 
                "registerListenerImpl", 
                SensorEventListener.class, Sensor.class, int.class, Handler.class, int.class, int.class, 
                delayHook
            );
            XposedBridge.log("SensorUnlocker: SystemSensorManager hooked successfully.");
        } catch (Throwable t) {
            XposedBridge.log("SensorUnlocker: SystemSensorManager hook missed.");
        }

        // 2. Blanket-hook all 4 public SensorManager overloads just in case
        try {
            XposedHelpers.findAndHookMethod("android.hardware.SensorManager", lpparam.classLoader, "registerListener", SensorEventListener.class, Sensor.class, int.class, delayHook);
            XposedHelpers.findAndHookMethod("android.hardware.SensorManager", lpparam.classLoader, "registerListener", SensorEventListener.class, Sensor.class, int.class, Handler.class, delayHook);
            XposedHelpers.findAndHookMethod("android.hardware.SensorManager", lpparam.classLoader, "registerListener", SensorEventListener.class, Sensor.class, int.class, int.class, delayHook);
            XposedHelpers.findAndHookMethod("android.hardware.SensorManager", lpparam.classLoader, "registerListener", SensorEventListener.class, Sensor.class, int.class, int.class, Handler.class, delayHook);
        } catch (Throwable t) {
            // Some overloads might not exist on older SDKs, safely ignore
        }
    }
}
