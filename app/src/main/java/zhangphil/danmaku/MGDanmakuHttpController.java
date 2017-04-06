package zhangphil.danmaku;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;

import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Phil on 2017/3/31.
 */

public class MGDanmakuHttpController {
    private DataMessageListener mDataMessageListener = null;

    private final int WHAT_START = 0xff0a;

    private boolean promise = false;

    private int interval = 0;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.what == WHAT_START) {
                handler.removeMessages(WHAT_START);

                try {
                    if (promise)
                        startRequestDanmaku();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };


    public void startRequestDanmaku() throws Exception {
        promise = true;

        Observable mObservable = Observable.fromCallable(new Callable<List<DanmakuMsg>>() {
            @Override
            public List<DanmakuMsg> call() throws Exception {
                //同步方法返回观察者需要的数据结果
                //在这里处理线程化的操作
                return syncFetchDanmakuData();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        mObservable.subscribe(new DisposableObserver<List<DanmakuMsg>>() {

            @Override
            public void onNext(@NonNull List<DanmakuMsg> lists) {
                if (mDataMessageListener != null ) {
                    mDataMessageListener.onDataMessageListener(lists);
                }
            }

            @Override
            public void onComplete() {
                fireRequest();
            }

            @Override
            public void onError(Throwable e) {
                fireRequest();
            }
        });
    }

    public void stopRequestDanmaku() {
        promise = false;
    }

    /**
     * 设置轮询的间隔时间
     *
     * @param interval 单位毫秒 默认是0
     */
    public void setHttpRequestInterval(int interval) {
        this.interval = interval;
    }

    private void fireRequest() {
        //这里将触发重启数据请求，在这里可以调节重启数据请求的节奏。
        //比如可以设置一定的时延
        handler.sendEmptyMessageDelayed(WHAT_START, interval);
    }

    /**
     * 从服务器接口获取弹幕
     * 注意！此方法特意做成同步方式获取
     *
     * @return
     */
    protected List<DanmakuMsg> syncFetchDanmakuData() {
        return null;
    }


    public interface DataMessageListener {
        void onDataMessageListener(@NonNull List<DanmakuMsg> lists);
    }

    public void setDataMessageListener(DataMessageListener listener) {
        mDataMessageListener = listener;
    }
}