package com.fastman92.main_activity_launcher;

import android.util.Log;
import android.widget.Toast;

public class Application extends ApplicationOriginal {
    
    @Override
    public void onCreate() {
        Functions.CodeOnApplicationLaunch(this);
        try {
            SettingsLoader.InitializeSettings(this, false);
        } catch (Exception e) {
            SettingsLoader.SetErrorMessage(e.toString());
            ShowErrorMessage(SettingsLoader.errorMessage);
        }
        super.onCreate();
    }

    void HandleException(Exception e) {
        ShowErrorMessage(e.toString());
    }

    void ShowErrorMessage(String msg) {
        Log.e(MainActivity.log_tag, msg);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
