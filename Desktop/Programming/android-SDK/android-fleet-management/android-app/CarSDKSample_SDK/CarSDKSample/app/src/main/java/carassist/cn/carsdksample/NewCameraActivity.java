package carassist.cn.carsdksample;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.io.IOException;

public class NewCameraActivity extends Activity {

    private static final String TAG = "NewCameraActivity";
    private Camera mCamera1, mCamera2;
    private SurfaceView mView1, mView2;
    SurfaceHolder.Callback callback1 = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {

            Log.d(TAG, "surfaceCreated: 1");
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, "surfaceChanged: 1");
            mCamera1.startPreview();
            try {
                mCamera1.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed: 1");
            try {
                //这里不set null前路就会黑
                mCamera1.setPreviewDisplay(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    SurfaceHolder.Callback callback2 = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated: 2");
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, "surfaceChanged: 2");
            mCamera2.startPreview();
            try {
                mCamera2.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed: 2");
            try {
                mCamera2.setPreviewDisplay(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_camera);
        mView1 = (SurfaceView) findViewById(R.id.view1);
        mView2 = (SurfaceView) findViewById(R.id.view2);
        mView1.getHolder().addCallback(callback1);
        mView2.getHolder().addCallback(callback2);

        mCamera1 = Camera.open(0);
        mCamera2 = Camera.open(1);

        findViewById(R.id.btnFront).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mView1.setVisibility(View.VISIBLE);
                mView2.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.btnDouble).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mView1.setVisibility(View.VISIBLE);
                mView2.setVisibility(View.VISIBLE);
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mView1.setVisibility(View.GONE);
                mView2.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
        mCamera1.release();
        mCamera2.release();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: ");
        super.onPause();


//        mCamera1.stopPreview();
//        mCamera2.stopPreview();
    }
}
