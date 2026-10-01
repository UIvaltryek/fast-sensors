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

        // Hook 1: Standard 3-parameter Unity listener
        XposedHelpers.findAndHookMethod(
            "android.hardware.SensorManager", 
            lpparam.classLoader, 
            "registerListener", 
            SensorEventListener.class, 
            Sensor.class, 
            int.class, 
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.args[2] = 0; // Overwrite delay request with SENSOR_DELAY_FASTEST
                }
            }
        );

        // Hook 2: 4-parameter Unity listener (Handler fallback)
        XposedHelpers.findAndHookMethod(
            "android.hardware.SensorManager", 
            lpparam.classLoader, 
            "registerListener", 
            SensorEventListener.class, 
            Sensor.class, 
            int.class, 
            Handler.class,
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.args[2] = 0; 
                }
            }
        );
    }
}
