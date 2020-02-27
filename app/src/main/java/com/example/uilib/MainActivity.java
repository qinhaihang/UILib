package com.example.uilib;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.example.widgetlib.TransformView2;

import java.io.File;

public class MainActivity extends AppCompatActivity implements TransformView2.CanvasCallback {

    private static final String basePath = Environment.getExternalStorageDirectory() + File.separator + "stbabyphoto" + File.separator;

    private TransformView2 mTransformView;
    private Bitmap mWaterMark;
    private ImageView mIvTest;
    private Bitmap mBgBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWaterMark = BitmapUtils.getDrawableBitmap(this, R.drawable.watermark);
        mBgBitmap = BitmapUtils.getDrawableBitmap(this, R.drawable.bg);

        //mIvTest = findViewById(R.id.iv_test);
        //mIvTest.setImageBitmap(mWaterMark);

        mTransformView = findViewById(R.id.transform_view);
        mTransformView.addSegmentPreviewView(mWaterMark.getWidth(),mWaterMark.getHeight());
        //mTransformView.addSegmentPreviewView(1000,1000);
        mTransformView.setCanvasCallback(this);
    }


    @Override
    public void onCanvasCreated() {

        mTransformView.drawBgTheme(mBgBitmap);

        new Thread(() -> {
            for (int i = 0; i < 2; i++) {
                SystemClock.sleep(1000);
                runOnUiThread(() -> mTransformView.drawSegmentBitmap(mWaterMark, new Rect()));
            }

        }).start();

        //runOnUiThread(() -> mTransformView.drawSegmentBitmap(mWaterMark, new Rect()));
    }

    public void click(View view) {
        Bitmap bitmap = mTransformView.savePic();
        String path = BitmapUtils.saveBitmapToSDPNG(bitmap,basePath,"test_" + System.currentTimeMillis(),100 );
        Log.d("qhh","path = " + path);
    }
}
