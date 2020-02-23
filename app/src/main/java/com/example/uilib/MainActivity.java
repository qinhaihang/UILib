package com.example.uilib;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.ImageView;

import com.example.widgetlib.TransformView2;

public class MainActivity extends AppCompatActivity implements TransformView2.CanvasCallback, TransformView2.OnTounchEventCallback {

    private TransformView2 mTransformView;
    private Bitmap mWaterMark;
    private ImageView mIvTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWaterMark = BitmapUtils.getDrawableBitmap(this, R.drawable.watermark);

        mIvTest = findViewById(R.id.iv_test);
        //mIvTest.setImageBitmap(mWaterMark);

        mTransformView = findViewById(R.id.transform_view);
        mTransformView.addSegmentPreviewView(mWaterMark.getWidth(),mWaterMark.getHeight());
        //mTransformView.addSegmentPreviewView(1000,1000);
        mTransformView.setCanvasCallback(this);
        mTransformView.setOnTounchEventCallback(this);
    }

    @Override
    public void onCanvasCreated() {
        new Thread(() -> {
            for (int i = 0; i < 2; i++) {
                SystemClock.sleep(1000);
                runOnUiThread(() -> mTransformView.drawSegmentBitmap(mWaterMark, new Rect()));
            }

        }).start();

        //runOnUiThread(() -> mTransformView.drawSegmentBitmap(mWaterMark, new Rect()));
    }

    @Override
    public void onActionMove() {
        runOnUiThread(() -> mTransformView.drawSegmentBitmap(mWaterMark, new Rect()));
    }
}
