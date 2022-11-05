package ca.uwaterloo.cheng;

import ca.uwaterloo.cheng.modules.Bits;
import ca.uwaterloo.cheng.modules.PropertiesCache;
import ca.uwaterloo.cheng.modules.Utils;

import java.io.IOException;

public class KeySetup {
    public static void main(String[] args) throws IOException {
        int lambda = 128;
        Bits Key_1 = Utils.get_random_rits(lambda);
        Bits Key_2 = Utils.get_random_rits(lambda);
        Bits MACKey_1 = Utils.get_random_rits(lambda);
        Bits MACKey_2 = Utils.get_random_rits(lambda);
        PropertiesCache cache = new PropertiesCache();
        cache.write("Key1", Utils.bitsToBase64(Key_1));
        cache.write("Key2", Utils.bitsToBase64(Key_2));
        cache.write("MACKey_1", Utils.bitsToBase64(MACKey_1));
        cache.write("MACKey_2", Utils.bitsToBase64(MACKey_2));
        System.out.println(cache.read("Key_1"));
    }
}
