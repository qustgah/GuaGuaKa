package com.example.zhaimeng.imooc_guaguaka.com.example.zhaimeng.imooc_guaguaka.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.example.zhaimeng.imooc_guaguaka.R;

/**
 * Created by Administrator on 2017/12/1.
 */

public class GuaGuaCardView extends View {

    private int CLICK_ACTION_THRESHOLD = 200;
    private Paint mOutterPaint;
    private Path mPath;//手指划屏幕的路径
    private Canvas mCanvas;
    private Bitmap mBitmap;//使用mOutterPaint在mBitmap上绘制
    private int mLastX;
    private int mLastY;
    private Bitmap mOutterBitmap;
    
    private String mText;
    private int mTextSize;
    private int mTextColor;
    private int pathWidth;
    private Drawable coverDrawable;
    private Bitmap backGround;
    private Paint mBackPaint;//绘制“谢谢参与”的画笔
    private Rect mTextBound;//“谢谢参与”的矩形范围

    private volatile boolean mComplete = false;//判断擦除的比例是否达到60%

    public GuaGuaCardView(Context context) {
        this(context, null);
    }

    public GuaGuaCardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GuaGuaCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //获得自定义属性
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GuaGuaCard, defStyleAttr, 0);
        mText = a.getString(R.styleable.GuaGuaCard_cardText);
        mTextColor = a.getColor(R.styleable.GuaGuaCard_cardTextColor, 0x000000);
        mTextSize = a.getDimensionPixelSize(R.styleable.GuaGuaCard_cardTextSize, 22);
        coverDrawable = a.getDrawable(R.styleable.GuaGuaCard_cardCoverDrawable);
        pathWidth = a.getDimensionPixelSize(R.styleable.GuaGuaCard_cardPathWidth, 30);
        a.recycle();
        init();
    }

    private void init() {
        mOutterPaint = new Paint();
        mPath = new Path();
        mTextBound = new Rect();
        mBackPaint = new Paint();
        mOutterBitmap = drawableToBitmap(coverDrawable);
    }

    public void setBackGround(Bitmap backGround) {
        this.backGround = backGround;
        if (this.backGround != null) {
            invalidate();
        }
    }

    public void setNoneGuaGua(boolean isComplete) {
        mComplete = isComplete;
        postInvalidate();
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        //初始化bitmap
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        
        setupOutPaint();//设置“橡皮擦”画笔的属性
        setupBackPaint();//设置绘制“谢谢参与”的画笔属性
        //画覆盖层的bitmap，是圆角矩形
        if (mOutterBitmap != null) {
            mCanvas.drawRect(new RectF(0, 0, width, height), mOutterPaint);
            mCanvas.drawBitmap(mOutterBitmap, null, new Rect(0, 0, width, height), null);    
        }
    }

    /**
     * 设置绘制“谢谢参与”的画笔属性
     */
    private void setupBackPaint() {
        if (mText != null) {
            mBackPaint.setColor(mTextColor);
            mBackPaint.setStyle(Paint.Style.FILL);
            mBackPaint.setTextSize(mTextSize);
            //获得画笔绘制文本的宽和高（矩形范围）
            mBackPaint.getTextBounds(mText, 0, mText.length(), mTextBound);    
        }
    }
    
    /**
     * 设置 橡皮擦 画笔的属性
     */
    private void setupOutPaint() {
        mOutterPaint.setColor(Color.parseColor("#ffffff"));
        mOutterPaint.setAntiAlias(true);
        mOutterPaint.setDither(true);
        mOutterPaint.setStrokeJoin(Paint.Join.ROUND);
        mOutterPaint.setStrokeCap(Paint.Cap.ROUND);
        mOutterPaint.setStyle(Paint.Style.FILL);
        mOutterPaint.setStrokeWidth(pathWidth);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();
        if (!mComplete) {
            getParent().requestDisallowInterceptTouchEvent(true);
            //绘制path
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mLastX = x;
                    mLastY = y;
                    mPath.moveTo(mLastX, mLastY);
                    break;
                case MotionEvent.ACTION_UP:
                    new Thread(mRunnable).start();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int dx = Math.abs(x - mLastX);//用户滑动的距离
                    int dy = Math.abs(y - mLastY);
                    if (dx > 3 || dy > 3) {
                        mPath.lineTo(x, y);
                    }
                    mLastX = x;
                    mLastY = y;
                    break;
            }
            invalidate();//执行此方法会调用onDraw方法绘制
            return true;
        }else {
            getParent().requestDisallowInterceptTouchEvent(false);
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mLastX = x;
                    mLastY = y;
                    break;
                case MotionEvent.ACTION_UP:
                    float endX = event.getX();
                    float endY = event.getY();
                    if (isAClick(mLastX, endX, mLastY, endY)) {
                        if (mComplete && mListener!= null) {
                            mListener.onComplete();
                        }
                    }
                    break;
            }
            return true;
        }
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            int width = getWidth();//拿到控件宽、高
            int height = getHeight();
            float wipeArea = 0;//已经擦除的比例
            float totalArea = width * height;
            Bitmap bitmap = mBitmap;
            int[] mPixels = new int[width * height];
            //获得bitmap的所有像素信息保存在mPixels中
            mBitmap.getPixels(mPixels, 0, width, 0, 0, width, height);
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    int index = j * width + i;
                    if (mPixels[index] == 0) {
                        wipeArea++;
                    }
                }
            }
            if (wipeArea > 0 && totalArea > 0) {
                int percent = (int) (wipeArea * 100 / totalArea);
                Log.i("cool", percent + "");
                if (percent > 40) {
                    //清楚掉覆盖图层区域
                    mComplete = true;
                    postInvalidate();
                }
            }
        }
    };

    @Override
    protected void onDraw(Canvas canvas) {
        if (backGround != null) {
            int width = getMeasuredWidth();
            int height = getMeasuredHeight();
            canvas.drawBitmap(backGround,null,new Rect(0, 0, width, height),null);
        } else if (mText != null) {
            canvas.drawText(mText, getWidth() / 2 - mTextBound.width() / 2, getHeight() / 2 + mTextBound.height() / 2, mBackPaint);//绘制“谢谢参与”的文本
        }
        if (!mComplete) {
            drawPath();
            canvas.drawBitmap(mBitmap, 0, 0, null);//使bitmap显示到屏幕上，在内存中准备好bitmap，然后在屏幕上绘制出来
        }
    }

    private void drawPath() {
        mOutterPaint.setStyle(Paint.Style.STROKE);
        mOutterPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        mCanvas.drawPath(mPath, mOutterPaint);
    }

    public interface OnGuaGuaKaCompleteListener {
        void onComplete();
    }

    private OnGuaGuaKaCompleteListener mListener;

    public void setOnGuaGuaKaCompleteListener(OnGuaGuaKaCompleteListener mListener) {
        this.mListener = mListener;
    }
    
    

    private boolean isAClick(float startX, float endX, float startY, float endY) {
        float differenceX = Math.abs(startX - endX);
        float differenceY = Math.abs(startY - endY);
        return !(differenceX > CLICK_ACTION_THRESHOLD/* =5 */ || differenceY > CLICK_ACTION_THRESHOLD);
    }

    public void setText(String text){
        this.mText = text;
        //获得画笔绘制文本的宽和高（矩形范围）
        mBackPaint.getTextBounds(mText, 0, mText.length(), mTextBound);
    }

    public Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }
        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
