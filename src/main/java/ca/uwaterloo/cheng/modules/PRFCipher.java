package ca.uwaterloo.cheng.modules;

import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIType;

import java.io.*;
import java.util.Arrays;
import java.util.List;

public class PRFCipher {


    public static Bits generateKey(Bits mk, int key_len, int i, int version) {
        Bits in = Utils.stringToBits(i+version+"random", key_len);
        return Utils.prf_to_len(mk,in,key_len);
    }

    public static Bits generateKey(Bits mk, int key_len, int i, int j, int version) {
        Bits in = Utils.stringToBits(i+j+version+"random", key_len);
        return Utils.prf_to_len(mk,in,key_len);
    }
    public static Bits extend_key(Bits key, int key_len, int ex_len) {
//        if (ex_len <= key_len) {
//            return key.get(0, ex_len);
//        }
//
//        int num = ex_len / key_len;
//        int reminder = ex_len % key_len;
//        if (reminder > 0) {
//            num = num + 1;
//        }
//        num = (int) Math.ceil(Utils.log2(num));
//        Bits in = (Bits) key.clone();
//        for (int i = 0; i < num; i++) {
//            in = Utils.dprg(in.length(), in);
//        }
        Bits out = Utils.prg(key,ex_len);
        return out;
    }

    public static Bits cipher(Bits key, Bits in) {
        if (key.length() != in.length()) {
            System.out.println("PRF-XOR Encryption fails: key's size and input's size are not equal!");
            System.exit(-1);
        }
        Bits res = (Bits) key.clone();
        res.xor(in);
        return res;
    }

//    public static Bits extract(Bits[] keys, int key_len, int ext_len, int pos){
//        int doc_size = keys.length;
//        Bits res = new Bits(doc_size);
//        for (int j = 0; j < doc_size; j++) {
//            Bits ex_key = extend_key(keys[j],key_len,ext_len);
//            res.set(j,ex_key.get(pos));
//        }
//        return res;
//    }

}
