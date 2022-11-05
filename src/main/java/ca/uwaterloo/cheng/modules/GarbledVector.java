package ca.uwaterloo.cheng.modules;

public class GarbledVector {

    private int numBitsPerSlot;

    private int numSlots;

    private Bits[] bitVector;

    private int[] signs;

    private int size;

    public GarbledVector(int numSlots, int numBitsPerSlot)
    {
        this.numSlots = numSlots;
        this.numBitsPerSlot = numBitsPerSlot;
        this.signs = new int[numSlots];
        this.bitVector = new Bits[numSlots];
        this.size =0;

        for(int i=0;i<numSlots;i++)
        {
            signs[i] = 0;
            bitVector[i] = Utils.get_random_rits(numBitsPerSlot);
        }
    }

    public void set(int index, Bits bits)
    {
        bitVector[index] = bits;
    }

    public void add(int index)
    {
        signs[index] = signs[index] + 1;
        size++;
    }

    public Bits get(int index)
    {
        return bitVector[index];
    }

    public Bits getall()
    {
        return Utils.concatenate(bitVector);
    }

    public void delete(int index)
    {
        if(signs[index] > 1)
        {
            signs[index] = signs[index] - 1;
        }
        else if(signs[index] == 1)
        {
            signs[index] = 0;
            bitVector[index] = Utils.get_random_rits(numBitsPerSlot);
        }
    }

    public int getSign(int index)
    {
        return signs[index];
    }

    public int getNumSlots() {
        return numSlots;
    }

    public int getNumBitsPerSlot() {
        return numBitsPerSlot;
    }

    public int getSize() {
        return size;
    }
}
