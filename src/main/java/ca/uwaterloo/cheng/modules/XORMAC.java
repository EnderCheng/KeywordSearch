package ca.uwaterloo.cheng.modules;

import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;

import java.util.ArrayList;

public class XORMAC {
    public static final int KEYBYTES = 16;
    public static final int MACBYTES = 16;

    GF2_128 key;

    public XORMAC(byte[] keys) {
        if (keys.length != KEYBYTES) {
            System.out.println("Error: XORMAC key size is incorrect!");
            System.exit(-1);
        }
        key = new GF2_128(keys);
    }

    public byte[] create(Bits input, byte[] iv) {
        if (iv.length != KEYBYTES) {
            System.out.println("Error: XORMAC IV size is incorrect!");
            System.exit(-1);
        }
        int val = (int) Utils.convert(input);
        GF2_128 in = new GF2_128(val);
        GF2_128 iv_in = new GF2_128(iv);
        GF2_128 out = new GF2_128();
        GF2_128.mul(out, in, key);
        GF2_128.add(out, out, iv_in);
        return out.toByteArray();
    }

    public byte[] create_without_iv(Bits input)
    {
        int val = (int) Utils.convert(input);
        GF2_128 in = new GF2_128(val);
        GF2_128 out = new GF2_128();
        GF2_128.mul(out, in, key);
        return out.toByteArray();
    }

    public boolean equal(byte[] in_1, byte[] in_2) {
        boolean validMac = true;

        for (int i = 0; i < MACBYTES; i++) {
            validMac &= in_1[i] == in_2[i];
        }
        return validMac;
    }

    public boolean verify(byte[] mac_val, Bits message, byte[] iv) {
        boolean validMac = true;
        if (iv.length != KEYBYTES) {
            System.out.println("Error: XORMAC IV size is incorrect!");
            System.exit(-1);
        }
        int val = (int) Utils.convert(message);
        GF2_128 in = new GF2_128(val);
        GF2_128 iv_in = new GF2_128(iv);
        GF2_128 out = new GF2_128();
        GF2_128.mul(out, in, key);
        GF2_128.add(out, out, iv_in);
        byte[] macout = out.toByteArray();
        for (int i = 0; i < MACBYTES; i++) {
            validMac &= mac_val[i] == macout[i];
        }
        return validMac;
    }

    public static byte[] Combine(ArrayList<byte[]> macs) {
        int len = macs.size();
        GF2_128 out = new GF2_128();
        for (int i = 1; i < len; i++) {
            byte[] mac_bytes = macs.get(i);
            GF2_128 mac = new GF2_128(mac_bytes);
            GF2_128.add(out, out, mac);
        }
        return out.toByteArray();
    }

    public static void main(String[] args) {
        int row = 2;
        int col = 2;
        int lambda = 128;
        int data_len = 20;

        PropertiesCache properties = new PropertiesCache();
        Bits mkey = Utils.base64ToBits(properties.read("Key1"), lambda);
        byte[] mackey = Utils.base64ToBits(properties.read("MACKey_1"), lambda).toByteArray();

        XORMAC xmac = new XORMAC(mackey);

        Bits[][] data = new Bits[col][row];
        for (int j = 0; j < col; j++) {
            for (int i = 0; i < row; i++) {
                data[j][i] = Utils.get_random_rits(data_len);
            }
        }

        byte[][][] iv = new byte[col][row][];
        byte[][] all_macs = new byte[col][];
        byte[][] out_iv = new byte[col][];
        for (int j = 0; j < col; j++) {
            byte[] combines = new byte[MACBYTES];
            out_iv[j] = new byte[MACBYTES];
            for (int i = 0; i < row; i++) {
                iv[j][i] = PRFCipher.generateKey(mkey, lambda, i, j, 0).toByteArray();
                combines = ByteUtils.xor(combines, xmac.create(data[j][i], iv[j][i]));
                out_iv[j] = ByteUtils.xor(out_iv[j], iv[j][i]);
            }
            all_macs[j] = combines;
        }
    }
}
