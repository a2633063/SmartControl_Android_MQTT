package com.zyc.zcontrol.deviceItem.rgbw;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
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

    TextView tv_tip;
    TextView tv_task;
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


    ImageView img_favorite;
    ImageView img_color;
    ImageView img_white;

    Bitmap[] bitmaps = new Bitmap[3];

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

    Canvas canvas;
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
                    // handler.removeMessages(2);
                    Send("{\"mac\":\"" + device.getMac() + "\",\"rgb\":" + msg.obj + ",\"gradient\":" + (chkGradient.isChecked() ? "1" : "0") + "}");
                    break;
                case 3: //取消显示X时间后自动关闭文本
                    tv_tip.setText("");
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

        tv_tip = view.findViewById(R.id.tv_tip);
        tv_task = view.findViewById(R.id.tv_task);
        tv_task.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), RGBWTaskActivity.class);
                intent.putExtra("mac", device.getMac());
                startActivity(intent);
            }
        });
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
                handler.sendMessageDelayed(msg, 10);
            }
        };
        seekBarR.setOnSeekBarChangeListener(seekBarSeekBarChangeListener);
        seekBarG.setOnSeekBarChangeListener(seekBarSeekBarChangeListener);
        seekBarB.setOnSeekBarChangeListener(seekBarSeekBarChangeListener);
        seekBarW.setOnSeekBarChangeListener(seekBarSeekBarChangeListener);
        //endregion
        //endregion
        //region 颜色/白光选择
        img_favorite = view.findViewById(R.id.img_favorite);
        img_color = view.findViewById(R.id.img_color);
        img_white = view.findViewById(R.id.img_white);
        tablayout = view.findViewById(R.id.tablayout);
        tablayout.addTab(tablayout.newTab().setText("彩色"));
        tablayout.addTab(tablayout.newTab().setText("白光"));
        tablayout.addTab(tablayout.newTab().setText("收藏夹"));
        tablayout.setSelected(false);
        tablayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (color_set.getTag() == null || ((int) color_set.getTag()) != tab.getPosition())
                    color_set.setVisibility(View.INVISIBLE);
                else
                    color_set.setVisibility(View.VISIBLE);
                img_color.setVisibility(View.INVISIBLE);
                img_white.setVisibility(View.INVISIBLE);
                img_favorite.setVisibility(View.INVISIBLE);
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
                        img_favorite.setVisibility(View.VISIBLE);
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

        tablayout.setSelected(true);

        tablayout.getTabAt(1).select();
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
        img_favorite.post(new Runnable() {
            @Override
            public void run() {
                img_favorite.setDrawingCacheEnabled(true);
                int h = img_favorite.getMeasuredHeight();
                int w = img_favorite.getMeasuredWidth();
                bitmaps[2] = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                canvas = new Canvas(bitmaps[2]);
                canvas.drawColor(0x00000000);
                Paint paint = new Paint();

                int i;
                int[] color_list = {0xffff0000, 0xff00ff00, 0xff0000ff, 0xffff0000, 0xff00ff00, 0xff0000ff, 0xff00ff00, 0xff0000ff, 0xffff0000, 0xff00ff00, 0xffff0000};
                for (i = 0; i < color_list.length; i++) {

                    paint.setColor(color_list[i]);
                    int x = i % 6 * h / 2;
                    int y = i / 6 * h / 2;
                    canvas.drawRect(x, y, x + (h >> 1), y + (h >> 1), paint);
                }

                int x = i % 6 * h / 2;
                int y = i / 6 * h / 2;
                Drawable add = getResources().getDrawable(R.drawable.ic_baseline_add_24);
                add.setBounds(x, y, x + (h >> 1), y + (h >> 1));
                add.draw(canvas);

                //canvas.drawBitmap(b, x + (h - b.getWidth()) / 2, y + (h - b.getHeight()) / 2, paint);

                img_favorite.setImageBitmap(bitmaps[2]);

                img_favorite.setDrawingCacheEnabled(false);
                img_favorite.invalidate();
            }
        });
        img_color.setTag(0);
        img_white.setTag(1);
        img_favorite.setTag(2);
        img_color.setOnTouchListener(ImageViewListener);
        img_white.setOnTouchListener(ImageViewListener);
        img_favorite.setOnTouchListener(ImageViewListener);


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
    private View.OnTouchListener ImageViewListener = new View.OnTouchListener() {

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

            int x = (int) arg1.getX();
            int y = (int) arg1.getY();
            if (x < 0 || y < 0 || y > bitmaps[id].getHeight() || x > bitmaps[id].getWidth()) {

                Log.d(Tag, "[" + x + "," + y + "]   " + "[" + bitmaps[id].getWidth() + "," + bitmaps[id].getHeight() + "]");
                return true;
            }

            try {
                int pixel = bitmaps[id].getPixel(x, y);//获取颜色
                int redValue = Color.red(pixel);
                int greenValue = Color.green(pixel);
                int blueValue = Color.blue(pixel);
                if (Color.alpha(pixel) == 0) return true;
                Log.d(Tag, "[" + x + "," + y + "]" + Color.alpha(pixel) + "," + redValue + "," + greenValue + "," + blueValue);


                color_set.setX(x + imageView.getX() - color_set.getWidth() / 2);
                color_set.setY(y + imageView.getY() - color_set.getHeight() / 2);
                color_set.setVisibility(View.VISIBLE);
                color_set.setTag(id);

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
                } else if (id == 1) {
                    seekBarR.setProgress(0);
                    seekBarG.setProgress(0);
                    seekBarB.setProgress(0);
                    seekBarW.setProgress(redValue);
                } else if (id == 2) {
                    if (redValue == greenValue && redValue == blueValue) {
                        seekBarR.setProgress(0);
                        seekBarG.setProgress(0);
                        seekBarB.setProgress(0);
                        seekBarW.setProgress(redValue);
                    } else {
                        seekBarR.setProgress(redValue);
                        seekBarG.setProgress(greenValue);
                        seekBarB.setProgress(blueValue);
                        seekBarW.setProgress(0);
                    }
                }

                Message msg = new Message();
                msg.obj = "[" + seekBarR.getProgress() + "," + seekBarG.getProgress() + "," +
                        seekBarB.getProgress() + "," + seekBarW.getProgress() + "]";
                msg.what = 2;
                handler.removeMessages(2);
                handler.sendMessageDelayed(msg, 100);

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

            //region 获取颜色
            if (jsonObject.has("rgb")) {
                JSONArray rgb = jsonObject.getJSONArray("rgb");
                if (rgb.length() == 4) {
                    seekBarR.setProgress(rgb.getInt(0));
                    seekBarG.setProgress(rgb.getInt(1));
                    seekBarB.setProgress(rgb.getInt(2));
                    seekBarW.setProgress(rgb.getInt(3));

                    int color = Color.argb(255, rgb.getInt(0), rgb.getInt(1), rgb.getInt(2));
                    seekBarR.getThumb().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                    seekBarG.getThumb().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                    seekBarB.getThumb().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                    seekBarW.getThumb().setColorFilter(Color.rgb(rgb.getInt(3), rgb.getInt(3), rgb.getInt(3)), PorterDuff.Mode.SRC_ATOP);
                    //seekBarR.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                    color_now.setCardBackgroundColor(color);
                }
            }
            //endregion
            //region 获取自动关闭时间
            if (jsonObject.has("auto_off_time")) {
                int auto_off_time = jsonObject.optInt("auto_off_time", 0);
                String str = "";
                if (auto_off_time > 0) {
                    if (auto_off_time >= 60) {
                        str += auto_off_time / 60 + "分";
                        if (auto_off_time % 60 == 0) str += "钟";
                    }
                    if (auto_off_time % 60 != 0)
                        str += auto_off_time % 60 + "秒";
                    str += "后自动关灯";
                }
                handler.removeMessages(3);
                handler.sendEmptyMessageDelayed(3, 2500);
                tv_tip.setText(str);


            }
            //endregion

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
