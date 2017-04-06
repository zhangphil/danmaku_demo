package zhangphil.danmaku;

/**
 * Created by Phil on 2017/3/31.
 */

import java.io.Serializable;

/**
 * 弹幕数据封装的类（bean）
 */
public class DanmakuMsg implements Serializable {
    public String msg = null;
    public boolean islive = true;
}