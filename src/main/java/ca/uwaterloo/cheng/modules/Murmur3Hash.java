package ca.uwaterloo.cheng.modules;

import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;

public class Murmur3Hash {

    private static final int SEED = 0x31ef;

    private static final int SEED_1 = 0x87fa;

    private static final int SEED_2 = 0x15be;

    private int SEEDs[];

    public Murmur3Hash(int seed, int num)
    {
        SEEDs = new int[num];
        for(int i = 0; i<num;i++)
        {
            SEEDs[i] = seed+i;
        }
    }

    public long hash(int index, byte[] bytes)
    {
        return  Hashing.murmur3_32_fixed(SEEDs[index]).newHasher().putBytes(bytes).hash().padToLong();
    }

    public static Bits fingerprint(byte[] bytes, int numBits)
    {
        long hash = Hashing.murmur3_128(SEED).newHasher().putBytes(bytes).hash().padToLong();
        int mask = (0x80000000 >> (numBits - 1)) >>> (Integer.SIZE - numBits);

        for (int bit = 0; (bit + numBits) <= Integer.SIZE; bit += numBits) {
            long ret = (hash >> bit) & mask;
            if (0 != ret) {
                return Utils.long_to_bits(ret,numBits);
            }
        }
        return new Bits(numBits);
    }

    public static long hash_1(byte[] bytes) {
        return Hashing.murmur3_32_fixed(SEED_1).newHasher().putBytes(bytes).hash().padToLong();
    }

    public static long hash_2(byte[] bytes) {
        return Hashing.murmur3_32_fixed(SEED_2).newHasher().putBytes(bytes).hash().padToLong();
    }

}
