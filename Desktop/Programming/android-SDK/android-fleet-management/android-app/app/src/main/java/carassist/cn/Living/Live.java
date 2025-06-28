/*
 * Copyright 2016,2017 www.carassist.cn. All Rights Reserved.
 *
 * This PROPRIETARY SOFTWARE is the property of Prolink Network Shenzhen Technologies, Inc.
 * and may contain trade secrets and/or other confidential information of
 * Prolink Technologies, Inc. This file shall not be disclosed to any third party,
 * in whole or in part, without prior written consent of Prolink.
 *
 * THIS PROPRIETARY SOFTWARE AND ANY RELATED DOCUMENTATION ARE PROVIDED AS IS,
 * WITH ALL FAULTS, AND WITHOUT WARRANTY OF ANY KIND EITHER EXPRESS OR IMPLIED,
 * AND Prolink Network Shenzhen Technologies, INC. DISCLAIMS ALL EXPRESS OR IMPLIED WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, QUIET ENJOYMENT OR NON-INFRINGEMENT.
 */

package carassist.cn.Living;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import carassist.cn.CarIntents;

/**
 * The API interface for live preview.
 * This class is used in device mirror side for uploading camera's live stream video/audio data to aliyun oss.
 *
 * On the other side the phone application will retrieve the stream data then do the live playing.
 *
 * the APK call this Live Interface need the private keep-alive mechiszm with phone side.
 * Becuase if phone Application startLiveUpload and then crashed
 * Device side will never receive the stopLiveUpload command, and keep uploading the data forever.
 *
 */
public final class Live {
    private static final String TAG = "Live";
    private static final int MSG_SERVER_PING = 100;
    private static final int PING_INTERVAL = (30 * 60 * 1000);

    private final Context mContext;
    private final String mAccessKeyID;
    private final String mAccessKeySecret;
    private final String[] mDomains;
    private String mFastestDomain;

    /**
     * Init Live object
     * @param ctx android context object
     * @param keyID aliyun oss access key id, such as "LTAad930393zckei"
     * @param keySecret aliyun oss access key secret, such as "Jok30xlkd03kx929xkadkexkyddZY"
     * @param domains all of the oss bulkets domains, such as "bulketname1.oss-cn-qingdao.aliyuncs.com,bulketname2.oss-cn-hangzhou.aliyuncs.com"
     */
    public Live(Context ctx, String keyID, String keySecret, String[] domains){
        mContext = ctx.getApplicationContext();
        mAccessKeyID = keyID;
        mAccessKeySecret = keySecret;
        mDomains = domains;

        //load previous fastest domain
        SharedPreferences pref = ctx.getSharedPreferences("live", Context.MODE_PRIVATE);
        mFastestDomain = pref.getString("fastest", "");
        if(mFastestDomain.length() < 1) {
            //choose first one as fastest
            mFastestDomain = mDomains[0];
        }

        if (mDomains.length > 1) {
            //start a thread to detect fastest domain by ping the domains
            mHandler.sendEmptyMessage(MSG_SERVER_PING);
        }
    }

    /**
     * front camera id
     */
    public static final int CameraFront = 'F';
    /**
     * back(rear) camera id
     */
    public static final int CameraBack = 'B';


    /**
     * start to upload camera's live stream to fastest domain, with a unique file name
     * you can only upload one camera at the same time.
     * Note, if front camera is previewing, we can't do rear-carame live upload.
     * @cameraID  CameraFront or CameraBack
     * @param uniqueFileName the unique file name for uploading, such as "deviceid/20170412202912", every device must choose a difference filename
     * Returns the fastest OSS domain
     */
    public String startLiveUpload(int cameraID, String uniqueFileName) {
//        if(!Live.isValidNetworkConnected(mContext)) {
//            return null;
//        }

        String endpoint = mContext.getSharedPreferences("live", 0).getString("fastest", mDomains[0]);
        String bucket = endpoint.substring(0, endpoint.indexOf('.'));
        String ep = endpoint.substring(endpoint.indexOf('.') + 1); // skip the bucket name

        String URL = "http://endpoint/http://" + ep + "/bucket/" + bucket + "/keyid/"
                + mAccessKeyID + "/keysecret/" + mAccessKeySecret + "/file/" + uniqueFileName;

        Intent intent = new Intent(CarIntents.ACTION_CAMERA_LIVING);
        intent.putExtra("action", 1);
        intent.putExtra("camid", cameraID);
        intent.putExtra("url", URL);

        mContext.sendBroadcast(intent);

        return endpoint;
    }

    /**
     * Stop the camera live uploading.
     */
    public void stopLiveUpload(int cameraID) {
        Intent intent = new Intent(CarIntents.ACTION_CAMERA_LIVING);
        intent.putExtra("action", 0);
        intent.putExtra("camid", cameraID);
        intent.putExtra("url", "");
        mContext.sendBroadcast(intent);
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SERVER_PING:
                    pingServers();
                    sendEmptyMessageDelayed(MSG_SERVER_PING, PING_INTERVAL);
                    break;
            }
        }
    };

    private void pingServers() {

//        if(!isValidNetworkConnected(mContext)) {
//            return;
//        }

        new Thread(new Runnable() {

            @Override
            public void run() {
                int minIndex = 0;
                long minTime = Long.MAX_VALUE;
                for (int i = 0; i < mDomains.length; i++) {
                    long ret = pingServer(mDomains[i]);
                    if (ret < minTime) {
                        minTime = ret;
                        minIndex = i;
                    }
                }
                // Log.d(TAG, "The fatest server is " + mOssServers[minIndex]);
                mContext.getSharedPreferences("live", 0).edit().putString("fastest", mDomains[minIndex])
                        .putLong("pingtime", System.currentTimeMillis()).apply();
            }
        }).start();
    }

    private long pingServer(String mDomain) {
        long aveTime = 0;
        int retryTimes = 10;
        long[] aveTimes = new long[retryTimes];
        for (int i = 0; i < retryTimes; i++) {
            aveTimes[i] = isEasyAccessed(mDomain);
        }
        int maxTimeIndex = 0, minTimeIndex = 0;

        for (int i = 1; i < retryTimes; i++) {
            if (aveTimes[i] > aveTimes[maxTimeIndex])
                maxTimeIndex = i;
            if (aveTimes[i] < aveTimes[minTimeIndex])
                minTimeIndex = i;
        }

        for (int i = 0; i < retryTimes; i++) {
            if (i != maxTimeIndex && i != minTimeIndex)
                aveTime += aveTimes[i];
        }
        aveTime = aveTime / (retryTimes - 2);
        // Log.d(TAG, "ping " + hostName + " ave delay is " + aveTime + "ms");
        return aveTime;
    }

    private long isEasyAccessed(String hostName) {
        try {
            long startTime = System.currentTimeMillis();
            InetSocketAddress ia = new InetSocketAddress(InetAddress.getByName(hostName), 80);
            SocketChannel sc = SocketChannel.open();
            sc.socket().connect(ia, 5000);
            long delay = System.currentTimeMillis() - startTime;
            Log.d(TAG, "ping " + hostName + " delay is " + delay);
            return delay;

        } catch (IOException ie) {
            ie.printStackTrace();
        }
        return Long.MAX_VALUE;
    }

    /*
    * only mobile network support Living if needed
    * */
    private static boolean isValidNetworkConnected(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.getState() == NetworkInfo.State.CONNECTED) {
                return true;
            }
        }
        return false;
    }


}