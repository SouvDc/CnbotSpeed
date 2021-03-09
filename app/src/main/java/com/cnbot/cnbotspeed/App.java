package com.cnbot.cnbotspeed;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.aispeech.dui.dds.DDS;
import com.aispeech.dui.dds.DDSErrorListener;
import com.cnbot.cnbotspeed.server.DDSService;

import org.json.JSONObject;

public class App extends Application {
    private static final String TAG = "application";
    private static Context mContext;
    @Override
    public void onCreate() {
        super.onCreate();

        mContext = this;
    }

    public static Context getContext() {
        if (mContext == null) {
            throw new RuntimeException("Unknown Error");
        }
        return mContext;
    }


}
