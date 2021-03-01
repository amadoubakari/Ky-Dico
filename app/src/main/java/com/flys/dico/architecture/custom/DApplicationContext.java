package com.flys.dico.architecture.custom;

import android.app.Application;
import android.content.Context;

import java.io.File;

public class DApplicationContext extends Application {

    private static Context mContext;
    private static DApplicationContext instance;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        instance = this;
    }

    @Override
    public void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    public static Context getContext() {
        return mContext;
    }

    public static DApplicationContext getInstance() {
        return instance;
    }

    public void clearApplicationData() {
        File cacheDirectory = getCacheDir();
        File applicationDirectory = new File(cacheDirectory.getParent());
        if (applicationDirectory.exists()) {
            String[] fileNames = applicationDirectory.list();
            for (String fileName : fileNames) {
                if (!fileName.equals("lib")) {
                    deleteFile(new File(applicationDirectory, fileName));
                }
            }
        }
    }

    public static boolean deleteFile(File file) {
        boolean deletedAll = true;
        if (file != null) {
            if (file.isDirectory()) {
                String[] children = file.list();
                for (int i = 0; i < children.length; i++) {
                    deletedAll = deleteFile(new File(file, children[i])) && deletedAll;
                }
            } else {
                deletedAll = file.delete();
            }
        }

        return deletedAll;
    }
}
