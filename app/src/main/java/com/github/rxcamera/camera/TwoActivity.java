package com.github.rxcamera.camera;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class TwoActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, Camera.PictureCallback {

    private TextureView mTextureView;
    private Camera camera;
    private SurfaceTexture surface;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextureView = findViewById(R.id.TextureView);
        mTextureView.setOnClickListener(v -> {
            camera.autoFocus(null);
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        //获取相机对象
        if (camera == null) {
            camera = getCamera();
        }
        //设置监听
        mTextureView.setSurfaceTextureListener(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    /**
     * 拍摄按钮的点击事件
     *
     * @param view
     */
    public void capture(View view) {
        //修改相机参数,先获取相机默认参数
        Camera.Parameters parameters = camera.getParameters();
        //设置相机拍照的格式
        parameters.setPictureFormat(ImageFormat.JPEG);
        //设置预览大小
        parameters.setPreviewSize(800, 400);
        //设置对焦模式--自动对角
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        //获取最清晰的焦距之后再拍摄
        camera.autoFocus((success, cameras) -> {
            //如果自动对焦完成
            if (success) {
                //触发异步图像捕获,快门回调，原始图像回调，压缩图像回调
                camera.takePicture(null, null, this);
                releaseCamera();
                camera = getCamera();
                startPreview(surface);
            }
        });
    }

    /**
     * 获取相机实例
     *
     * @return
     */
    public Camera getCamera() {
        try {
            return camera = Camera.open();
        } catch (Exception e) {
            //这里捕获异常的原因是因为:如果同一台相机被其他应用程序打开，这将抛出一个RuntimeException。
            return null;
        }

    }

    /**
     * 开始绑定相机和显示表面
     *
     * @param surface
     */
    private void startPreview(SurfaceTexture surface) {
        try {
            //将表面的结构设置为实时预览。
            camera.setPreviewTexture(surface);
            //将预览显示的顺时针旋转设置为度数
            camera.setDisplayOrientation(90);
            // 开始捕捉并绘制预览帧到屏幕。
            camera.startPreview();
            //设置自动对焦距
            camera.autoFocus(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 释放相机资源
     */
    public void releaseCamera() {
        if (camera != null) {
            //预览回调设置为null
            camera.setPreviewCallback(null);
            // 停止捕捉并绘制预览帧到屏幕。并重新设置相机以备将来调用
            camera.stopPreview();
            //释放相机资源
            camera.release();
            camera = null;
        }
    }


    //======================================生命周期========================================================

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        this.surface = surface;
        startPreview(surface);
    }


    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        //重启整个功能
        camera.stopPreview();
        startPreview(surface);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        releaseCamera();
        return true;//销毁时候不再渲染
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }


//    ==============================另外一个拍摄后的回调==============================

    @Override//data：完整图像数据
    public void onPictureTaken(byte[] data, Camera caera) {
        File tempFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/temp/" + System.currentTimeMillis() + ".jpg");
        try {
            FileOutputStream stream = new FileOutputStream(tempFile);
            stream.write(data);
            stream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
