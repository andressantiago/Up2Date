package com.zarfnates.up2date;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.Objects;

public final class Up2Date {
    private static final String PLAY_STORE_BASE_URL = "https://play.google.com/store/apps/details?id=";
    private static final String TAG = Up2Date.class.getSimpleName();
    private static String packageName;
    private static String currentAppVersion;
    private static String PLAY_STORE_SELECTOR = "div[itemprop=softwareVersion]";
    private static VersionListener versionListener;
    private static String fakePackageName=null;

    public static void setFakePackageName(String fakePackageName) {
        Up2Date.fakePackageName = fakePackageName;
    }

    public static void checkVersion(Context context, VersionListener versionListener) {

        if (context == null) {
            return;
        }

        Up2Date.versionListener = versionListener;

        Context applicationContext = context.getApplicationContext();
        if(fakePackageName==null)
            packageName = applicationContext.getPackageName();
        else
        {
            packageName=fakePackageName;
        }

        try {
            PackageInfo packageInfo = applicationContext.getPackageManager().getPackageInfo(packageName, 0);
            currentAppVersion = packageInfo.versionName.trim();
            Log.i(TAG, "checkVersion: " + currentAppVersion);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            notifyError(FailureReason.NO_PACKAGE_INFO);
            return;
        }

        new GetVersionCode().execute(PLAY_STORE_BASE_URL + packageName);
    }

    private static void notifyError(int errorCode) {
        if (Up2Date.versionListener == null) {
            return;
        }
        Up2Date.versionListener.versionCheckError(errorCode);
    }

    private static void notify(String storeVersion) {
        if (Up2Date.versionListener == null) {
            return;
        }

        boolean upToDate = false;
        if (storeVersion!=null && storeVersion.equals(currentAppVersion)) {
            upToDate = true;
        }

        Up2Date.versionListener.versionCheckSuccess(upToDate, storeVersion);
    }

    private static class GetVersionCode extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String url = params[0];
            try {
                return Jsoup.connect(url).get()
                        .select(PLAY_STORE_SELECTOR)
                        .first()
                        .ownText();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            if (s == null) {
                Up2Date.notifyError(FailureReason.PLAY_STORE_RESPONSE_ERROR);
            } else {
                Up2Date.notify(s.trim());
            }
        }
    }

    public interface VersionListener {
        void versionCheckSuccess(boolean upToDate, String versionName);

        void versionCheckError(int reasonCode);
    }
}
