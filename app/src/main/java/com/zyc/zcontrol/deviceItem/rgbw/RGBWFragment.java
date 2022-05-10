package com.zyc.zcontrol.deviceItem.rgbw;


import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.tabs.TabLayout;
import com.zyc.zcontrol.R;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceFragment;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceRGBW;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 */
public class RGBWFragment extends DeviceFragment {
    public final static String Tag = "RGBWFragment";


    DeviceRGBW device;
    //region 控件
    private SwipeRefreshLayout mSwipeLayout;

    TextView textViewR;
    TextView textViewG;
    TextView textViewB;
    TextView textViewW;
    SeekBar seekBarR;
    SeekBar seekBarG;
    SeekBar seekBarB;
    SeekBar seekBarW;
    Button btn_close;
    Button btn_open;
    CheckBox chkGradient;
    CardView color_now;
    CardView color_set;


    LinearLayout ll_favorite;
    ImageView img_color;
    ImageView img_white;

    Bitmap[] bitmaps = new Bitmap[2];

    TabLayout tablayout;
    //endregion


    public RGBWFragment() {

        // Required empty public constructor
    }

    @SuppressLint("ValidFragment")
    public RGBWFragment(DeviceRGBW device) {
        super(device.getName(), device.getMac());
        this.device = device;
    }

    //region Handler
    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {// handler接收到消息后就会执行此方法
            switch (msg.what) {
                case 1:
                    Send("{\"mac\":\"" + device.getMac() + "\",\"rgb\":null}");
                    break;
                case 2:
                    Log.d(Tag, "send color:" + msg.obj);

                    Send("{\"mac\":\"" + device.getMac() + "\",\"rgb\":" + msg.obj + ",\"gradient\":" + (chkGradient.isChecked() ? "1" : "0") + "}");
                    break;
            }
        }
    };

    //endregion
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.rgbw_fragment, container, false);

        //region 控件初始化

        color_now = view.findViewById(R.id.color_now);
        color_set = view.findViewById(R.id.color_set);
        textViewR = view.findViewById(R.id.textViewR);
        textViewG = view.findViewById(R.id.textViewG);
        textViewB = view.findViewById(R.id.textViewB);
        textViewW = view.findViewById(R.id.textViewW);

        //region 拖动条 处理viewpage/SwipeRefreshLayout滑动冲突事件
        seekBarR = view.findViewById(R.id.seekBarR);
        seekBarG = view.findViewById(R.id.seekBarG);
        seekBarB = view.findViewById(R.id.seekBarB);
        seekBarW = view.findViewById(R.id.seekBarW);
        //region 处理viewpage/SwipeRefreshLayout滑动冲突事件
        View.OnTouchListener seekBarTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                view.getParent().requestDisallowInterceptTouchEvent(true);
                if (event.getAction() == MotionEvent.ACTION_UP) mSwipeLayout.setEnabled(true);
                else mSwipeLayout.setEnabled(false);
                return false;
            }
        };

        seekBarR.setOnTouchListener(seekBarTouchListener);
        seekBarG.setOnTouchListener(seekBarTouchListener);
        seekBarB.setOnTouchListener(seekBarTouchListener);
        seekBarW.setOnTouchListener(seekBarTouchListener);
        //endregion

        //region SeekBar拖动事件函数
        SeekBar.OnSeekBarChangeListener seekBarSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(Tag, "fromUser:" + fromUser);
                textViewR.setText(String.format(Locale.getDefault(), "R:%03d", seekBarR.getProgress()));
                textViewG.setText(String.format(Locale.getDefault(), "G:%03d", seekBarG.getProgress()));
                textViewB.setText(String.format(Locale.getDefault(), "B:%03d", seekBarB.getProgress()));
                textViewW.setText(String.format(Locale.getDefault(), "W:%03d", seekBarW.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Message msg = new Message();
                msg.obj = "[" + seekBarR.getProgress() + "," + seekBarG.getProgress() + "," +
                        seekBarB.getProgress() + "," + seekBarW.getProgress() + "]";
                msg.what = 2;
                handler.sendMessageDelayed(msg, 1);
            }
        };
        seekBarR.setOnSeekBarChangeListener(seekBarSeekBarChangeListener);
        seekBarG.setOnSeekBarChangeListener(seekBarSeekBarChangeListener);
        seekBarB.setOnSeekBarChangeListener(seekBarSeekBarChangeListener);
        seekBarW.setOnSeekBarChangeListener(seekBarSeekBarChangeListener);
        //endregion
        //endregion
        //region 颜色/白光选择
        ll_favorite = view.findViewById(R.id.ll_favorite);
        img_color = view.findViewById(R.id.img_color);
        img_white = view.findViewById(R.id.img_white);
        tablayout = view.findViewById(R.id.tablayout);
        tablayout.addTab(tablayout.newTab().setText("彩色"));
        tablayout.addTab(tablayout.newTab().setText("白光"));
        tablayout.addTab(tablayout.newTab().setText("收藏夹"));
        tablayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                color_set.setVisibility(View.INVISIBLE);
                img_color.setVisibility(View.INVISIBLE);
                img_white.setVisibility(View.INVISIBLE);
                ll_favorite.setVisibility(View.INVISIBLE);
                switch (tab.getPosition()) {
                    case 0:
                        img_color.setVisibility(View.VISIBLE);
                        color_set.setCardBackgroundColor(0xffffffff);
                        break;
                    case 1:
                        img_white.setVisibility(View.VISIBLE);
                        color_set.setCardBackgroundColor(0xffFF4081);
                        break;
                    case 2:
                        ll_favorite.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        img_color.post(new Runnable() {
            @Override
            public void run() {
                img_color.setDrawingCacheEnabled(true);
                bitmaps[0] = Bitmap.createBitmap(img_color.getDrawingCache());
                img_color.setDrawingCacheEnabled(false);
            }
        });
        img_white.post(new Runnable() {
            @Override
            public void run() {
                img_white.setDrawingCacheEnabled(true);
                bitmaps[1] = Bitmap.createBitmap(img_white.getDrawingCache());
                img_white.setDrawingCacheEnabled(false);
            }
        });
        img_color.setTag(0);
        img_white.setTag(1);
        img_color.setOnTouchListener(InamgeViewListener);
        img_white.setOnTouchListener(InamgeViewListener);


        //endregion
        //region 开关按钮
        btn_open = view.findViewById(R.id.btn_open);
        btn_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Send("{\"mac\":\"" + device.getMac() + "\",\"on\":1,\"gradient\":" + (chkGradient.isChecked() ? "1" : "0") + "}");
            }
        });
        btn_close = view.findViewById(R.id.btn_close);
        btn_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Send("{\"mac\":\"" + device.getMac() + "\",\"on\":0,\"gradient\":" + (chkGradient.isChecked() ? "1" : "0") + "}");
            }
        });
        //endregion
        chkGradient = view.findViewById(R.id.chkGradient);
        //region SwipeLayout更新当前状态
        mSwipeLayout = view.findViewById(R.id.swipeRefreshLayout);
        mSwipeLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimaryDark, R.color.colorAccent, R.color.colorPrimary);
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                handler.sendEmptyMessageDelayed(1, 0);
                mSwipeLayout.setRefreshing(false);
            }
        });
        //endregion

        //region log 相关
        setLogTextView((TextView) view.findViewById(R.id.tv_log));
        //endregion

        //endregion
        super.onCreateView(inflater, container, savedInstanceState);
        return view;
    }


    //region 按钮事件
    //ImageView触摸事件
    private View.OnTouchListener InamgeViewListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View arg0, MotionEvent arg1) {
            int id = (int) arg0.getTag();
            ImageView imageView = (ImageView) arg0;

            if (arg1.getAction() == MotionEvent.ACTION_DOWN) {
                mSwipeLayout.setEnabled(false);
            } else if (arg1.getAction() == MotionEvent.ACTION_UP
                    || arg1.getAction() == MotionEvent.ACTION_CANCEL) {
                mSwipeLayout.setEnabled(true);
            }
            if (bitmaps[id] == null) return true;

            imageView.getParent().getParent().getParent().getParent().requestDisallowInterceptTouchEvent(true);
            imageView.getParent().getParent().getParent().requestDisallowInterceptTouchEvent(true);
            //imageView.getParent().getParent().requestDisallowInterceptTouchEvent(true);
            //imageView.getParent().requestDisallowInterceptTouchEvent(true);

            int x = (int) arg1.getX();
            int y = (int) arg1.getY();
            if (x < 0 || y < 0 || y > bitmaps[id].getHeight() || x > bitmaps[id].getWidth()) {

                Log.d(Tag, "[" + x + "," + y + "]   " + "[" + bitmaps[id].getWidth() + "," + bitmaps[id].getHeight() + "]");
                return true;
            }

            color_set.setX(x + imageView.getX() - color_set.getWidth() / 2);
            color_set.setY(y + imageView.getY() - color_set.getHeight() / 2);
            color_set.setVisibility(View.VISIBLE);
            try {
                int pixel = bitmaps[id].getPixel(x, y);//获取颜色
                int redValue = Color.red(pixel);
                int greenValue = Color.green(pixel);
                int blueValue = Color.blue(pixel);
                Log.d(Tag, "[" + x + "," + y + "]" + redValue + "," + greenValue + "," + blueValue);
                ((CardView) color_set.getChildAt(0)).setCardBackgroundColor(pixel);
//                if (redValue != 255 && greenValue != 255 && blueValue != 255)
//                    return true;
//                if (pixel == 0 || (redValue == 0 && greenValue == 0 && blueValue == 0))
//                    return true; //仅判断pixel会偶尔无法跳出w

                if (id == 0) {
                    seekBarR.setProgress(redValue);
                    seekBarG.setProgress(greenValue);
                    seekBarB.setProgress(blueValue);
                    seekBarW.setProgress(0);
                } else {
                    seekBarR.setProgress(0);
                    seekBarG.setProgress(0);
                    seekBarB.setProgress(0);
                    seekBarW.setProgress(redValue);
                }

//                HSL = RGBtoHSL(redValue, greenValue, blueValue);
                // Sflag = true;//sendRGB();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return true;

        }

    };
    //endregion


    void Send(String message) {
        boolean b = getActivity().getSharedPreferences("Setting_" + device.getMac(), 0).getBoolean("always_UDP", false);
        super.Send(b, device.getSendMqttTopic(), message);
    }

    //数据接收处理更新函数
    public void Receive(String ip, int port, String topic, String message) {
        super.Receive(ip, port, topic, message);

        //region 接收availability数据,非Json,单独处理
        if (topic != null && topic.endsWith("availability")) {
            String regexp = "device/(.*?)/([0123456789abcdef]{12})/(.*)";
            Pattern pattern = Pattern.compile(regexp);
            Matcher matcher = pattern.matcher(topic);
            if (matcher.find() && matcher.groupCount() == 3) {
                String device_mac = matcher.group(2);
                if (device_mac.equals(device.getMac())) {
                    device.setOnline(message.equals("1"));
                    Log(device.isOnline() ? "设备在线" : "设备离线" + "(请确认设备是否有连接mqtt服务器)");
                }
                return;
            }
        }
        //endregion

        try {
            JSONObject jsonObject = new JSONObject(message);
            if (!jsonObject.has("mac") || !jsonObject.getString("mac").equals(device.getMac())) {
                return;
            }
            if (jsonObject.has("name")) device.setName(jsonObject.getString("name"));

            if (jsonObject.has("rgb")) {
                JSONArray rgb = jsonObject.getJSONArray("rgb");
                if (rgb.length() == 4) {
                    seekBarR.setProgress(rgb.getInt(0));
                    seekBarG.setProgress(rgb.getInt(1));
                    seekBarB.setProgress(rgb.getInt(2));
                    seekBarW.setProgress(rgb.getInt(3));

                    int color = Color.argb(255, rgb.getInt(0), rgb.getInt(1), rgb.getInt(2));
                    seekBarR.getThumb().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                    //seekBarR.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                    color_now.setCardBackgroundColor(color);
                }

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //region 事件监听调用函数,主要为在子类中重写此函数实现在service建立成功/mqtt连接成功/失败时执行功能
    //Service建立成功时调用    此函数需要时在子类中重写
    public void ServiceConnected() {
        handler.sendEmptyMessageDelayed(1, 300);
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
