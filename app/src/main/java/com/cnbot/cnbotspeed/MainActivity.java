package com.cnbot.cnbotspeed;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.cnbot.cnbotspeed.asr.AsrTools;
import com.cnbot.cnbotspeed.server.DDSService;
import com.cnbot.cnbotspeed.tts.TtsTools;
import com.cnbot.cnbotspeed.wakeup.WakeupTools;

public class MainActivity extends AppCompatActivity {

    private WakeupTools wakeupTools;
    private AsrTools asrTools;
    private TtsTools ttsTools;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        initService();
    }


    private void initService() {
        startService(new Intent(this, DDSService.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, DDSService.class));
        if(wakeupTools != null){
            wakeupTools.release();
        }
        if(ttsTools != null){
            ttsTools.release();
        }
    }

    public void onStartWakeUpClick(View view) {
        wakeupTools.enableWakeup();
    }

    public void onStopWakeUpClick(View view) {
        wakeupTools.disableWakeup();
    }

    public void onGetMainWakeUpClick(View view) {
        wakeupTools.getMainWakeupWords();
    }

    public void onInitWakeupClick(View view) {
        wakeupTools = WakeupTools.getInstance(MainActivity.this);
        wakeupTools.initWakeup();
    }

    public void onStartAsrClick(View view) {
        asrTools.startListening();
    }

    public void onStopAsrClick(View view) {
        asrTools.stopListening();
    }

    public void onInitAsrClick(View view) {
        asrTools = AsrTools.getInstance(MainActivity.this);
        asrTools.initAsr();
    }

    public void onStopTtsClick(View view) {
        ttsTools.shupWithNULL();
    }

    public void onStartTtsClick(View view) {
        ttsTools.speek("求知若饥，虚心若愚");
    }

    public void onInitTtsClick(View view) {
        ttsTools = TtsTools.getInstance(MainActivity.this);
        ttsTools.initTts();
    }


}