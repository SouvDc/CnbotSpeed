package com.cnbot.cnbotspeed.tts;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import com.aispeech.dui.dds.DDS;
import com.aispeech.dui.dds.agent.tts.TTSEngine;
import com.aispeech.dui.dds.exceptions.DDSNotInitCompleteException;
import com.cnbot.cnbotspeed.wakeup.WakeupTools;

public class TtsTools {
    private final static String TAG = "TtsTools";
    private Context mContext;
    private static TtsTools mInstance;

    private TtsTools(Context context) {
        mContext = context;
    }

    public static TtsTools getInstance(Context context) {
        if (mInstance == null) {
            synchronized (TtsTools.class) {
                if (mInstance == null) {
                    mInstance = new TtsTools(context);

                }
            }
        }
        return mInstance;
    }

    public void initTts(){
        try {
            DDS.getInstance().getAgent().getTTSEngine().setListener(new TTSEngine.Callback() {
                /**
                 * 开始合成时的回调
                 * @param ttsId 当前TTS的id， 对话过程中的播报ttsid默认为0，通过speak接口调用的播报，ttsid由speak接口指定。
                 */
                @Override
                public void beginning(String ttsId) {
                    Log.d(TAG, "TTS开始播报 ttsId = " + ttsId);
                }

                /**
                 * 合成的音频数据的回调，可能会返回多次，data长度为0表示音频结束
                 * @param data 音频数据
                 */
                @Override
                public void received(byte[] data) {
                    Log.d(TAG, "收到音频，此方法会回调多次，直至data为0，音频结束 data = " + data.length);
                }

                /**
                 * TTS播报完成的回调
                 * @param status 播报结束的状态。
                 *               正常播报结束为0
                 *               播报中途被打断结束为1
                 */
                @Override
                public void end(String ttsId, int status) {
                    Log.d(TAG, "TTS播报结束 status = " + status + ", ttsId = " + ttsId);
                }

                /**
                 * 合成过程中出现错误的回调
                 * @param error 错误信息
                 */
                @Override
                public void error(String error) {
                    Log.d(TAG, "出现错误，" + error);
                }
            });
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 优先级0-保留，与aios语音交互同级，仅限内部使用
     * 优先级1-正常，默认选项，同级按序播放
     * 优先级2-重要，可以插话优先级1，同级按序播放，播报完毕后继续播报刚才被插话的优先级1
     * 优先级3-紧急，可以打断优先级1或优先级2，同级按序播放，播报完毕后播报下一句优先级2
     * @param content
     */
    public void speek(String content){
        Log.i(TAG, "speek: " + content);
        try {
//            DDS.getInstance().getAgent().getTTSEngine().speak(content, 1);

            DDS.getInstance().getAgent().getTTSEngine().speak(content, 1, "100", AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止当前播报
     */
    public void shupWithZero() {
        try {
            DDS.getInstance().getAgent().getTTSEngine().shutup("0");
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止所有播报
     */
    public void shupWithNULL() {
        try {
            DDS.getInstance().getAgent().getTTSEngine().shutup("");
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }


    public void release(){
        DDS.getInstance().getAgent().getTTSEngine().destroy();
    }
}
