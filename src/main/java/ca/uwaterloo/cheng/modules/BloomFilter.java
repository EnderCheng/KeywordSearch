package ca.uwaterloo.cheng.modules;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class BloomFilter {

    private static final int seed = 0x1f61;

    private int capacity;
    private double fPP;
    private int numHashes;
    private int numSlots;

    private Murmur3Hash hashes;

    public BloomFilter(int capacity, double fPP) {
        this.capacity = capacity;
        this.fPP = fPP;
        this.numSlots = (int) Math.ceil((capacity * Math.log(fPP)) / Math.log(1 / Math.pow(2, Math.log(2))));
        this.numHashes = (int) Math.ceil((numSlots / capacity) * Math.log(2));
        hashes = new Murmur3Hash(seed, numHashes);
    }

    public ArrayList<Integer> getHashPositions(String data) {
        ArrayList<Integer> exists = new ArrayList<Integer>();
        for (int i = 0; i < numHashes; i++) {
            int pos = (int) (hashes.hash(i, data.getBytes(StandardCharsets.UTF_8)) % numSlots);
            if (!exists.contains(pos)) {
                exists.add(pos);
            }
        }
        return exists;
    }

    public BloomVector insert(String data, BloomVector vector) {
        for(int i=0;i<numHashes;i++)
        {
            int pos = (int) (hashes.hash(i,data.getBytes(StandardCharsets.UTF_8)) % numSlots);
            vector.set(pos);
        }
        return vector;
    }

    public boolean query(String data, BloomVector vector) {
        boolean res = true;
        for(int i=0;i<numHashes;i++)
        {
            int pos = (int) (hashes.hash(i,data.getBytes(StandardCharsets.UTF_8)) % numSlots);
            if(vector.get(pos) == false)
            {
                res = false;
                break;
            }
        }
        return res;
    }

    public long getCapacity() {
        return capacity;
    }

    public double getfPP() {
        return fPP;
    }

    public int getNumHashes() {
        return numHashes;
    }

    public int getNumSlots() {
        return numSlots;
    }

    public static void main(String[] args) {
        int capcity = 100;
        double fPP = Math.pow(10,-4);
        BloomFilter bloomFilter = new BloomFilter(capcity,fPP);
        BloomVector bvector = new BloomVector(bloomFilter.getNumSlots());
        for(int i=0;i<100;i++)
        {
            bvector = bloomFilter.insert("keyword"+i,bvector);
        }
        System.out.println(bvector.toBitString());
        System.out.println(bloomFilter.query("keyword0",bvector));
        System.out.println(bloomFilter.query("keyword10",bvector));
        System.out.println(bloomFilter.query("keyword99",bvector));
        System.out.println(bloomFilter.query("keyword100",bvector));
    }

}
