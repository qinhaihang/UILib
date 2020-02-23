package com.example.widgetlib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

/**
 * @author qinhaihang
 * @time 2020/2/21 16:59
 * @des
 * @packgename com.example.widgetlib
 */
public class TransformView2 extends RelativeLayout {

    private static final String TAG = TransformView2.class.getSimpleName();

    private PaintFlagsDrawFilter mDrawFilter =
            new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    private Context mContext;

    private Paint mPaint;

    private TextureView mTextureView;
    private Canvas mTextureViewCanvas;
    private Rect mSegmentRect;

    private CanvasCallback mCanvasCallback;

    private Bitmap mSegmentBitmap;
    private int mSegmentBitmapWidth;
    private int mSegmentBitmapHeight;
    private int mInitW;
    private int mInitH;

    private Matrix mScaleMatix = new Matrix();
    private float mScaleX;
    private float mScaleY;

    private PointF mMidPoint = new PointF(); //记录中间点
    private PointF mLastPoint1 = new PointF(); // 上次事件的第一个触点
    private PointF mLastPoint2 = new PointF(); // 上次事件的第二个触点
    private PointF mCurrentPoint1 = new PointF(); // 本次事件的第一个触点
    private PointF mCurrentPoint2 = new PointF(); // 本次事件的第二个触点
    private PointF scaleCenter = new PointF();
    private float mScaleFactor = 1.0f; // 当前的缩放倍数
    private boolean mCanScale = false; // 是否可以缩放

    private PointF mCurrentMidPoint = new PointF(); // 当前各触点的中点
    protected PointF mLastMidPoint = new PointF(); // 图片平移时记录上一次ACTION_MOVE的点

    private boolean mCanDrag = false; //是否可以拖动
    private float mOneMoveX;
    private float mOneMoveY;

    private boolean mCanRotate = false; // 判断是否可以旋转
    private PointF mLastVector = new PointF(); // 记录上一次触摸事件两指所表示的向量
    private PointF mCurrentVector = new PointF(); // 记录当前触摸事件两指所表示的向量
    private float mRotationDegree;
    private float mDefaultRotationDegree;
    private float mActionDownX2;
    private float mActionDownY2;
    private float mActionDownX1;
    private float mActionDownY1;

    private OnTounchEventCallback mOnTounchEventCallback;


    public TransformView2(Context context) {
        this(context,null,0);
    }

    public TransformView2(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public TransformView2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        Log.d(TAG,"init");
        mContext = context;

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        mPaint.setDither(true);

        //添加segment之后的图像预览控件
        //addSegmentPreviewView();

    }

    public void addSegmentPreviewView(int w,int h) {

        Log.d(TAG,"addSegmentPreviewView, w = " + w +",h = " + h);

        mInitW = w;
        mInitH = h;

        LayoutParams textureViewParams = new LayoutParams(w, h);
        textureViewParams.addRule(CENTER_IN_PARENT);
        mTextureView = new TextureView(mContext);
        mTextureView.setOpaque(false);
        addView(mTextureView, getChildCount(), textureViewParams);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.d(TAG,"onSurfaceTextureAvailable");
                if(mCanvasCallback != null){
                    mCanvasCallback.onCanvasCreated();
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                Log.d(TAG,"onSurfaceTextureSizeChanged");
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                Log.d(TAG,"onSurfaceTextureDestroyed");
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                Log.d(TAG,"onSurfaceTextureUpdated");
            }
        });
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        Log.i(TAG,"onLayout");
        layoutSegmentView();
    }

    private void layoutSegmentView() {
        if(mSegmentBitmap != null){

            mTextureView.setScaleX(mScaleFactor);
            mTextureView.setScaleY(mScaleFactor);

            float x = mLastMidPoint.x - mSegmentBitmap.getWidth() / 2;
            float y = mLastMidPoint.y - mSegmentBitmap.getHeight() / 2;

            if(x < 0){
                x = 0;
            }

            if(y < 0){
                y = 0;
            }

            mTextureView.setX(x);
            mTextureView.setY(y);

        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        mMidPoint = getMidPointOfFinger(event);

        switch (event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:

                // 每次触摸事件开始都初始化mLastMidPonit
                mLastMidPoint.set(mMidPoint);

                mCanScale = false;

                if (event.getPointerCount() == 2) {
                    // 旋转、平移、缩放分别使用三个判断变量，避免后期某个操作执行条件改变
                    mCanScale = true;
                    mLastPoint1.set(event.getX(0), event.getY(0));
                    mLastPoint2.set(event.getX(1), event.getY(1));
                    mCanRotate = true;
                    mActionDownX2 = event.getX(1);
                    mActionDownY2 = event.getY(1);
                    /*mLastVector.set(event.getX(1) - event.getX(0),
                            event.getY(1) - event.getY(0));*/
                } else if (event.getPointerCount() == 1) {
                    mCanDrag = true;
                    mActionDownX1 = event.getX(0);
                    mActionDownY1 = event.getY(0);
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if (mCanDrag) translate(event);
                if (mCanScale) scale(event);
                if (mCanRotate) rotate(event);

                if(mCanScale || mCanDrag || mCanRotate){
                    if(mOnTounchEventCallback != null){
                        mOnTounchEventCallback.onActionMove();
                    }
                }

                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_POINTER_UP:

                if(mCanRotate){
                    mDefaultRotationDegree = mRotationDegree;
                }

                mCanRotate = false;
                mCanScale = false;
                mCanDrag = false;

                break;
        }

        return true;
    }

    private void rotate(MotionEvent event) {

        float x1 = event.getX(0);
        float y1 = event.getY(0);
        float x2 = event.getX(1);
        float y2 = event.getY(1);

        mRotationDegree = angleBetweenLines(mActionDownX1, mActionDownY1, mActionDownX2, mActionDownY2,
                x1, y1, x2, y2);
        mTextureView.setRotation(mRotationDegree);

        // 计算当前两指触点所表示的向量
        /*mCurrentVector.set(event.getX(1) - event.getX(0),
                event.getY(1) - event.getY(0));
        // 获取旋转角度
        mRotationDegree = getRotateDegree(mLastVector, mCurrentVector);
        mTextureView.setRotation(mRotationDegree);
        Log.d(TAG,"mRotationDegree = " + mRotationDegree);
        mLastVector.set(mCurrentVector);*/
    }

    /**
     * 使用Math#atan2(double y, double x)方法求上次触摸事件两指所示向量与x轴的夹角，
     * 再求出本次触摸事件两指所示向量与x轴夹角，最后求出两角之差即为图片需要转过的角度
     *
     * @param lastVector    上次触摸事件两指间连线所表示的向量
     * @param currentVector 本次触摸事件两指间连线所表示的向量
     * @return 两向量夹角，单位“度”，顺时针旋转时为正数，逆时针旋转时返回负数
     */
    private float getRotateDegree(PointF lastVector, PointF currentVector) {
        //上次触摸事件向量与x轴夹角
        float lastRad = (float)Math.atan2(lastVector.y, lastVector.x);
        //当前触摸事件向量与x轴夹角
        float currentRad = (float) Math.atan2(currentVector.y, currentVector.x);
        // 两向量与x轴夹角之差即为需要旋转的角度
        /*double rad = currentRad - lastRad;
        //“弧度”转“度”
        return (float) Math.toDegrees(rad);*/

        float angle = (( float ) Math.toDegrees(lastRad - currentRad)) % 360;
        if ( angle < -180.f ) angle += 360.0f;
        if ( angle > 180.f ) angle -= 360.0f;
        return -angle;
    }

    /**
     * 计算刚开始触摸的两个点构成的直线和滑动过程中两个点构成直线的角度
     *
     * @param fX  初始点一号x坐标
     * @param fY  初始点一号y坐标
     * @param sX  初始点二号x坐标
     * @param sY  初始点二号y坐标
     * @param nfX 终点一号x坐标
     * @param nfY 终点一号y坐标
     * @param nsX 终点二号x坐标
     * @param nsY 终点二号y坐标
     * @return 构成的角度值
     */
    private float angleBetweenLines(float fX, float fY, float sX, float sY, float nfX, float nfY, float nsX, float nsY) {
        float angle1 = ( float ) Math.atan2((fY - sY), (fX - sX));
        float angle2 = ( float ) Math.atan2((nfY - nsY), (nfX - nsX));

        float angle = (( float ) Math.toDegrees(angle1 - angle2)) % 360;
        if ( angle < -180.f ) angle += 360.0f;
        if ( angle > 180.f ) angle -= 360.0f;
        return -angle;
    }

    private void translate(MotionEvent event) {

        mOneMoveX = event.getX();
        mOneMoveY = event.getY();

        mLastMidPoint.set(mMidPoint);

    }

    /**
     * 计算所有触点的中点
     *
     * @param event 当前触摸事件
     * @return 本次触摸事件所有触点的中点
     */
    private PointF getMidPointOfFinger(MotionEvent event) {
        // 初始化mCurrentMidPoint
        mCurrentMidPoint.set(0f, 0f);
        int pointerCount = event.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            mCurrentMidPoint.x += event.getX(i);
            mCurrentMidPoint.y += event.getY(i);
        }
        mCurrentMidPoint.x /= pointerCount;
        mCurrentMidPoint.y /= pointerCount;
        return mCurrentMidPoint;
    }

    private void scale(MotionEvent event) {
        PointF scaleCenter = getScaleCenter();

        // 初始化当前两指触点
        mCurrentPoint1.set(event.getX(0), event.getY(0));
        mCurrentPoint2.set(event.getX(1), event.getY(1));
        // 计算缩放比例
        float scaleFactor = distance(mCurrentPoint1, mCurrentPoint2)
                / distance(mLastPoint1, mLastPoint2);

        // 更新当前图片的缩放比例
        mScaleFactor *= scaleFactor;

        mScaleMatix.postScale(scaleFactor, scaleFactor,
                scaleCenter.x, scaleCenter.y);
        mLastPoint1.set(mCurrentPoint1);
        mLastPoint2.set(mCurrentPoint2);
    }

    /**
     * 获取两点间距离
     */
    private float distance(PointF point1, PointF point2) {
        float dx = point2.x - point1.x;
        float dy = point2.y - point1.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * 获取图片的缩放中心，该属性可在外部设置，或通过xml文件设置
     * 默认中心点为图片中心
     *
     * @return 图片的缩放中心点
     */
    private PointF getScaleCenter() {
        // 使用全局变量避免频繁创建变量
        /*switch (mScaleBy) {
            case SCALE_BY_IMAGE_CENTER:
                scaleCenter.set(mImageRect.centerX(), mImageRect.centerY());
                break;
            case SCALE_BY_FINGER_MID_POINT:
                scaleCenter.set(mLastMidPoint.x, mLastMidPoint.y);
                break;
        }*/
        scaleCenter.set(mLastMidPoint.x, mLastMidPoint.y);
        return scaleCenter;
    }

    private void drawSegmentBitmap(){
        mTextureViewCanvas = mTextureView.lockCanvas();
        Log.i(TAG,"drawSegmentBitmap ");
        if (mTextureViewCanvas != null && mSegmentBitmap != null) {
            Log.i(TAG,"drawSegmentBitmap , mTextureViewCanvas = " + mTextureViewCanvas.hashCode());
            mSegmentBitmapWidth = mSegmentBitmap.getWidth();
            mSegmentBitmapHeight = mSegmentBitmap.getHeight();

            //Rect rect = new Rect(0, 0, mSegmentBitmapWidth, mSegmentBitmapHeight);

            mTextureViewCanvas.setDrawFilter(mDrawFilter);
            mTextureViewCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
            mTextureViewCanvas.drawColor(Color.parseColor("#f2f2f2"));
            //mTextureViewCanvas.drawBitmap(mSegmentBitmap, rect, rect, mPaint);
            mTextureViewCanvas.drawBitmap(mSegmentBitmap,0,0,mPaint);
            //mTextureViewCanvas.drawBitmap(mSegmentBitmap,mScaleMatix,mPaint);
            mTextureView.unlockCanvasAndPost(mTextureViewCanvas);
        }
    }

    public void drawSegmentBitmap(Bitmap segmentBitmap, Rect rect) {
        mSegmentRect = rect;
        mSegmentBitmap = segmentBitmap;
        drawSegmentBitmap();
        requestLayout();
    }

    public void setCanvasCallback(CanvasCallback canvasCallback) {
        mCanvasCallback = canvasCallback;
    }

    public interface CanvasCallback{
        void onCanvasCreated();
    }

    public void setOnTounchEventCallback(OnTounchEventCallback onTounchEventCallback) {
        mOnTounchEventCallback = onTounchEventCallback;
    }

    public interface OnTounchEventCallback{
        void onActionMove();
    }
}
