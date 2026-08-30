package com.fastman92.main_activity_launcher;

import android.util.Log;
import android.widget.Toast;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Application extends ApplicationOriginal {

    private static final String LOG_FILE_NAME = "crash_log.txt";

    @Override
    public void onCreate() {
        // التقاط أي كراش مفاجئ غير متوقع (Uncaught Exception) وتسجيله قبل إغلاق التطبيق
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            logCrashToFile(throwable);
            System.exit(1);
        });

        writeLog("--- بدء تشغيل التطبيق (Application onCreate) ---");
        Functions.CodeOnApplicationLaunch(this);

        try {
            SettingsLoader.InitializeSettings(this, false);
            writeLog("تم تحميل الإعدادات بنجاح في Application.");
        } catch (Exception e) {
            writeLog("خطأ أثناء تحميل الإعدادات: " + e.getMessage());
            SettingsLoader.SetErrorMessage(e.toString());
            ShowErrorMessage(SettingsLoader.errorMessage);
        }

        super.onCreate();
    }

    void HandleException(Exception e) {
        writeLog("تم التقاط استثناء (HandleException): " + e.getMessage());
        ShowErrorMessage(e.toString());
    }

    void ShowErrorMessage(String msg) {
        Log.e(MainActivity.log_tag, msg);
        writeLog("[ERROR] " + msg);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    // دالة كتابة الأحداث في ملف نصي
    public void writeLog(String text) {
        try {
            File filesDir = getExternalFilesDir(null);
            if (filesDir == null) {
                filesDir = getFilesDir();
            }
            File logFile = new File(filesDir, LOG_FILE_NAME);
            
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
            FileWriter writer = new FileWriter(logFile, true);
            writer.append("[").append(timeStamp).append("] ").append(text).append("\n");
            writer.flush();
            writer.close();
        } catch (Exception e) {
            Log.e(MainActivity.log_tag, "فشل كتابة السجل إلى الملف: " + e.getMessage());
        }
    }

    // دالة استخراج وتخزين تفاصيل الكراش الكاملة (Stack Trace)
    private void logCrashToFile(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();

        Log.e(MainActivity.log_tag, "FATAL CRASH DETECTED:\n" + stackTrace);
        writeLog("=== FATAL CRASH DETECTED ===");
        writeLog(stackTrace);
        writeLog("============================\n");
    }
}
