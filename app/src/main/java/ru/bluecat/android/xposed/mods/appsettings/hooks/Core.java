package ru.bluecat.android.xposed.mods.appsettings.hooks;

import android.content.res.XModuleResources;

import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import ru.bluecat.android.xposed.mods.appsettings.Constants;

public class Core implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static XSharedPreferences prefs;
    public static XModuleResources mResources;

    private static final Map<String, Integer> PRESET_DPI_MAP = new HashMap<>();
    static {
        PRESET_DPI_MAP.put("bin.mt.plus", 200);
        PRESET_DPI_MAP.put("com.xtc.b", 200);
    }

    public static int getEffectiveDpi(String packageName, XSharedPreferences prefs) {
        int dpi = prefs.getInt(packageName + Constants.PREF_DPI, 0);
        if (dpi != 0) {
            return dpi;
        }
        Integer preset = PRESET_DPI_MAP.get(packageName);
        if (preset != null) {
            return preset;
        }
        return prefs.getInt(Constants.PREF_DEFAULT + Constants.PREF_DPI, 0);
    }

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {

        mResources = XModuleResources.createInstance(startupParam.modulePath, null);
        if(prefs == null) prefs = getModulePrefs();

        if(prefs != null) {
            prefs.reload();
            onZygoteLoad.adjustSystemDimensions(prefs);
            onZygoteLoad.dpiInSystem(prefs);
            onZygoteLoad.activitySettings(prefs);
        }
    }

    public static XSharedPreferences getModulePrefs() {
        XSharedPreferences pref = new XSharedPreferences(Constants.MY_PACKAGE_NAME, Constants.PREFS);
        return pref.getFile().canRead() ? pref : null;
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) {
        if (lpparam.packageName.equals(Constants.MY_PACKAGE_NAME)) {
            XposedHelpers.findAndHookMethod(Constants.MY_PACKAGE_NAME + ".MainActivity",
                    lpparam.classLoader,
                    "isModuleActive",
                    XC_MethodReplacement.returnConstant(true));
        }

        if(prefs != null) prefs.reload(); else return;

        if (lpparam.packageName.equals("android")) {
            AndroidServer.resident(lpparam, prefs);
            AndroidServer.recentTasks(lpparam, prefs);
            AndroidServer.UnrestrictedGetTasks(lpparam, prefs);
            AndroidServer.notificationManager(lpparam, prefs);
            PackagePermissions.init(lpparam, prefs);
        }
        onPackageLoad.screenSettings(lpparam, prefs);
        onPackageLoad.soundPool(lpparam, prefs);
        onPackageLoad.legacyMenu(lpparam, prefs);
    }

    static boolean isActive(XSharedPreferences prefs, String packageName) {
        return prefs.getBoolean(packageName + Constants.PREF_ACTIVE, false);
    }

    static boolean isActive(XSharedPreferences prefs, String packageName, String sub) {
        return prefs.getBoolean(packageName + Constants.PREF_ACTIVE, false) &&
                prefs.getBoolean(packageName + sub, false);
    }
}