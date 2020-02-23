package com.example.uilib;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * @author qinhaihang_vendor
 * @time 2019/12/27 17:08
 * @des
 * @packgename com.sensetime.commonutils.image
 */
public class BitmapUtils {

    private static final String TAG = BitmapUtils.class.getSimpleName();

    public static final String TEST_PIC = Environment.getExternalStorageDirectory() + File.separator + "baby" + File.separator;

    public static String saveBitmapToSDPNG(Bitmap bitmap, String basePath , String imagename, int quality) {

        try {
            // 文件夹不存在则创建文件夹
            File folder = new File(basePath);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            // 文件存在则删除，用于覆盖保存
            String path = basePath + imagename + ".png";
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
            if (!file.exists()) {
                file.createNewFile();
            }

            FileOutputStream out = new FileOutputStream(path);
            bitmap.compress(Bitmap.CompressFormat.PNG, quality, out);

            out.flush();
            out.close();
            return path;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Bitmap getBitmap(Context context, int vectorDrawableId) {
        Bitmap bitmap = null;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            Drawable vectorDrawable = context.getDrawable(vectorDrawableId);
            bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                    vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            vectorDrawable.draw(canvas);
        } else {
            bitmap = BitmapFactory.decodeResource(context.getResources(), vectorDrawableId);
        }
        return bitmap;
    }

    public static Bitmap getBitmapFromFile(String picPath){
        Bitmap bitmap = BitmapFactory.decodeFile(picPath);
        return bitmap;
    }

    public static Bitmap getBitmapFromFile(String picPath, int reqW, int reqH){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(picPath,options);
        options.inJustDecodeBounds = false;

        int outWidth = options.outWidth;
        int outHeight = options.outHeight;

        Log.i("EditPhotoPresenter","outWidth = " + outWidth + ", outHeight = "+ outHeight);

        if(outWidth > 2000 && outHeight > 2000){
            //inSampleSize的作用就是可以把图片的长短缩小inSampleSize倍，所占内存缩小inSampleSize的平方
            options.inSampleSize = caculateSampleSize(options,reqW,reqH);
            return BitmapFactory.decodeFile(picPath,options);
        }else{
            return BitmapFactory.decodeFile(picPath);
        }

    }

    /**
     * 计算出所需要压缩的大小
     * @param options
     * @param reqWidth  我们期望的图片的宽，单位px
     * @param reqHeight 我们期望的图片的高，单位px
     * @return
     */
    private static int caculateSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int sampleSize = 1;
        int picWidth = options.outWidth;
        int picHeight = options.outHeight;
        Log.i(TAG,"original pic w = " + picWidth + " , h = " + picHeight);
        if (picWidth > reqWidth || picHeight > reqHeight) {
            int halfPicWidth = picWidth / 2;
            int halfPicHeight = picHeight / 2;
            while (halfPicWidth / sampleSize > reqWidth || halfPicHeight / sampleSize > reqHeight) {
                sampleSize *= 2;
            }
        }
        return sampleSize;
    }

    public static Bitmap getDrawableBitmap(Context context, int drawabvleId){
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
    public static Bitmap scaleBitmap(Bitmap origin, float ratio) {
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

    public static Bitmap scaleBitmap(Bitmap origin, float ratioX,float ratioY) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.preScale(ratioX, ratioY);
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (newBM.equals(origin)) {
            return newBM;
        }
        origin.recycle();
        return newBM;
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, float degrees) {
        if (bitmap != null) {
            Matrix m = new Matrix();
            m.postRotate(degrees);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
            return bitmap;
        }
        return null;
    }

    public static Bitmap cutBitmap(Bitmap bitmap, Rect cutRect){
        Bitmap cutBitmap = Bitmap.createBitmap(bitmap, cutRect.left, cutRect.top, cutRect.width(), cutRect.height());
        bitmap.recycle();
        return cutBitmap;
    }

    public static byte[] convertToByte(Bitmap bitmap){
        int byteCount = bitmap.getByteCount();
        ByteBuffer byteBuffer = ByteBuffer.allocate(byteCount);

        bitmap.copyPixelsToBuffer(byteBuffer);

        byte[] array = byteBuffer.array();

        return array;
    }

    public static int getPicOrientation(String filePath){
        try {
            ExifInterface exif = new ExifInterface(filePath);
            if(exif == null){
                return 1;
            }
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Log.d(TAG,"getPicOrientation " + orientation);
            int stOrientation = 1;
            switch (orientation){
                case ExifInterface.ORIENTATION_NORMAL:
                    stOrientation = 1; // up
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    stOrientation = 2; // left
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    stOrientation = 4; // down
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    stOrientation = 8; // right
                    break;
            }
            return stOrientation;

        } catch (IOException e) {
            //e.printStackTrace();
        }
        return 1;
    }

    public static Bitmap getFilterBitmap(Bitmap srcBitmap){
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        srcBitmap.compress(Bitmap.CompressFormat.PNG,100,bos);
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        Bitmap dstBitmap = BitmapFactory.decodeStream(bis);
        return dstBitmap;
    }

    public static String bitmapToBase64(Bitmap bitmap){

        String result = "";
        ByteArrayOutputStream bos = null;
        try {
            if(bitmap != null){
                bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG,100,bos);
                byte[] bytes = bos.toByteArray();
                byte[] encode = Base64.encode(bytes, Base64.NO_WRAP);
                result = new String(encode,"UTF-8");
            }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(bos != null){
                try {
                    bos.flush();
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        return result;
    }

    public static Bitmap bitmapFromBase64(String imageBase64){

        byte[] bytes = Base64.decode(imageBase64, Base64.NO_WRAP);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

    }

}
