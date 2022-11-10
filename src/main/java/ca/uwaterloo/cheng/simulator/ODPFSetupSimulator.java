package ca.uwaterloo.cheng.simulator;

import ca.uwaterloo.cheng.modules.*;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ODPFSetupSimulator {

    public static void main(String[] args) {
        double totaltime = 0;
        int times = 50;

        int q = 1;
        int d = 1;
        int ver = 0;
        boolean isMAC = false;
        int max_size = 100;
        int mode = 1;
        int doc_size = 1;
        double[] fPP = new double[]{Math.pow(10, -3), Math.pow(10, -4), Math.pow(10, -5), Math.pow(10, -6)};
        String path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/synthetic_keywords_" + doc_size + "_" + max_size + ".csv";
//        String path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/sample_keywords.csv";
        int lambda = 128;
        double start, end;
        CoverFamily cff = new CoverFamily(q, d);
        int chi = cff.getLines();
        GarbledBloomFilter gbfilter = new GarbledBloomFilter(max_size, fPP[mode]);
        int num_slots = gbfilter.getNumSlots();
        int num_bits_per_slots = gbfilter.getNumBitsPerSlot();

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
//            ivs = Utils.read_mac(iv_path, chi);
//            MACs = Utils.read_mac(mac_path, chi);
        }
        for (int repeat = 0; repeat < times; repeat++) {
            int real_num_docs = 0;
            Bits[] enc_ctx = new Bits[doc_size];
            try {
                FileInputStream fstream = new FileInputStream(path);
                BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
                start = System.nanoTime();
                String strLine;
                while ((strLine = br.readLine()) != null) {
                    GarbledVector gvector = new GarbledVector(num_slots, num_bits_per_slots);
                    List<String> word = Arrays.asList(strLine.split(","));
                    if (word.size() > max_size) {
                        System.out.println("Error: The maximum number of keywords is not correct: " + word.size());
                        System.exit(-1);
                    }
                    for (int i = 0; i < word.size(); i++) {
                        gvector = gbfilter.insert(word.get(i), gvector);
                    }
                    Bits[] ctx = new Bits[num_slots];
                    int start_col = real_num_docs * num_bits_per_slots;
                    int end_col = (real_num_docs + 1) * num_bits_per_slots;
                    enc_ctx[real_num_docs] = gvector.getall();
                    for (int j = 0; j < num_slots; j++) {
                        ctx[j] = gvector.get(j);
                        ctx[j].xor(ext_key_col[j].get(start_col, end_col));
                    }
                    enc_ctx[real_num_docs] = Utils.concatenate(ctx);
                    enc_ctx[real_num_docs].xor(ext_key_row[real_num_docs]);
//                enc_ctx[real_num_docs].xor(Utils.read_keys(row_key_original_path,row_len,real_num_docs));
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
