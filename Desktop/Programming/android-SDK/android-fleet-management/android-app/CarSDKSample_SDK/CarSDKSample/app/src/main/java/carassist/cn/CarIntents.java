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

package carassist.cn;

public final class CarIntents {

    //系统已经“唤醒”时将发出如下广播，请对应接收处理:
    public static final String ACTION_WAKEUP = "com.car.wakeup";

    //系统即将“休眠”时将发出如下广播，请对应接收处理:
    public static final String ACTION_GOTOSLEEP = "com.car.gotosleep";

    public static final String ACTION_NAVI_GO = "com.car.navi.go";
    public static final String EXTRA_NAVI_GO_LAT = "lat";
    public static final String EXTRA_NAVI_GO_LNG = "lng";
    public static final String EXTRA_NAVI_GO_ADDR = "addr";
    public static final String EXTRA_NAVI_GO_TYPE = "coordtype";

    public static final String ACTION_NET_FIREWALL = "com.car.firewall";

    public static final String ACTION_VOICE_CMD = "com.car.voicecmd";
    public static final String EXTRA_VOICE_CMD_ID = "id";
    public static final String EXTRA_VOICE_CMD_MSG = "msg";

    public static final String ACTION_APK_INSTALL = "android.intent.action.SILENCE_INSTALL";
    public static final String EXTRA_PATH_INSTALL = "appPath";
    public static final String EXTRA_PACKAGE_INSTALL = "packageName";
    public static final String EXTRA_CLASS_INSTALL = "className";
    public static final String EXTRA_START_INSTALL = "autostart";

    public static final String ACTION_SD_FORMAT = "com.car.sdformat";

    public static final String ACTION_SOFTAP_CONFIG = "com.car.apconfig";
    public static final String EXTRA_SSID_SOFTAP = "ssid";
    public static final String EXTRA_PWD_SOFTAP = "pwd";

    public static final String ACTION_FORCE_SLEEP = "com.car.launcher.FORCE_SLEEP";

    public static final String ACTION_CAMERA_LIVING = "com.car.camera.living";
    public static final int CAMERA_FRONT = 'F';
    public static final int CAMERA_BACK = 'B';

    public static final String ACTION_MONITOR_NOTIFY = "com.car.monitor.notify";

    public static final String ACTION_RECORD_FILE = "com.car.record.file";
    public static final String ACTION_DELETE_FILE = "com.car.delete.file";
    public static final String ACTION_CAMERA_LIVING_CALLBACK = "com.car.camera.living.callback";
    public static final String CAMERA_LIVING_BROADCAST_RESULT  =    "result";
    public static final String CAMERA_LIVING_BROADCAST_ERROR   =    "error";
    public static final String CAMERA_LIVING_BROADCAST_ERROR_NUMBER  =  "errno";
    public static final String CAMERA_LIVING_BROADCAST_BITRATE       =  "bitrate";
    public static final String CAMERA_LIVING_BROADCAST_CAMERAID      =  "camid";

    public static final String ACTION_RECORDING_STORAGE_SLOW = "com.car.writestorage.slow";

    public static final String ACTION_SET_PROP = "com.car.setprop";
    public static final String EXTRA_SET_PROP_KEY = "key";
    public static final String EXTRA_SET_PROP_VAL = "val";
    
    public static final String ACTION_CAMERA_SNAPSHOT = "com.car.camera.snapshot";
    public static final String ACTION_CAMERA_SNAPSHOT_CALLBACK = "com.car.camera.snapshot.callback";

    public static final String ACTION_CAPTURE_CUSTOM_VIDEO = "com.car.custom.capture";

    public static final String ACTION_CAPTURE_FILE_INFO = "com.car.capture.file";
}

