package ca.uwaterloo.cheng.modules;

import org.checkerframework.checker.units.qual.A;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.math.DoubleMath.log2;
import static com.google.common.math.LongMath.divide;
import static java.lang.Math.ceil;
import static java.math.RoundingMode.CEILING;
import static java.math.RoundingMode.HALF_DOWN;

public class CuckooFilter {

    private final int MAX_KICKS = 1000;
    private int capacity;
    private int numEntriesPerBucket;
    private double fPP;
    private double loadFactor;
    private int numBuckets;
    private int numBitsPerEntry;
    private GarbledBloomFilter gbf;
    private GarbledVector[]  vectors;
    private ArrayList<Bits>[] buckets;

    private ArrayList<String>[] values;

    public CuckooFilter(int capacity, int numBuckets, double fPP, double loadFactor)
    {
        this.capacity = capacity;
        this.fPP = fPP;
        this.loadFactor = loadFactor;
        this.numBuckets = numBuckets;
        this.numEntriesPerBucket = optimialEntrisPerBucket();
        this.numBitsPerEntry = optimalBitsPerEntry();
        vectors = new GarbledVector[numBuckets];
        gbf = new GarbledBloomFilter(numEntriesPerBucket,fPP);
        buckets = new ArrayList[numBuckets];
        values = new ArrayList[numBuckets] ;
        int numSlots = gbf.getNumSlots();
        for(int i=0;i<numBuckets;i++)
        {
            vectors[i] = new GarbledVector(numSlots,gbf.getNumBitsPerSlot());
            buckets[i] = new ArrayList<Bits>();
            values[i] = new ArrayList<String>();
        }
    }

//    public CuckooFilter(int capacity, int numEntriesPerBucket, int numBitsPerSlot, double fPP, double loadFactor)
//    {
//        this.capacity = capacity;
//        this.numEntriesPerBucket = numEntriesPerBucket;
//        this.fPP = fPP;
//        this.loadFactor = loadFactor;
//        this.numBuckets = optimalNumberOfBuckets();
//        this.numBitsPerEntry = optimalBitsPerEntry();
//        vectors = new GarbledVector[numBuckets];
//        gbf = new GarbledBloomFilter(numEntriesPerBucket,fPP,numBitsPerSlot);
//        buckets = new ArrayList[numBuckets];
//        values = new ArrayList[numBuckets];
//        int numSlots = gbf.getNumSlots();
//        for(int i=0;i<numBuckets;i++)
//        {
//            vectors[i] = new GarbledVector(numSlots,numBitsPerSlot);
//            buckets[i] = new ArrayList<Bits>();
//            values[i] = new ArrayList<String>();
//        }
//    }

    public GarbledVector[] getVectors() {
        return vectors;
    }

    public boolean add(String data)
    {
        final long hash_1 = Murmur3Hash.hash_1(data.getBytes(StandardCharsets.UTF_8));
        Bits fingerprint = Murmur3Hash.fingerprint(data.getBytes(StandardCharsets.UTF_8), numBitsPerEntry);
        int hash_fingerprint =  (int) (Murmur3Hash.hash_1(fingerprint.toByteArray()) % numBuckets);
        int index_1 = (int) (hash_1 % numBuckets);
        int index_2 = (int) ((index_1 ^ hash_fingerprint) % numBuckets);
        boolean state = false;
        if(buckets[index_1].size() < numEntriesPerBucket)
        {
            buckets[index_1].add(fingerprint);
            values[index_1].add(data);
            vectors[index_1] = gbf.insert(data,vectors[index_1]);
//            if(data.equals("91110"))
//            {
//                System.out.println("index_1:"+index_1);
//            }
            state = true;
        }
        else if(buckets[index_2].size() < numEntriesPerBucket)
        {
            buckets[index_2].add(fingerprint);
            values[index_2].add(data);
            vectors[index_2] = gbf.insert(data,vectors[index_2]);
//            if(data.equals("91110"))
//            {
//                System.out.println("index_2:"+index_2);
//            }
            state = true;
        }
        else {
            SecureRandom sr = new SecureRandom();
            sr.setSeed(System.nanoTime());
            int index = sr.nextBoolean() ? index_1 : index_2;
//            int index = index_2;
            for(int i=0;i<MAX_KICKS;i++)
            {
                int entry = Utils.getRandomNumber(0,numEntriesPerBucket);
                Bits tmp = buckets[index].get(entry);
                String kick_data = values[index].get(entry);
                values[index].remove(kick_data);
                buckets[index].remove(tmp);
                vectors[index] = gbf.remove(kick_data,vectors[index]);
//                System.out.println("remove "+kick_data+" from "+index);
                vectors[index] = gbf.insert(data,vectors[index]);
                values[index].add(data);
                buckets[index].add(fingerprint);
//                System.out.println("insert "+data+" into "+index);
                fingerprint = tmp;
                data = kick_data;
                final long new_hash_1 = Murmur3Hash.hash_1(data.getBytes(StandardCharsets.UTF_8));
                Bits new_fingerprint = Murmur3Hash.fingerprint(data.getBytes(StandardCharsets.UTF_8), numBitsPerEntry);
                int new_hash_fingerprint =  (int) (Murmur3Hash.hash_1(new_fingerprint.toByteArray()) % numBuckets);
                int new_index_1 = (int) (new_hash_1 % numBuckets);
                int new_index_2 = (int) ((new_index_1 ^ new_hash_fingerprint) % numBuckets);
                if(index !=new_index_1)
                {
                    index = new_index_1;
                }
                else{
                    index = new_index_2;
                }
//                int hash_fingerprint_kick =  (int) (Murmur3Hash.hash_1(fingerprint.toByteArray()) % numBuckets);
//                index = (int) ((index ^ hash_fingerprint_kick) % numBuckets);
                if(buckets[index].size() < numEntriesPerBucket)
                {
                    buckets[index].add(fingerprint);
                    values[index].add(data);
                    vectors[index] = gbf.insert(data,vectors[index]);
//                    System.out.println("insert "+data+" into "+index);
                    state = true;
                    break;
                }
//                if (i == MAX_KICKS - 1) {
//                    System.out.println(kick_data);
//                    System.out.println("Insert Failure!");
//                }
            }
        }
        return state;
    }

    public int[] getBuckets(String data)
    {
        final long hash_1 = Murmur3Hash.hash_1(data.getBytes(StandardCharsets.UTF_8));
        Bits fingerprint = Murmur3Hash.fingerprint(data.getBytes(StandardCharsets.UTF_8), numBitsPerEntry);
        int hash_fingerprint =  (int) (Murmur3Hash.hash_1(fingerprint.toByteArray()) % numBuckets);
        int index_1 = (int) (hash_1 % numBuckets);
        int index_2 = (int) ((index_1 ^ hash_fingerprint) % numBuckets);
        return new int[]{index_1,index_2};
    }

    public GarbledBloomFilter getGbf() {
        return gbf;
    }

    public Bits getBits()
    {
        Bits[] tmp = new Bits[numBuckets];
        for(int i=0;i<numBuckets;i++)
        {
            tmp[i] = vectors[i].getall();
        }
        return Utils.concatenate(tmp);
    }

    public boolean lookup(String data, int type)
    {
        final long hash_1 = Murmur3Hash.hash_1(data.getBytes(StandardCharsets.UTF_8));
        Bits fingerprint = Murmur3Hash.fingerprint(data.getBytes(StandardCharsets.UTF_8), numBitsPerEntry);
        int hash_fingerprint =  (int) (Murmur3Hash.hash_1(fingerprint.toByteArray()) % numBuckets);
        int index_1 = (int) (hash_1 % numBuckets);
        int index_2 = (int) ((index_1 ^ hash_fingerprint) % numBuckets);
        if(type == 0) {
            if (buckets[index_1].contains(fingerprint) || buckets[index_2].contains(fingerprint)) {
                return true;
            } else {
                return false;
            }
        }
        else if (type == 1)
        {
            if (gbf.query(data, vectors[index_1]) || gbf.query(data, vectors[index_2])) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public double getfPP() {
        return fPP;
    }

    public double getLoadFactor() {
        return loadFactor;
    }

    public int getNumBitsPerEntry() {
        return numBitsPerEntry;
    }

    public int getNumEntriesPerBucket() {
        return numEntriesPerBucket;
    }

    public long getCapacity() {
        return capacity;
    }

    public long getNumBuckets() {
        return numBuckets;
    }

    public String toString() {
        return "CuckooFilter{" +
                "capacity=" + getCapacity() +
                ", fpp=" + getfPP() +
                ", loadFactor=" + getLoadFactor() +
                ", numBuckets=" + getNumBuckets() +
                ", numEntriesPerBucket=" + getNumEntriesPerBucket() +
                ", numBitsPerEntry=" + getNumBitsPerEntry() +
                '}';
    }

    private int optimalNumberOfBuckets() {
        return (int) Math.ceil((Math.ceil(capacity/loadFactor))/numEntriesPerBucket);
    }

    private int optimialEntrisPerBucket(){
        return (int) Math.ceil((Math.ceil(capacity/loadFactor))/numBuckets);
    }

    private int optimalBitsPerEntry() {
        return log2(2 * numEntriesPerBucket / fPP, HALF_DOWN);
    }

    public static void main(String[] args) {
        String path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/synthetic_keywords_1000_100.csv";
        try {
            String strLine;
            FileInputStream fstream = new FileInputStream(path);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            while ((strLine = br.readLine()) != null) {
                CuckooFilter cf = new CuckooFilter(100, 10, Math.pow(10,-6), 0.9);
                List<String> word = Arrays.asList(strLine.split(","));
                if (word.size() > 100) {
                    System.out.println("Error: The maximum number of keywords is not correct: " + word.size());
                    System.exit(-1);
                }
                for (int i = 0; i < word.size(); i++) {
                    boolean state = cf.add(word.get(i));
                    if(!state)
                    {
                        System.out.println("Cannot build index! Please Retry!");
                        System.exit(-1);
                    }
                }
            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }
//        for(int k=0;k<10;k++) {
//            int capacity = 4000;
//            CuckooFilter cf = new CuckooFilter(4000, 10,1.0E-6, 1);
//            System.out.println(cf.getNumBuckets()*cf.getVectors()[0].getNumSlots()*cf.getVectors()[0].getNumBitsPerSlot());
//            System.out.println(cf.getBits().length());
//            for (int i = 0; i < capacity; i += 1) {
//                boolean state = cf.add("testgfhdfgh"+Integer.toString(i));
//                if (!state) {
//                    System.out.println("Insert Failure!");
//                }
//            }
//
//            for (int i = 0; i < capacity; i += 1) {
//                boolean state = cf.lookup("testgfhdfgh"+Integer.toString(i), 0);
//                if (!state) {
//                    System.out.println(i);
//                    System.out.println("Look up Failure!");
//                }
//            }
//        }

    }

}
