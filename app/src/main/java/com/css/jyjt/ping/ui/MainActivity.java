package com.css.jyjt.ping.ui;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.css.jyjt.ping.R;
import com.css.jyjt.ping.bean.ResultBean;
import com.css.jyjt.ping.utils.PingHelper;
import com.gyf.immersionbar.ImmersionBar;
import com.pengxh.app.multilib.base.DoubleClickExitActivity;
import com.pengxh.app.multilib.utils.DensityUtil;
import com.pengxh.app.multilib.utils.StatusBarColorHelper;
import com.pengxh.app.multilib.widget.EasyToast;
import com.pengxh.app.multilib.widget.dialog.InputDialog;

import java.lang.ref.WeakReference;
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
    @BindView(R.id.minDelayedView)
    TextView minDelayedView;
    @BindView(R.id.maxDelayedView)
    TextView maxDelayedView;
    @BindView(R.id.startPing)
    ImageButton startPing;

    private String[] spinnerItems = new String[]{"请输入您要检测的地址", "www.hao123.com", "www.baidu.com", "www.google.com", "59.255.102.5", "其他"};
    private Timer pingTimer;
    private long sendTimes = 0;//发送次数
    private long receiveTimes = 0;//接收次数
    private long failedTimes = 0;//失败次数
    private boolean isRunning = false;//定时器是否还在运行中
    private String address;
    private double min = 9999d;//最小延迟初始值
    private double max = 0d;//最大延迟初始值
    private WeakReferenceHandler handler;

    @Override
    public int initLayoutView() {
        return R.layout.activity_main;
    }

    @Override
    public void initData() {
        StatusBarColorHelper.setColor(this, getResources().getColor(R.color.colorPrimary));
        ImmersionBar.with(this).init();
        handler = new WeakReferenceHandler(this);
    }

    @Override
    public void initEvent() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerItems);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        addressSpinner.setDropDownVerticalOffset(DensityUtil.dp2px(this, 48));
        addressSpinner.setAdapter(spinnerAdapter);
        addressSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String s = spinnerItems[pos];
                if (s.equals("其他")) {
                    new InputDialog.Builder()
                            .setContext(MainActivity.this)
                            .setTitle("")
                            .setOutsideCancelable(false)
                            .setNegativeButton("")
                            .setPositiveButton("")
                            .setOnDialogClickListener(new InputDialog.OnDialogClickListener() {
                                @Override
                                public void onConfirmClick(String value) {

                                }

                                @Override
                                public void onCancelClick() {

                                }
                            }).build().show();
                } else {
                    address = s;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });
        //清屏
        pingResultView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                pingResultView.setText("");
                return true;
            }
        });
    }

    @OnClick({R.id.startPing, R.id.stopPing})
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.startPing:
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
                        message.obj = PingHelper.ping(address);
                        handler.sendMessage(message);
                        //次数统计
                        sendTimes++;
                        //timer状态管理
                        isRunning = true;
                        handler.sendEmptyMessage(111);
                    }
                }, 0, 1000);
                break;
            case R.id.stopPing:
                if (isRunning && pingTimer != null) {
                    pingTimer.cancel();
                    isRunning = false;
                    handler.sendEmptyMessage(111);
                }
                break;
            default:
                break;
        }
    }

    /**
     * 继承Handler，解决Handler内存泄漏
     */
    private static class WeakReferenceHandler extends Handler {
        private WeakReference<MainActivity> mWeakReference;

        WeakReferenceHandler(MainActivity activity) {
            mWeakReference = new WeakReference<>(activity);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            MainActivity mainActivity = mWeakReference.get();
            switch (msg.what) {
                case 110:
                    mainActivity.sendTimesView.setText("发送数据：" + mainActivity.sendTimes + "次");
                    ResultBean resultBean = (ResultBean) msg.obj;
                    if (resultBean == null) {
                        EasyToast.showToast("无法访问目标地址，请检查！", EasyToast.WARING);
                        return;
                    }
                    mainActivity.resultHostView.setText(resultBean.getTitle());
                    String s = resultBean.getContent();
                    int endIndex = s.indexOf("--- ");
                    String content = s.substring(0, endIndex).trim();
                    mainActivity.pingResultView.append(content + "\n");
                    if (s.contains("100% packet loss")) {
//                    --- www.google.com ping statistics ---
//                            1 packets transmitted, 0 received, 100% packet loss, time 0ms
                        mainActivity.failedTimes++;
                        mainActivity.failedTimesView.setText("返回失败：" + mainActivity.failedTimes + "次");
                    } else {
//                    --- www.a.shifen.com ping statistics ---
//                            1 packets transmitted, 1 received, 0% packet loss, time 0ms
//                    rtt min/avg/max/mdev = 11.871/11.871/11.871/0.000 ms
                        mainActivity.receiveTimes++;
                        mainActivity.receiveTimesView.setText("返回成功：" + mainActivity.receiveTimes + "次");
                        /**
                         * 获取延迟秒数
                         * 64 bytes from 220.181.107.181: icmp_seq=1 ttl=52 time=33.7 ms
                         *
                         * [64 bytes from 220.181.107.181: icmp_seq, 1 ttl, 52 time, 34.0 ms]
                         *
                         * 23.4 ms
                         * */
                        double millis = Double.parseDouble(content.split("=")[3].split(" ")[0]);
                        if (millis < mainActivity.min) {
                            mainActivity.min = millis;
                            mainActivity.minDelayedView.setText("最低延迟：" + mainActivity.min + "ms");
                        }
                        if (millis > mainActivity.max) {
                            mainActivity.max = millis;
                            mainActivity.maxDelayedView.setText("最高延迟：" + mainActivity.max + "ms");
                        }
                    }
                    if (mainActivity.sendTimes != 0) {
                        NumberFormat numberInstance = NumberFormat.getNumberInstance();
                        numberInstance.setMaximumFractionDigits(2);
                        double rate = ((double) mainActivity.failedTimes / mainActivity.sendTimes) * 100;
                        mainActivity.lossRateView.setText("丢包率：" + numberInstance.format(rate) + "%");
                    }
                    break;
                case 111:
                    if (mainActivity.isRunning) {
                        mainActivity.startPing.setEnabled(false);
                    } else {
                        mainActivity.startPing.setEnabled(true);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
