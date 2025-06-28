package com.miramems.carmotion;

import android.content.ComponentName;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;

public class carMotion {
    private static final String TAG = "CarMotion";
    private Handler mHandler = new Handler();
    private int mStateBak = -1;
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                int state = Get_Alg_State();
                //Log.d(TAG, "Get_Alg_State=" + state);
                if (state != mStateBak) {
                    mStateBak = state;
                    int event = state / 100;
                    int value = state % 100;
                    Log.d(TAG, "call HandleEvent(" + event + ", " + value + ")");
                    HandleEvent(event, value);
                }
            } catch (Exception e) {
                Log.e(TAG, "Get_Alg_State Exception: " + e);
            } catch (java.lang.UnsatisfiedLinkError err) {
                Log.e(TAG, "Get_Alg_State UnsatisfiedLinkError" );
            }
            mHandler.removeCallbacks(mRunnable);
            mHandler.postDelayed(mRunnable, 250);
        }
    };

    public class CarMotionAlgorithm {
        public static final int NONE = 0;
        public static final int VIDEOING = 1;
        public static final int PARKING_T = 2;
        public static final int VIOLENT = 3;
        public static final int STATE = 4;
        public static final int JOLT = 5;
    }

    public class CarMotionEVENT {
        public static final int NONE_NOTIFY = 0;
        public static final int VIDEOING_NOTIFY = 1;
        public static final int PARKING_NOTIFY = 2;
        public static final int VIOLENT_NOTIFY = 3;
        public static final int STATE_NOTIFY = 4;
        public static final int JOLT_NOTIFY = 5;
    }

    public class CarMotionSwitchCmd {
        public static final int DISABLE_X = 0;
        public static final int ENABLE_X = 1;
    }

    public class CarMotionViolent {
        public static final int VIOLENT_NONE = 0;
        public static final int VIOLENT_SPEED_UP = 1;
        public static final int VIOLENT_SPEED_DOWN = 2;
        public static final int VIOLENT_TURN_LEFT = 3;
        public static final int VIOLENT_TURN_RIGHT = 4;
    }

    public class CarMotionPinNum {
        public static final int PIN_NONE = 0;
        public static final int PIN_ONE = 1;
        public static final int PIN_TWO = 2;
    }

    public class CarMotionPinLevel {
        public static final int NONE = 0;
        public static final int PIN_LEVEL_LOW = 1;
        public static final int PIN_LEVEL_HIGH = 2;
    }

    public class CarMotionPinLatch { // 门闩,上锁
        public static final int LATCH_NONE = 0;
        public static final int LATCH_250MS = 1;
        public static final int LATCH_500MS = 2;
        public static final int LATCH_1S = 3;
        public static final int LATCH_2S = 4;
        public static final int LATCH_4S = 5;
        public static final int LATCH_8S = 6;
        public static final int LATCH_1MS = 10;
        public static final int LATCH_2MS = 11;
        public static final int LATCH_25MS = 12;
        public static final int LATCH_50MS = 13;
        public static final int LATCH_100MS = 14;
        public static final int LATCH_EVER = 15;
    }

    ;

    public interface carMotionEventListener {

        void OncarMotionEvent(int event, int value);
    }

    private ArrayList<carMotionEventListener> mListenerArray = new ArrayList<carMotionEventListener>();

    public void RegisteOncarMotionEventListener(carMotionEventListener I) {
        mListenerArray.add(I);
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(mRunnable, 250);
    }

    public void UnRegisteOncarMotionEventListener(carMotionEventListener I) {
        mListenerArray.remove(I);
        if (mListenerArray.size() > 0) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    public void HandleEvent(int event, int data) {

        for (carMotionEventListener I : mListenerArray) {
            I.OncarMotionEvent(event, data);
        }
    }

    public native int Init(int videoing_pin_num, int videoing_pin_level, int parking_pin_num, int parking_pin_level);

    public native void Control(int algorithm, int cmd);

    public native int QueryControl(int algorithm);

    public native void Parking_Set_Parma(int threshold, int latch_mode, int duration);

    public native void Videoing_Set_Parma(int threshold, int latch_mode, int duration);

    public native void Violent_Set_Parma(int level);

    public native void Direction_Set_Parma(int dir);

    public native void Slope_Set_Parma(int level, int min, float angle_f, float angle_t);

    public native void Set_Debug_Level(int level);

    public native int Get_Alg_State();

    static {
        System.loadLibrary("carMotion");
    }

}
