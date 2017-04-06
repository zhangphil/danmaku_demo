package zhangphil.danmaku;

import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.ui.widget.DanmakuView;

/**
 * Created by Phil on 2017/4/1.
 */

public class MGDanmaku {
    private final String TAG = getClass().getName() + UUID.randomUUID();

    private MGDanmakuHttpController mMGDanmakuHttpController;
    private DanmakuView mDanmakuView;
    private AcFunDanmakuParser mParser;

    private DanmakuContext mDanmakuContext;

    private final int MAX_DANMAKU_LINES = 8; //弹幕在屏幕显示的最大行数

    private ConcurrentLinkedQueue<DanmakuMsg> mQueue = null; //所有的弹幕数据存取队列，在这里做线程的弹幕取和存
    private ArrayList<DanmakuMsg> danmakuLists = null;//每次请求最新的弹幕数据后缓存list

    private final int WHAT_GET_LIST_DATA = 0xffab01;
    private final int WHAT_DISPLAY_SINGLE_DANMAKU = 0xffab02;

    /**
     * 每次弹幕的各种颜色从这里面随机的选一个
     */
    private final int[] colors = {
            Color.RED,
            Color.YELLOW,
            Color.BLUE,
            Color.GREEN,
            Color.CYAN,
            Color.DKGRAY};

    //弹幕开关总控制
    // true正常显示和请求
    // false则取消
    private boolean isDanmukuEnable = false;

    public MGDanmaku(@NonNull DanmakuView view, @NonNull MGDanmakuHttpController controller) {
        this.mDanmakuView = view;
        this.mMGDanmakuHttpController = controller;

        initDanmaku();

        danmakuLists = new ArrayList<>();
        mQueue = new ConcurrentLinkedQueue<>();

        mMGDanmakuHttpController.setDataMessageListener(new MGDanmakuHttpController.DataMessageListener() {
            @Override
            public void onDataMessageListener(@NonNull List<DanmakuMsg> lists) {
                danmakuLists = (ArrayList<DanmakuMsg>) lists;
                for (int i = 0; i < danmakuLists.size(); i++) {
                    Log.d("前端获得数据", danmakuLists.get(i).msg);
                }

                addListData();
            }
        });

        Log.d(getClass().getName(), TAG);
    }

    private Handler mDanmakuHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case WHAT_GET_LIST_DATA:
                    addListData();
                    break;

                case WHAT_DISPLAY_SINGLE_DANMAKU:
                    mDanmakuHandler.removeMessages(WHAT_DISPLAY_SINGLE_DANMAKU);
                    displayDanmaku();
                    break;
            }
        }
    };

    private void addListData() {
        if (danmakuLists != null && !danmakuLists.isEmpty()) {
            mDanmakuHandler.removeMessages(WHAT_GET_LIST_DATA);

            mQueue.addAll(danmakuLists);
            danmakuLists.clear();

            mDanmakuHandler.sendEmptyMessage(WHAT_DISPLAY_SINGLE_DANMAKU);
        }
    }


    private void initDanmaku() {
        // 设置最大显示行数
        HashMap<Integer, Integer> maxLinesPair = new HashMap<>();
        maxLinesPair.put(BaseDanmaku.TYPE_SCROLL_RL, MAX_DANMAKU_LINES); // 滚动弹幕最大显示5行

        // 设置是否禁止重叠
        HashMap<Integer, Boolean> overlappingEnablePair = new HashMap<>();
        overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_RL, true);
        overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_TOP, true);

        mDanmakuContext = DanmakuContext.create();

        //普通文本弹幕也描边设置样式
        //如果是图文混合编排编排，最后不要描边
        mDanmakuContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 10) //描边的厚度
                .setDuplicateMergingEnabled(false)
                //.setScrollSpeedFactor(0.1f) //弹幕的速度。注意！此值越小，速度越快，比如0.1！值越大，速度越慢。// by phil
                .setScaleTextSize(1.2f)  //缩放的值
//        .setCacheStuffer(new BackgroundCacheStuffer())  // 绘制背景使用BackgroundCacheStuffer
                .setMaximumLines(maxLinesPair)
                .preventOverlapping(overlappingEnablePair);

        mParser = new AcFunDanmakuParser();

        mDanmakuView.prepare(mParser, mDanmakuContext);

        //mDanmakuView.showFPS(true);
        mDanmakuView.enableDanmakuDrawingCache(true);

        if (mDanmakuView != null) {
            mDanmakuView.setCallback(new master.flame.danmaku.controller.DrawHandler.Callback() {
                @Override
                public void updateTimer(DanmakuTimer timer) {
                }

                @Override
                public void drawingFinished() {

                }

                @Override
                public void danmakuShown(BaseDanmaku danmaku) {
                    Log.d("弹幕飘过", "文本:" + danmaku.text);
                }

                @Override
                public void prepared() {
                    mDanmakuView.start();
                }
            });
        }
    }

    /**
     * 驱动弹幕显示机制重新运作起来
     */
    private void startDanmaku() {
        mDanmakuView.show();
        //mDanmakuView.start();

        try {
            mMGDanmakuHttpController.startRequestDanmaku();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mDanmakuHandler.sendEmptyMessage(WHAT_GET_LIST_DATA);
        mDanmakuHandler.sendEmptyMessage(WHAT_DISPLAY_SINGLE_DANMAKU);
    }

    private void stopDanmaku() {
        mMGDanmakuHttpController.stopRequestDanmaku();

        if (mDanmakuView != null) {
            mDanmakuView.hide();
            mDanmakuView.clearDanmakusOnScreen();
            mDanmakuView.clear();
        }

        mDanmakuHandler.removeMessages(WHAT_GET_LIST_DATA);
        mDanmakuHandler.removeMessages(WHAT_DISPLAY_SINGLE_DANMAKU);

        danmakuLists.clear();
        mQueue.clear();
    }

    public void setDanmakuRunning(boolean enable) {
        //如果是重复设置，则跳过
        if (isDanmukuEnable == enable) {
            return;
        }

        this.isDanmukuEnable = enable;

        //Log.d("isDanmukuEnable", String.valueOf(isDanmukuEnable));

        if (isDanmukuEnable) {
            startDanmaku();

            try {

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            stopDanmaku();
        }
    }


    public boolean getDanmakuRunning() {
        return isDanmukuEnable;
    }

    public void sendMsg(@NonNull DanmakuMsg danmakuMsg) {
        displayDanmaku(danmakuMsg);
    }

    public void onResume() {
        if (mDanmakuView != null && mDanmakuView.isPrepared() && mDanmakuView.isPaused()) {
            mDanmakuView.resume();

            //弹幕重新进入可见，重启网络请求准备数据
            try {
                mMGDanmakuHttpController.startRequestDanmaku();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void onPause() {
        if (mDanmakuView != null && mDanmakuView.isPrepared()) {
            mDanmakuView.pause();

            //如果当前弹幕由于用户按home键使得弹幕view进入pause不可见状态，那么久不要请求网络数据了
            mMGDanmakuHttpController.stopRequestDanmaku();
        }
    }

    public void onDestroy() {
        if (mDanmakuView != null) {
            // dont forget release!
            mDanmakuView.release();
            mDanmakuView = null;
        }

        stopDanmaku();
    }

    private void displayDanmaku(@NonNull DanmakuMsg dm) {
        //如果当前的弹幕由于Android生命周期的原因进入暂停状态，那么不应该不停的消耗弹幕数据
        //要知道，在这里发出一个handler消息，那么将会消费（删掉）ConcurrentLinkedQueue头部的数据
        if (isDanmukuEnable) {
            if (!TextUtils.isEmpty(dm.msg)) {
                addDanmaku(dm.msg, dm.islive);
            }
        }
    }

    private void displayDanmaku() {
        if (isShouldDisplayDanmaku()) {
            DanmakuMsg dm = mQueue.poll();
            if (!TextUtils.isEmpty(dm.msg)) {
                addDanmaku(dm.msg, dm.islive);

                //上面已经把一条弹幕在屏幕上飘过了，继续飘下一条。
                //如果不希望飘出来的弹幕像砍刀砍出来一样整齐，那么可以考虑发一个随机的delay时延
                if (isShouldDisplayDanmaku()) {
                    mDanmakuHandler.sendEmptyMessageDelayed(WHAT_DISPLAY_SINGLE_DANMAKU, (long) (Math.random() * 400) + 100);
                }
            }
        }
    }

    private boolean isShouldDisplayDanmaku() {
        boolean b = !mQueue.isEmpty() && getDanmakuRunning();
        return b;
    }

    private void addDanmaku(CharSequence cs, boolean islive) {
        BaseDanmaku danmaku = mDanmakuContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
        if (danmaku == null || mDanmakuView == null) {
            return;
        }

        //danmaku.duration = new Duration(1000);//时延，弹幕在屏幕上飘过的时长
        danmaku.text = cs;
        danmaku.padding = 5;
        danmaku.priority = 0;  // 可能会被各种过滤器过滤并隐藏显示
        danmaku.isLive = islive;
        danmaku.setTime(mDanmakuView.getCurrentTime());
        danmaku.textSize = 20f * (mParser.getDisplayer().getDensity() - 0.6f); //文本弹幕字体大小
        danmaku.textColor = getRandomColor(); //文本的颜色
        danmaku.textShadowColor = getRandomColor(); //文本弹幕描边的颜色
        //danmaku.underlineColor = Color.DKGRAY; //文本弹幕下划线的颜色
        danmaku.borderColor = getRandomColor(); //边框的颜色

        mDanmakuView.addDanmaku(danmaku);
    }

    /**
     * 从一系列颜色中随机选择一种颜色
     *
     * @return
     */
    private int getRandomColor() {
        int i = ((int) (Math.random() * 10)) % colors.length;
        return colors[i];
    }
}