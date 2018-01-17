package com.camerapreview;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private SurfaceView surfaceView;
    private Camera mCamera;
    private SurfaceHolder sh;
    private TextView textview;
    private Button change_Button;
    private Button dopic_Button;
    private Button capture;
    private Spinner spinner;
    private int cameraId;
    private android.view.ViewGroup.LayoutParams lp;

    private String path = Environment.getExternalStorageDirectory().getAbsolutePath();
    String imagePath = path + File.separator + "temp.jpeg";
    String imagePath2 = path + File.separator + "temp2.jpeg";
    String yuvPath = path + File.separator + "temp.yuv";
    private boolean isPreviewing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 窗口去掉标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        textview = (TextView) findViewById(R.id.textview);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        change_Button = (Button) findViewById(R.id.change);
        dopic_Button = (Button) findViewById(R.id.dopic);
        capture = (Button) findViewById(R.id.capture);
        spinner = (Spinner) findViewById(R.id.support_preview_formats);
        lp = surfaceView.getLayoutParams();
        sh = surfaceView.getHolder();
        sh.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        sh.addCallback(this);
        change_Button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mCamera.stopPreview();
                mCamera.release();
                if (cameraId == 0) {
                    cameraId = 1;
                } else {
                    cameraId = 0;
                }
                openCameraAndSetSurfaceviewSize(cameraId);
                // the surfaceview is ready after the first launch
                setAndStartPreview(sh);
            }
        });
        dopic_Button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                doTakePicture();
            }
        });

        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCamera != null && isPreviewing) {
                    isCapture = true;
                }

            }
        });
        findViewById(R.id.support_formats).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCamera != null) {
                    Camera.Parameters parameters = mCamera.getParameters();
                    printCameraPara(parameters);
                } else {
                    Log.e("Mainactivity", "mCamera not open");
                }


            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        cameraId = 0;// default id
        openCameraAndSetSurfaceviewSize(cameraId);

    }

    @Override
    protected void onPause() {
        super.onPause();
        kill_camera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        kill_camera();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        setAndStartPreview(sh);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.w("MainActivity", "--surfaceChanged--" + format);
//        setPreviewSize(width, height);
//        setAndStartPreview(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    /*
       为了实现拍照的快门声音及拍照保存照片需要下面三个回调变量
       */
    Camera.ShutterCallback mShutterCallback = new Camera.ShutterCallback() {
        //快门按下的回调，在这里我们可以设置类似播放“咔嚓”声之类的操作。默认的就是咔嚓。
        public void onShutter() {
        }
    };
    Camera.PictureCallback mRawCallback = new Camera.PictureCallback() {
        // 拍摄的未压缩原数据的回调,可以为null
        public void onPictureTaken(byte[] data, Camera camera) {
        }
    };
    Camera.PictureCallback mJpegPictureCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap b = null;
            if (null != data) {
                b = BitmapFactory.decodeByteArray(data, 0, data.length);
                mCamera.stopPreview();
                isPreviewing = false;
            }
            if (null != b) {
                //设置FOCUS_MODE_CONTINUOUS_VIDEO)之后，myParam.set("rotation", 90)失效。
                //图片竟然不能旋转了，故这里要旋转下
                Bitmap rotaBitmap = ImageUtil.getRotateBitmap(b, 90.0f);
                rotaBitmap = Bitmap.createScaledBitmap(rotaBitmap, 320, 240, true);
                FileUtil.saveBitmap(rotaBitmap, imagePath);
            }
            mCamera.startPreview(); //再次进入预览
            isPreviewing = true;
        }
    };

    private void printCameraPara(Camera.Parameters parameters) {
        CamParaUtil.getInstance().printSupportPictureSize(parameters);
        CamParaUtil.getInstance().printSupportPreviewSize(parameters);
        CamParaUtil.getInstance().printSupportFocusMode(parameters);
        CamParaUtil.getInstance().printSupportPictureFormat(parameters);
        CamParaUtil.getInstance().printSupportPreviewFormat(parameters);
        final List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
        final String[] mItems = new String[mSupportedPreviewSizes.size()];
        for (int i = 0; i < mSupportedPreviewSizes.size(); i++) {
            mItems[i] = mSupportedPreviewSizes.get(i).width + "X" + mSupportedPreviewSizes.get(i).height;
        }
        // 建立Adapter并且绑定数据源
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//绑定 Adapter到控件
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

                String size = mItems[pos];
                Toast.makeText(MainActivity.this, "修改为:" + size, Toast.LENGTH_SHORT).show();
/*                ViewGroup.LayoutParams params = surfaceView.getLayoutParams();
                Camera.Size size1 = mSupportedPreviewSizes.get(pos);
                params.width = size1.width;
                params.height = size1.height;
                surfaceView.setLayoutParams(params);*/
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });

    }

    private void openCameraAndSetSurfaceviewSize(int cameraId) {
        int availableCamera= android.hardware.Camera.getNumberOfCameras();
        Log.e("Mainactivity",availableCamera + "--$$--");
        mCamera = Camera.open(cameraId);
        Camera.Parameters parameters = mCamera.getParameters();
        printCameraPara(parameters);

        Camera.CameraInfo info = new Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int camera_number = Camera.getNumberOfCameras();
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            textview.setText("There are " + camera_number + " camera." + "This is the Front Camera!");
        } else {
            textview.setText("There are " + camera_number + " camera." + "This is the Back Camera!");
        }
        List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, 640, 480);
        if (mPreviewSize != null) {
            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            parameters.setPictureSize(mPreviewSize.width, mPreviewSize.height);
        } else {
            parameters.setPreviewSize(640, 480);
            parameters.setPictureSize(640, 480);
        }
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
        }
        parameters.setPictureFormat(ImageFormat.JPEG); //Sets the image format for picture 设定相片格式为JPEG，默认为NV21
//        parameters.setPreviewFormat(ImageFormat.RGB_565); //Sets the image format for picture 设定相片格式为JPEG，默认为NV21
        mCamera.setParameters(parameters);
        Log.e("MainActivity", "-----------------openCameraAndSetSurfaceviewSize-----------");

    }

    private void setPreviewSize(int width, int height) {
        Log.e("Mainactivity", android.hardware.Camera.getNumberOfCameras() + "----");
        Camera.Parameters parameters = mCamera.getParameters();

        parameters.setPreviewSize(width, height);
        mCamera.setParameters(parameters);
        Log.e("MainActivity", "-----------------setPreviewSize-----------");

    }

    private void setAndStartPreview(SurfaceHolder holder) {
        try {
            Log.e("MainActivity", "--------setAndStartPreview------");
            mCamera.setPreviewDisplay(holder);
            mCamera.setDisplayOrientation(getPreviewDegree(MainActivity.this));
            mCamera.startPreview();
            mCamera.setPreviewCallback(this);
            isPreviewing = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void doTakePicture() {
        try {
            if (isPreviewing && (mCamera != null)) {
                Log.d("MainActivity", "---doTakePicture----");
                mCamera.takePicture(mShutterCallback, null, mJpegPictureCallback);
            } else {

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void kill_camera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            isPreviewing = false;
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }


    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }


    // 提供一个静态方法，用于根据手机方向获得相机预览画面旋转的角度
    public static int getPreviewDegree(Activity activity) {
        // 获得手机的方向
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degree = 0;
        // 根据手机的方向计算相机预览画面应该选择的角度
        switch (rotation) {
            case Surface.ROTATION_0:
                degree = 90;
                break;
            case Surface.ROTATION_90:
                degree = 0;
                break;
            case Surface.ROTATION_180:
                degree = 270;
                break;
            case Surface.ROTATION_270:
                degree = 180;
                break;
        }
        Log.w("MainActivity", "degree=" + degree);
        return degree;
    }

    boolean isCapture = false;
    private Object synobject = new Object();

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

        if (isCapture) {
            synchronized (synobject) {
                decodeToBitMap(data, camera);
            }
            isCapture = false;
        }

    }

    public void decodeToBitMap(byte[] data, Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        try {
            int imageFormat = parameters.getPreviewFormat();
            Log.e("MainActivity", "prviewData:" + imageFormat);
            FileUtil.saveYUVToFile(data, yuvPath);
            if (imageFormat == ImageFormat.NV21) {
                int w = parameters.getPreviewSize().width;
                int h = parameters.getPreviewSize().height;
                YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, w, h, null);
                Rect rect = new Rect(0, 0, w, h);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(rect, 100, baos);
                byte[] jData = baos.toByteArray();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                Bitmap bitmap = BitmapFactory.decodeByteArray(jData, 0, jData.length);
                FileUtil.saveBitmap(bitmap, imagePath2);
              /*  Bitmap rotaBitmap = ImageUtil.getRotateBitmap(bitmap, 90.0f);
                FileUtil.saveBitmap(rotaBitmap, imagePath2);*/
                //E9专用
              /*  int[] pixels = FrameDecodeUtil.convertYUV420_NV21toRGB8888(data, w, h);
                Bitmap bt = Bitmap.createBitmap(pixels,w, h, Bitmap.Config.RGB_565);
                FileUtil.saveBitmap(bt, imagePath2);*/
            } else if (imageFormat == ImageFormat.JPEG || imageFormat == ImageFormat.RGB_565 || imageFormat == ImageFormat.FLEX_RGBA_8888) {
                Toast.makeText(MainActivity.this, "Format: JPEG||RGB_565", Toast.LENGTH_SHORT).show();
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                FileUtil.saveBitmap(bitmap, imagePath2);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e("MainActivity", "---Error:" + ex.getMessage());
        }
    }



/*    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Type.Builder yuvType, rgbaType;
    private Allocation in, out;

    private void initFrameDecod() {
        rs = RenderScript.create(this);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
    }
     private void yuvFastDecode(byte[] data, int w, int h) {
        if (yuvType == null) {
            yuvType = new Type.Builder(rs, Element.U8(rs)).setX(data.length);
            in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
            rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(w).setY(h);
            out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
        }
        in.copyFrom(data);
        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);
        Bitmap bmpout = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.copyTo(bmpout);
        FileUtil.saveBitmap(bmpout, imagePath2);
    }
    */
}
