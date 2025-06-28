package carassist.cn.Living;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import carassist.cn.CarIntents;
import carassist.cn.SystemProperties;

/*
 * 1 create a tcp server
 * 2 send broadcast for start living
 * 3 read and parse the private living format to retrieve the audio/video/gps packet
 * 4 use the raw packet if needed.
 * */
public class LocalLiving extends Thread {
    private final static boolean DEBUG = false;
    private final static String TAG = "LocalLiving";
    private final static int BUFFER_SIZE = 1920*1080*2;
    private ServerSocket mListenSocket;
    private int mPort;
    private String mURL;
    private Socket mClient;
    private Context mContext;
    private int mCamID;
    private final static boolean NO_SUB_STREAM =  SystemProperties.getInt("ro.cam.nosub", 0) == 1;

    public interface ILocalLivingCallback {
        void onH264FrameFromLocalSocket(int width, int height, int type, ByteBuffer data);
    }

    private ILocalLivingCallback mCallback;
    private int mWidth, mHeight;

    public LocalLiving(Context ctx, int camID, ILocalLivingCallback callback) {
        start();
        mContext = ctx.getApplicationContext();
        mCamID = camID;
        mCallback = callback;
        if (NO_SUB_STREAM) {
            if (camID == CarIntents.CAMERA_FRONT) {
                mWidth = 1920;
                mHeight = 1080;
            } else {
                mWidth = 1280;
                mHeight = 720;
            }
        } else {
            mWidth = 640;
            mHeight = 480;
        }
        Log.d(TAG, "new LocalLiving " + (char)mCamID + ", " + mWidth + "x" + mHeight);
    }
    @Override
    public void run() {

        while(true) {
            try {
                mListenSocket = new ServerSocket(0, 0, InetAddress.getByName("0.0.0.0"));
                break;
            } catch (Exception e) {
                Log.d(TAG, "Start ServerSocket Failed:" + e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }

        // find the local port
        mPort = mListenSocket.getLocalPort();
        mURL = "tcp://127.0.0.1:" + mPort;

        Log.d(TAG, "Start Listen mURL " + mURL);

        while(true) {
            try {
                mClient = mListenSocket.accept();
                Log.d(TAG, "mListenSocket accepted");
                parseStream();
            } catch (Exception e) {
                Log.e(TAG, "ServerSocket Error ", e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    private void parseStream() {
        InputStream in;
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE); //suppose big enough buffer
        buffer.clear();
        try {
            in = mClient.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        while(true) {
            int freespace = buffer.capacity() - buffer.position();
            try {
                int readed = in.read(buffer.array(), buffer.position(), freespace);
                if (readed > 0) {
                    buffer.position(buffer.position() + readed);
                    if (DEBUG) Log.d(TAG, "readed " + readed);
                } else {
                    continue;
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Socket error, restart accept");
                return;
            }

            while(true) {
                buffer.flip();
                MediaPacket packet = new MediaPacket(buffer);
                int packetLen = packet.length();
                if (packetLen > 0) {
                    buffer.position(packetLen);
                    buffer.compact();
                    if (buffer.position() == 0) break;
                } else {
                    buffer.position(buffer.limit());
                    buffer.limit(buffer.capacity());
                    Log.d(TAG, "Total Data = " + buffer.position() + " bytes, but without a full packet");
                    break;
                }
            }
        }
    }

    public void startLiving() {
        Log.d(TAG, "startLiving");
        Intent intent = new Intent(CarIntents.ACTION_CAMERA_LIVING);
        intent.putExtra("action", 1);
        intent.putExtra("camid", mCamID);
        intent.putExtra("url", mURL);
        mContext.sendBroadcast(intent);
    }

    public void stopLiving() {
        Log.d(TAG, "stopLiving");
        Intent intent = new Intent(CarIntents.ACTION_CAMERA_LIVING);
        intent.putExtra("action", 0);
        intent.putExtra("camid", mCamID);
        intent.putExtra("url", "");
        mContext.sendBroadcast(intent);
    }

    private byte[] mSpsBuf = new byte[64];
    private int mSpsLen = 0;
    private byte[] mPpsBuf = new byte[64];
    private int mPpsLen = 0;

    class MediaPacket {
        private final int HEADER_SIZE = 2 + 1 + 1 + 4 + 4 + 16;
        private final int HEADER_EXTENTION_SIZE = 0;
        private final ByteBuffer payload = ByteBuffer.allocateDirect(BUFFER_SIZE);

        private byte[] MAGIC_NUMBER = {(byte)0xD1, 0x1D};

        private byte[] mSN = new byte[16];
        private int[] mGPS = new int[3];
        private int mLength = 0;

        public MediaPacket(ByteBuffer buffer) {
            if (buffer.limit() <= HEADER_SIZE) {
                return;
            }
            try {
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                byte magic1 = buffer.get();
                byte magic2 = buffer.get();
                if((magic1 != MAGIC_NUMBER[0]) || (magic2 != MAGIC_NUMBER[1])) {
                    Log.e(TAG, "MAGIC NUMBER ERROR magic1 = " + Integer.toHexString(magic1) + " magic2 = " + Integer.toHexString(magic2));
                    return;
                }

                byte type = buffer.get();
                switch (type & 3) {
                    case 1:
                        if (DEBUG) Log.d(TAG, "Video Packet Comming");
                        break;
                    case 2:
                        if (DEBUG) Log.d(TAG, "Audio Packet Comming");
                        break;
                    default:
                        Log.d(TAG, "Error Packet Type = " + type);
                        return;
                }

                byte headerLen = buffer.get();
                int payloadTime = buffer.getInt();
                int payloadLen = buffer.getInt();
                if (DEBUG) {
                    Log.d(TAG, "packet payloadTime " + payloadTime + " headerLen " + headerLen +
                            " payloadLen " + payloadLen + " bufferLen " + buffer.limit());
                }
                if (buffer.limit() < (headerLen + payloadLen)) {
                    return;
                }

                buffer.get(mSN, 0, 16);

                if ((type & 0x4) != 0) {
                    if (DEBUG) Log.d(TAG, "GPS Packet Comming with Video");
                    mGPS[0] = buffer.getInt();
                    mGPS[1] = buffer.getInt();
                    mGPS[2] = buffer.getInt();

                    if (DEBUG) {
                        Log.d(TAG, "GPS Info Lat:" + ((double)mGPS[0] / 1000000) + " Lon:" + ((double)mGPS[1] / 1000000));
                        Log.d(TAG, "altitude: " + ((mGPS[2] >> 16) & 0x3FFF));
                        Log.d(TAG, "bearing: " + ((mGPS[2] >> 7) & 0x1FF));
                        Log.d(TAG, "speed: " + (mGPS[2] & 0x7F));
                    }
                }

                if ((type & 3) == 1) {
                    payload.clear();
                    int frameType = 0;
                    byte[] data = buffer.array();
                    int nalType = data[headerLen+4] & 0x1F;
                    switch (nalType) {
                        case 5:
                            if (DEBUG) Log.d(TAG, "Nal type is IDR frame");
                            if (DEBUG && mSpsLen>0) Log.d(TAG, String.format("SPS: %02x %02x %02x %02x %02x %02x",
                                    mSpsBuf[0], mSpsBuf[1], mSpsBuf[2], mSpsBuf[3], mSpsBuf[4], mSpsBuf[5]));
                            if (mSpsLen>0) payload.put(mSpsBuf, 0, mSpsLen);
                            if (mPpsLen>0) payload.put(mPpsBuf, 0, mPpsLen);
                            payload.put(buffer.array(), buffer.position(), payloadLen);
                            mSpsLen = 0;
                            mPpsLen = 0;
                            break;
                        case 7:
                            if (DEBUG) Log.d(TAG, "Nal type is SPS " + payloadLen);
                            if (payloadLen < mSpsBuf.length) {
                                buffer.get(mSpsBuf, 0, payloadLen);
                                mSpsLen = payloadLen;
                            } else {
                                if (DEBUG) {
                                    Log.d(TAG, "payloadLen: " + payloadLen);
                                    Log.d(TAG, "buffer:" + buffer.toString());
                                    Log.d(TAG, "payload:" + payload.toString());
                                }
                                payload.put(buffer.array(), buffer.position(), payloadLen);
                            }
                            break;
                        case 8:
                            if (DEBUG) Log.d(TAG, "Nal type is PPS");
                            if (payloadLen < mPpsBuf.length) {
                                buffer.get(mPpsBuf, 0, payloadLen);
                                mPpsLen = payloadLen;
                            } else {
                                payload.put(buffer.array(), buffer.position(), payloadLen);
                            }
                            break;
                        default:
                            if (DEBUG) Log.d(TAG, "Nal type is B/P frame, " + nalType);
                            frameType = 1;
                            payload.put(buffer.array(), buffer.position(), payloadLen);
                            break;
                    }
                    if (payload.position() > 0 && mCallback != null) mCallback.onH264FrameFromLocalSocket(mWidth, mHeight, frameType, payload);
                } else if ((type & 3) == 2) {
                    //Log.d(TAG, "Audio Packet，len=" + payloadLen);
                }

                mLength = headerLen + payloadLen;
                // Log.d(TAG, "Full Packet Size = " + mLength);
            } catch (Exception e) {
                Log.e(TAG, "ByteBuffer read failed", e);
                return;
            }

        }
        public int length() {
            return mLength;
        }
    }
}
