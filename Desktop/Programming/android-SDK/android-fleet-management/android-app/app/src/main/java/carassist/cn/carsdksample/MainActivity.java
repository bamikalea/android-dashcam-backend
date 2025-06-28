package carassist.cn.carsdksample;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.miramems.carmotion.carMotion;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;

import carassist.cn.API;
import carassist.cn.CarIntents;
import carassist.cn.Living.Live;
import carassist.cn.Living.LocalLiving;
import com.fleetmanagement.custom.R;

public class MainActivity extends Activity implements API.CarMotionListener {
    private static final String TAG = "API.MainActivity";
    Button mTakePic, mTakeVideo, mViewFront, mViewRear, mStartFrontLiving, mStopFrontLiving;
    TextView mMsgShow;
    API mApi;
    Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Start the Fleet Management Server Communication Service
        Intent serviceIntent = new Intent(this, com.fleetmanagement.custom.services.ServerCommunicationService.class);
        startService(serviceIntent);
        Log.i(TAG, "Started ServerCommunicationService");

        mTakePic = (Button) findViewById(R.id.takepic);
        mTakeVideo = (Button) findViewById(R.id.takevideo);
        mViewFront = (Button) findViewById(R.id.viewfront);
        mViewRear = (Button) findViewById(R.id.viewrear);
        mStartFrontLiving = (Button) findViewById(R.id.startLiving);
        mStopFrontLiving = (Button) findViewById(R.id.stopLiving);
        mViewFront.setEnabled(false);
        mViewRear.setEnabled(false);
        mMsgShow = (TextView) findViewById(R.id.msgshow);
        mApi = new API(this);
        mApi.setAutoSleepTime(0);
        mApi.registerCarMotionListener(this);

        final LocalLiving mLocalLiving = new LocalLiving(this, CarIntents.CAMERA_FRONT, null); // local tcp living
        final Live mCamLiving; // oss living

        // FIXME: write the correct OSS info here before support oss living
        String OSSAccessKeyID = "xxx";
        String OSSAccessKeySecret = "xxxxxx";
        String[] OSSDomains = { "liveshenzhen.xxx", "livehangzhou.xxx", "liveqingdao.xxx" }; // FIXME: set all the oss
                                                                                             // domain if exist, we will
                                                                                             // choose the fastest one
        String url = "xxx.xxx.aliyuncs.com";
        // end

        String filePath = "/sdcard/Pictures/F2017_04_27_145528.jpg";
        mApi.uploadFile2Oss(filePath, new File(filePath).getName(), url, OSSAccessKeyID, OSSAccessKeySecret);

        mTakePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mViewFront.setEnabled(false);
                mViewRear.setEnabled(false);
                mApi.takePicture(0, new API.TakeCallback() {
                    @Override
                    public void onTakeProgress(final int progressPrecentage) {
                        mMsgShow.setText("taking picture: " + progressPrecentage + "%");
                    }

                    @Override
                    public void onTakeResult(final String jsonString) {
                        mMsgShow.setText("take picture result: " + jsonString);
                        try {
                            JSONTokener tokener = new JSONTokener(jsonString);
                            JSONObject joResult = new JSONObject(tokener);
                            if (joResult.has("imgurl")) {
                                if (joResult.getString("imgurl").length() > 0) {
                                    final String imgPath = joResult.getString("imgurl");
                                    mViewFront.setEnabled(true);
                                    mViewFront.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Intent intent = new Intent();
                                            intent.setAction(Intent.ACTION_VIEW);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            Uri uri = Uri.fromFile(new File(imgPath));
                                            intent.setDataAndType(uri, "image/*");
                                            startActivity(intent);
                                        }
                                    });
                                }
                            }

                            if (joResult.has("imgurlrear")) {
                                if (joResult.getString("imgurlrear").length() > 0) {
                                    final String imgPath = joResult.getString("imgurlrear");
                                    mViewRear.setEnabled(true);
                                    mViewRear.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Intent intent = new Intent();
                                            intent.setAction(Intent.ACTION_VIEW);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            Uri uri = Uri.fromFile(new File(imgPath));
                                            intent.setDataAndType(uri, "image/*");
                                            startActivity(intent);
                                        }
                                    });
                                }
                            }
                        } catch (Exception e) {
                            // ignore this error
                            e.printStackTrace();
                        }

                    }
                });
            }
        });

        mTakeVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mViewFront.setEnabled(false);
                mViewRear.setEnabled(false);
                mApi.takeVideo(0, 10, 10, new API.TakeCallback() {
                    @Override
                    public void onTakeProgress(final int progressPrecentage) {
                        mMsgShow.setText("taking video: " + progressPrecentage + "%");

                    }

                    @Override
                    public void onTakeResult(final String jsonString) {
                        mMsgShow.setText("takd video result: " + jsonString);
                        try {
                            JSONTokener tokener = new JSONTokener(jsonString);
                            JSONObject joResult = new JSONObject(tokener);
                            if (joResult.has("videourl")) {
                                if (joResult.getString("videourl").length() > 0) {
                                    final String imgPath = joResult.getString("videourl");
                                    mViewFront.setEnabled(true);
                                    mViewFront.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Intent intent = new Intent();
                                            intent.setAction(Intent.ACTION_VIEW);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            Uri uri = Uri.fromFile(new File(imgPath));
                                            intent.setDataAndType(uri, "video/*");
                                            startActivity(intent);
                                        }
                                    });
                                }
                            }

                            if (joResult.has("videourlrear")) {
                                if (joResult.getString("videourlrear").length() > 0) {
                                    final String imgPath = joResult.getString("videourlrear");
                                    mViewRear.setEnabled(true);
                                    mViewRear.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Intent intent = new Intent();
                                            intent.setAction(Intent.ACTION_VIEW);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            Uri uri = Uri.fromFile(new File(imgPath));
                                            intent.setDataAndType(uri, "video/*");
                                            startActivity(intent);
                                        }
                                    });
                                }
                            }
                        } catch (Exception e) {
                            // ignore this error
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        mCamLiving = new Live(getApplicationContext(), OSSAccessKeyID, OSSAccessKeySecret, OSSDomains);

        mStartFrontLiving.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // tcp living
                // mLocalLiving.startLiving();

                // oss living
                String ep = mCamLiving.startLiveUpload(Live.CameraFront, "TWNFC66DRWAAHU9D_0123456789");
                Log.d(TAG, "startLivingUpload endpoint = " + ep);

                // FIXME: pass the ep and uniqueFileName information to phone side
                // phone side write these info to OSS library, and then start living

            }
        });

        mStopFrontLiving.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // tcp living
                // mLocalLiving.stopLiving();

                // oss living
                mCamLiving.stopLiveUpload(Live.CameraFront);
            }
        });

        findViewById(R.id.cameraPreview).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, CameraActivity.class);
                MainActivity.this.startActivity(intent);
            }
        });
    }

    public void testUART(View v) {
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, M41SerialPortTestActivity.class);
        MainActivity.this.startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        // mApi.registerCarMotionListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        // mApi.unregisterCarMotionListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        mApi.unregisterCarMotionListener(this);
    }

    public void onViolentEvent(final int value) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String event = "";
                switch (value) {
                    case carMotion.CarMotionViolent.VIOLENT_SPEED_DOWN:
                        event = "speed down";
                        break;

                    case carMotion.CarMotionViolent.VIOLENT_SPEED_UP:
                        event = "speed up";
                        break;

                    case carMotion.CarMotionViolent.VIOLENT_TURN_LEFT:
                        event = "turn left";
                        break;

                    case carMotion.CarMotionViolent.VIOLENT_TURN_RIGHT:
                        event = "turn right";
                        break;

                    default:
                        event = "unknown event";
                        break;
                }

                mMsgShow.setText("CarMotion Event: " + event);
                Log.d(TAG, event);
            }
        });
    }
}
