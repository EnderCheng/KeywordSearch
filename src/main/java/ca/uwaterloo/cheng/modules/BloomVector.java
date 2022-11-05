package ca.uwaterloo.cheng.modules;

public class BloomVector {


    private int numSlots;

    private boolean[] bitVector;

    public BloomVector(int numSlots)
    {
        this.numSlots = numSlots;
        this.bitVector = new boolean[numSlots];

        for(int i=0;i<numSlots;i++)
        {
            bitVector[i] = false;
        }
    }

    public boolean[] getBitVector() {
        return bitVector;
    }

    public void set(int index)
    {
        bitVector[index] = true;
    }


    public boolean get(int index)
    {
        return bitVector[index];
    }

    public int getNumSlots() {
        return numSlots;
    }

    public String toBitString()
    {
        StringBuilder res = new
                StringBuilder("");
        for(int i=0;i<numSlots;i++)
        {
            if(bitVector[i]) {
                res.append('1');
            }else{
                res.append('0');
            }
        }
        return res.toString();
    }

}
