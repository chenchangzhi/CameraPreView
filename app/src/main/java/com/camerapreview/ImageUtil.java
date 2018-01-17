package com.camerapreview;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.Camera;

public class ImageUtil {
    public static Bitmap getRotateBitmap(Bitmap b, float rotateDegree, int i) {
        Matrix matrix = new Matrix();
        matrix.postRotate((float) rotateDegree);
        if (i == Camera.CameraInfo.CAMERA_FACING_FRONT) {//使用前置摄像头
            matrix.setScale(-1, 1);
        }
        Bitmap rotaBitmap = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, false);

        return rotaBitmap;
    }

    public static Bitmap getRotateBitmap(Bitmap b, float rotateDegree){
        Matrix matrix = new Matrix();
        matrix.postRotate((float)rotateDegree);
        Bitmap rotaBitmap = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, false);
        return rotaBitmap;
    }
}
