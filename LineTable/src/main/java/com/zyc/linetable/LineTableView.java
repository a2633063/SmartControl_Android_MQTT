package com.zyc.linetable;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class LineTableView extends View {
    public static final String Tag = "CalendarView_Tag";


    public static final float TEXT_SPACE = 50f;


    private List<WeightHistoryData> mDataList = null;


    private Paint mPaint;
    private Paint mPaintDot;

    private static final float DOT_SIZE = 5;
    private int signShow = 1;
    public static final int SIGN = 0;
    public static final int DOT = 1;
    private float buttonSpace = 100;

    private float buttonHeight = 100;
    private float titleHeight = 120;
    private float titleSize = 80;
    private float textSize = 40;
    private float titleLong;

    private float linePaddingTop;
    private float linePaddingBottom;
    private float ZerolinePaddingBottom;
    private int weightTextColor;
    private float weightTextSize = 40;

    private int timeTextColor;
    private float timeTextSize = 40;

    private int textColor = 0xffa0a0a0;
    private int titleColor = 0xffa0a0a0;

    private int mViewHeight;
    private int mViewWidth;


    float AxisYspace;
    private float[] AxisX = new float[7];
    private float[] AxisY = new float[7];

    /**
     * 滑动的距离
     */
    private float mMoveLen = 0;
    private boolean isInit = false;

    Context context;


    public LineTableView(Context context) {
        this(context, null);
    }

    public LineTableView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LineTableView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        TypedArray mTypedArray = context.obtainStyledAttributes(attrs,
                R.styleable.MyLineTableView);

        //获取自定义属性和默认值
        linePaddingTop = mTypedArray.getDimension(R.styleable.MyLineTableView_linePaddingTop, 80);
        linePaddingBottom = mTypedArray.getDimension(R.styleable.MyLineTableView_linePaddingBottom, 50);
        ZerolinePaddingBottom = mTypedArray.getDimension(R.styleable.MyLineTableView_zeroLinePaddingBottom, 50);
        weightTextSize = mTypedArray.getDimension(R.styleable.MyLineTableView_weightTextSize, 30);
        weightTextColor = mTypedArray.getColor(R.styleable.MyLineTableView_weightTextColor, 0xffa0a0a0);
        timeTextSize = mTypedArray.getDimension(R.styleable.MyLineTableView_timeTextSize, 20);
        timeTextColor = mTypedArray.getColor(R.styleable.MyLineTableView_timeTextColor, 0xffa0a0a0);

//        signShow = mTypedArray.getInt(R.styleable.MyLineTableView_signShow, SIGN);
        if (titleHeight < titleSize) titleHeight = titleSize + 10;
        mTypedArray.recycle();
        init();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

//        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
//        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);


        // 处理高为 wrap_content 的情况
        if (heightSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(widthSpecSize, (int) (AxisY[6] + AxisY[6] - AxisY[5]));
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
        mViewHeight = getMeasuredHeight();
        mViewWidth = getMeasuredWidth();
        isInit = true;
        invalidate();
    }

    private void init() {
        if (mDataList == null)
            mDataList = new ArrayList<WeightHistoryData>();
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setColor(textColor);
        mPaint.setTextSize(textSize);

        mPaintDot = new Paint(mPaint);
        mPaintDot.setColor(0xffB27F46);

        //region 计算横坐标AxisY
        float y = titleHeight + textSize;
        Paint.FontMetricsInt fmi = mPaint.getFontMetricsInt();

        float baseline = (float) (y - (fmi.bottom / 2.0 + fmi.top / 2.0));
        AxisYspace = (float) (fmi.bottom / 2.0 + fmi.top / 2.0);
        AxisY[0] = baseline;
        for (int i = 1; i < AxisY.length; i++) {
            AxisY[i] = AxisY[i - 1] + TEXT_SPACE + textSize;
        }

        //endregion

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 根据index绘制view
        if (isInit)
            drawData(canvas);
    }

    final static String[] week = {
            "日", "一", "二", "三", "四", "五", "六"
    };

    private void drawData(Canvas canvas) {
        int i, j;
        float pad = getMeasuredWidth() / 11;
        int maxWeight = 0;
        int minWeight = 50000;
        float ZerolinePaddingHeight = getMeasuredHeight() - ZerolinePaddingBottom;
        float lineHeight = ZerolinePaddingHeight - linePaddingTop - linePaddingBottom;
        float[] coordinate = new float[10];

        //计算最大最小值
        for (i = 0; i < mDataList.size(); i++) {
            if (maxWeight < mDataList.get(i).weight) maxWeight = mDataList.get(i).weight;
            if (minWeight > mDataList.get(i).weight) minWeight = mDataList.get(i).weight;
        }

        //计算各点纵坐标
        for (i = 0; i < 10; i++) {
            coordinate[i] = ZerolinePaddingHeight;
        }

        if (maxWeight == minWeight) {
            for (i = 10 - mDataList.size(), j = 0; i < 10; i++, j++) {
                coordinate[i] = ZerolinePaddingHeight - linePaddingBottom -  lineHeight / 2;
            }
        } else {
            for (i = 10 - mDataList.size(), j = 0; i < 10; i++, j++) {
                coordinate[i] = ZerolinePaddingHeight - linePaddingBottom - (float) (mDataList.get(j).weight - minWeight) * lineHeight / (maxWeight - minWeight);
            }
        }


        //region 画分隔线
        mPaint.setColor(0xffe0e0e0);
        canvas.drawRect(0, ZerolinePaddingHeight, mViewWidth, ZerolinePaddingHeight + 5, mPaint);// 长方形
        //endregion


        //region 画折线
        mPaint.setStrokeWidth(5);
        mPaint.setColor(0xffff0000);
        Path path = new Path();
        path.moveTo(pad, coordinate[0]);// 此点为多边形的起点
        for (i = 1; i < 10; i++) {
            path.lineTo(pad * i + pad, coordinate[i]);
        }
        // path.close(); // 使这些点构成封闭的多边形
        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(path, mPaint);
        //endregion

        //region 画点
        mPaint.setStrokeWidth(20);
        mPaint.setColor(0xffff0000);
        for (i = 0; i < 10; i++)
            canvas.drawCircle(pad * i + pad, coordinate[i], 1, mPaint);
        //endregion


        //region 画数值
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextSize(weightTextSize);
        mPaint.setColor(weightTextColor);
        for (i = 10 - mDataList.size(), j = 0; i < 10; i++, j++) {
            canvas.drawText(mDataList.get(j).weight / 100 + "." + mDataList.get(j).weight % 100,
                    pad * i + pad, coordinate[i] - 20, mPaint);
        }

        //endregion
        //region 画日期
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextSize(timeTextSize);
        mPaint.setColor(timeTextColor);
        for (i = 10 - mDataList.size(), j = 0; i < 10; i++, j++) {
            canvas.drawText(mDataList.get(j).getTimeString("MM/dd"),
                    pad * i + pad, ZerolinePaddingHeight + ZerolinePaddingBottom / 3 * 2, mPaint);
        }

        //endregion


    }

    public List<WeightHistoryData> getWeightList() {
        return mDataList;
    }


}

