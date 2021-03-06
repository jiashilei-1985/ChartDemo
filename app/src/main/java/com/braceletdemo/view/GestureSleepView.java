package com.braceletdemo.view;

import java.util.Random;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.braceletdemo.util.DensityUtil;

/**
 * @author gaopengfei
 */
public class GestureSleepView extends View {

    /** 画文字、画睡眠质量、画Touch时竖线的画笔 */
    private Paint mTxtPaint, mSleepQualityPaint, mTouchEffectPaint;
    /** 当前view的宽高尺寸(像素) */
    private int mViewWidth, mViewHeight;
    /** 每分钟所占宽度 */
    private float mUnitWidth;
    /** 每小时所占宽度(左右两边预留10像素间距) */
    private float mUnitTimeWidth;
    /** 下方时间轴的预设高度 */
    private int mTimelineHeight = 30;
    /** 总的分钟数(默认12个小时) */
    private int mUnitCount = 60 * 12;
    /** 起始时间,小时为单位,外后推延12个小时 */
    private int mStartTime = 21;
    /** 每分钟的睡眠质量数据 */
    private int mData[];
    /** 加盐,防止数据绘制时溢出,具体盐分要看具体数据 */
    float mSalt;

    /** 睡眠质量touch时的回调 */
    private OnSleepQualityTouchListener mSleepQualityTouchListener;
    /** touch的X轴坐标 */
    private float mTouchX = -5;

    /** 缓冲 */
    private Canvas mCacheCanvas;
    private Bitmap mCacheBitmap;

    public GestureSleepView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // 初始化文字画笔,宽度2,打开抗锯齿,画白色
        mTxtPaint = new Paint();
        mTxtPaint.setStrokeWidth(2);
        mTxtPaint.setAntiAlias(true);
        mTxtPaint.setColor(Color.WHITE);

        // 初始化睡眠质量画笔,开启抗锯齿,画#80E1C2
        mSleepQualityPaint = new Paint();
        mSleepQualityPaint.setAntiAlias(true);
        mSleepQualityPaint.setColor(Color.parseColor("#80E1C2"));

        // 初始化onTouch时竖线的画笔
        mTouchEffectPaint = new Paint();
        mTouchEffectPaint.setAntiAlias(true);
        mTouchEffectPaint.setColor(Color.parseColor("#FF9000"));
        mTouchEffectPaint.setStrokeWidth(2.0f);

        mSalt = DensityUtil.dip2px(getContext(), 75.0f / 200.0f);

        mCacheCanvas = new Canvas();
    }

    {
        mData = new int[mUnitCount];
        updata();
    }

    private void updata() {
        // 模拟数据,到时候这里替换成网络请求就可以了
        Random r = new Random();
        for (int i = 0; i < mData.length; i++) {
            mData[i] = r.nextInt(200);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            this.mViewWidth = getWidth();
            this.mViewHeight = getHeight();
            this.mUnitTimeWidth = mViewWidth / 13.0f;
            this.mUnitWidth = this.mViewWidth / (float) mData.length;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mCacheBitmap == null) {// 没缓冲过,增加一份缓冲,以后直接画缓冲中的bitmap再加上touch效果就行了
            mCacheBitmap = Bitmap.createBitmap(mViewWidth, mViewHeight, Config.ARGB_8888);
            mCacheCanvas.setBitmap(mCacheBitmap);
            drawTimeline(mCacheCanvas);
            drawSleepQuality(mCacheCanvas);
        }
        canvas.drawBitmap(mCacheBitmap, 0, 0, mTxtPaint);
        drawTouchEffect(canvas);
    }

    /** 字体大小22,时间12个小时 */
    private void drawTimeline(Canvas canvas) {
        canvas.drawLine(0, mViewHeight - mTimelineHeight, mViewWidth, mViewHeight - mTimelineHeight, mTxtPaint);
        mTxtPaint.setTextSize(22);
        for (int i = 0; i < 13; i++) {
            String time;
            if (mStartTime % 24 < 10) {
                time = "0" + mStartTime++ % 24;
            } else {
                time = " " + mStartTime++ % 24;
            }
            canvas.drawText(time, 10 + i * mUnitTimeWidth, mViewHeight - 5, mTxtPaint);
        }
        mStartTime = 21;// 还原时间
    }

    /** 绘制睡眠质量 */
    private void drawSleepQuality(Canvas canvas) {

        for (int i = 0; i < mData.length; i++) {
            canvas.drawRect(i * mUnitWidth, mViewHeight - mTimelineHeight - mData[i] * mSalt, i * mUnitWidth + mUnitWidth, mViewHeight - mTimelineHeight,
                    mSleepQualityPaint);
        }
    }

    /** 绘制touch竖线与下方小球 */
    private void drawTouchEffect(Canvas canvas) {
        canvas.drawLine(mTouchX, 0, mTouchX, mViewHeight - mTimelineHeight, mTouchEffectPaint);
        canvas.drawCircle(mTouchX, mViewHeight - mTimelineHeight, 5, mTouchEffectPaint);
        // 找到touch坐标对应的数据回调显示
        if (mSleepQualityTouchListener != null) {
            int position = (int) (mTouchX / mUnitWidth);// 对应的数据坐标
            if (position >= 0 && position < mData.length) {
                int data = mData[position];
                mSleepQualityTouchListener.OnSleepQualityTouch(data);
            }
        }
    }

    /** 这个onTouchEvent是显示竖线右上角显示对应数据的效果 */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.mTouchX = event.getX();
        System.out.println("touch" + mTouchX);
        invalidate();
        return true;
    }

    public interface OnSleepQualityTouchListener {
        public void OnSleepQualityTouch(int data);
    }

    /** touch时回调方法 */
    public void SetOnSleepQualityTouchListener(OnSleepQualityTouchListener listener) {
        this.mSleepQualityTouchListener = listener;
    }
}
