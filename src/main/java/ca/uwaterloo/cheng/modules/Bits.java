package ca.uwaterloo.cheng.modules;

import java.util.BitSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Bits extends BitSet{

    private int len;

    public Bits(int len)
    {
        super(len);
        this.len = len;
    }

    @Override
    public int length() {
        return len;
    }

    @Override
    public Bits get(int fromIndex, int toIndex) {
        BitSet set = super.get(fromIndex, toIndex);
        int index_length = toIndex - fromIndex;
        return Utils.bitset_to_bits(set,index_length);
    }

    @Override
    public String toString() {
        if (this == null) {
            return null;
        }
        return IntStream.range(0, this.length())
                .mapToObj(b -> String.valueOf(this.get(b) ? 1 : 0))
                .collect(Collectors.joining());
    }

}
