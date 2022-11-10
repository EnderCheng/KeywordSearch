package ca.uwaterloo.cheng.simulator;

import ca.uwaterloo.cheng.modules.*;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class VDPFSimulator {
    public static void main(String[] args) {
        int times = 1;
        double search_delay = 0;
        double client_gen_delay = 0;
        double client_dec_delay = 0;
        double uplink = 0;
        double downlink = 0;

        int q = 1;
        int d = 1;
        boolean isMAC = false;
        int max_size = 1000;
        int mode = 1;
        int doc_size = 4000;
        double[] fPP = new double[]{Math.pow(10, -3), Math.pow(10, -4), Math.pow(10, -5), Math.pow(10, -6)};
        int lambda = 128;
        int M = max_size/10;
        double start, end;
        int num_search = 1;
        String[] search_key_words = new String[num_search];
        for (int i = 0; i < num_search; i++) {
            search_key_words[i] = "search" + i;
        }
        CoverFamily cff = new CoverFamily(q, d);
        int chi = cff.getLines();
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
//        Bits[] index = Utils.readODPFIndex(keyword_path, num_slots, num_bits_per_slots, doc_size);
//        Bits[] ext_key_col = Utils.read_all_keys(col_key_path, key_len, num_slots);
//        Bits[] ext_key_row = Utils.read_all_keys(row_key_path, key_len, num_slots);
        byte[][] ivs = null;
        byte[][] MACs = null;
        Random rd = new Random();
        XORMAC[] mac = new XORMAC[chi];
        if (isMAC) {
            ivs = new byte[chi][num_slots * XORMAC.MACBYTES];
            MACs = new byte[chi][num_slots * XORMAC.MACBYTES];
            byte[][] mackeys = new byte[chi][XORMAC.KEYBYTES];
            for (int i = 0; i < chi; i++) {
                rd.nextBytes(mackeys[i]);
                mac[i] = new XORMAC(mackeys[i]);
                rd.nextBytes(ivs[i]);
                rd.nextBytes(MACs[i]);
            }
//            ivs = Utils.read_mac(iv_path, chi);
//            MACs = Utils.read_mac(mac_path, chi);
        }
        for (int repeat = 0; repeat < times; repeat++) {
            // Client - Search
            start = System.nanoTime();
            int bit_len = Utils.len_long(M);
            DPFValue dpfValue = new DPFValue(lambda, bit_len, slots_per_buckets);
            Bits[][][] keys = new Bits[search_key_words.length][][];
            for (int i = 0; i < search_key_words.length; i++) {
                int[] indexes = cf.getBuckets(search_key_words[i]);
//            System.out.println("indexes:"+ Arrays.toString(indexes));
                keys[i] = new Bits[indexes.length][];
                for (int j = 0; j < indexes.length; j++) {
                    int bucket_num = indexes[j];
                    Bits input = Utils.long_to_bits(bucket_num, bit_len);
                    ArrayList<Integer> positions = gbf.getHashPositions(search_key_words[i]);
                    Bits val = new Bits(slots_per_buckets);
                    for (int z = 0; z < positions.size(); z++) {
                        val.set(positions.get(z));
                    }
                    keys[i][j] = dpfValue.Gen(input, val);
                }
            }
//        segs[indexes[0]] = null;
//        gbf.getHashPositions(search_key_words[i]);

            end = System.nanoTime();
//            System.out.println("Client - search query length (Server_0):" + search_key_words.length * 2 * keys[0][0][0].length());
//            System.out.println("Client - search query length (Server_1):" + search_key_words.length * 2 * keys[0][0][1].length());
//            System.out.println("Client - search query generation latency:" + (end - start) / 1000000);
            client_gen_delay = client_gen_delay + (end - start) / 1000000;
            uplink = uplink + search_key_words.length * 2 * keys[0][0][0].length();
//            System.out.println("uplink analysis: "+ (2*(lambda+(lambda+2)*bit_len+slots_per_buckets)));

            int num_of_servers = 2;
            CountDownLatch countDownLatch = new CountDownLatch(num_of_servers);

            //Server_0 - Search
//            System.out.println("Start Searching...");
            start = System.nanoTime();
            var ref_0 = new Object() {
                byte[][][][] mac_0 = new byte[search_key_words.length][2][chi][XORMAC.MACBYTES];
            };

            Bits[][] res_bits_0 = new Bits[search_key_words.length][2];
            for (int i = 0; i < search_key_words.length; i++) {
                for (int j = 0; j < 2; j++) {
                    res_bits_0[i][j] = new Bits(num_bits_per_slots * doc_size);
                }
            }
            System.out.println("rotate:"+(M*2));
            byte[][] finalMacs_0 = MACs;
            Thread worker_0 = new Thread(() -> {
                try {
//                    System.out.println("Server_0 Searching...");
                    for (int i = 0; i < search_key_words.length; i++) {
                        for (int j = 0; j < 2; j++) {
                            int num = 0;
                            for (int z = 0; z < M; z++) {
                                Bits tmp = dpfValue.Eval(false, keys[i][j][0], Utils.long_to_bits(z, bit_len));
//                            System.out.println(j+"-Server 0_(" + z + "):" + tmp);
                                for (int y = 0; y < slots_per_buckets; y++) {
                                    if (tmp.get(y)) {
                                        res_bits_0[i][j].xor(index[num]);
                                        if (isMAC) {
                                            for (int x = 0; x < chi; x++) {
                                                ref_0.mac_0[i][j][x] = ByteUtils.xor(ref_0.mac_0[i][j][x], Utils.get_range(finalMacs_0, x, num * XORMAC.MACBYTES, XORMAC.MACBYTES));
                                            }
                                        }
                                    }
                                    num++;
                                    if (num == num_slots)
                                        break;
                                }
                                if (num == num_slots)
                                    break;
                            }
                        }
                    }
                    countDownLatch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            //Server_1 - Search
            start = System.nanoTime();
            var ref_1 = new Object() {
                byte[][][][] mac_1 = new byte[search_key_words.length][2][chi][XORMAC.MACBYTES];
            };

            Bits[][] res_bits_1 = new Bits[search_key_words.length][2];
            for (int i = 0; i < search_key_words.length; i++) {
                for (int j = 0; j < 2; j++) {
                    res_bits_1[i][j] = new Bits(num_bits_per_slots * doc_size);
                }
            }

            byte[][] finalMacs_1 = MACs;
            Thread worker_1 = new Thread(() -> {
                try {
//                    System.out.println("Server_1 Searching...");
                    for (int i = 0; i < search_key_words.length; i++) {
                        for (int j = 0; j < 2; j++) {
                            int num = 0;
                            for (int z = 0; z < M; z++) {
                                Bits tmp = dpfValue.Eval(true, keys[i][j][1], Utils.long_to_bits(z, bit_len));
//                            System.out.println(j+"-Server 1_(" + z + "):" + tmp);
                                for (int y = 0; y < slots_per_buckets; y++) {
                                    if (tmp.get(y)) {
                                        res_bits_1[i][j].xor(index[num]);
                                        if (isMAC) {
                                            for (int x = 0; x < chi; x++) {
                                                ref_1.mac_1[i][j][x] = ByteUtils.xor(ref_1.mac_1[i][j][x], Utils.get_range(finalMacs_1, x, num * XORMAC.MACBYTES, XORMAC.MACBYTES));
                                            }
                                        }
                                    }
                                    num++;
                                    if (num == num_slots)
                                        break;
                                }
                                if (num == num_slots)
                                    break;
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
//            System.out.println("Server_0 - response length:" + search_key_words.length * 2 * res_bits_0[0][0].length());
//            System.out.println("Server_1 - response length:" + search_key_words.length * 2 * res_bits_1[0][0].length());
//            System.out.println("Server_0 - mac response length:" + search_key_words.length * 2 * chi * XORMAC.MACBYTES * 8);
//            System.out.println("Server_1 - mac response length:" + search_key_words.length * 2 * chi * XORMAC.MACBYTES * 8);
//            System.out.println("Server - search latency:" + (end - start) / 1000000);

            downlink = downlink + search_key_words.length * 2 * res_bits_0[0][0].length();
//            System.out.println("downlink analysis: "+ (2*doc_size*num_bits_per_slots));
            if (isMAC) {
                downlink = downlink + search_key_words.length * 2 * chi * XORMAC.MACBYTES * 8;
//                System.out.println("total downlink analysis: "+ (2*(doc_size*num_bits_per_slots+chi*lambda)));
            }
            search_delay = search_delay+ (end - start) / 1000000;


            //Client - Combine Query Results to Recover the Plaintext
            start = System.nanoTime();
            for (int i = 0; i < search_key_words.length; i++) {
                ArrayList<Integer> positions = gbf.getHashPositions(search_key_words[i]);
                int[] pos = positions.stream().mapToInt(l -> l).toArray();
                int[] indexes = cf.getBuckets(search_key_words[i]);
                Bits[] res_bits = new Bits[2];
                for (int j = 0; j < 2; j++) {
                    res_bits[j] = new Bits(key_len);
                    res_bits[j].xor(res_bits_0[i][j]);
                    res_bits[j].xor(res_bits_1[i][j]);
//                System.out.println("res:"+res_bits);
                    if (isMAC) {
                        for (int z = 0; z < chi; z++) {
                            byte[] tmp = new byte[XORMAC.MACBYTES];
                            byte[] query_mac = ByteUtils.xor(ref_0.mac_0[i][j][z], ref_1.mac_1[i][j][z]);
                            byte[] randomness = new byte[XORMAC.MACBYTES];
                            for (int x = 0; x < pos.length; x++) {
                                randomness = ByteUtils.xor(randomness, Utils.get_range(ivs, z, (indexes[j] * slots_per_buckets + pos[x]) * XORMAC.MACBYTES, XORMAC.MACBYTES));
                            }
                            for (int y = 0; y < doc_size; y++) {
                                Bits in_bits = res_bits[j].get(y * num_bits_per_slots, (y + 1) * num_bits_per_slots);
                                tmp = ByteUtils.xor(tmp, mac[z].create_without_iv(in_bits));
                            }
                            byte[] test_bytes = ByteUtils.xor(tmp, randomness);
                            if (!Arrays.equals(test_bytes, query_mac)) {
//                            System.out.println("Query Results Do not Pass Integrity Check");
                            }
                        }
                    }

                    Bits col_keys = new Bits(key_len);
                    Bits row_keys = new Bits(key_len);
                    for (int x = 0; x < pos.length; x++) {
                        col_keys.xor(ext_key_col[indexes[j] * slots_per_buckets + pos[x]]);
//                    Utils.read_keys(col_key_path, key_len, pos[i]);
                        row_keys.xor(ext_key_row[indexes[j] * slots_per_buckets + pos[x]]);
//                    Utils.read_keys(row_key_path, key_len, pos[i]);
                    }
                    res_bits[j].xor(col_keys);
                    res_bits[j].xor(row_keys);
                }

                Bits testbits = new Bits(num_bits_per_slots);
                testbits.xor(Murmur3Hash.fingerprint(search_key_words[i].getBytes(StandardCharsets.UTF_8), num_bits_per_slots));
//                System.out.println("test:"+Murmur3Hash.fingerprint(search_key_words[i].getBytes(StandardCharsets.UTF_8), num_bits_per_slots));

                for (int x = 0; x < doc_size; x++) {
                    if (testbits.equals(res_bits[0].get(x * num_bits_per_slots, (x + 1) * num_bits_per_slots)) || testbits.equals(res_bits[1].get(x * num_bits_per_slots, (x + 1) * num_bits_per_slots))) {
//                    System.out.println("Doc Identifier:" + x +", keyword:"+search_key_words[i]);
                    }
                }
            }

            end = System.nanoTime();
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

