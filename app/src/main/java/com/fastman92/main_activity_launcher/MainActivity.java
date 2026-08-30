package com.fastman92.main_activity_launcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {
    public static final String log_tag = "fastman92 app launcher";
    public static int debugStaticIntCheck = 0;
    
    private static ComponentName activityToBeLoadedComponentName = null;
    private static boolean originalActivityAlreadyStarted = false;
    
    private final int SETTINGS_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (originalActivityAlreadyStarted) {
            GoToOriginalActivity();
            return;
        }

        try {
            ActivityInfo ai = getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
            Bundle metaData = ai.metaData;
            if (metaData == null) {
                throw new Exception("Activity's metaData is null.");
            }
            
            String activityToLaunchStr = metaData.getString("ACTIVITY_TO_LAUNCH");
            if (activityToLaunchStr == null) {
                throw new Exception("meta-data ACTIVITY_TO_LAUNCH not found in activity.");
            }

            activityToBeLoadedComponentName = new ComponentName(getPackageName(), activityToLaunchStr);
            Intent settingsIntent = new Intent(this, SettingsLoader.class);
            startActivityForResult(settingsIntent, SETTINGS_REQUEST_CODE);
        } catch (Exception e) {
            HandleException(e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode != SETTINGS_REQUEST_CODE) {
            return;
        }

        if (data == null) {
            Log.e(log_tag, "Unable to start SettingsLoader activity.");
            finish();
            return;
        }

        try {
            if (resultCode == RESULT_OK) {
                Log.i(log_tag, "Launching an original activity.");
                GoToOriginalActivity();
            } else {
                String errorMessage = data.getStringExtra("errorMessage");
                if (errorMessage == null) {
                    errorMessage = "Unknown error from settings loader";
                }
                throw new Exception(errorMessage);
            }
        } catch (Exception e) {
            HandleException(e);
        }
    }

    void HandleException(Exception e) {
        ShowErrorMessage(e.toString());
        LauncherActivityHasGotNothingMoreToDo();
    }

    void ShowErrorMessage(String msg) {
        Log.e(log_tag, msg);
        Functions.ShowMessageBox(this, msg, dialogInterface -> finish());
    }

    private void LauncherActivityHasGotNothingMoreToDo() {
    }

    private void GoToOriginalActivity() {
        Intent originalActivityIntent = new Intent();
        originalActivityIntent.setComponent(activityToBeLoadedComponentName);
        originalActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(originalActivityIntent);
        finish();
        originalActivityAlreadyStarted = true;
    }
}


