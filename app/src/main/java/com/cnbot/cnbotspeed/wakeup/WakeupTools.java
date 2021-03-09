package com.cnbot.cnbotspeed.wakeup;

import android.content.Context;
import android.os.WorkSource;
import android.util.Log;

import com.aispeech.dui.dds.DDS;
import com.aispeech.dui.dds.agent.wakeup.WakeupCallback;
import com.aispeech.dui.dds.agent.wakeup.word.WakeupWord;
import com.aispeech.dui.dds.exceptions.DDSNotInitCompleteException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class WakeupTools {
    private static final String TAG = WakeupTools.class.getSimpleName();

    private Context mContext;
    private static WakeupTools mInstance;

    private WakeupTools(Context context) {
        mContext = context;
    }

    public static WakeupTools getInstance(Context context) {
        if (mInstance == null) {
            synchronized (WakeupTools.class) {
                if (mInstance == null) {
                    mInstance = new WakeupTools(context);

                }
            }
        }
        return mInstance;
    }

    public void initWakeup(){
        DDS.getInstance().getAgent().getWakeupEngine().setWakeupCallback(new WakeupCallback() {
            @Override
            public JSONObject onWakeup(JSONObject jsonObject) {
                Log.d(TAG, "onWakeupResult = " + jsonObject.toString());
                JSONObject result = new JSONObject();
                try {
                    result.put("greeting", "你好");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return result;
            }
        });
    }

    public void enableWakeup() {
        // 开启语音唤醒
        try {
            Log.i(TAG, "enableWakeup: 开启语音唤醒");
            DDS.getInstance().getAgent().getWakeupEngine().enableWakeup();

        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }

    public void disableWakeup() {
        // 关闭语音唤醒
        try {
            Log.i(TAG, "disableWakeup: 关闭语音唤醒");
            DDS.getInstance().getAgent().getWakeupEngine().disableWakeup();
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }

    public void setWakeupDoa(int doa){
        try {
            DDS.getInstance().getAgent().getWakeupEngine().setWakeupDoa(doa);
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }

    public void getMainWakeupWords(){
        try {
            List<WakeupWord> wakeupWords = DDS.getInstance().getAgent().getWakeupEngine().getMainWakeupWords();


            for (WakeupWord w :wakeupWords) {
                Log.i(TAG, "getMainWakeupWords: " + w.getName());
            }
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }

    public void release(){
        disableWakeup();
        DDS.getInstance().getAgent().getWakeupEngine().destroy();
    }
}
