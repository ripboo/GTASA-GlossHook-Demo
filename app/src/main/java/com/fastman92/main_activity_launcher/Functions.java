package com.fastman92.main_activity_launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.os.Process;
import android.util.Log;
import java.io.File;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Functions {
    public static String oldExternalStorageDirectory;
    public static final Pattern getPathToDirectoryInsideAndroidFromAndroidDir_pattern = Pattern.compile(".*\\/Android(_unprotected)(/)(.*?)(?=/)");
    private static final Pattern getPathToSDcardFromAndroidDir_pattern = Pattern.compile(".*(?=\\/Android(_unprotected)?(?=\\/|$))");
    private static boolean bCodeOnApplicationLaunchExecuted = false;

    public static native void OnApplicationStartup_7548652();

    public static void MkDirIfDoesNotExist(File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static File AndroidDATAdirToUnprotected(File dir_protected) {
        if (dir_protected == null) {
            return null;
        }
        String AndroidUnprotectedDir = getPathToAndroidUnprotectedDir(dir_protected.getAbsolutePath());
        if (AndroidUnprotectedDir == null) {
            return null;
        }
        return new File(AndroidUnprotectedDir + "/data/" + Settings.AndroidDATAdirectory + "/files");
    }

    public static File AndroidOBBdirToUnprotected(File dir_protected) {
        if (dir_protected == null) {
            return null;
        }
        String AndroidUnprotectedDir = getPathToAndroidUnprotectedDir(dir_protected.getAbsolutePath());
        if (AndroidUnprotectedDir == null) {
            return null;
        }
        return new File(AndroidUnprotectedDir + "/obb/" + Settings.AndroidOBBdirectory);
    }

    public static File AndroidAssetPacksDirToUnprotected(File dir_protected) {
        if (dir_protected == null) {
            return null;
        }
        String AndroidUnprotectedDir = getPathToAndroidUnprotectedDir(dir_protected.getAbsolutePath());
        if (AndroidUnprotectedDir == null) {
            return null;
        }
        String AssetPacksPath = AndroidUnprotectedDir + "/data/" + Settings.AndroidOBBdirectory + "/files/assetpacks";
        return new File(AssetPacksPath);
    }

    public static String getPathToAndroidUnprotectedDir(String path) {
        Matcher m = getPathToSDcardFromAndroidDir_pattern.matcher(path);
        if (m.find()) {
            String pathToSDcard = m.group();
            if (pathToSDcard.equals(oldExternalStorageDirectory)) {
                pathToSDcard = Settings.ExternalStorageDirectory;
            }
            return pathToSDcard + "/" + Settings.AndroidDirectory;
        }
        return null;
    }

    public static void ShowMessageBox(Activity activity, String msg, final DialogInterface.OnCancelListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getApplication().getApplicationInfo().loadLabel(activity.getPackageManager()).toString());
        builder.setMessage(msg);
        builder.setNeutralButton("OK", (dialogInterface, i) -> listener.onCancel(dialogInterface));
        
        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.setOnCancelListener(listener);
        alertDialog.show();
    }

    public static void CodeOnApplicationLaunch(Context context) {
        if (bCodeOnApplicationLaunchExecuted) {
            return;
        }
        String packageName = (context != null) ? context.getPackageName() : SettingsGenerated.packageName;
        Log.i(MainActivity.log_tag, "Starting application " + packageName + ", PID: " + Process.myPid());
        bCodeOnApplicationLaunchExecuted = true;
    }

    static String[] getAPKExpansionFiles(Context ctx, int mainVersion, int patchVersion) {
        String s = ctx.getPackageName();
        Vector<String> ret = new Vector<>();
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File expPath = new File(Environment.getExternalStorageDirectory().toString() + Settings.OBB_PATH_PREFIX + s);
            if (expPath.exists()) {
                if (mainVersion > 0) {
                    String s1 = expPath + File.separator + "main." + mainVersion + "." + s + ".obb";
                    if (new File(s1).isFile()) {
                        ret.add(s1);
                    }
                }
                if (patchVersion > 0) {
                    String s2 = expPath + File.separator + "patch." + mainVersion + "." + s + ".obb";
                    if (new File(s2).isFile()) {
                        ret.add(s2);
                    }
                }
            }
        }
        return ret.toArray(new String[0]);
    }

    static String[] getAPKExpansionFiles2(Context ctx, int mainVersion, int patchVersion) {
        return getAPKExpansionFiles(ctx, mainVersion, patchVersion);
    }
}


