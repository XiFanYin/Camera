package com.github.rxcamera.camera;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;

public class OneActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.one_activity);
    }

    public void capture(View view) {
        new RxPermissions(this)
                .request(Manifest.permission.CAMERA)
                .subscribe(aBoolean -> {
                    if (aBoolean) {
                        Intent intnet = new Intent(this, TwoActivity.class);
                        startActivity(intnet);
                    } else {
                        Toast.makeText(this, "没有打开相机权限", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    public void capture2(View view) {
        new RxPermissions(this)
                .request(Manifest.permission.CAMERA)
                .subscribe(aBoolean -> {
                    if (aBoolean) {
                        Intent intnet = new Intent(this, ThereActivity.class);
                        startActivity(intnet);
                    } else {
                        Toast.makeText(this, "没有打开相机权限", Toast.LENGTH_SHORT).show();
                    }
                });
    }



}
