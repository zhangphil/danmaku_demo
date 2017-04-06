package zhangphil.danmaku;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Phil on 2017/4/5.
 */

public class MyDanmakuHttp extends MGDanmakuHttpController{

    private int msgId = 0;

    @Override
    protected List<DanmakuMsg> syncFetchDanmakuData() {
        try {
            Thread.sleep((int) (Math.random() * 3000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int count = (int) (Math.random() * 10);

        //装配模拟数据
        List<DanmakuMsg> danmakuMsgs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            DanmakuMsg danmakuMsg = new DanmakuMsg();
            danmakuMsg.msg = String.valueOf(msgId++);
            danmakuMsgs.add(danmakuMsg);
        }

        return danmakuMsgs;
    }
}
