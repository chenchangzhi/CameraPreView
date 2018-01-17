package com.camerapreview;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Main3Activity extends Activity {
    private String TAG = "Main3Activity";
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private Context context;
    private AutoFitTextureView mTextureView;
    private TextView textview;
    private Button change_Button;
    private Button takepic_Button;
    private Button catchpreview_button;
    private Spinner spinner;
    private boolean isfirst = true;
    // 是否需要闪光灯支持
    boolean availableFlash = false;
    // 是否自动对焦支持
    boolean availableFocus = false;
    /**
     * 处理拍照等工作的子线程
     */
    private HandlerThread mBackgroundThread;
    private Handler childHandler, mainHandler;
    /**
     * 预览数据的尺寸
     */
    private Size mPreviewSize;

    /**
     * 正在使用的相机
     */
    private CameraDevice mCameraDevice;
    /**
     * 预览用的获取会话
     */
    private CameraCaptureSession mCaptureSession;
    /**
     * 摄像头Id 0 为后  1 为前
     */
    private String mCameraId = "0";
    private CameraManager cameraManager;
    private ImageReader mImageReader;

    /**
     * SurfaceTexture监听器
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            // SurfaceTexture就绪后回调执行打开相机操作
            initCamera2(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            // 预览方向改变时, 执行转换操作
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    /**
     * 相机状态改变的回调函数
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            // 当相机打开执行以下操作:
            // 2. 将正在使用的相机指向将打开的相机
            // 3. 创建相机预览会话
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            // 当相机失去连接时执行以下操作:
            // 2. 关闭相机
            // 3. 将正在使用的相机指向null
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            // 当相机发生错误时执行以下操作:
            // 2. 关闭相机
            // 3, 将正在使用的相机指向null
            // 4. 获取当前的活动, 并结束它
            cameraDevice.close();
            mCameraDevice = null;
            finish();
        }
    };

    /**
     * 拍照回调
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Log.w(TAG, "mCaptureCallback-->拍照完成");
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            Log.w(TAG, "mCaptureCallback-->拍照失败");
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            Log.w(TAG, "mCaptureCallback-->拍照进行中。。。！");
        }
    };
    /**
     * 预览会话回调函数
     */
    private CameraCaptureSession.CaptureCallback mPreviewCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            if (isCapture) {
                synchronized (synobject) {
                    decodeToBitMap();
                }
                isCapture = false;
            }
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            Log.e(TAG, "mPreviewCallback-->onCaptureFailed");
        }

        @Override
        public void onCaptureSequenceCompleted(CameraCaptureSession session, int sequenceId, long frameNumber) {
            super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
        }

        @Override
        public void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId) {
            super.onCaptureSequenceAborted(session, sequenceId);
        }
    };

    boolean isCapture = false;
    private Object synobject = new Object();
    ImageReader.OnImageAvailableListener mImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(final ImageReader reader) {
            synchronized (reader) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // 获取捕获的照片数据
                        Image mImage = reader.acquireNextImage();
                        ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);
                        // 使用IO流将照片写入指定文件
                        String fileName = "takepic.jpg";
                        File mFile = new File(getExternalFilesDir(null), fileName);
                        Log.w(TAG, "mFile-->" + mFile.getAbsolutePath());
                        FileOutputStream output = null;
                        try {
                            output = new FileOutputStream(mFile);
                            output.write(bytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            mImage.close();
                            if (null != output) {
                                try {
                                    output.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }).start();
            }

        }
    };

    public void decodeToBitMap() {
        if (mImageReader != null) {
            imageCount++;
            Bitmap bitmap = mTextureView.getBitmap();
            // 使用IO流将照片写入指定文件
            String fileName = "previewpic" + imageCount + ".jpg";
            File mFile = new File(getExternalFilesDir(null), fileName);
            try {
                FileOutputStream fout = new FileOutputStream(mFile);
                BufferedOutputStream bos = new BufferedOutputStream(fout);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                bos.flush();
                bos.close();
                bitmap.recycle();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        isfirst = true;
        // 窗口去掉标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main3);
        initView();
    }


    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        if (mTextureView.isAvailable()) {
            initCamera2(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }


    private void initView() {
        mTextureView = (AutoFitTextureView) findViewById(R.id.auto_textureView);
        textview = (TextView) findViewById(R.id.textview);
        change_Button = (Button) findViewById(R.id.change);
        takepic_Button = (Button) findViewById(R.id.dopic);
        catchpreview_button = (Button) findViewById(R.id.capture);
        spinner = (Spinner) findViewById(R.id.support_preview_formats);

        change_Button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                closeCamera();
                if (mCameraId.equals("0")) {
                    mCameraId = "1";
                } else if (mCameraId.equals("1")) {
                    mCameraId = "0";
                }

                initCamera2(mTextureView.getWidth(), mTextureView.getHeight());
            }
        });
        takepic_Button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                takePicture();
            }
        });

        catchpreview_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCapture = true;
            }
        });
        findViewById(R.id.support_formats).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                printCameraInfo();
            }
        });
    }

    /**
     * 开启子线程
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        childHandler = new Handler(mBackgroundThread.getLooper());
        mainHandler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                int what = msg.what;
                if (what == 1) {
                    if (isfirst) {
                        Size[] previewSize = (Size[]) msg.obj;
                        initSpinn(previewSize);
                    }

                }
                super.handleMessage(msg);
            }
        };
    }

    /**
     * 停止子线程
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            childHandler.removeCallbacksAndMessages(null);
            mainHandler.removeCallbacksAndMessages(null);
            mBackgroundThread.join();
            mBackgroundThread = null;
            childHandler = null;
            mainHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void initCamera2(int width, int height) {
        if (checkPermission()) {
            cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            printCameraInfo();
            try {
                // 获取指定摄像头的特性
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(mCameraId);
                // 获取摄像头支持的配置属性
                StreamConfigurationMap configurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                // 获取摄像头支持的最大尺寸
                Size largest = Collections.max(Arrays.asList(configurationMap.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
                initPreviewSize(characteristics, configurationMap, largest, width, height);
                initImageReader(configurationMap, largest);
                // 配置格式转换
                configureTransform(width, height);
                openCamera2();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

    }

    private void openCamera2() throws CameraAccessException {
        //打开摄像头
        cameraManager.openCamera(mCameraId, mStateCallback, mainHandler);
        if (mCameraId.equals("0")) {
            textview.setText("There are " + mCameraId + " camera." + "This is the Front Camera!");
        } else {
            textview.setText("There are " + mCameraId + " camera." + "This is the Back Camera!");
        }
    }

    /**
     * 关闭正在使用的相机
     */
    private void closeCamera() {
        // 关闭捕获会话
        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        // 关闭当前相机
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        // 关闭拍照处理器
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    /**
     * 打印相机参数
     */
    private void printCameraInfo() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (String cameraId : cameraManager.getCameraIdList()) {
                        // 获取指定摄像头的特性
                        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                        // 获取摄像头支持的配置属性
                        StreamConfigurationMap configurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                        if (configurationMap == null) {
                            continue;
                        }

                        //获取图片支持的格式
                        int[] pictureSupportFormats = configurationMap.getOutputFormats();
                        for (int i = 0; i < pictureSupportFormats.length; i++) {
                            Log.w(TAG, cameraId + "---pictureSupportFormats-->" + pictureSupportFormats[i]);
                            // 获取摄像头支持的图片尺寸
                            if (configurationMap.isOutputSupportedFor(pictureSupportFormats[i])) {
                                Size[] pictureSize = configurationMap.getOutputSizes(SurfaceTexture.class);
                                for (int j = 0; j < pictureSize.length; j++) {
                                    Log.w(TAG, cameraId + "---pictureSize-->" + pictureSize[j].getWidth() + "X" + pictureSize[j].getHeight());
                                }
                            }
                        }
                        //获取预览画面输出的尺寸，SurfaceTexture
                        if (configurationMap.isOutputSupportedFor(SurfaceTexture.class)) {
                            Size[] previewSize = configurationMap.getOutputSizes(SurfaceTexture.class);
                            for (int i = 0; i < previewSize.length; i++) {
                                Log.d(TAG, cameraId + "---previewSize-->" + previewSize[i].getWidth() + "X" + previewSize[i].getHeight());
                            }
                            Message msg = mainHandler.obtainMessage(1);
                            msg.obj = previewSize;
                            mainHandler.sendMessage(msg);
                        }


                        // 是否需要闪光灯支持
                        availableFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                        Log.d(TAG, cameraId + "---闪光灯支持-->" + availableFlash);
                        // 是否对焦支持
                        int[] afModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
                        for (int i = 0; i < afModes.length; i++) {
                            Log.d(TAG, cameraId + "---afModes-->" + afModes[i]);
                            //因为使用的对焦模式为CONTROL_AF_MODE_CONTINUOUS_PICTURE，所以依次判断是否支持对焦
                            if (afModes[i] == CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE) {
                                availableFocus = true;
                            }
                        }

                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }


    private void initSpinn(final Size[] previewSize) {
        final String[] mItems = new String[previewSize.length];
        for (int i = 0; i < previewSize.length; i++) {
            mItems[i] = previewSize[i].getWidth() + "X" + previewSize[i].getHeight();
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
                if (isfirst) {
                    isfirst = false;
                    return;
                }
                // 关闭拍照处理器
                if (null != mImageReader) {
                    initPreviewSize(previewSize[pos].getWidth(), previewSize[pos].getHeight());
                    mImageReader = ImageReader.newInstance(previewSize[pos].getWidth(), previewSize[pos].getWidth(), formate, 2);
                    mImageReader.setOnImageAvailableListener(mImageAvailableListener, null);
                    createCameraPreviewSession();
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private int imageCount = 0;
    int formate = ImageFormat.JPEG;

    /**
     * 初始化ImageReader
     *
     * @param configurationMap
     * @param largest
     */
    private void initImageReader(StreamConfigurationMap configurationMap, Size largest) {
        // 选用最高画质
        // maxImages是ImageReader一次可以访问的最大图片数量
        // 创建一个ImageReader对象，用于获取摄像头的图像数据
        if (!configurationMap.isOutputSupportedFor(formate)) {
            formate = ImageFormat.NV21;
        }
   /*     //获取图片支持的格式
        int[] pictureSupportFormats = configurationMap.getOutputFormats();
        if (pictureSupportFormats.length > 0) {
            formate = pictureSupportFormats[0];
        }*/

        mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), formate, 2);
        mImageReader.setOnImageAvailableListener(mImageAvailableListener, null);
    }

    private void initPreviewSize(CameraCharacteristics characteristics, StreamConfigurationMap configurationMap, Size largest, int viewWidth, int viewHeight) {
        int MAX_PREVIEW_WIDTH = 1920;
        int MAX_PREVIEW_HEIGHT = 1080;
        // 获取手机目前的旋转方向(横屏还是竖屏, 对于"自然"状态下高度大于宽度的设备来说横屏是ROTATION_90
        // 或者ROTATION_270,竖屏是ROTATION_0或者ROTATION_180)
        int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
        // 获取相机传感器的方向("自然"状态下垂直放置为0, 顺时针算起, 每次加90读)
        // 注意, 这个参数, 是由设备的生产商来决定的, 大多数情况下, 该值为90, 以下的switch这么写
        // 是为了配适某些特殊的手机
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        boolean swappedDimensions = false;
        switch (displayRotation) {
            // ROTATION_0和ROTATION_180都是竖屏只需做同样的处理操作
            // 显示为竖屏时, 若传感器方向为90或者270, 则需要进行转换(标志位置true)
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    swappedDimensions = true;
                }
                break;
            // ROTATION_90和ROTATION_270都是横屏只需做同样的处理操作
            // 显示为横屏时, 若传感器方向为0或者180, 则需要进行转换(标志位置true)
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    swappedDimensions = true;
                }
                break;
            default:
                Log.e(TAG, "Display rotation is invalid: " + displayRotation);
        }

        // 获取当前的屏幕尺寸, 放到一个点对象里
        Point displaySize = new Point();
        int width = viewWidth;
        int height = viewHeight;
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        // 旋转前的预览宽度(相机给出的), 通过传进来的参数获得
        int rotatedPreviewWidth = width;
        // 旋转前的预览高度(相机给出的), 通过传进来的参数获得
        int rotatedPreviewHeight = height;
        // 将当前的显示尺寸赋给最大的预览尺寸(能够显示的尺寸, 用来计算用的(texture可能比它小需要配适))
        int maxPreviewWidth = displaySize.x;
        int maxPreviewHeight = displaySize.y;

        // 如果需要进行画面旋转, 将宽度和高度对调
        if (swappedDimensions) {
            rotatedPreviewWidth = height;
            rotatedPreviewHeight = width;
            maxPreviewWidth = displaySize.y;
            maxPreviewHeight = displaySize.x;
        }

        // 尺寸太大时的极端处理
        if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
            maxPreviewWidth = MAX_PREVIEW_WIDTH;
        }

        if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
            maxPreviewHeight = MAX_PREVIEW_HEIGHT;
        }

        // 自动计算出最适合的预览尺寸
        // 第一个参数:map.getOutputSizes(SurfaceTexture.class)表示SurfaceTexture支持的尺寸List
        mPreviewSize = chooseOptimalSize(configurationMap.getOutputSizes(SurfaceTexture.class),
                rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                maxPreviewHeight, largest);
        // 下面这两个是计算后的previewSize=======================================
        Log.w(TAG, "mPreviewSize.getWidth: " + mPreviewSize.getWidth());
        Log.w(TAG, "mPreviewSize.getHeight: " + mPreviewSize.getHeight());
        // =================================================================
        // 获取当前的屏幕方向
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 如果方向是横向(landscape)
            mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        } else {
            // 方向不是横向(即竖向)
            mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
        }
    }

    private void initPreviewSize(int viewWidth, int viewHeight) {

        // 自动计算出最适合的预览尺寸
        // 第一个参数:map.getOutputSizes(SurfaceTexture.class)表示SurfaceTexture支持的尺寸List
        mPreviewSize = new Size(viewWidth, viewHeight);

        // 获取当前的屏幕方向
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 如果方向是横向(landscape)
            mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        } else {
            // 方向不是横向(即竖向)
            mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
        }

        // 下面这两个是计算后的previewSize=======================================
        Log.w(TAG, "mPreviewSize.getWidth: " + mPreviewSize.getWidth());
        Log.w(TAG, "mPreviewSize.getHeight: " + mPreviewSize.getHeight());
        // =================================================================
    }

    /**
     * 返回最合适的预览尺寸
     *
     * @param choices           相机希望输出类支持的尺寸list
     * @param textureViewWidth  texture view 宽度
     * @param textureViewHeight texture view 高度
     * @param maxWidth          能够选择的最大宽度
     * @param maxHeight         能够选择的醉倒高度
     * @param aspectRatio       图像的比例(pictureSize, 只有当pictureSize和textureSize保持一致, 才不会失真)
     * @return 最合适的预览尺寸
     */
    private Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // 存放小于等于限定尺寸, 大于等于texture控件尺寸的Size
        List<Size> bigEnough = new ArrayList<>();
        // 存放小于限定尺寸, 小于texture控件尺寸的Size
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                // option.getHeight() == option.getWidth() * h / w 用来保证
                // pictureSize的 w / h 和 textureSize的 w / h 一致
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // 1. 若存在bigEnough数据, 则返回最大里面最小的
        // 2. 若不存bigEnough数据, 但是存在notBigEnough数据, 则返回在最小里面最大的
        // 3. 上述两种数据都没有时, 返回空, 并在日志上显示错误信息
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }


    /**
     * 屏幕方向发生改变时调用转换数据方法
     *
     * @param viewWidth  mTextureView 的宽度
     * @param viewHeight mTextureView 的高度
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == mTextureView || null == mPreviewSize) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    /**
     * 检查权限
     *
     * @return
     */
    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return false;
        }
        return true;
    }

    private void requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            } else {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                creatFailDialog(getString(R.string.request_permission)).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    private AlertDialog creatFailDialog(String message) {
        return new AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .create();
    }


    /**
     * 创建预览对话
     */
    private void createCameraPreviewSession() {
        try {
            // 获取texture实例
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            // 设置宽度和高度
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            // 用来开始预览的输出surface
            Surface surface = new Surface(texture);
            // 预览请求构建
            final CaptureRequest.Builder mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            // 创建预览的捕获会话
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    // 相机关闭时, 直接返回
                    if (null == mCameraDevice) {
                        return;
                    }
                    if (mCaptureSession != null) {
                        mCaptureSession.close();
                    }

                    // 会话可行时, 将构建的会话赋给field
                    mCaptureSession = cameraCaptureSession;
                    try {
                        // 指定自动对焦模式为 CONTROL_AF_MODE_AUTO 模式，非点击对焦的时候，模式应该为 CONTROL_AF_MODE_CONTINUOUS_PICTURE 或者 CONTROL_AF_MODE_CONTINUOUS_VIDEO。
                        if (availableFocus) {
                            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
                            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
                        }

                        // 自动闪光
                        if (availableFlash) {
                            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                        }

                        // 构建上述的请求
                        CaptureRequest mPreviewRequest = mPreviewRequestBuilder.build();
                        // 重复进行上面构建的请求, 以便显示预览
                        try {
                            //等待1s，防止camera service未连接
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        mCaptureSession.setRepeatingRequest(mPreviewRequest, mPreviewCallback, childHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(Main3Activity.this, "配置失败！", Toast.LENGTH_SHORT).show();
                }
            }, childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 拍照
     */
    private void takePicture() {
        if (mCameraDevice == null) return;

        try {
            // 创建拍照需要的CaptureRequest.Builder
            final CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            // 将imageReader的surface作为CaptureRequest.Builder的目标
            captureRequestBuilder.addTarget(mImageReader.getSurface());
            // 自动对焦
            // 指定自动对焦模式为 CONTROL_AF_MODE_AUTO 模式，非点击对焦的时候，模式应该为 CONTROL_AF_MODE_CONTINUOUS_PICTURE 或者 CONTROL_AF_MODE_CONTINUOUS_VIDEO。
            if (availableFocus) {
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
                captureRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            }
            // 自动曝光
            if (availableFlash) {
                captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            }
            // 获取手机方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            // 根据设备方向计算设置照片的方向
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            //拍照
            CaptureRequest mCaptureRequest = captureRequestBuilder.build();
            if (mCaptureSession != null) {
                mCaptureSession.capture(mCaptureRequest, mCaptureCallback, childHandler);

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

}
