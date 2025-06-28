package carassist.cn.carsdksample;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class Util {
    public static byte[] hexStr2Bytes(String str) {
        if (str == null) {
            return null;
        }
        if (str.length() == 0) {
            return new byte[0];
        }
        byte[] byteArray = new byte[str.length() / 2];
        for (int i = 0; i < byteArray.length; i++){
            String subStr = str.substring(2 * i, 2 * i + 2);
            byteArray[i] = (byte)(Integer.parseInt(subStr, 16) & 0xFF);
        }
        return byteArray;
    }

    public static void dump(byte[] buf, int off, int size) {

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream p = new PrintStream(os);

        p.print("   ");
        for (int i = 0; i < size; i++) {
            byte b = buf[i + off];
            p.printf("%02x", b);
            if (((i + 1) % 64) == 0) {
                p.print("\n   ");
            }
        }
        p.println();
        p.flush();
    }
}
