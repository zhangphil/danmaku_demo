package zhangphil.danmaku;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import master.flame.danmaku.ui.widget.DanmakuView;

public class MainActivity extends Activity {
    private MGDanmaku mMGDanmaku;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DanmakuView mDanmakuView = (DanmakuView) findViewById(R.id.danmakuView);

        MGDanmakuHttpController mMGDanmakuHttpController = new MyDanmakuHttp();
        mMGDanmakuHttpController.setHttpRequestInterval(0);
        mMGDanmaku = new MGDanmaku(mDanmakuView, mMGDanmakuHttpController);

        CheckBox checkBox = (CheckBox) findViewById(R.id.checkBox);
        checkBox.setChecked(mMGDanmaku.getDanmakuRunning());
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mMGDanmaku.setDanmakuRunning(isChecked);
            }
        });

        Button sendText = (Button) findViewById(R.id.sendText);
        sendText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DanmakuMsg msg = new DanmakuMsg();
                msg.msg = "zhangphil: " + System.currentTimeMillis();

                mMGDanmaku.sendMsg(msg);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMGDanmaku.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMGDanmaku.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMGDanmaku.onDestroy();
    }
}