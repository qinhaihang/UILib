package com.example.widgetlib;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.InputStream;

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

    private RelativeLayout mSegmentPreviewRoot;
    private TextureView mTextureView;
    private ImageView mBgImageView;
    private View mTopMaskLayerView;
    private View mBottomMaskLayerView;

    private Canvas mTextureViewCanvas;
    private Rect mSegmentRect;

    private CanvasCallback mCanvasCallback;

    private Bitmap mBgBitmap;
    private Bitmap mSegmentBitmap;
    private int mSegmentBitmapWidth;
    private int mSegmentBitmapHeight;
    private int mInitW;
    private int mInitH;
    private int mIconMargin = 10;

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

        //添加绘制背景的控件
        addBgView();

        //添加segment之后的图像预览控件
        //addSegmentPreviewView();

        //添加预览比例蒙层
        addMaskLayer();

    }

    private void addBgView() {
        LayoutParams bgLayoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        mBgImageView = new ImageView(mContext);
        mBgImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        addView(mBgImageView, bgLayoutParams);
    }

    private void addMaskLayer() {
        LayoutParams topMaskLayerParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        topMaskLayerParams.height = 0;
        mTopMaskLayerView = new View(mContext);
        mTopMaskLayerView.setBackgroundColor(Color.BLACK);
        addView(mTopMaskLayerView, getChildCount(), topMaskLayerParams);

        LayoutParams bottomMaskLayerParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        bottomMaskLayerParams.height = 0;
        bottomMaskLayerParams.addRule(ALIGN_PARENT_BOTTOM);
        mBottomMaskLayerView = new View(mContext);
        mBottomMaskLayerView.setBackgroundColor(Color.BLACK);
        addView(mBottomMaskLayerView, getChildCount(), bottomMaskLayerParams);
    }

    public void addSegmentPreviewView(int w,int h) {

        Log.d(TAG,"addSegmentPreviewView, w = " + w +",h = " + h);

        mInitW = w;
        mInitH = h;

        //加载需要使用的icon
        Bitmap scaleUpBitmap = getDrawableBitmap(mContext, R.drawable.scale_up);
        Bitmap scaleDownBitmap = getDrawableBitmap(mContext, R.drawable.scale_down);
        int segmentPreviewH = scaleDownBitmap.getHeight() + scaleUpBitmap.getHeight() + h + mIconMargin;

        //添加预览界面容器，用于添加预览图像以及预览图像框、图标
        LayoutParams segmentPreviewRootParams = new LayoutParams(w, segmentPreviewH);
        segmentPreviewRootParams.addRule(CENTER_IN_PARENT);
        mSegmentPreviewRoot = new RelativeLayout(mContext);
        mSegmentPreviewRoot.setBackgroundColor(Color.parseColor("#000000"));
        addView(mSegmentPreviewRoot,segmentPreviewRootParams);

        //缩小提示图标
        LayoutParams scaleDownLayoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        scaleDownLayoutParams.addRule(ALIGN_PARENT_RIGHT);
        scaleDownLayoutParams.bottomMargin = 5;
        ImageView scaleDown = new ImageView(mContext);
        scaleDown.setId(R.id.id_transform_scale_down);
        scaleDown.setImageBitmap(scaleDownBitmap);
        mSegmentPreviewRoot.addView(scaleDown,scaleDownLayoutParams);

        //放大提示图标
        LayoutParams scaleUpLayoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        scaleUpLayoutParams.addRule(ALIGN_PARENT_END);
        scaleUpLayoutParams.addRule(BELOW,R.id.id_transform_scale_down);
        scaleUpLayoutParams.bottomMargin = 5;
        ImageView scaleUp = new ImageView(mContext);
        scaleUp.setId(R.id.id_transform_scale_up);
        scaleUp.setImageBitmap(scaleUpBitmap);
        mSegmentPreviewRoot.addView(scaleUp,scaleUpLayoutParams);

        //绘制回显预览图
        LayoutParams textureViewParams = new LayoutParams(w,h);
        textureViewParams.addRule(BELOW,R.id.id_transform_scale_up);
        mTextureView = new TextureView(mContext);
        mTextureView.setId(R.id.id_transform_segment_texture);
        mTextureView.setOpaque(false);
        mSegmentPreviewRoot.addView(mTextureView,textureViewParams);
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
        mSegmentPreviewRoot.setRotation(mRotationDegree);

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

        float x = mLastMidPoint.x - mSegmentBitmap.getWidth() / 2;
        float y = mLastMidPoint.y - mSegmentBitmap.getHeight() / 2;

        if(x < 0){
            x = 0;
        }

        if(y < 0){
            y = 0;
        }

        mSegmentPreviewRoot.setX(x);
        mSegmentPreviewRoot.setY(y);

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

        mSegmentPreviewRoot.setScaleX(mScaleFactor);
        mSegmentPreviewRoot.setScaleY(mScaleFactor);
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

    /************************** 图片绘制部分  *********************************/

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
    }

    public void drawBgTheme(Bitmap bgBitmap) {
        mBgBitmap = bgBitmap;
        mBgImageView.setImageBitmap(bgBitmap);
    }

    public Bitmap getSegmentBitmap(){
        int width = mTextureView.getWidth();
        int height = mTextureView.getHeight();
        Log.d(TAG,"segment textureView width " + width + ", height " + height);
        return mTextureView.getBitmap(width,height);
    }

    public Bitmap savePic(){
        int width = getWidth();
        int height = getHeight();
        Log.i(TAG, "width = " + width + " , height = " + height);

        Bitmap canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(canvasBitmap);

        canvas.drawColor(Color.WHITE);

        canvas.drawBitmap(mBgBitmap, null, new RectF(0, 0, width, height), mPaint);

        Bitmap segmentBitmap = getSegmentBitmap();
        if(segmentBitmap != null){

            segmentBitmap = scaleBitmap(segmentBitmap, mScaleFactor);

            segmentBitmap = rotateBitmap(segmentBitmap,mRotationDegree);

            float x = mSegmentPreviewRoot.getX();
            float y = mSegmentPreviewRoot.getY();
            canvas.drawBitmap(segmentBitmap,x,y,mPaint);

        }

        //调整比例画幅
        /*Bitmap waterMarkBitmap = BitmapUtils.getDrawableBitmap(mContext, R.drawable.watermark);

        int markBitmapWidth = waterMarkBitmap.getWidth();
        int markBitmapHeight = waterMarkBitmap.getHeight();
        int x = width - markBitmapWidth;
        int y = height - markBitmapHeight;

        //调整比例之后需要切图
        int topMaskLayerViewHeight = mTopMaskLayerView.getHeight();
        int bottomMaskLayerViewHeight = mBottomMaskLayerView.getHeight();
        //LogUtils.i(TAG,"topMaskLayerViewHeight = " + topMaskLayerViewHeight + " , bottomMaskLayerViewHeight = " + bottomMaskLayerViewHeight);

        canvas.drawBitmap(waterMarkBitmap,x,y - bottomMaskLayerViewHeight,mPaint);

        if(topMaskLayerViewHeight != 0 && bottomMaskLayerViewHeight != 0){
            Rect cutRect = new Rect(0,topMaskLayerViewHeight,width,
                    height - bottomMaskLayerViewHeight);
            Bitmap cutBitmap = BitmapUtils.cutBitmap(canvasBitmap, cutRect);
            return cutBitmap;
        }*/

        return canvasBitmap;
    }

    /**
     * 调整画幅比例
     * @param topMaskLayerH
     * @param bottomMaskLayerH
     */
    public void updateMaskLayer(int topMaskLayerH, int bottomMaskLayerH) {

        RelativeLayout.LayoutParams topMaskLayerParams = (LayoutParams) mTopMaskLayerView.getLayoutParams();
        topMaskLayerParams.height = topMaskLayerH;
        mTopMaskLayerView.setLayoutParams(topMaskLayerParams);

        RelativeLayout.LayoutParams bottomMaskLayerParams = (LayoutParams) mBottomMaskLayerView.getLayoutParams();
        bottomMaskLayerParams.height = bottomMaskLayerH;
        mBottomMaskLayerView.setLayoutParams(bottomMaskLayerParams);

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

    /**************         需要使用的内部工具类        *****************/
    private Bitmap getDrawableBitmap(Context context, int drawabvleId){
        Resources r = context.getResources();
        InputStream inputStream = r.openRawResource(drawabvleId);
        BitmapDrawable bmpDraw = new BitmapDrawable(r,inputStream);
        Bitmap bmp = bmpDraw.getBitmap();
        return bmp;
    }

    /**
     * 按比例缩放图片
     *
     * @param origin 原图
     * @param ratio  比例
     * @return 新的bitmap
     */
    private Bitmap scaleBitmap(Bitmap origin, float ratio) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.preScale(ratio, ratio);
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (newBM.equals(origin)) {
            return newBM;
        }
        origin.recycle();
        return newBM;
    }

    private Bitmap rotateBitmap(Bitmap bitmap, float degrees) {
        if (bitmap != null) {
            Matrix m = new Matrix();
            m.postRotate(degrees);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
            return bitmap;
        }
        return null;
    }
}
