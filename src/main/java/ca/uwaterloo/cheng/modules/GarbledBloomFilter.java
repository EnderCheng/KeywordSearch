package ca.uwaterloo.cheng.modules;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class GarbledBloomFilter {

    private static final int seed = 0x1f60;

    private int capacity;
    private double fPP;
    private int numHashes;
    private int numSlots;
    private int numBitsPerSlot;

    private Murmur3Hash hashes;

    public GarbledBloomFilter(int capacity, double fPP, int numBitsPerSlot)
    {
        this.capacity = capacity;
        this.fPP = fPP;
        this.numBitsPerSlot = numBitsPerSlot;
        this.numSlots = (int) Math.ceil((capacity * Math.log(fPP)) / Math.log(1 / Math.pow(2, Math.log(2))));
        this.numHashes = (int) Math.ceil((numSlots / capacity) * Math.log(2));
        hashes = new Murmur3Hash(seed,numHashes);
    }

    public GarbledBloomFilter(int capacity, double fPP)
    {
        this.capacity = capacity;
        this.fPP = fPP;
        this.numSlots = (int) Math.ceil((capacity * Math.log(fPP)) / Math.log(1 / Math.pow(2, Math.log(2))));
        this.numHashes = (int) Math.round((numSlots / capacity) * Math.log(2));
        this.numBitsPerSlot = numHashes;
        hashes = new Murmur3Hash(seed,numHashes);
    }

    public ArrayList<Integer> getHashPositions(String data) {
        ArrayList<Integer> exists = new ArrayList<Integer>();
        for(int i=0;i<numHashes;i++)
        {
            int pos = (int) (hashes.hash(i,data.getBytes(StandardCharsets.UTF_8)) % numSlots);
            if(!exists.contains(pos)) {
                exists.add(pos);
            }
        }
        return exists;
    }

    public GarbledVector remove(String data, GarbledVector vector)
    {
        ArrayList<Integer> exists = new ArrayList<Integer>();
        for(int i=0;i<numHashes;i++)
        {
            int pos = (int) (hashes.hash(i,data.getBytes(StandardCharsets.UTF_8)) % numSlots);
            if(!exists.contains(pos)) {
                exists.add(pos);
            }
        }
        for(int i = 0; i<exists.size(); i++)
        {
            vector.delete(exists.get(i));
        }

        return vector;
    }

    public GarbledVector insert(String data, GarbledVector vector)
    {
//        Bits test = new Bits(numBitsPerSlot);
        Bits finalShares = Murmur3Hash.fingerprint(data.getBytes(StandardCharsets.UTF_8), numBitsPerSlot);
//        if(data.equals("1199")) {
//            System.out.println("fingerprint:" + finalShares);
//        }
        int emptySlot = -1;
        ArrayList<Integer> exists = new ArrayList<Integer>();
        for(int i=0;i<numHashes;i++)
        {
            int pos = (int) (hashes.hash(i,data.getBytes(StandardCharsets.UTF_8)) % numSlots);
//            System.out.print(pos+ " ");
            if(vector.getSign(pos) == 0)
            {
                if(emptySlot == -1 || emptySlot == pos)
                {
                    emptySlot = pos;
                }
                else{
                    Bits tmp = Utils.get_random_rits(numBitsPerSlot);
                    vector.set(pos,tmp);
                    vector.add(pos);
                    exists.add(pos);
                    finalShares.xor(tmp);
//                    if(data.equals("1199")) {
//                        if(pos == 1281)
//                        {
//                            System.out.println("test: 1281");
//                        }
//                        System.out.println(pos);
//                        test.xor(vector.get(pos));
//                        System.out.println(tmp);
//                    }
                }
            }
            else {
                if(!exists.contains(pos)) {
                    finalShares.xor(vector.get(pos));
                    vector.add(pos);
                    exists.add(pos);
//                    if(data.equals("1199")) {
//                        if(pos == 1281)
//                        {
//                            System.out.println("test: 1281");
//                        }
//                        System.out.println(pos);
//                        test.xor(vector.get(pos));
//                        System.out.println(vector.get(pos));
//                    }
                }
            }
        }
        if(emptySlot != -1) {
            vector.set(emptySlot, finalShares);
            vector.add(emptySlot);
//            if(data.equals("1199")) {
//                if(emptySlot == 1281)
//                {
//                    System.out.println("test: 1281");
//                }
//                System.out.println(emptySlot);
//                test.xor(vector.get(emptySlot));
//                System.out.println(finalShares);
//            }
        }
        else {
            System.out.println("Conflicted!");
            System.exit(-1);
        }
//        if(data.equals("1199")) {
//            System.out.println("test:"+test);
//        }
        return vector;
//        System.out.print("\n");
    }

    public boolean query(String data, GarbledVector vector)
    {
        if(vector.getSize() == 0)
        {
            return false;
        }
        Bits fingerprint = Murmur3Hash.fingerprint(data.getBytes(StandardCharsets.UTF_8), numBitsPerSlot);
//        if(data.equals("1199")) {
//            System.out.println("fingerprint:" + fingerprint);
//        }
        Bits recovery = new Bits(numBitsPerSlot);
        ArrayList<Integer> exists = new ArrayList<Integer>();
        for(int i=0;i<numHashes;i++)
        {
            int pos = (int) (hashes.hash(i,data.getBytes(StandardCharsets.UTF_8)) % numSlots);
            if(!exists.contains(pos)) {
                exists.add(pos);
                recovery.xor(vector.get(pos));
            }
        }
//        System.out.println("Positions:"+exists.toString());
//        if(data.equals("1199")) {
//            System.out.println("recovery:"+recovery);
//        }
        if(fingerprint.toLongArray().length > 0 && recovery.toLongArray().length > 0 && fingerprint.toLongArray()[0] == recovery.toLongArray()[0])
        {
            return true;
        }
        return false;
    }

    public long getCapacity() {
        return capacity;
    }

    public int getNumBitsPerSlot() {
        return numBitsPerSlot;
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
        int capacity = 100;
        double fPP = 1.0E-4;
        int numBits = 20;
        GarbledBloomFilter gbf = new GarbledBloomFilter(capacity,fPP,numBits);
        int numSlots =  gbf.getNumSlots();
        GarbledVector vector = new GarbledVector(numSlots,numBits);
        int[] tests = new int[]{36,164,293,356,438,494,503,729,881,1003,1116,1159,1199,1611,1733,1744,1804,2055,2075,2416,2571,2603,2847,2988,3024,3280,3321,3605,3606,3626,3875,3894,4281,4479,4643,4744,4745,4970,5047,5077,5126,5489,5543,5711,5768,5806,5817,6117,6226,6791,6865,6868,6936,7076,7090,7113,7450,7517,7602,7642,7773,7984,8090,8098,8518,8648,8713,8950,9050,9131,9250,9325,9360,9366,9578,9823};
        for(int i=0;i<tests.length;i++) {
            String data = Integer.toString(tests[i]);
            vector = gbf.insert(data, vector);
        }

        for(int i=0;i<tests.length;i++) {
            String data = Integer.toString(tests[i]);
            boolean res = gbf.query(data, vector);
            if (!res) {
                System.out.println("Error!");
            }
        }

        vector = gbf.remove("36",vector);
        boolean test = gbf.query("36", vector);
        if (test) {
            System.out.println("Error!");
        }

        vector = gbf.remove("293",vector);
        test = gbf.query("293", vector);
        if (test) {
            System.out.println("Error!");
        }

        for(int i=0;i<tests.length;i++) {
            String data = Integer.toString(tests[i]);
            boolean res = gbf.query(data, vector);
            if (!res) {
                System.out.println("Error!");
            }
        }

        vector = gbf.insert("294",vector);
        test = gbf.query("294", vector);
        if (!test) {
            System.out.println("Error!");
        }

//        ArrayList<Integer> positions = gbf.getHashPositions(data);
//        for(int i=0;i<positions.size();i++)
//        {
//            System.out.println(positions.get(i));
//        }
    }
}
