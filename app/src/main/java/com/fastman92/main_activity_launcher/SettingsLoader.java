package com.fastman92.main_activity_launcher;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SettingsLoader extends Activity {
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 100;
    private static final int REQUEST_MANAGE_EXTERNAL_STORAGE_PERMISSION = 101;
    private static final String launcherPropertyPrefix = "LAUNCHER_";

    public static String XMLsettingsFilePath = null;
    public static boolean useLauncherSettingsXml;
    public static String errorMessage = null;
    
    static Context context = null;

    private static boolean settingsInitializationAttemptedInActivity = false;
    private static boolean bCheckForMultipleDirs = false;
    
    private static final HashMap<String, String> settingsStoredInFile = new HashMap<>();
    private static final HashMap<String, String> settingsAll = new HashMap<>();

    private interface OnRequestPermissions {
        void onRequestPermissions();
    }

    private void requestPermissions(final OnRequestPermissions var) {
        Functions.ShowMessageBox(this, "Permission to access files is needed. Please accept.", dialogInterface -> var.onRequestPermissions());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean bNeedToWaitForPermissions = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                bNeedToWaitForPermissions = true;
                requestPermissions(() -> {
                    try {
                        startActivityForResult(new Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION", Uri.parse("package:" + getPackageName())), REQUEST_MANAGE_EXTERNAL_STORAGE_PERMISSION);
                    } catch (Exception e) {
                        Intent intent = new Intent("android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION");
                        startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE_PERMISSION);
                    }
                });
            }
        } else if (checkPermission("android.permission.WRITE_EXTERNAL_STORAGE", Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED) {
            bNeedToWaitForPermissions = true;
            requestPermissions(() -> requestPermissionsCompat(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION));
        }

        if (!bNeedToWaitForPermissions) {
            try {
                InitializeSettingsInActivity(this);
            } catch (Exception e) {
                SetErrorMessage(e.toString());
            }
            OnSettingsLoaded();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MANAGE_EXTERNAL_STORAGE_PERMISSION) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                    throw new Exception("Unable to obtain MANAGE_EXTERNAL_STORAGE permission.");
                }
                InitializeSettingsInActivity(this);
            } catch (Exception e) {
                SetErrorMessage(e.toString());
            }
            OnSettingsLoaded();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION) {
            try {
                if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    throw new Exception("Unable to obtain WRITE_EXTERNAL_STORAGE permission.");
                }
                InitializeSettingsInActivity(this);
            } catch (Exception e) {
                SetErrorMessage(e.toString());
            }
            OnSettingsLoaded();
        }
    }

    private void requestPermissionsCompat(final String[] permissions, final int requestCode) {
        for (String permission : permissions) {
            if (TextUtils.isEmpty(permission)) {
                throw new IllegalArgumentException("Permission request for permissions " + Arrays.toString(permissions) + " must not contain null or empty values");
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, requestCode);
        } else {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                int[] grantResults = new int[permissions.length];
                PackageManager packageManager = getPackageManager();
                String packageName = getPackageName();
                for (int i = 0; i < permissions.length; i++) {
                    grantResults[i] = packageManager.checkPermission(permissions[i], packageName);
                }
                onRequestPermissionsResult(requestCode, permissions, grantResults);
            });
        }
    }

    private void FinishThisActivity() {
        Intent intent = new Intent();
        if (errorMessage != null) {
            intent.putExtra("errorMessage", errorMessage);
        }

        if (Settings.bSettingsLoaded) {
            for (Map.Entry<String, String> entry : new TreeMap<>(settingsAll).entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }
            intent.putExtra("ExternalFilesDir", Settings.getExternalFilesDir(this, null));
            intent.putExtra("ExternalOBBfilesDir", Settings.getObbDir(this));
            intent.putExtra("ExternalAndroidPacksDir", Settings.getAssetPacksDir(this));
            setResult(RESULT_OK, intent);
        } else {
            setResult(RESULT_CANCELED, intent);
        }
        finish();
    }

    private void OnSettingsLoaded() {
        if (Settings.showMessageBoxBeforeStartingApplication) {
            Functions.ShowMessageBox(this, "Message before an application gets started.", dialogInterface -> FinishThisActivity());
        } else {
            FinishThisActivity();
        }
    }

    static void SetErrorMessage(String msg) {
        errorMessage = msg;
    }

    public static void InitializeSettings(Context context2, boolean inActivity) throws Exception {
        if (inActivity) {
            XMLsettingsFilePath = context2.getExternalFilesDir(null).getAbsolutePath() + File.separator + "launcher_settings.xml";
        } else {
            XMLsettingsFilePath = context2.getFilesDir().getAbsolutePath() + File.separator + "launcher_settings.xml";
        }
        
        errorMessage = null;
        if (settingsInitializationAttemptedInActivity) {
            return;
        }

        context = context2;
        if (inActivity) {
            settingsInitializationAttemptedInActivity = true;
        }

        settingsAll.clear();
        settingsStoredInFile.clear();

        useLauncherSettingsXml = GetPropertyYesOrNo("USE_LAUNCHER_SETTINGS_XML", false);
        if (useLauncherSettingsXml) {
            if (new File(XMLsettingsFilePath).exists()) {
                if (inActivity) {
                    Log.i(MainActivity.log_tag, "Configuration file \"" + XMLsettingsFilePath + "\" exists. Reading it.");
                }
                ReadSettingsFile();
            } else if (inActivity) {
                Log.i(MainActivity.log_tag, "Configuration file \"" + XMLsettingsFilePath + "\" does not exist. Skipping it.");
            }
        } else if (inActivity) {
            Log.i(MainActivity.log_tag, "Configuration file \"" + XMLsettingsFilePath + "\" will not be used. Skipping it.");
        }

        Functions.oldExternalStorageDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
        Settings.ExternalStorageDirectory = Functions.oldExternalStorageDirectory;
        if (new File("/mnt/windows/BstSharedFolder/use_as_sdcard.txt").exists()) {
            Settings.ExternalStorageDirectory = "/mnt/windows/BstSharedFolder";
        }

        Settings.originalPackageName = GetPropertyString("ORIGINAL_PACKAGE_NAME", false);
        GetPropertyString("USE_ANDROID_UNPROTECTED_DIRECTORY");
        if (GetPropertyYesOrNo("USE_ANDROID_UNPROTECTED_DIRECTORY")) {
            Settings.AndroidDirectory = "Android_unprotected";
        } else {
            Settings.AndroidDirectory = "Android";
        }

        Settings.AndroidDATAdirectory = GetPropertyString("ANDROID_DATA_DIRECTORY");
        Settings.AndroidOBBdirectory = GetPropertyString("ANDROID_OBB_DIRECTORY");
        Settings.OBB_PATH_PREFIX = "/" + Settings.AndroidDirectory + "/obb/";

        if (!Settings.AndroidDirectory.equals("Android_unprotected")) {
            CheckValueDependingOnAndroidUnprotected("ANDROID_DATA_DIRECTORY");
            CheckValueDependingOnAndroidUnprotected("ANDROID_OBB_DIRECTORY");
        }

        bCheckForMultipleDirs = GetPropertyYesOrNo("CHECK_FOR_MULTIPLE_DIRS", false);
        if (inActivity) {
            CheckIfDirectoriesExist(Settings.getExternalFilesDirs(context2, null), true, true);
            CheckIfDirectoriesExist(Settings.getObbDirs(context2), true, false);
            if (GetPropertyYesOrNo("ASSETPACKS_USED", false)) {
                CheckIfDirectoryExists(Settings.getAssetPacksDir(context2), false, false);
            }
        }

        Settings.showMessageBoxBeforeStartingApplication = GetPropertyYesOrNo("SHOW_MESSAGE_BOX_ON_APPLICATION_STARTUP");
        if (inActivity) {
            StringBuilder settingsStr = new StringBuilder();
            int id = 0;
            for (Map.Entry<String, String> entry : new TreeMap<>(settingsAll).entrySet()) {
                if (id != 0) {
                    settingsStr.append(", ");
                }
                settingsStr.append(entry.getKey()).append(" = ").append(entry.getValue());
                id++;
            }
            Log.i(MainActivity.log_tag, "Settings (" + settingsStr + ")");
        }
        Settings.bSettingsLoaded = true;
    }

    private static void InitializeSettingsInActivity(Context context2) throws Exception {
        InitializeSettings(context2, true);
        if (useLauncherSettingsXml && !new File(XMLsettingsFilePath).exists()) {
            Log.i(MainActivity.log_tag, "Configuration file \"" + XMLsettingsFilePath + "\" does not exist. Writing it.");
            WriteSettingsFile();
        }

        if (useLauncherSettingsXml) {
            File destFile = new File(context2.getFilesDir().getAbsolutePath() + File.separator + "launcher_settings.xml");
            try (FileChannel src = new FileInputStream(XMLsettingsFilePath).getChannel();
                 FileChannel dest = new FileOutputStream(destFile).getChannel()) {
                dest.transferFrom(src, 0L, src.size());
            }
        }
    }

    private static String GetPropertyString(String key) throws Exception {
        return GetPropertyString(key, true);
    }

    private static String GetPropertyString(String key, boolean lookIntoSettingsFile) throws Exception {
        String keyWithPrefix = launcherPropertyPrefix + key;
        ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        Bundle bundle = ai.metaData;
        String result = bundle.getString(keyWithPrefix);

        if (result == null) {
            throw new Exception("Unable to retrieve property " + keyWithPrefix + " from APK manifest file");
        }

        if (lookIntoSettingsFile) {
            String customValue = settingsStoredInFile.get(keyWithPrefix);
            if (customValue != null) {
                result = customValue;
            } else {
                settingsStoredInFile.put(keyWithPrefix, result);
            }
        }

        settingsAll.put(keyWithPrefix, result);
        return result;
    }

    private static boolean GetPropertyYesOrNo(String key) throws Exception {
        return GetPropertyYesOrNo(key, true);
    }

    private static boolean GetPropertyYesOrNo(String key, boolean lookIntoSettingsFile) throws Exception {
        String value = GetPropertyString(key, lookIntoSettingsFile);
        if ("yes".equals(value)) {
            return true;
        }
        if ("no".equals(value)) {
            return false;
        }
        throw new Exception(key + ", wrong value: " + value);
    }

    private static void CheckIfDirectoriesExist(File[] dirs, boolean bCreateNoMedia, boolean bCreateReadmeInAndroidUnprotected) throws Exception {
        for (File dir : dirs) {
            CheckIfDirectoryExists(dir, bCreateNoMedia, bCreateReadmeInAndroidUnprotected);
            if (!bCheckForMultipleDirs) {
                return;
            }
        }
    }

    private static void CheckIfDirectoryExists(File dir, boolean bCreateNoMedia, boolean bCreateReadmeInAndroidUnprotected) throws Exception {
        Functions.MkDirIfDoesNotExist(dir);
        if (dir.exists()) {
            if (bCreateNoMedia) {
                Matcher m = Functions.getPathToDirectoryInsideAndroidFromAndroidDir_pattern.matcher(dir.getAbsolutePath());
                if (m.find()) {
                    File file = new File(m.group() + File.separator + ".nomedia");
                    if (!file.exists()) {
                        try {
                            file.createNewFile();
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            if (!bCreateReadmeInAndroidUnprotected) {
                return;
            }
        }

        File androidDir = new File(Functions.getPathToAndroidUnprotectedDir(dir.getAbsolutePath()));
        Functions.MkDirIfDoesNotExist(androidDir);
        if (!androidDir.exists()) {
            throw new Exception("Please create the following directory:\n" + androidDir.getAbsolutePath() + "\n\nYou can do it using your file manager. It can't be done by this application and you need to do it manually.");
        }

        if (bCreateReadmeInAndroidUnprotected && Settings.AndroidDirectory.equals("Android_unprotected")) {
            String promoPath = androidDir + File.separator + "About.txt";
            long timestampInAboutFile = 0;
            File promoFile = new File(promoPath);

            if (promoFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(promoFile))) {
                    String intLine = reader.readLine();
                    if (intLine != null) {
                        try {
                            timestampInAboutFile = Long.parseLong(intLine);
                        } catch (NumberFormatException e) {
                            timestampInAboutFile = 0;
                        }
                    }
                }
            }

            if (SettingsGenerated.apkModifierCompileTimestamp > timestampInAboutFile) {
                String strToWrite = SettingsGenerated.apkModifierCompileTimestamp + "\n\nfastman92 APK modifier\n\nProject that allows for Android_unprotected directory, which provides an easy access to files that will be used by an application. Solves the problem of restricted access to Android directory.\nHas got many other features as well.\n\nAvailable for many different applications.\n\nLink: https://gtaforums.com/topic/979211-fastman92-apk-modifier/";
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(promoFile))) {
                    writer.write(strToWrite);
                }
            }
        }
        throw new Exception("Unable to create " + dir.getAbsolutePath());
    }

    private static void ReadSettingsFile() throws Exception {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new File(XMLsettingsFilePath));
            doc.getDocumentElement().normalize();
            Node configuration = doc.getElementsByTagName("CONFIGURATION").item(0);
            NodeList list = configuration.getChildNodes();

            for (int temp = 0; temp < list.getLength(); temp++) {
                Node node = list.item(temp);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    settingsStoredInFile.put(node.getNodeName(), node.getTextContent());
                }
            }
        } catch (Exception e) {
            throw new Exception("Error while reading " + XMLsettingsFilePath);
        }
    }

    private static void WriteSettingsFile() throws Exception {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("CONFIGURATION");
        doc.appendChild(rootElement);

        for (Map.Entry<String, String> entry : new TreeMap<>(settingsStoredInFile).entrySet()) {
            Element element = doc.createElement(entry.getKey());
            element.appendChild(doc.createTextNode(entry.getValue()));
            rootElement.appendChild(element);
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(XMLsettingsFilePath));
        transformer.transform(source, result);
    }

    private static void CheckValueDependingOnAndroidUnprotected(String key) throws Exception {
        String keyWithPrefix = launcherPropertyPrefix + key;
        String value = settingsAll.get(keyWithPrefix);
        if (!value.equals(context.getPackageName())) {
            throw new Exception(keyWithPrefix + " should be " + context.getPackageName() + " when not " + launcherPropertyPrefix + "USE_ANDROID_UNPROTECTED_DIRECTORY.\nCurrent value: " + Settings.AndroidDATAdirectory);
        }
    }
}
