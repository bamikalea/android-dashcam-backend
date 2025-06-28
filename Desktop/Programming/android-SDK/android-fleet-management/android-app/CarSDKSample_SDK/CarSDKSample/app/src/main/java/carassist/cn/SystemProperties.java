package carassist.cn;

import android.util.Log;

import java.lang.reflect.Method;

public class SystemProperties {

    private static final String TAG = "SystemProperties";

    // String SystemProperties.get(String key, String def){}
    public static String get(String key, String def) {
        init();
        String value = def;
        try {
            value = (String) mGetMethod.invoke(mClassType, key, def);
        } catch (Exception e) {
            Log.e(TAG, "SystemProperties get:", e);
        }
        return value;
    }

    // boolean SystemProperties.getBoolean(String key, boolean def){}
    public static boolean getBoolean(String key, boolean def) {
        init();
        boolean value = def;
        try {
            Boolean v = (Boolean) mGetBooleanMethod.invoke(mClassType, key, def);
            value = v.booleanValue();
        } catch (Exception e) {
            Log.e(TAG, "SystemProperties getBoolean:", e);
        }
        return value;
    }

    // int SystemProperties.get(String key, int def){}
    public static int getInt(String key, int def) {
        init();
        int value = def;
        try {
            Integer v = (Integer) mGetIntMethod.invoke(mClassType, key, def);
            value = v.intValue();
        } catch (Exception e) {
            Log.e(TAG, "SystemProperties getInt:", e);
        }
        return value;
    }

    // void SystemProperties.get(String key, String def){}
    public static void set(String key, String val) {
        init();
        try {
            mSetMethod.invoke(mClassType, key, val);
        } catch (Exception e) {
            Log.e(TAG, "SystemProperties set:", e);
        }
    }

    // -------------------------------------------------------------------
    private static Class<?> mClassType = null;
    private static Method mGetMethod = null;
    private static Method mGetIntMethod = null;
    private static Method mGetBooleanMethod = null;
    private static Method mSetMethod = null;

    private static void init() {
        try {
            if (mClassType == null) {
                mClassType = Class.forName("android.os.SystemProperties");
                mGetMethod = mClassType.getDeclaredMethod("get", String.class, String.class);
                mGetIntMethod = mClassType.getDeclaredMethod("getInt", String.class, int.class);
                mGetBooleanMethod = mClassType.getDeclaredMethod("getBoolean", String.class, boolean.class);
                mSetMethod = mClassType.getDeclaredMethod("set", String.class, String.class);
            }
        } catch (Exception e) {
            Log.e(TAG, "SystemProperties error:", e);
        }
    }
}
