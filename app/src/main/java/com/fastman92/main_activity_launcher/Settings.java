package com.fastman92.main_activity_launcher;

import android.content.Context;
import java.io.File;

public class Settings {
    public static String AndroidDATAdirectory;
    public static String AndroidDirectory;
    public static String AndroidOBBdirectory;
    public static String ExternalStorageDirectory;
    public static String OBB_PATH_PREFIX;
    public static String originalPackageName;
    public static boolean showMessageBoxBeforeStartingApplication;
    public static boolean bSettingsLoaded = false;
    public static final String apkModifierCompileDateTime = SettingsGenerated.apkModifierCompileDateTime;

    public static File getExternalFilesDir(Context context, String type) {
        File dir_unprotected = Functions.AndroidDATAdirToUnprotected(context.getExternalFilesDir(type));
        if (dir_unprotected == null) {
            return null;
        }
        Functions.MkDirIfDoesNotExist(dir_unprotected);
        return dir_unprotected;
    }

    public static File getExternalFilesDir(Context context) {
        return getExternalFilesDir(context, null);
    }

    public static File[] getExternalFilesDirs(Context context, String type) {
        File[] array = context.getExternalFilesDirs(type);
        for (int i = 0; i < array.length; i++) {
            array[i] = Functions.AndroidDATAdirToUnprotected(array[i]);
        }
        return array;
    }

    public static File getObbDir(Context context) {
        File dir_unprotected = Functions.AndroidOBBdirToUnprotected(context.getObbDir());
        if (dir_unprotected == null) {
            return null;
        }
        Functions.MkDirIfDoesNotExist(dir_unprotected);
        return dir_unprotected;
    }

    public static File getAssetPacksDir(Context context) {
        return Functions.AndroidAssetPacksDirToUnprotected(context.getObbDir());
    }

    public static File[] getObbDirs(Context context) {
        File[] array = context.getObbDirs();
        for (int i = 0; i < array.length; i++) {
            array[i] = Functions.AndroidOBBdirToUnprotected(array[i]);
        }
        return array;
    }
}
