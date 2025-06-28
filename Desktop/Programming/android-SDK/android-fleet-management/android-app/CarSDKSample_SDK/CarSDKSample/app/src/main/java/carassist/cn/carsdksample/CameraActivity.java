package carassist.cn.carsdksample;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.TextureView;
import android.view.View;

import java.io.File;
import java.io.FileInputStream;

public class CameraActivity extends Activity implements TextureView.SurfaceTextureListener, View.OnClickListener {
    private static final String TAG = "API.CameraActivity";

    private Camera mCamera1, mCamera2;
    private TextureView mView1, mView2;

    private int mFirstCamID = 0;
    private Handler mHandler = new Handler();
    private Runnable mCamSwitcher = new Runnable() {
        @Override
        public void run() {
            try {
                Log.d(TAG, "mCamSwitcher run ... mFirstCamID=" + mFirstCamID);
                if (mFirstCamID == 0) {
                    mCamera1.setPreviewTexture(mView1.getSurfaceTexture());
                    if (mCamera2 != null) mCamera2.setPreviewTexture((mView2 == null) ? null : mView2.getSurfaceTexture());
                } else {
                    mCamera1.setPreviewTexture((mView2 == null) ? null : mView2.getSurfaceTexture());
                    if (mCamera2 != null) mCamera2.setPreviewTexture(mView1.getSurfaceTexture());
                }
            } catch (Exception e) {
                Log.e(TAG, "mCamSwitcher Exception: " + e);

                try {
                    mCamera1.setPreviewTexture(null);
                    if (mCamera2 != null) mCamera2.setPreviewTexture(null);
                } catch (Exception e2) {
                    Log.e(TAG, "setPreviewTexture(null) in Runnable Exception: " + e2);
                }

                Log.d(TAG, "mCamSwitcher retry ... ");
                mHandler.postDelayed(mCamSwitcher, 200);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        mView1 = (TextureView)findViewById(R.id.view1);
        mView2 = (TextureView)findViewById(R.id.view2);

        mView1.setSurfaceTextureListener(this);
        mView1.setOnClickListener(this);
        mView2.setSurfaceTextureListener(this);
        mView2.setOnClickListener(this);

        mCamera1 = Camera.open(0);
        mCamera1.startPreview();

        int camID = get2ndCameraID();
        Log.d(TAG, "2ndCameraID=" + camID);
        if (camID >= 0) {
            mCamera2 = Camera.open(camID);
            mCamera2.startPreview();
        }

        // 把mView2置为null之后就是一个view切换预览两个摄像头
        mView2.setVisibility(View.GONE);
        mView2 = null;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureAvailable");
        mHandler.postDelayed(mCamSwitcher, 50);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureDestroyed");
        try {
            mCamera1.setPreviewTexture(null);
            if (mCamera2 != null) mCamera2.setPreviewTexture(null);
        } catch (Exception e) {
            Log.e(TAG, "setPreviewTexture(null) in onSurfaceTextureDestroyed Exception: " + e);
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        //Log.d(TAG, "onSurfaceTextureUpdated");
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick");
        if (mFirstCamID == 0) {
            mFirstCamID = 1;
        } else {
            mFirstCamID = 0;
        }

        try {
            mCamera1.setPreviewTexture(null);
            if (mCamera2 != null) mCamera2.setPreviewTexture(null);
        } catch (Exception e) {
            Log.e(TAG, "setPreviewTexture(null) Exception: " + e);
        }

        mHandler.removeCallbacks(mCamSwitcher);
        mHandler.postDelayed(mCamSwitcher, 100);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        mHandler.postDelayed(mCamSwitcher, 1000);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        mHandler.removeCallbacks(mCamSwitcher);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        try {
            mCamera1.stopPreview();
            mCamera1.setPreviewTexture(null);
            mCamera1.release();
            if (mCamera2 != null) {
                mCamera2.stopPreview();
                mCamera2.setPreviewTexture(null);
                mCamera2.release();
            }
        } catch (Exception e) {
            Log.e(TAG, "onDestroy Exception: " + e);
        }
        mHandler.removeCallbacksAndMessages(null);
    }

    public static boolean cvbsExists() {
        boolean exists = false;

        // cat /sys/devices/virtual/misc/cvbs/status
        String value = readSysValue("/sys/devices/virtual/misc/cvbs/status");
        if (value != null && value.startsWith("1")) {
            exists = true;
        }
        //Log.d(TAG, "exists: cvbs/statu= " + value + ", exists = " + exists);
        return exists;
    }

    public static boolean usbExists() {
        for (int i=0; i<10; i++) {
            try {
                File file = new File("/dev/video" + i);
                if (file.exists()) {
                    return true;
                }
            } catch (Exception ex) {
            }
        }

        return false;
    }

    public static String readSysValue(String node) {
        try {
            byte[] bytes = new byte[64];
            FileInputStream fis = new FileInputStream(node);
            int len = fis.read(bytes);
            fis.close();
            String value = new String(bytes, 0, len);
            return value;
        } catch (Exception e) {
            // fail silently
        }
        return null;
    }

    public static int get2ndCameraID() {
        int sdk = android.os.Build.VERSION.SDK_INT;
        Log.d(TAG, "get2ndCameraID, sdk=" + sdk);
        if (sdk == 22) { // 4G - mt6735
            if (cvbsExists()) {
                return 1;
            } else {
                if (usbExists()) {
                    return 2;
                } else {
                    return -1;
                }
            }
        }

        // 3G
        if (usbExists()) {
            return 1;
        } else {
            return -1;
        }
    }
}
