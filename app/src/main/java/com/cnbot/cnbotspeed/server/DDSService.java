package com.cnbot.cnbotspeed.server;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.aispeech.dui.dds.DDS;
import com.aispeech.dui.dds.DDSAuthListener;
import com.aispeech.dui.dds.DDSConfig;
import com.aispeech.dui.dds.DDSInitListener;
import com.aispeech.dui.dds.exceptions.DDSNotInitCompleteException;

import java.util.UUID;

import static com.aispeech.dui.dds.utils.AuthUtil.getDeviceId;

public class DDSService extends Service {
    public static final String TAG = "DDSService";
    private IDDSEventListener iddsEventListener;
    private int mAuthCount = 0;// 授权次数,用来记录自动授权

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return ddsServiceBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initDDS();
    }

    private void initDDS() {
        DDS.getInstance().setDebugMode(0); //在调试时可以打开sdk调试日志，在发布版本时，请关闭
        DDS.getInstance().init(getApplicationContext(), createConfig(), mInitListener, mAuthListener);
    }

    // dds初始状态监听器,监听init是否成功
    private DDSInitListener mInitListener = new DDSInitListener() {
        @Override
        public void onInitComplete(boolean isFull) {
            Log.d(TAG, "onInitComplete:" + isFull);
            if (isFull) {
                // 发送一个init成功的广播
                int isInit = DDS.getInstance().getInitStatus();
                // 2  表示初始化完成
                Log.i(TAG, "onInitComplete: " + isInit);
                try {
                    boolean isAuth = DDS.getInstance().isAuthSuccess();
                    Log.i(TAG, "onInitComplete: isAuth = " + isAuth);
                    if(!isAuth){
                        doAutoAuth();
                        return;
                    }
                    if(iddsEventListener !=null){
                        iddsEventListener.audoSuccessListener();
                    }
                } catch (DDSNotInitCompleteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onError(int what, final String msg) {
            Log.e(TAG, "Init onError: " + what + ", error: " + msg);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    // dds认证状态监听器,监听auth是否成功
    private DDSAuthListener mAuthListener = new DDSAuthListener() {
        @Override
        public void onAuthSuccess() {
            Log.d(TAG, "onAuthSuccess");
            // 发送一个认证成功的广播
        }

        @Override
        public void onAuthFailed(final String errId, final String error) {
            Log.e(TAG, "onAuthFailed: " + errId + ", error:" + error);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            "授权错误:" + errId + ":\n" + error + "\n请查看手册处理", Toast.LENGTH_SHORT).show();
                }
            });

        }
    };

    // 执行自动授权
    private void doAutoAuth(){
        // 自动执行授权5次,如果5次授权失败之后,给用户弹提示框
        if (mAuthCount < 5) {
            try {
                DDS.getInstance().doAuth();
                mAuthCount++;
            } catch (DDSNotInitCompleteException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "doAutoAuth: 连续授权失败");
        }
    }

    // 创建dds配置信息
    private DDSConfig createConfig() {
        DDSConfig config = new DDSConfig();
        // 基础配置项
        config.addConfig(DDSConfig.K_PRODUCT_ID, "279600474"); // 产品ID -- 必填
        config.addConfig(DDSConfig.K_USER_ID, "aispeech");  // 用户ID -- 必填
        config.addConfig(DDSConfig.K_ALIAS_KEY, "test");   // 产品的发布分支 -- 必填
        config.addConfig(DDSConfig.K_PRODUCT_KEY, "cd94ec672274589b81e581676d5f5284");// Product Key -- 必填
        config.addConfig(DDSConfig.K_PRODUCT_SECRET, "9afbfb0284e25dc345d307c312a9a70d");// Product Secre -- 必填
        config.addConfig(DDSConfig.K_API_KEY, "2f0c35c9e95a2f0c35c9e95a6045815a");  // 产品授权秘钥，服务端生成，用于产品授权 -- 必填
        config.addConfig(DDSConfig.K_DEVICE_ID, getDeviceId(getApplicationContext()));//填入唯一的deviceId -- 选填

        // 更多高级配置项,请参考文档: https://www.dui.ai/docs/ct_common_Andriod_SDK 中的 --> 四.高级配置项

        config.addConfig(DDSConfig.K_DUICORE_ZIP, "0e338e582baa051ff48b4ba96016c171.zip"); // 预置在指定目录下的DUI内核资源包名, 避免在线下载内核消耗流量, 推荐使用
        config.addConfig(DDSConfig.K_CUSTOM_ZIP, "product.zip");
        config.addConfig(DDSConfig.K_USE_UPDATE_DUICORE, "false"); //设置为false可以关闭dui内核的热更新功能，可以配合内置dui内核资源使用

        // 获取性别/年龄信息配置项
//        config.addConfig(DDSConfig.K_USE_AGE,"true");//sys.dialog.start消息是否返回年龄信息
//        config.addConfig(DDSConfig.K_USE_GENDER,"true");//sys.dialog.start消息是否返回性别信息

        // Java节点配置项
        // config.addConfig(DDSConfig.K_USE_LOCAL_PCM_SERVER, "true"); // 使用专用音频流通道
        // config.addConfig("USE_JAVA_NODE", "true"); // 使用java节点, 修改此配置切换java节点与lua节点,  测试兼容性

        // 声纹配置项
        // config.addConfig(DDSConfig.K_VPRINT_ENABLE, "true");// 是否开启声纹功能
        // config.addConfig(DDSConfig.K_USE_VPRINT_IN_WAKEUP, "true");// 是否开启声纹功能并在唤醒中使用声纹判断
        // config.addConfig(DDSConfig.K_VPRINT_BIN, "/sdcard/vprint.bin");// 声纹资源绝对路径

        // DEVICENAME配置项
        // config.addConfig(DDSConfig.K_DEVICE_NAME, "xxx");

        // 资源更新配置项
        // config.addConfig(DDSConfig.K_CUSTOM_ZIP, "product.zip"); // 预置在指定目录下的DUI产品配置资源包名, 避免在线下载产品配置消耗流量, 推荐使用
        // config.addConfig(DDSConfig.K_USE_UPDATE_NOTIFICATION, "false"); // 是否使用内置的资源更新通知栏

        // 录音配置项
//         config.addConfig(DDSConfig.K_RECORDER_MODE, "internal"); //录音机模式：external（使用外置录音机，需主动调用拾音接口）、internal（使用内置录音机，DDS自动录音）
//         config.addConfig(DDSConfig.K_IS_REVERSE_AUDIO_CHANNEL, "false"); // 录音机通道是否反转，默认不反转
//         config.addConfig(DDSConfig.K_AUDIO_SOURCE, MediaRecorder.AudioSource.DEFAULT); // 内置录音机数据源类型
//         config.addConfig(DDSConfig.K_AUDIO_BUFFER_SIZE, (16000 * 1 * 16 * 100 / 1000)); // 内置录音机读buffer的大小

        // TTS配置项
//         config.addConfig(DDSConfig.K_STREAM_TYPE, AudioManager.STREAM_MUSIC); // 内置播放器的STREAM类型
//         config.addConfig(DDSConfig.K_TTS_MODE, "external"); // TTS模式：external（使用外置TTS引擎，需主动注册TTS请求监听器）、internal（使用内置DUI TTS引擎）
//         config.addConfig(DDSConfig.K_CUSTOM_TIPS, "{\"71304\":\"请讲话\",\"71305\":\"不知道你在说什么\",\"71308\":\"咱俩还是聊聊天吧\"}"); // 指定对话错误码的TTS播报。若未指定，则使用产品配置。

        // 唤醒配置项
//         config.addConfig(DDSConfig.K_WAKEUP_ROUTER, "partner"); //唤醒路由：partner（将唤醒结果传递给partner，不会主动进入对话）、dialog（将唤醒结果传递给dui，会主动进入对话）
//         config.addConfig(DDSConfig.K_WAKEUP_BIN, "/sdcard/wakeup.bin"); //商务定制版唤醒资源的路径。如果开发者对唤醒率有更高的要求，请联系商务申请定制唤醒资源。
         config.addConfig(DDSConfig.K_ONESHOT_MIDTIME, "500");// OneShot配置：
         config.addConfig(DDSConfig.K_ONESHOT_ENDTIME, "0");// OneShot配置：
        config.addConfig(DDSConfig.K_NR_ENABLE, "true");
        config.addConfig(DDSConfig.K_VAD_DISABLE_SIGNAL, "true");

        // 识别配置项
         config.addConfig(DDSConfig.K_ASR_ENABLE_PUNCTUATION, "false"); //识别是否开启标点
//         config.addConfig(DDSConfig.K_ASR_ROUTER, "partner"); //识别路由：partner（将识别结果传递给partner，不会主动进入语义）、dialog（将识别结果传递给dui，会主动进入语义）
         config.addConfig(DDSConfig.K_VAD_TIMEOUT, 5000); // VAD静音检测超时时间，默认8000毫秒
         config.addConfig(DDSConfig.K_ASR_ENABLE_TONE, "true"); // 识别结果的拼音是否带音调
         config.addConfig(DDSConfig.K_ASR_TIPS, "true"); // 识别完成是否播报提示音
//         config.addConfig(DDSConfig.K_VAD_BIN, "/sdcard/vad.bin"); // 商务定制版VAD资源的路径。如果开发者对VAD有更高的要求，请联系商务申请定制VAD资源。
        config.addConfig(DDSConfig.K_ASR_NOT_DROP_WAKEUP,"true"); //识别是否过滤唤醒词

        // 调试配置项
        // config.addConfig(DDSConfig.K_CACHE_PATH, "/sdcard/cache"); // 调试信息保存路径,如果不设置则保存在默认路径"/sdcard/Android/data/包名/cache"
//         config.addConfig(DDSConfig.K_WAKEUP_DEBUG, "true"); // 用于唤醒音频调试, 开启后在 "/sdcard/Android/data/包名/cache" 目录下会生成唤醒音频
        // config.addConfig(DDSConfig.K_VAD_DEBUG, "true"); // 用于过vad的音频调试, 开启后在 "/sdcard/Android/data/包名/cache" 目录下会生成过vad的音频
//         config.addConfig(DDSConfig.K_ASR_DEBUG, "true"); // 用于识别音频调试, 开启后在 "/sdcard/Android/data/包名/cache" 目录下会生成识别音频
        // config.addConfig(DDSConfig.K_TTS_DEBUG, "true");  // 用于tts音频调试, 开启后在 "/sdcard/Android/data/包名/cache/tts/" 目录下会自动生成tts音频

        // 麦克风阵列配置项
         config.addConfig(DDSConfig.K_MIC_TYPE, "6"); // 设置硬件采集模组的类型 0：无。默认值。 1：单麦回消 2：线性四麦 3：环形六麦 4：车载双麦 5：家具双麦 6: 环形四麦  7: 新车载双麦
//         config.addConfig(DDSConfig.K_MIC_ARRAY_AEC_CFG, "/data/aec.bin"); // 麦克风阵列aec资源的磁盘绝对路径,需要开发者确保在这个路径下这个资源存在
         config.addConfig(DDSConfig.K_MIC_ARRAY_BEAMFORMING_CFG, "/sdcard/UCA_asr_ch4-2-ch4_60mm_com_20200515_v1.3.4.bin"); // 麦克风阵列beamforming资源的磁盘绝对路径，需要开发者确保在这个路径下这个资源存在
        config.addConfig(DDSConfig.K_AEC_MODE, "external");
        // 全双工/半双工配置项
        // config.addConfig(DDSConfig.K_DUPLEX_MODE, "HALF_DUPLEX");// 半双工模式
        // config.addConfig(DDSConfig.K_DUPLEX_MODE, "FULL_DUPLEX");// 全双工模式

        // 声纹配置项
        // config.addConfig(DDSConfig.K_VPRINT_ENABLE, "true");// 是否使用声纹
        // config.addConfig(DDSConfig.K_USE_VPRINT_IN_WAKEUP, "true");// 是否与唤醒结合使用声纹
        // config.addConfig(DDSConfig.K_VPRINT_BIN, "/sdcard/vprint.bin");// 声纹资源的绝对路径

        // asrpp配置荐
        // config.addConfig(DDSConfig.K_USE_GENDER, "true");// 使用性别识别
        // config.addConfig(DDSConfig.K_USE_AGE, "true");// 使用年龄识别

        Log.i(TAG, "config->" + config.toString());
        return config;
    }

    // 获取手机的唯一标识符: deviceId
    private String getDeviceId(Context context) {
        TelephonyManager telephonyMgr = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
        String imei = telephonyMgr.getDeviceId();
        String serial = Build.SERIAL;
        String uuid;
        if (TextUtils.isEmpty(imei)) {
            imei = "unkown";
        } else if (TextUtils.isEmpty(serial)) {
            serial = "unkown";
        }
        uuid = UUID.nameUUIDFromBytes((imei + serial).getBytes()).toString();
        return uuid;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 在退出app时将dds组件注销
        DDS.getInstance().release();
    }


    /**
     *  定义Binder类-当然也可以写成外部类
     */
    private DDSServiceBinder ddsServiceBinder = new DDSServiceBinder();

    public void setIddsEventListener(IDDSEventListener iddsEventListener) {
        this.iddsEventListener = iddsEventListener;
    }

    public class DDSServiceBinder extends Binder{
        public Service getService() {
            return DDSService.this;
        }

    }

    public interface IDDSEventListener{
        void audoSuccessListener();
    }


}
