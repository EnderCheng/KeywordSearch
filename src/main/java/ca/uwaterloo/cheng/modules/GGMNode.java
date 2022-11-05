package ca.uwaterloo.cheng.modules;

public class GGMNode {
    private int index;
    private int level;

    public GGMNode(int index, int level)
    {
        this.index = index;
        this.level = level;
    }

    public int getIndex() {
        return index;
    }

    public int getLevel() {
        return level;
    }

    public Bits toBits()
    {
        return Utils.long_to_bits(index,level + 1);
    }

    public Bits getKey(Bits MK) {
        return Utils.prf(MK,toBits());
    }
}
