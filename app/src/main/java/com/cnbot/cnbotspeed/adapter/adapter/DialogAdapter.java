package com.cnbot.cnbotspeed.adapter.adapter;

import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.aispeech.ailog.AILog;
import com.aispeech.dui.dds.DDS;
import com.cnbot.cnbotspeed.R;
import com.cnbot.cnbotspeed.bean.MessageBean;
import com.cnbot.cnbotspeed.view.pageview.view.PageRecyclerView;
import com.cnbot.cnbotspeed.view.pageview.view.PageView;

import java.util.ArrayList;
import java.util.LinkedList;

import static com.aispeech.dui.oauth.OAuthSdk.getContext;


public class DialogAdapter extends RecyclerView.Adapter {

    private static final String TAG = "DialogAdapter";
    public static LinkedList<MessageBean> mList;
    public static String mState;

    public DialogAdapter(LinkedList<MessageBean> list) {
        mList = list;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        AILog.i(TAG, "onCreateViewHolder" + viewType);
        View view = null;
        RecyclerView.ViewHolder holder = null;
        switch (viewType) {
            case MessageBean.TYPE_INPUT:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.msg_input, parent, false);
                holder = new InputViewHolder(view);
                break;
            case MessageBean.TYPE_OUTPUT:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.msg_output, parent, false);
                holder = new OutputViewHolder(view);
                break;

            default:
        }
        return holder;
    }

    private void bindPageView(final PageView pageview, final MessageBean message, final int position) {
        AILog.i(TAG, "bindPageView" + position + ", listwidget page: " + message.getCurrentPage() + ", pageview: " + pageview);
        ArrayList<MessageBean> items = message.getMessageBeanList();
        pageview.setPageRow(message.getItemsPerPage());
        ListWidgetAdapter adapter = new ListWidgetAdapter(getContext(), R.layout.item_horizontal_grid2, position);
        pageview.setAdapter(adapter);
        pageview.updateAll(items);

        Runnable setCurrentItemRunnable = () -> pageview.setCurrentItem(message.getCurrentPage() - 1, false);

        Runnable addPageChangeListenerRunnable = () -> pageview.addOnPageChangeListener(new ListWidgetPageChangeListener(position));
        pageview.postDelayed(setCurrentItemRunnable, 200);
        pageview.postDelayed(addPageChangeListenerRunnable, 1000);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        try {
            MessageBean message = mList.get(position);
            if (message == null) {
                return;
            }
            int itemViewType = message.getType();
            switch (itemViewType) {
                case MessageBean.TYPE_INPUT:
                    ((InputViewHolder) holder).content.setText(message.getText());
                    break;
                case MessageBean.TYPE_OUTPUT:
                    ((OutputViewHolder) holder).content.setText(message.getText());
                    break;

            }
        }catch (Exception e){
            mList.clear();
        }
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }


    @Override
    public int getItemViewType(int position) {
        Log.d(TAG, "getItemViewType " + position);
        if (position >= mList.size()) {
            return 0;
        }
        return mList.get(position).getType();
    }

    class OutputViewHolder extends RecyclerView.ViewHolder {
        private TextView content;

        public OutputViewHolder(View itemView) {
            super(itemView);
            content = itemView.findViewById(R.id.content);
        }
    }

    class InputViewHolder extends RecyclerView.ViewHolder {
        private TextView content;

        public InputViewHolder(View itemView) {
            super(itemView);
            content = itemView.findViewById(R.id.content);
        }
    }


    public class ListWidgetPageChangeListener implements PageRecyclerView.OnPageChangeListener {

        private int mPosition;

        ListWidgetPageChangeListener(int position) {
            mPosition = position;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            MessageBean message = mList.get(mPosition);
            message.setCurrentPage(position + 1);
            if (!mState.equals("avatar.silence") && mPosition == mList.size() - 1) {
                int targetPage = position + 1;
                DDS.getInstance().getAgent().publishSticky("list.page.switch", "{\"pageNumber\":" + targetPage + "}");
            }
//            listener.onSelected(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }

    private OnPageChangeListener listener;

    public void setOnPageChangeListener(OnPageChangeListener listener) {
        this.listener = listener;
    }

    public interface OnPageChangeListener {
        void onSelected(int position);
    }


}
