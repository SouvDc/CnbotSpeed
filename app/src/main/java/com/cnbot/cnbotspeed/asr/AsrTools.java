package com.cnbot.cnbotspeed.asr;

import android.content.Context;
import android.util.Log;

import com.aispeech.dui.dds.DDS;
import com.aispeech.dui.dds.agent.ASREngine;
import com.aispeech.dui.dds.exceptions.DDSNotInitCompleteException;
import com.cnbot.cnbotspeed.utils.AssetsUtil;
import com.cnbot.cnbotspeed.wakeup.WakeupTools;

public class AsrTools {
    private static final String TAG = AsrTools.class.getSimpleName();
    private Context mContext;
    private static AsrTools mInstance;

    private AsrTools(Context context) {
        mContext = context;
    }

    public static AsrTools getInstance(Context context) {
        if (mInstance == null) {
            synchronized (WakeupTools.class) {
                if (mInstance == null) {
                    mInstance = new AsrTools(context);

                }
            }
        }
        return mInstance;
    }


    public void initAsr(){
        try {
            DDS.getInstance().getAgent().getASREngine().startListening(new ASREngine.Callback() {
                    @Override
                    public void beginningOfSpeech() {
                        Log.i(TAG, "检测到用户开始说话");
                    }

                    @Override
                    public void bufferReceived(byte[] buffer) {
                        Log.i(TAG, "用户说话的音频数据");
                    }

                    @Override
                    public void endOfSpeech() {
                        Log.i(TAG, "检测到用户结束说话");
                    }

                    @Override
                    public void partialResults(String results) {
                        Log.i(TAG, "实时识别结果反馈:" + results);
                    }

                    @Override
                    public void finalResults(String results) {
                        Log.i(TAG, "最终识别结果反馈");
                    }

                    @Override
                    public void error(String error) {
                        Log.i(TAG, "识别过程中发生的错误:" + error );
                    }

                    @Override
                    public void rmsChanged(float rmsdB) {
                        Log.i(TAG, "用户说话的音量分贝" + rmsdB);
                    }
                });
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }


    public void startListening() {
        getAsrModel();
        //获取识别引擎
        ASREngine asrEngine = DDS.getInstance().getAgent().getASREngine();


    }

    public void stopListening() {
        try {
            //获取识别引擎
            ASREngine asrEngine = DDS.getInstance().getAgent().getASREngine();
            //主动结束此次识别
            asrEngine.stopListening();
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }

    public void cancelListening() {
        try {
            //获取识别引擎
            ASREngine asrEngine = DDS.getInstance().getAgent().getASREngine();
            asrEngine.cancel();
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }

    }

    public void disableVolume() {
        // 设置实时回传音量大小, 默认为true
        // 设置false之后, ASREngine.Callback.rmsChanged()不再回传音量变化值
        try {
            DDS.getInstance().getAgent().getASREngine().enableVolume(false);
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }

    }

    public void enableVolume() {
        // 设置实时回传音量大小, 默认为true
        // 设置false之后, ASREngine.Callback.rmsChanged()不再回传音量变化值
        try {
            DDS.getInstance().getAgent().getASREngine().enableVolume(true);
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }

    }

    public void updateAsrModel1() {

        try {
            DDS.getInstance().getAgent().getASREngine().updateAsrModel("aihome");
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }

    public void updateAsrModel2() {

        try {
            DDS.getInstance().getAgent().getASREngine().updateAsrModel("airobot");
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }

    public void updateAsrModel3() {

        try {
            DDS.getInstance().getAgent().getASREngine().updateAsrModel("aicar");
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }

    public void updateAsrModel4() {
        try {
            DDS.getInstance().getAgent().getASREngine().updateAsrModel("aicommon");
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }

    public void getAsrModel() {
        try {
            String asrMode = DDS.getInstance().getAgent().getASREngine().getAsrModel();
            Log.d(TAG, "asrMode:" + asrMode);
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }

    public void enableVad() {

        try {
            DDS.getInstance().getAgent().getASREngine().enableVad();
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }

    public void disableVad() {

        try {
            DDS.getInstance().getAgent().getASREngine().disableVad();
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }

    public void getGenderWithPcm() {
        byte[] buffer = AssetsUtil.getFile(mContext, "xiaole.wav");
        try {
            String genderResult = DDS.getInstance().getAgent().getASREngine().getGenderWithPcm(buffer);
            Log.d(TAG, "genderResult = " + genderResult);
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }

    public void getAsrppWithPcm() {
        // 需要添加此配置才能生效
        // config.addConfig(DDSConfig.K_USE_AGE,"true");
        // config.addConfig(DDSConfig.K_USE_GENDER,"true");
        byte[] buffer = AssetsUtil.getFile(mContext, "xiaole.wav");
        try {
            String asrppResult = DDS.getInstance().getAgent().getASREngine().getAsrppWithPcm(buffer, ASREngine.AsrppType.GENDER, ASREngine.AsrppType.AGE);
            Log.d(TAG, "asrppResult = " + asrppResult);
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }

    public void release(){
//        DDS.getInstance().getAgent().getASREngine().
    }

}
