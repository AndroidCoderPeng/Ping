package com.css.jyjt.ping.ui;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.css.jyjt.ping.R;
import com.css.jyjt.ping.ResultBean;
import com.css.jyjt.ping.utils.StatusBarColorHelper;
import com.gyf.immersionbar.ImmersionBar;
import com.pengxh.app.multilib.base.DoubleClickExitActivity;
import com.pengxh.app.multilib.widget.EasyToast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.OnClick;

public class MainActivity extends DoubleClickExitActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    @BindView(R.id.addressSpinner)
    Spinner addressSpinner;
    @BindView(R.id.resultHostView)
    TextView resultHostView;
    @BindView(R.id.pingResultView)
    TextView pingResultView;
    @BindView(R.id.sendTimesView)
    TextView sendTimesView;
    @BindView(R.id.receiveTimesView)
    TextView receiveTimesView;
    @BindView(R.id.failedTimesView)
    TextView failedTimesView;
    @BindView(R.id.lossRateView)
    TextView lossRateView;
    @BindView(R.id.startPing)
    ImageButton startPing;

    private Timer pingTimer;
    private long sendTimes = 0;//发送次数
    private long receiveTimes = 0;//接收次数
    private long failedTimes = 0;//失败次数
    private boolean isRunning = false;//定时器是否还在运行中
    private String address;

    @Override
    public int initLayoutView() {
        return R.layout.activity_main;
    }

    @Override
    public void initData() {
        StatusBarColorHelper.setColor(this, getResources().getColor(R.color.colorPrimary));
        ImmersionBar.with(this).init();
    }

    @Override
    public void initEvent() {
        addressSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String s = getResources().getStringArray(R.array.spinner_values)[pos];
                if (s.equals("其他")) {

                } else {
                    address = s;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });
    }

    @OnClick({R.id.startPing, R.id.stopPing})
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.startPing) {
            if (address.equals("") || address.contains("请输入您要检测的地址")) {
                EasyToast.showToast("输入错误，请检查！", EasyToast.WARING);
                return;
            }
            pingTimer = new Timer();
            pingTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Message message = handler.obtainMessage();
                    message.what = 110;
                    message.obj = ping(address);
                    handler.sendMessage(message);
                    //次数统计
                    sendTimes++;
                    //timer状态管理
                    isRunning = true;
                    handler.sendEmptyMessage(111);
                }
            }, 0, 1000);
        } else if (v.getId() == R.id.stopPing) {
            if (isRunning && pingTimer != null) {
                pingTimer.cancel();
                isRunning = false;
                handler.sendEmptyMessage(111);
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 110) {
                sendTimesView.setText("发送数据：" + sendTimes + "次");

                ResultBean resultBean = (ResultBean) msg.obj;
                if (resultBean == null) {
                    EasyToast.showToast("无法访问目标地址，请检查！", EasyToast.WARING);
                    return;
                }
                resultHostView.setText(resultBean.getTitle());
                String content = resultBean.getContent();
//                Log.d(TAG, content);
                int endIndex = content.indexOf("--- ");
                pingResultView.append(content.substring(0, endIndex).trim() + "\n");
                if (content.contains("100% packet loss")) {
//                    --- www.google.com ping statistics ---
//                            1 packets transmitted, 0 received, 100% packet loss, time 0ms
                    failedTimes++;
                    failedTimesView.setText("返回失败：" + failedTimes + "次");
                } else {
//                    --- www.a.shifen.com ping statistics ---
//                            1 packets transmitted, 1 received, 0% packet loss, time 0ms
//                    rtt min/avg/max/mdev = 11.871/11.871/11.871/0.000 ms
//                    receiveTimes++;
//                    receiveTimesView.setText("返回成功：" + receiveTimes + "次");
                    receiveTimes++;
                    receiveTimesView.setText("返回成功：" + receiveTimes + "次");
                }
                if (sendTimes != 0) {
                    NumberFormat numberInstance = NumberFormat.getNumberInstance();
                    numberInstance.setMaximumFractionDigits(2);
                    double rate = ((double) failedTimes / sendTimes) * 100;
                    lossRateView.setText("丢包率：" + numberInstance.format(rate) + "%");
                }
            } else if (msg.what == 111) {
                if (isRunning) {
                    startPing.setEnabled(false);
                } else {
                    startPing.setEnabled(true);
                }
            }
        }
    };

    /**
     * 每秒ping一次目的IP
     */
    private ResultBean ping(String address) {
        ResultBean resultBean = new ResultBean();
        StringBuilder builder = new StringBuilder();
        Runtime runtime = Runtime.getRuntime();
        Process process = null;
        try {
            process = runtime.exec("ping -c 1 -w 1 " + address);
            InputStreamReader streamReader = new InputStreamReader(process.getInputStream());
            BufferedReader reader = new BufferedReader(streamReader);
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            String s = builder.toString();
            if (s.equals("")) {
                return null;
            }
            int index = s.indexOf("bytes of data.");
            resultBean.setTitle(s.substring(0, index + 14));
            resultBean.setContent(s.substring(index + 15));
            return resultBean;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroy();
            }
            runtime.gc();
        }
        return null;
    }
}
