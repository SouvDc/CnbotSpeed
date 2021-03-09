package com.cnbot.cnbotspeed;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.aispeech.ailog.AILog;
import com.aispeech.dui.dds.DDS;
import com.aispeech.dui.dds.exceptions.DDSNotInitCompleteException;
import com.cnbot.cnbotspeed.adapter.adapter.DialogAdapter;
import com.cnbot.cnbotspeed.bean.MessageBean;
import com.cnbot.cnbotspeed.observer.DuiMessageObserver;
import com.cnbot.cnbotspeed.server.DDSService;
import com.cnbot.cnbotspeed.view.WrapContentLinearLayoutManager;

import java.util.LinkedList;

public class HomeActivity extends AppCompatActivity implements DuiMessageObserver.MessageCallback{
    private static final String TAG = HomeActivity.class.getSimpleName();
    private Handler mHandler = new Handler();
    private TextView mInputTv;// 下面的状态展示textview
    private RecyclerView mRecyclerView;// 列表展示控件
    private boolean mIsActivityShowing = false;// 当前页面是否可见

    private LinkedList<MessageBean> mMessageList = new LinkedList<>();// 当前消息容器
    private DuiMessageObserver mMessageObserver = new DuiMessageObserver();// 消息监听器

    private DialogAdapter mDialogAdapter;  // 各种UI控件的实现在DialogAdapter类里
    private DDSService.DDSServiceBinder ddsServiceBinder;
    private DDSService ddsService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initView();
        initService();
    }

    private void initView() {
        mInputTv = (TextView) this.findViewById(R.id.input_tv);

        mInputTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    DDS.getInstance().getAgent().avatarClick();
                } catch (DDSNotInitCompleteException e) {
                    e.printStackTrace();
                }
            }
        });

        mRecyclerView = (RecyclerView) this.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new WrapContentLinearLayoutManager(this));

        mDialogAdapter = new DialogAdapter(mMessageList);
        mRecyclerView.setAdapter(mDialogAdapter);
    }

    private void initService() {
        bindService(new Intent(this, DDSService.class), serviceConnection, Service.BIND_AUTO_CREATE);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ddsServiceBinder = (DDSService.DDSServiceBinder) service;
            ddsService = (DDSService) ddsServiceBinder.getService();
            ddsService.setIddsEventListener(new DDSService.IDDSEventListener() {
                @Override
                public void audoSuccessListener() {
                    registMsg();
                    enableWakeup();
                    refreshTv("等待唤醒...");
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if (DDS.getInstance().getAgent() == null) {
            Log.e(TAG, "onStart agent is null, return ...");
            return;
        }
        mIsActivityShowing = true;

        registMsg();
    }

    private void registMsg() {
        // 注册消息监听器
        mMessageObserver.regist(this, mMessageList);
    }

    @Override
    protected void onStop() {
        AILog.d(TAG, "onStop() " + this.hashCode());
        mIsActivityShowing = false;
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DDS.getInstance().getAgent() == null) {
            Log.e(TAG, "onResume agent is null, return ...");
            return;
        }

        sendHiMessage();
        refreshTv("等待唤醒...");
        enableWakeup();
    }

    @Override
    protected void onPause() {
        mMessageObserver.unregist();
        refreshTv("等待唤醒...");
        disableWakeup();
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
//        stopService(new Intent(this, DDSService.class));
        unbindService(serviceConnection);
    }


    // 打开唤醒，调用后才能语音唤醒
    void enableWakeup() {
        try {
            DDS.getInstance().getAgent().getWakeupEngine().enableWakeup();
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }

    // 关闭唤醒, 调用后将无法语音唤醒
    void disableWakeup() {
        try {
            DDS.getInstance().getAgent().stopDialog();
            DDS.getInstance().getAgent().getWakeupEngine().disableWakeup();
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }



    // 更新 tv状态
    private void refreshTv(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mInputTv.setText(text);
            }
        });
    }



    @Override
    public void onMessage() {
        notifyItemInserted();
    }

    @Override
    public void onState(String state) {
        switch (state) {
            case "avatar.silence":
                refreshTv("等待唤醒...");
                break;
            case "avatar.listening":
                refreshTv("监听中...");
                break;
            case "avatar.understanding":
                refreshTv("理解中...");
                break;
            case "avatar.speaking":
                refreshTv("播放语音中...");
                break;
        }
        DialogAdapter.mState = state;
    }

    // dds初始化成功之后,展示一个打招呼消息,告诉用户可以开始使用
    public void sendHiMessage() {
        String[] wakeupWords = new String[0];
        String minorWakeupWord = null;
        try {
            // 获取主唤醒词
            wakeupWords = DDS.getInstance().getAgent().getWakeupEngine().getWakeupWords();
            // 获取副唤醒词
            minorWakeupWord = DDS.getInstance().getAgent().getWakeupEngine().getMinorWakeupWord();
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
        String hiStr = "";
        if (wakeupWords != null && minorWakeupWord != null) {
            hiStr = getString(R.string.hi_str2, wakeupWords[0], minorWakeupWord);
        } else if (wakeupWords != null && wakeupWords.length == 2) {
            hiStr = getString(R.string.hi_str2, wakeupWords[0], wakeupWords[1]);
        } else if (wakeupWords != null && wakeupWords.length > 0) {
            hiStr = getString(R.string.hi_str, wakeupWords[0]);
        }
        Log.e("dengzi", "histr = " + hiStr);
        if (!TextUtils.isEmpty(hiStr)) {
            MessageBean bean = new MessageBean();
            bean.setText(hiStr);
            bean.setType(MessageBean.TYPE_OUTPUT);
            mMessageList.add(bean);
            mDialogAdapter.notifyItemInserted(mMessageList.size());
            mRecyclerView.smoothScrollToPosition(mMessageList.size());
        }
    }

    // 更新ui列表展示
    public void notifyItemInserted() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mDialogAdapter.notifyDataSetChanged();
                mRecyclerView.smoothScrollToPosition(mMessageList.size());
            }
        });
    }
}