package ca.uwaterloo.cheng.simulator;

import ca.uwaterloo.cheng.modules.*;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class VDPFSetupSimulator {
    public static void main(String[] args) {
        double totaltime = 0;
        int times = 50;

        int q = 1;
        int d = 1;
        int ver = 0;
        boolean isMAC = true;
        int max_size = 1000;
        int mode = 2;
        int doc_size = 4000;
        double[] fPP = new double[]{Math.pow(10, -3), Math.pow(10, -4), Math.pow(10, -5), Math.pow(10, -6)};
//        String path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/synthetic_keywords_" + doc_size + "_" + max_size + ".csv";
        String path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/sample_keywords.csv";
        int lambda = 128;
        double start, end;
        CoverFamily cff = new CoverFamily(q, d);
        int chi = cff.getLines();
        int M = max_size/10;
        double loadFactor = 0.9;
        CuckooFilter cf = new CuckooFilter(max_size, M, fPP[mode], loadFactor);
        GarbledBloomFilter gbf = cf.getGbf();
        int num_buckets = (int) cf.getNumBuckets();
        int slots_per_buckets = cf.getVectors()[0].getNumSlots();
        int num_slots = slots_per_buckets * num_buckets;
        int num_bits_per_slots = cf.getVectors()[0].getNumBitsPerSlot();
        System.out.println("Length of GBF-encoded Keywords: " + num_slots * num_bits_per_slots);

//        int slot_len = (int) Math.ceil((num_slots + 0.0) / M);
        System.out.println("Each segment' length:" + slots_per_buckets);
        Bits[] index = new Bits[num_slots];
        Bits[] ext_key_col = new Bits[num_slots];
        Bits[] ext_key_row = new Bits[num_slots];
        int key_len = num_bits_per_slots * doc_size;
        for (int i = 0; i < num_slots; i++) {
            index[i] = Utils.get_random_rits(key_len);
            ext_key_col[i] = Utils.get_random_rits(key_len);
            ext_key_row[i] = Utils.get_random_rits(key_len);
        }
        byte[][] ivs = null;
        byte[][] MACs = null;
        Random rd = new Random();
        XORMAC[] mac = new XORMAC[chi];
        Bits[] ivkeys = new Bits[chi];
        if (isMAC) {
            ivs = new byte[chi][num_slots * XORMAC.MACBYTES];
            MACs = new byte[chi][num_slots * XORMAC.MACBYTES];
            byte[][] mackeys = new byte[chi][XORMAC.KEYBYTES];
            for (int i = 0; i < chi; i++) {
                ivkeys[i] = Utils.get_random_rits(lambda);
                rd.nextBytes(mackeys[i]);
                mac[i] = new XORMAC(mackeys[i]);
                rd.nextBytes(ivs[i]);
                rd.nextBytes(MACs[i]);
            }
        }
        for (int repeat = 0; repeat < times; repeat++) {
            int real_num_docs = 0;
            Bits[] enc_ctx = new Bits[doc_size];
            try {
                FileInputStream fstream = new FileInputStream(path);
                BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
                String strLine;
                start = System.nanoTime();
                while ((strLine = br.readLine()) != null) {
                    cf = new CuckooFilter(max_size, M, fPP[mode], loadFactor);
                    List<String> word = Arrays.asList(strLine.split(","));
                    if (word.size() > max_size) {
                        System.out.println("Error: The maximum number of keywords is not correct: " + word.size());
                        System.exit(-1);
                    }
                    for (int i = 0; i < word.size(); i++) {
                        boolean state = cf.add(word.get(i));
                        if (!state) {
                            System.out.println("Cannot build index! Please Retry with smaller LoadFactor (0-1)!");
                            System.exit(-1);
                        }
                    }
                    Bits[] ctx = new Bits[num_slots];
                    int start_col = real_num_docs * num_bits_per_slots;
                    int end_col = (real_num_docs + 1) * num_bits_per_slots;
                    for (int j = 0; j < num_slots; j++) {
                        int bucket = j / slots_per_buckets;
                        int ind = j % slots_per_buckets;
                        ctx[j] = cf.getVectors()[bucket].get(ind);
                        ctx[j].xor(ext_key_col[j].get(start_col, end_col));
                    }
                    enc_ctx[real_num_docs] = Utils.concatenate(ctx);
                    enc_ctx[real_num_docs].xor(ext_key_row[real_num_docs]);
                    real_num_docs++;
                }
                end = System.nanoTime();
                totaltime = totaltime + (end-start)/1000000;
                if (real_num_docs != doc_size) {
                    System.out.println("Error: The number of documents is not correct: " + real_num_docs);
                    System.exit(-1);
                }
                fstream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (isMAC) {
                start = System.nanoTime();
                byte[][] all_doc_rnds = new byte[chi][num_slots * XORMAC.MACBYTES];
                for (int i = 0; i < chi; i++) {
                    for (int j = 0; j < doc_size; j++) {
                        byte[] Gamma = Utils.prf_iv_doc(ivkeys[i], "random" + j + ver + "iv", lambda, num_slots);
                        all_doc_rnds[i] = ByteUtils.xor(all_doc_rnds[i], Gamma);
                    }
                }

                byte[][] macs = new byte[chi][num_slots * XORMAC.MACBYTES];
                for (int i = 0; i < chi; i++) {
                    byte[][] macs_tmp = new byte[num_slots][XORMAC.MACBYTES];
                    for (int z = 0; z < num_slots; z++) {
                        Bits data = new Bits(num_bits_per_slots);
                        for (int j = 0; j < doc_size; j++) {
                            data.xor(index[z].get(j * num_bits_per_slots, (j + 1) * num_bits_per_slots));
                        }
                        macs_tmp[z] = mac[i].create_without_iv(data);
                    }
                    macs[i] = Utils.flatten2DArray(macs_tmp);
                    macs[i] = ByteUtils.xor(macs[i], all_doc_rnds[i]);
                }
                end = System.nanoTime();
                totaltime = totaltime + (end-start)/1000000;
            }
        }
        System.out.println(totaltime / times);
    }
}
