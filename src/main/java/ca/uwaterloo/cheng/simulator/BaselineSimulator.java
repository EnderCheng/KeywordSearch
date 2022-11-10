package ca.uwaterloo.cheng.simulator;

import ca.uwaterloo.cheng.modules.*;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.checkerframework.checker.guieffect.qual.UI;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class BaselineSimulator {
    public static void main(String[] args) {
        int times = 1;
        double search_delay = 0;
        double client_gen_delay = 0;
        double client_dec_delay = 0;
        double uplink = 0;
        double downlink = 0;

            boolean isMAC = false;
            int max_size = 1000;
            int mode = 1;
            int doc_size = 4000;
            double[] fPP = new double[]{Math.pow(10, -3), Math.pow(10, -4), Math.pow(10, -5), Math.pow(10, -6)};
            int lambda = 128;
            double start, end;
            int num_search = 1;
            String[] search_key_words = new String[num_search];
            for (int i = 0; i < num_search; i++) {
                search_key_words[i] = "search" + i;
            }

            BloomFilter bfilter = new BloomFilter(max_size, fPP[mode]);
            int num_bits_per_row = bfilter.getNumSlots();
            Bits[] index = new Bits[num_bits_per_row];
            Bits[] all_dec_keys = new Bits[num_bits_per_row];
            SecureRandom random = new SecureRandom();

            for (int i = 0; i < num_bits_per_row; i++) {
                index[i] = Utils.get_random_rits(doc_size);
                all_dec_keys[i] = Utils.get_random_rits(doc_size);
            }
//        Bits[] index = Utils.readBaselineIndex(keyword_path, doc_size, max_size, num_bits_per_row);
//        Bits[] all_dec_keys = Utils.read_all_keys(key_path, doc_size, num_bits_per_row);
            byte[][] MACs = new byte[num_bits_per_row][MAC.MACBYTES];
            Random rd = new Random();
            MAC mac = null;
            if (isMAC) {
                byte[] mackey = new byte[MAC.KEYBYTES];
                rd.nextBytes(mackey);
                mac = new MAC(mackey);
                for (int i = 0; i < num_bits_per_row; i++) {
                    rd.nextBytes(MACs[i]);
                }
//            MACs = Utils.read_mac(mac_path, num_bits_per_row);
            }
        for (int repeat = 0; repeat < times; repeat++) {
            // Client - Search
            start = System.nanoTime();
            Set<Integer> joinedSet = new HashSet<>();
            for (int i = 0; i < search_key_words.length; i++) {
                joinedSet.addAll(bfilter.getHashPositions(search_key_words[i]));
            }
            ArrayList<Integer> positions = new ArrayList<>(joinedSet);
            int bit_len = Utils.len_long(num_bits_per_row);
//            System.out.println("test:"+num_bits_per_row);
            DPF dpf = new DPF(lambda, bit_len);
            Bits[] inputs = new Bits[positions.size()];
            Bits[][] queries = new Bits[inputs.length][];
            for (int i = 0; i < inputs.length; i++) {
                inputs[i] = Utils.long_to_bits(positions.get(i), bit_len);
                queries[i] = dpf.Gen(inputs[i]);
            }
            end = System.nanoTime();
//            System.out.println("Client - search query length (Server_0):" + queries[0][0].length() * inputs.length);
//            System.out.println("Client - search query length (Server_1):" + queries[0][1].length() * inputs.length);
//            System.out.println("Client - search query generation latency:" + (end - start) / 1000000);
//        time_eval[2] = time_eval[2] + (end - start) / 1000000;
            client_gen_delay = client_gen_delay + (end - start) / 1000000;
            uplink = uplink + queries[0][0].length() * inputs.length;
//            System.out.println("uplink analysis: "+ bfilter.getNumHashes()*(lambda+(lambda+2)*bit_len));
//            System.out.println("len:"+inputs.length);
//            System.out.println("len:"+queries[0][0].length());
//            System.out.println("uplink:"+uplink);

            int num_of_servers = 2;
            CountDownLatch countDownLatch = new CountDownLatch(num_of_servers);

            //Server_0 - Search
//            System.out.println("Start Searching...");
            start = System.nanoTime();
            byte[][] mac_0 = new byte[inputs.length][];
            if (isMAC) {
                for (int i = 0; i < inputs.length; i++) {
                    mac_0[i] = new byte[lambda / 8];
                }
            }

            System.out.println("rotate:"+(inputs.length*num_bits_per_row));
            Bits[] res_bits_0 = new Bits[inputs.length];
            byte[][] finalMACs_0 = MACs;
            Thread worker_0 = new Thread(() -> {
                try {
//                    System.out.println("Server_0 Searching...");
                    for (int i = 0; i < inputs.length; i++) {
                        res_bits_0[i] = new Bits(doc_size);
                        for (int j = 0; j < num_bits_per_row; j++) {
                            Bits bits_b = Utils.long_to_bits(j, bit_len);
                            boolean res = dpf.Eval(false, queries[i][0], bits_b);
                            if (res) {
                                res_bits_0[i].xor(index[j]);
                                if (isMAC) {
//                                mac_0[i] = ByteUtils.xor(mac_0[i], Utils.read_mac_baseline(mac_path,j));
                                    mac_0[i] = ByteUtils.xor(mac_0[i], finalMACs_0[j]);
                                }
                            }
                        }
                    }
                    countDownLatch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            //Server_1 - Search
            Bits[] res_bits_1 = new Bits[inputs.length];
            byte[][] mac_1 = new byte[inputs.length][];
            if (isMAC) {
                for (int i = 0; i < inputs.length; i++) {
                    mac_1[i] = new byte[lambda / 8];
                }
            }
            byte[][] finalMACs_1 = MACs;
            Thread worker_1 = new Thread(() -> {
                try {
//                    System.out.println("Server_1 Searching...");
                    for (int i = 0; i < inputs.length; i++) {
                        res_bits_1[i] = new Bits(doc_size);
                        for (int j = 0; j < num_bits_per_row; j++) {
                            Bits bits_b = Utils.long_to_bits(j, bit_len);
                            boolean res = dpf.Eval(true, queries[i][1], bits_b);
                            if (res) {
                                res_bits_1[i].xor(index[j]);
                                if (isMAC) {
//                                mac_1[i] = ByteUtils.xor(mac_1[i], Utils.read_mac_baseline(mac_path,j));
                                    mac_1[i] = ByteUtils.xor(mac_1[i], finalMACs_1[j]);
                                }
                            }
                        }
                    }
                    countDownLatch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            worker_0.start();
            worker_1.start();

            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            end = System.nanoTime();
//            System.out.println("Server_0 - response length:" + res_bits_0[0].length() * inputs.length);
//            System.out.println("Server_1 - response length:" + res_bits_1[1].length() * inputs.length);
//            System.out.println("Server_0 - mac response length:" + mac_0[0].length * inputs.length * 8);
//            System.out.println("Server_1 - mac response length:" + mac_1[0].length * inputs.length * 8);
            downlink = downlink + res_bits_1[1].length() * inputs.length;
//            System.out.println("downlink analysis: "+ bfilter.getNumHashes()*doc_size);
            if (isMAC) {
                downlink = downlink + mac_0[0].length * inputs.length * 8;
//                System.out.println("total downlink analysis: "+ bfilter.getNumHashes()*(doc_size+lambda));
            }
            search_delay = search_delay+ (end - start) / 1000000;
//            System.out.println("Server - search latency:" + (end - start) / 1000000);
//        time_eval[3] = (end - start) / 1000000;
            //Client - Combine Query Results to Recover the Plaintext
            start = System.nanoTime();
            Bits[] res_bits = new Bits[inputs.length];
            int[] pos = positions.stream().mapToInt(i -> i).toArray();

            for (int i = 0; i < inputs.length; i++) {
                byte[] query_mac = null;
                byte[] test_mac = null;
                if (isMAC) {
                    query_mac = ByteUtils.xor(mac_0[i], mac_1[i]);
                    test_mac = new byte[lambda / 8];
                }
                res_bits[i] = new Bits(doc_size);
                res_bits[i].xor(res_bits_0[i]);
                res_bits[i].xor(res_bits_1[i]);
                Bits dec_key = all_dec_keys[pos[i]];
//            Utils.read_keys(key_path,doc_size,pos[i]);
                for (int j = 0; j < doc_size; j++) {
                    boolean value = res_bits[i].get(j);
                    if (isMAC) {
                        String data;
                        if (value) {
                            data = "1" + j + pos[i] + 0;
                        } else {
                            data = "0" + j + pos[i] + 0;
                        }
                        test_mac = ByteUtils.xor(test_mac, mac.create(Utils.stringToBits(data)));
                    }

                }
                if (isMAC) {
                    if (!mac.equal(test_mac, query_mac)) {
//                    System.out.println("Query Results Do not Pass Integrity Check");
                    }
                }
                res_bits[i].xor(dec_key);
            }

            for (int i = 0; i < doc_size; i++) {
                int count = 0;
                for (int j = 0; j < inputs.length; j++) {
                    if (res_bits[j].get(i))
                        count++;
                }
                if (count == inputs.length) {
//                System.out.println("Doc Identifier:" + i);
                }
            }
            end = System.nanoTime();
//            System.out.println("Client - search query decryption latency:" + (end - start) / 1000000);
            client_dec_delay = client_dec_delay + (end - start) / 1000000;
        }
        System.out.println(client_gen_delay/times);
        System.out.println(search_delay/times);
        System.out.println(client_dec_delay/times);
        System.out.println("total:"+((client_gen_delay+search_delay+client_dec_delay)/times));
        System.out.println(uplink/times);
        System.out.println(downlink/times);
        System.out.println("total comm:"+((uplink+downlink)/times));
    }
}
