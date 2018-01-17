package com.camerapreview;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

public class FileUtil {
    private static final String TAG = "FileUtil";
    private static final File parentPath = Environment.getExternalStorageDirectory();
    private static String storagePath = "";
    private static final String DST_FOLDER_NAME = "PlayCamera";

    /**
     * ��ʼ������·��
     *
     * @return
     */
    private static String initPath() {
        if (storagePath.equals("")) {
            storagePath = parentPath.getAbsolutePath() + "/" + DST_FOLDER_NAME;
            File f = new File(storagePath);
            if (!f.exists()) {
                f.mkdir();
            }
        }
        return storagePath;
    }

    /**
     * ����Bitmap��sdcard
     *
     * @param b
     */
    public static void saveBitmap(Bitmap b, String imageName) {
        try {
            Log.d(TAG,"imageName="+imageName);
            FileOutputStream fout = new FileOutputStream(imageName);
            BufferedOutputStream bos = new BufferedOutputStream(fout);
            b.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
            b.recycle();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //-----------------------保存图片---------------------------------------
    public static void saveImageToFile(byte[] buffer, String imageName) {
        if (buffer == null) {
            Log.i("MyPicture", "自定义相机Buffer: null");
        } else {
            try {
                Log.i("MyPicture", "saveImageToFile");
                FileOutputStream fos = new FileOutputStream(new File(imageName));
                fos.write(buffer);
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static void saveYUVToFile(byte[] buffer, String imageName) {
        if (buffer == null) {
            Log.i("MyPicture", "自定义相机Buffer: null");
        } else {
            try {
                Log.i("MyPicture", "saveYUVToFile");
                FileOutputStream fos = new FileOutputStream(new File(imageName));
                fos.write(buffer);
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
