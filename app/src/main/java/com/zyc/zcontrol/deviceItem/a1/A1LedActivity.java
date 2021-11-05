package com.zyc.zcontrol.deviceItem.a1;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.zyc.zcontrol.MainApplication;
import com.zyc.zcontrol.R;
import com.zyc.zcontrol.ServiceActivity;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceA1;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class A1LedActivity extends ServiceActivity {
    public final static String Tag = "A1LedActivity";


    private ArrayList<SeekBar> seekBar = new ArrayList<SeekBar>();
    private ArrayList<TextView> textView = new ArrayList<TextView>();
    ImageView imageView;
    CardView color_now;
    Bitmap bitmap;

    DeviceA1 device;

    Boolean Sflag = false;
    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {// handler接收到消息后就会执行此方法
            switch (msg.what) {
                case 1:
                    Send("{\"mac\": \"" + device.getMac() + "\",\"color\":null}");
                    break;
                case 2:
                    handler.removeMessages(2);
                    if (Sflag) {
                        handler.removeMessages(3);
                        color_now.setVisibility(View.VISIBLE);
                        Sflag = false;
                        int a = seekBar.get(0).getProgress() * 0x10000
                                + seekBar.get(1).getProgress() * 0x100
                                + seekBar.get(2).getProgress();
                        color_now.setCardBackgroundColor(a | 0xff000000);
                        Send("{\"mac\": \"" + device.getMac() + "\",\"color\":" + a + "}");
                        handler.sendEmptyMessageDelayed(3,1500);
                    }
                    break;
                case 3:
                    color_now.setVisibility(View.INVISIBLE);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a1_activity_led);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//左侧添加一个默认的返回图标
        getSupportActionBar().setHomeButtonEnabled(true); //设置返回键可用

        //region 设备
        Intent intent = this.getIntent();
        try {
            device = (DeviceA1) ((MainApplication) getApplication()).getDevice(intent.getStringExtra("mac"));
            if (device == null) {
                throw new Exception("获取数据出错:" + intent.getStringExtra("mac")); // 异常信息
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(A1LedActivity.this, "数据错误!请联系开发者", Toast.LENGTH_SHORT).show();
            finish();
        }
        //endregion

        this.setTitle("设置led颜色 "+device.getName());

        //region 控件初始化
        seekBar.add((SeekBar) findViewById(R.id.seekBarR));
        seekBar.add((SeekBar) findViewById(R.id.seekBarG));
        seekBar.add((SeekBar) findViewById(R.id.seekBarB));
        seekBar.get(0).setOnSeekBarChangeListener(seekListener);
        seekBar.get(1).setOnSeekBarChangeListener(seekListener);
        seekBar.get(2).setOnSeekBarChangeListener(seekListener);

        textView.add((TextView) findViewById(R.id.textViewR));
        textView.add((TextView) findViewById(R.id.textViewG));
        textView.add((TextView) findViewById(R.id.textViewB));
        color_now=findViewById(R.id.color_now);
        color_now.setVisibility(View.INVISIBLE);
        imageView = findViewById(R.id.hsl);
        //bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        imageView.post(new Runnable() {
            @Override
            public void run() {
                imageView.setDrawingCacheEnabled(true);
                bitmap = Bitmap.createBitmap(imageView.getDrawingCache());
                imageView.setDrawingCacheEnabled(false);
            }
        });


        imageView.setOnTouchListener(InamgeViewListener);
        findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (seekBar.get(0).getProgress() == 0 && seekBar.get(1).getProgress() == 0 && seekBar.get(2).getProgress() == 0) {
                    seekBar.get(0).setProgress(255);
                    seekBar.get(1).setProgress(255);
                    seekBar.get(2).setProgress(255);
                } else {
                    seekBar.get(0).setProgress(0);
                    seekBar.get(1).setProgress(0);
                    seekBar.get(2).setProgress(0);
                }
                Sflag = true;
            }
        });
        //endregion

        //启动定时器
        TimerTask mTimerTask = new TimerTask() {
            public void run() {
                handler.sendEmptyMessageDelayed(2, 0);
            }
        };
        Timer timer = new Timer(true);
        timer.schedule(mTimerTask, 200, 200); //延时200ms后执行，200ms执行一次
        //timer.cancel(); //退出计时器
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //滚动条监视事件
    private SeekBar.OnSeekBarChangeListener seekListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar arg, int progress, boolean fromUser) {


            textView.get(0).setText("R:" + String.format("%03d", seekBar.get(0).getProgress()));
            textView.get(1).setText("G:" + String.format("%03d", seekBar.get(1).getProgress()));
            textView.get(2).setText("B:" + String.format("%03d", seekBar.get(2).getProgress()));

            if (fromUser)
                Sflag = true;//sendRGB();

        }

        @Override
        public void onStartTrackingTouch(SeekBar arg0) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar arg0) {
            Sflag = true;//sendRGB();
        }

    };


    //ImageView触摸事件
    private View.OnTouchListener InamgeViewListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View arg0, MotionEvent arg1) {

            if (bitmap == null) return true;
            imageView.getParent().requestDisallowInterceptTouchEvent(true);

            int x = (int) arg1.getX();
            int y = (int) arg1.getY();

            if (y > bitmap.getHeight()) return true;
            if (x > bitmap.getHeight()) return true;

            try {
                int pixel = bitmap.getPixel(x, y);//获取颜色
                int redValue = Color.red(pixel);
                int greenValue = Color.green(pixel);
                int blueValue = Color.blue(pixel);

                if (redValue != 255 && greenValue != 255 && blueValue != 255)
                    return true;
                if (pixel == 0 || (redValue == 0 && greenValue == 0 && blueValue == 0))
                    return true; //仅判断pixel会偶尔无法跳出w
                seekBar.get(0).setProgress(redValue);
                seekBar.get(1).setProgress(greenValue);
                seekBar.get(2).setProgress(blueValue);

//                HSL = RGBtoHSL(redValue, greenValue, blueValue);
                Sflag = true;//sendRGB();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return true;

        }

    };


    //region 数据接收发送处理函数
    void Send(String message) {
        Log.d(Tag, "Send:" + message);
        boolean b = getSharedPreferences("Setting_" + device.getMac(), 0).getBoolean("always_UDP", false);
        super.Send(b, device.getSendMqttTopic(), message);
    }

    public void Receive(String ip, int port, String topic, String message) {

        try {
            JSONObject jsonObject = new JSONObject(message);
            if (!jsonObject.has("mac") || !jsonObject.getString("mac").equals(device.getMac())) {
                return;
            }

//            if (jsonObject.has("color")){
//                int color=jsonObject.optInt("color",0);
//                seekBar.get(0).setProgress((color>>16)&0xff);
//                seekBar.get(1).setProgress((color>>8)&0xff);
//                seekBar.get(2).setProgress((color>>0)&0xff);
//            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    //endregion
    //region 事件监听调用函数,主要为在子类中重写此函数实现在service建立成功/mqtt连接成功/失败时执行功能
    //Service建立成功时调用    此函数需要时在子类中重写
    public void ServiceConnected() {
        handler.sendEmptyMessageDelayed(1, 0);
    }

    //mqtt连接成功时调用    此函数需要时在子类中重写
    public void MqttConnected() {
        handler.sendEmptyMessageDelayed(1, 0);
    }

    //mqtt连接断开时调用    此函数需要时在子类中重写
    public void MqttDisconnected() {
        handler.sendEmptyMessageDelayed(1, 0);
    }
    //endregion
}