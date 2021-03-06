package com.github.rxcamera.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.Arrays;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


public class ThereActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, View.OnClickListener {

    private TextureView mTextureView;
    private boolean mFlashSupported;
    private String mCameraId;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private ImageView imageView;
    private CameraCharacteristics characteristics;
    private ImageReader mImageReader;
    private CameraCaptureSession mCameraCaptureSession;
    private long oldTime;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_there);
        //找到显示控件
        mTextureView = findViewById(R.id.textureView);
        //按钮设置点击事件
        findViewById(R.id.btn_cop).setOnClickListener(this);
        imageView = findViewById(R.id.imageView);
    }


    @Override
    public void onResume() {
        super.onResume();
        mTextureView.setSurfaceTextureListener(this);
    }


    //==================================SurfaceTexture生命周期回调===========================================================
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        openCamera();
    }

    @Override//当大小改变后回调
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override//销毁时候回调，返回值决定是否需要手动释放SurfaceTexture，true表示不用手动释放
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }


    /**
     * 打开相机
     */
    @SuppressLint("MissingPermission")
    private void openCamera() {
        //调用这个方法去初始化摄像头id，和摄像头是否支持闪光灯成员变量
        getCamera2Id();

        CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        //打开相机核心方法
        try {
            //参数1：需要打开相机的id，参数2：打开相机过程中的回调，参数3:可以传递一个null
            manager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                @Override//当摄像机设备完成打开时，这个方法就被调用了。
                public void onOpened(@NonNull CameraDevice camera) {
//                    代表系统摄像头。该类的功能类似于早期的Camera类。
                    mCameraDevice = camera;
                    //这里表示我们顺利打开了摄像头，接下来就是我们要预览
                    previewCamera();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                    mCameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    mCameraDevice = null;
                    finish();
                }

            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    /**
     * 开始预览
     * 预览需要通过Session去请求，而Session需要参数
     */
    private void previewCamera() {
        // 预览的输出Surface。
        Surface surface = new Surface(mTextureView.getSurfaceTexture());
        //预览过程中获取到的数据
        Size[] jpegSize = null;
        if (characteristics != null) {
            jpegSize = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
        }
        int with = 640;
        int height = 480;
        if (jpegSize != null && jpegSize.length > 0) {
            with = jpegSize[0].getWidth();
            height = jpegSize[0].getHeight();
        }
        mImageReader = ImageReader.newInstance(with, height, ImageFormat.JPEG, 1);
        mImageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireLatestImage();
            //我们可以将这帧数据转成字节数组，类似于Camera1的PreviewCallback回调的预览帧数据
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            image.close();
            Observable.just(data)
                    .subscribeOn(Schedulers.io())
//                    .filter(new Predicate<byte[]>() {
//                        @Override
//                        public boolean test(byte[] bytes) throws Exception {
//                            Log.e("rrrrrrrrrr", System.currentTimeMillis()+"rrrrrr");
//                            if (System.currentTimeMillis() - oldTime > 3000) {
//                                oldTime = System.currentTimeMillis();
//                                return true;
//                            } else {
//                                return false;
//                            }
//                        }
//                    })
                    .map(bytes -> {

                        return BitmapFactory.decodeByteArray(data, 0, data.length);
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(bitmap -> {
                        imageView.setImageBitmap(bitmap);
                    });

        }, null);


        //创建捕获请求
        try {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        //添加显示表面
        mPreviewRequestBuilder.addTarget(surface);
        //获取显示数据的表面添加
        mPreviewRequestBuilder.addTarget(mImageReader.getSurface());

        //创建会话
        try {
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {

                @Override//当摄像机设备完成配置时，这个方法就会被调用，并且会话可以开始处理捕获请求
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    bindPreview(session);
                }

                @Override//如果相机设备配置失败
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    /**
     * @param session
     */
    private void bindPreview(@NonNull CameraCaptureSession session) {
        mCameraCaptureSession = session;

        // 相机已经关闭
        if (null == mCameraDevice) {
            return;
        }
        // 自动对焦应
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        //闪光灯自动开启
        if (mFlashSupported) {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }

        // 转换层请求数据类型
        CaptureRequest mPreviewRequest = mPreviewRequestBuilder.build();


        try {
            //发起预览请求
            session.setRepeatingRequest(mPreviewRequest, null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取相机ID
     */
    private void getCamera2Id() {
        CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                //获取相机的相关参数
                characteristics = manager.getCameraCharacteristics(cameraId);
                // 不使用前置摄像头。
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }
                // 检查闪光灯是否支持。
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;
                mCameraId = cameraId;
                break;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            //获取摄像头列表可能出现的异常
        } catch (NullPointerException e) {
            Toast.makeText(this, "不支持Camera2API", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();

        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }

        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_cop:


                break;


            default:
                break;
        }

    }
}
