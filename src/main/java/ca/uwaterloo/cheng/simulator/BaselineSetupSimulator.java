package ca.uwaterloo.cheng.simulator;

import ca.uwaterloo.cheng.modules.*;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class BaselineSetupSimulator {
    public static void main(String[] args) {
        double totaltime = 0;
        int times = 1;
        int ver = 0;
        boolean isMAC = true;
        int max_size = 1000;
        int mode = 2;
        int doc_size = 4000;
//        String path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/synthetic_keywords_" + doc_size + "_" + max_size + ".csv";
        String path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/sample_keywords.csv";
        double[] fPP = new double[]{Math.pow(10, -3), Math.pow(10, -4), Math.pow(10, -5), Math.pow(10, -6)};
        int lambda = 128;
        double start, end;
//        int num_search = 1;
//        String[] search_key_words = new String[num_search];
//        for (int i = 0; i < num_search; i++) {
//            search_key_words[i] = "search" + i;
//        }
        BloomFilter bfilter = new BloomFilter(max_size, fPP[mode]);
        int num_bits_per_row = bfilter.getNumSlots();
        SecureRandom random = new SecureRandom();
        Bits[] ext_key = new Bits[doc_size];
        for (int i = 0; i < doc_size; i++) {
            ext_key[i] = Utils.get_random_rits(num_bits_per_row);
        }

        Bits[] index = new Bits[num_bits_per_row];
        for (int i = 0; i < num_bits_per_row; i++) {
            index[i] = Utils.get_random_rits(doc_size);
        }

        Random rd = new Random();
        MAC mac = null;
        if (isMAC) {
            byte[] mackey = new byte[MAC.KEYBYTES];
            rd.nextBytes(mackey);
            mac = new MAC(mackey);
        }
        for (int repeat = 0; repeat < times; repeat++) {
            int real_num_docs = 0;
            Bits[] ciphers = new Bits[doc_size];
            try {
                FileInputStream fstream = new FileInputStream(path);
                BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
                String strLine;
                start = System.nanoTime();
                while ((strLine = br.readLine()) != null) {
                    BloomVector bvector = new BloomVector(num_bits_per_row);
                    List<String> word = Arrays.asList(strLine.split(","));
                    if (word.size() > max_size) {
                        System.out.println("Error: The maximum number of keywords is not correct: " + word.size());
                        System.exit(-1);
                    }
//                double test1 = System.nanoTime();
                    for (int i = 0; i < word.size(); i++) {
                        bvector = bfilter.insert(word.get(i), bvector);
                    }
                    ciphers[real_num_docs] = Utils.boolarray_to_bits(bvector.getBitVector());
                    ciphers[real_num_docs].xor(ext_key[real_num_docs]);
                    real_num_docs++;
                }
                end = System.nanoTime();
                totaltime = totaltime + (end - start) / 1000000;
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
                byte[][] MACs = new byte[num_bits_per_row][];
                for (int i = 0; i < num_bits_per_row; i++) {
                    MACs[i] = new byte[MAC.MACBYTES];
                    for (int j = 0; j < doc_size; j++) {
                        boolean value = index[i].get(j);
                        String data;
                        if (value) {
                            data = "1" + j + i + ver;
                        } else {
                            data = "0" + j + i + ver;
                        }
                        MACs[i] = ByteUtils.xor(MACs[i], mac.create(Utils.stringToBits(data)));
                    }
                }
                end = System.nanoTime();
                totaltime = totaltime + (end - start) / 1000000;
            }
        }
        System.out.println(totaltime / times);
    }
}
