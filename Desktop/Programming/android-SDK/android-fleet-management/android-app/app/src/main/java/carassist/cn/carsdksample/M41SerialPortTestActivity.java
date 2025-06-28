package carassist.cn.carsdksample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.Nullable;
import android.util.Log;
import android.widget.TextView;

import java.nio.charset.Charset;
import java.util.Locale;

import carassist.cn.MyUart;
import com.fleetmanagement.custom.R;

public class M41SerialPortTestActivity extends Activity implements MyUart.UartRead {
    private static final String TAG = "M41SerialPortTestActivi";

    private TextView tvDisplay;

    public static int MSG_UART_CONNECT = 100;
    public static int MSG_UART_MSG_TEST = 101;
    public static int MSG_SHOW_DATA = 102;

    MyUart mMyUart;
    private byte[] mReceiveData;
    private int mReceiveSize;
    private String mWhich;

    private String[] sendData = {
            "55AA011802030005BAE3D0C7C39A0800E1AF77B1A335FB8B262BD94F22D5BCB0555B79AED778839A34D271170001012203140800040110012200050022000500004BC00000000BB800002710000088B800009C4001F401F4000000B400E6015901CC01CC010E018101F401F408FC08FC08FC08FC08FC08FC08FC08FC03E803E800004F56197C1DCC7475C641A887B7C5CC0E55F33724206ED5504810A16490AB42D1EC69C143E8A1114965AC000000000048415022033014000439010401100120000600200006000084C00000C801F403E8044C04B03232000000B400B400FA00FA00FA00FA00B400FA00FA00FA00FA03E803E803E803E803E803E803E803E803E803E8019001F40000000102030405060708090A0B0C0D0E0FA03BDD55AA",
            "a1a2a3a4a5a6a7a8a9a0b1b2b3b4b5b6b7b8b9b0c1c2c3c4c5c6c7c8c9c0d1d2d3d4d5d6d7d8d9d0e1e2e3e4e5e6e7e8e9e0f1f2f3f4f5f6f7f8f9f0112233",
            "a1a2a3a4a5a6a7a8a9a0b1b2b3b4b5b6b7b8b9b0c1c2c3c4c5c6c7c8c9c0d1d2d3d4d5d6d7d8d9d0e1e2e3e4e5e6e7e8e9e0f1f2f3f4f5f6f7f8f9f011223344",
            "a1a2a3a4a5a6a7a8a9a0b1b2b3b4b5b6b7b8b9b0c1c2c3c4c5c6c7c8c9c0d1d2d3d4d5d6d7d8d9d0e1e2e3e4e5e6e7e8e9e0f1f2f3f4f5f6f7f8f9f01122334455"
    };
    private int sendIndex = 0;
    @SuppressLint("HandlerLeak")
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_UART_CONNECT) {
                mHandler.removeMessages(MSG_UART_CONNECT);
                if (mMyUart != null) {
                    if (!mMyUart.connect()) {
                        Log.d(TAG, "handleMessage: connect fail");
                        mHandler.sendEmptyMessageDelayed(MSG_UART_CONNECT, 3000);
                    } else {
                        Log.d(TAG, "handleMessage: connect success");
                        mHandler.sendEmptyMessageDelayed(MSG_UART_MSG_TEST, 3000);
                    }
                }
            } else if (msg.what == MSG_UART_MSG_TEST) {
                mHandler.removeMessages(MSG_UART_MSG_TEST);
                // for test
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "send test data to " + MyUart.UART_2 + ", " + MyUart.UART_3);
                        String test = "receive uart2";
                        String test2 = "receive uart3";
                        mMyUart.sendData(Util.hexStr2Bytes(sendData[sendIndex++]), MyUart.UART_2);
                        // mMyUart.sendData(test2.getBytes(), MyUart.UART_3);
                        if (sendIndex < sendData.length)
                            mHandler.sendEmptyMessageDelayed(MSG_UART_MSG_TEST, 5000);
                    }
                }).start();
            } else if (msg.what == MSG_SHOW_DATA) {
                tvDisplay.setText(String.format(Locale.getDefault(), "%s/%d/%s", new String(mReceiveData,
                        Charset.defaultCharset()), mReceiveSize, mWhich));
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_m41_serial_port_test);
        tvDisplay = (TextView) findViewById(R.id.tvDisplay);
        mMyUart = new MyUart();
        mMyUart.setListener(this);
        mHandler.sendEmptyMessage(MSG_UART_CONNECT);
    }

    @Override
    public void onDataRead(byte[] data, int size, String which) {
        Log.d(TAG, "onDataRead: size:" + size + ", which:" + which);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < size) {
            sb.append(String.format(Locale.getDefault(), "%02x", data[i]));
            i++;
        }
        Log.d(TAG, "onDataRead: data:" + sb);
        mReceiveData = data;
        mReceiveSize = size;
        mWhich = which;
        // mHandler.sendEmptyMessage(MSG_SHOW_DATA);
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "onDisconnected: ");
        mHandler.removeMessages(MSG_UART_CONNECT);
        mHandler.sendEmptyMessageDelayed(MSG_UART_CONNECT, 3000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(MSG_UART_CONNECT);
        mHandler.removeMessages(MSG_UART_MSG_TEST);
    }
}
