package carassist.cn;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class MyUart {
    private static final String TAG = "MyUart";
    public static final String SocketName = "uart";

    public static final String UART_1 = "uart1";
    public static final String UART_2 = "uart2";
    public static final String UART_3 = "uart3";

    InputStream mIn = null;
    OutputStream mOut = null;
    UartRead mUartReadListener = null;
    private final byte[] mTempBuffer = new byte[4];

    public interface UartRead {
        void onDataRead(byte[] data, int size, String which);
        void onDisconnected();
    }

    public MyUart() {

    }

    public void setListener(UartRead l) {
        mUartReadListener = l;
    }

    public boolean connect() {
        if (mIn != null) {
            try {
                mIn.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mIn = null;
        }
        if (mOut != null) {
            try {
                mOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mOut = null;
        }
        LocalSocket s = null;
        LocalSocketAddress l;

        s = new LocalSocket();
        l = new LocalSocketAddress(SocketName, LocalSocketAddress.Namespace.RESERVED);
        try {
            s.connect(l);
            mIn = s.getInputStream();
            mOut = s.getOutputStream();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            int group = readInt32(mIn);
                            if (group == 3) {
                                String which = readString(mIn);

                                if (which != null) {
                                    //for demo
                                    if (which.equals(UART_2)) {
                                        int size = readInt32(mIn);
                                        if (size > 0) {
                                            int odd = readInt32(mIn);
                                            byte[] data = new byte[size + odd];
                                            readUntil(mIn, size + odd, data);
                                            if (mUartReadListener != null) {
                                                mUartReadListener.onDataRead(data, size, which);
                                            }
                                        }
                                    } else if (which.equals(UART_1)) {
                                    } else if (which.equals(UART_3)) {
                                        int size = readInt32(mIn);
                                        if (size > 0) {
                                            int odd = readInt32(mIn);
                                            byte[] data = new byte[size + odd];
                                            readUntil(mIn, size + odd, data);
                                            if (mUartReadListener != null) {
                                                mUartReadListener.onDataRead(data, size, which);
                                            }
                                        }
                                    } else {
                                        Log.d("API", "Not support now: " + which);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            try {
                                if (mIn != null)
                                    mIn.close();
                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                            mIn = null;
                            if (mUartReadListener != null) {
                                mUartReadListener.onDisconnected();
                            }
                            break;
                        }
                    }
                }
            }).start();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void int2Bytes(int value, byte[] out, int offset) {
        out[offset+0] = (byte) (value >>> 24);
        out[offset+1] = (byte) (value >>> 16);
        out[offset+2] = (byte) (value >>> 8);
        out[offset+3] = (byte) (value & 0xff);
    }

    void appendInt32(int value, ByteArrayOutputStream baos) throws IOException {
        int2Bytes(4, mTempBuffer, 0);
        baos.write(mTempBuffer);
        int2Bytes(value, mTempBuffer, 0);
        baos.write(mTempBuffer);
    }

    void appendString(String str, ByteArrayOutputStream baos) throws IOException {
        int val = str.length();
        int mod = val % 4;
        if (mod > 0) {
            val += 4 - mod;
        }
        int2Bytes(val, mTempBuffer, 0);
        baos.write(mTempBuffer);
        baos.write(str.getBytes());
        if (mod > 0) {
            for (int i = 0; i < 4 - mod; i++) {
                mTempBuffer[0] = 0;
                baos.write(mTempBuffer, 0, 1);
            }
        }
    }

    void appendBytes(byte[] data, ByteArrayOutputStream baos) throws IOException {
        int val = data.length;
        int mod = val % 4;
        int odd = 0;
        if (mod > 0) {
            odd = 4 - mod;
        }
        appendInt32(val, baos);
        if (val > 0) {
            appendInt32(odd, baos);
            baos.write(data);
            for (int i = 0; i < odd; i++) {
                mTempBuffer[0] = 0;
                baos.write(mTempBuffer, 0, 1);
            }
        }
    }

    int bytes2Int(byte[] bytes) {
        int ret;
        int temp0 = bytes[0] & 0xFF;
        int temp1 = bytes[1] & 0xFF;
        int temp2 = bytes[2] & 0xFF;
        int temp3 = bytes[3] & 0xFF;
        ret = ((temp0 << 24) + (temp1 << 16) + (temp2 << 8) + temp3);
        return ret;
    }

    String readString(InputStream in) throws IOException {
        int n = readUntil(in, 4, mTempBuffer);
        if (n == 4) {
            int len = bytes2Int(mTempBuffer);
            byte[] str = new byte[len];
            readUntil(in, len, str);
            return new String(str).trim();
        }
        return null;
    }
    int readInt32(InputStream in) throws IOException {
        int n = readUntil(in, 4, mTempBuffer);
        if (n == 4) {
            n = readUntil(in, 4, mTempBuffer);
            return bytes2Int(mTempBuffer);
        }
        return -1;
    }

    int readUntil(InputStream in, int size, byte[] buffer) throws IOException {
        int n = 0;
        while((size - n) > 0) {
            int r = in.read(buffer, n, size - n);
            if (r < 0) {
                throw new IOException("broken");
            }
            n += r;
        }
//        for (int i = 0; i < size; i++) {
//            Log.d("API", String.format("%d:, %x", i, buffer[i]));
//        }
        return size;
    }

    /*
     *  @param data: 需要往串口发送的数据
     *  @param which: 需要操作的串口名称，为uart1，uart2, uart3中的一个
     */
    public void sendData(byte[] data, String which) {
        if (mOut == null) return;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            appendInt32(3, baos);
            appendString(which, baos);
            appendBytes(data, baos);
            if (mOut != null) {
                mOut.write(baos.toByteArray());
                Log.d(TAG, "sendData: data:"+ Arrays.toString(data)+", which:"+which);
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                if (mOut != null)
                    mOut.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            mOut = null;
            if (mUartReadListener != null) {
                mUartReadListener.onDisconnected();
            }
        }
    }

}
