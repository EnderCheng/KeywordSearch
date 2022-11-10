package ca.uwaterloo.cheng.simulator;

import ca.uwaterloo.cheng.modules.*;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class ODPFSimulator {
    public static void main(String[] args) {
        int times = 1;
        double search_delay = 0;
        double client_gen_delay = 0;
        double client_dec_delay = 0;
        double uplink = 0;
        double downlink = 0;

        int q = 1;
        int d = 1;
        boolean isMAC = true;
        int max_size = 1000;
        int mode = 1;
        int doc_size = 4000;
        int M = max_size/10;
        double[] fPP = new double[]{Math.pow(10, -3), Math.pow(10, -4), Math.pow(10, -5), Math.pow(10, -6)};
        int lambda = 128;
        double start, end;
        int num_search = 1;
        String[] search_key_words = new String[num_search];
        for (int i = 0; i < num_search; i++) {
            search_key_words[i] = "search" + i;
        }
        CoverFamily cff = new CoverFamily(q, d);
        int chi = cff.getLines();

        GarbledBloomFilter gbfilter = new GarbledBloomFilter(max_size, fPP[mode]);
        int num_slots = gbfilter.getNumSlots();
        int num_bits_per_slots = gbfilter.getNumBitsPerSlot();
//        System.out.println(num_bits_per_slots);

//        if (M > num_slots) {
//            System.out.println("M should not be larger than the number of slots decided by GBF!");
//            System.exit(-1);
//        }
        int slot_len = (int) Math.ceil((num_slots + 0.0) / M);
        System.out.println("Segment number:" + M);
        System.out.println("Each segment' length:" + slot_len);
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

        // Client - Search
        for (int repeat = 0; repeat < times; repeat++) {
            start = System.nanoTime();
            List<Integer> FullList = new ArrayList<>();
            for (int i = 0; i < search_key_words.length; i++) {
                FullList.addAll(gbfilter.getHashPositions(search_key_words[i]));
            }
            Map<Integer, Long> counts = FullList.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
            ArrayList<Integer> positions = new ArrayList<>();
            for (Map.Entry<Integer, Long> entry : counts.entrySet()) {
                int key = entry.getKey();
                long value = entry.getValue();
//            System.out.println("key:" + key);
                if (value % 2 != 0) {
                    positions.add(key);
                }
            }
            Bits[] segs = new Bits[M];
            for (int i = 0; i < M; i++) {
                segs[i] = new Bits(slot_len);
            }

            Set<Integer> inv_segs = new HashSet<>();
            for (int i = 0; i < positions.size(); i++) {
                int map_in = positions.get(i) % slot_len;
                int pos = (int) Math.floor((positions.get(i) + 0.0) / slot_len);
                segs[pos].set(map_in);
                inv_segs.add(pos);
            }

            long[] alpha = inv_segs.stream().mapToLong(Number::longValue).toArray();
//        System.out.println("queries:" + Arrays.toString(alpha));
            Bits[] beta = new Bits[alpha.length];
            for (int i = 0; i < alpha.length; i++) {
                beta[i] = segs[(int) alpha[i]];
//            System.out.println(beta[i]);
            }
//            System.out.println("mhat:"+Utils.cuckoo_params_gen(gbfilter.getNumHashes(),lambda));
            DMPFValue dpmfv = new DMPFValue(lambda, M, lambda, slot_len);
            Bits[] queries = dpmfv.Gen(alpha, beta, gbfilter.getNumHashes() * search_key_words.length);
            end = System.nanoTime();
//            System.out.println("Client - search query length (Server_0):" + queries[0].length());
//            System.out.println("Client - search query length (Server_1):" + queries[1].length());
//            System.out.println("Client - search query generation latency:" + (end - start) / 1000000);
            client_gen_delay = client_gen_delay + (end - start) / 1000000;
            uplink = uplink + queries[0].length();
//            int mhat = Utils.cuckoo_params_gen(gbfilter.getNumHashes(),lambda)*gbfilter.getNumHashes();
//            System.out.println("uplink analysis: "+ (mhat*(lambda+(lambda+2)*Utils.len_long((long)Math.ceil((3*M)/mhat))+slot_len)+lambda));
//            System.out.println("test:" + queries[0].length());

            int num_of_servers = 2;
            CountDownLatch countDownLatch = new CountDownLatch(num_of_servers);

            //Server_0 - Search
//            System.out.println("Start Searching...");
            start = System.nanoTime();
            var ref_0 = new Object() {
                byte[][] mac_0 = new byte[chi][XORMAC.MACBYTES];
            };
            System.out.println("rotate:"+(M*3));
            Bits res_bits_0 = new Bits(num_bits_per_slots * doc_size);
            byte[][] finalMacs_0 = MACs;
            Thread worker_0 = new Thread(() -> {
                try {
                    int num = 0;
//                    System.out.println("Server_0 Searching...");
//                System.out.println(M*slot_len);
//                System.out.println(num_slots);
                    for (int i = 0; i < M; i++) {
                        Bits tmp = dpmfv.Eval(false, queries[0], i, gbfilter.getNumHashes() * search_key_words.length, slot_len);
//                    System.out.println("Server 0_(" + i + "):" + tmp);
                        for (int j = 0; j < slot_len; j++) {
                            if (tmp.get(j)) {
//                                System.out.println("test"+index[num].length());
                                res_bits_0.xor(index[num]);
                                if (isMAC) {
                                    for (int z = 0; z < chi; z++) {
//                                    ref_0.mac_0[z] = ByteUtils.xor(ref_0.mac_0[z], Utils.read_mac_range(mac_path, z,num*XORMAC.MACBYTES,XORMAC.MACBYTES));
                                        ref_0.mac_0[z] = ByteUtils.xor(ref_0.mac_0[z], Utils.get_range(finalMacs_0, z, num * XORMAC.MACBYTES, XORMAC.MACBYTES));
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
                    countDownLatch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            //Server_1 - Search
            Bits res_bits_1 = new Bits(num_bits_per_slots * doc_size);
            var ref_1 = new Object() {
                byte[][] mac_1 = new byte[chi][XORMAC.MACBYTES];
            };
            byte[][] finalMacs_1 = MACs;
            Thread worker_1 = new Thread(() -> {
                try {
                    int num = 0;
//                    System.out.println("Server_1 Searching...");
                    for (int i = 0; i < M; i++) {
                        Bits tmp = dpmfv.Eval(true, queries[1], i, gbfilter.getNumHashes() * search_key_words.length, slot_len);
//                    System.out.println("Server 1_(" + i + "):" + tmp);
                        for (int j = 0; j < slot_len; j++) {
                            if (tmp.get(j)) {
                                res_bits_1.xor(index[num]);
                                if (isMAC) {
                                    for (int z = 0; z < chi; z++) {
//                                    ref_1.mac_1[z] = ByteUtils.xor(ref_1.mac_1[z], Utils.read_mac_range(mac_path, z,num*XORMAC.MACBYTES,XORMAC.MACBYTES));
                                        ref_1.mac_1[z] = ByteUtils.xor(ref_1.mac_1[z], Utils.get_range(finalMacs_1, z, num * XORMAC.MACBYTES, XORMAC.MACBYTES));
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
//            System.out.println("Server_0 - response length:" + res_bits_0.length());
//            System.out.println("Server_1 - response length:" + res_bits_1.length());
//            System.out.println("Server_0 - mac response length:" + ref_0.mac_0[0].length * chi * 8);
//            System.out.println("Server_1 - mac response length:" + ref_1.mac_1[0].length * chi * 8);
//            System.out.println("Server - search latency:" + (end - start) / 1000000);

            downlink = downlink + res_bits_0.length();
//            System.out.println("downlink analysis: "+ gbfilter.getNumHashes()*doc_size);
            if (isMAC) {
                downlink = downlink + ref_0.mac_0[0].length * chi * 8;
//                System.out.println("total downlink analysis: "+ (gbfilter.getNumHashes()*doc_size+chi*lambda));
            }
            search_delay = search_delay + (end - start) / 1000000;


            //Client - Combine Query Results to Recover the Plaintext
            start = System.nanoTime();
            Bits res_bits = new Bits(key_len);
            int[] pos = positions.stream().mapToInt(i -> i).toArray();
            res_bits.xor(res_bits_0);
            res_bits.xor(res_bits_1);

            if (isMAC) {
                for (int z = 0; z < chi; z++) {
                    byte[] tmp = new byte[XORMAC.MACBYTES];
                    byte[] query_mac = ByteUtils.xor(ref_0.mac_0[z], ref_1.mac_1[z]);
                    byte[] randomness = new byte[XORMAC.MACBYTES];
                    for (int i = 0; i < pos.length; i++) {
//                    randomness = ByteUtils.xor(randomness,Utils.read_mac_range(iv_path,z,pos[i]*XORMAC.MACBYTES,XORMAC.MACBYTES));
                        randomness = ByteUtils.xor(randomness, Utils.get_range(ivs, z, pos[i] * XORMAC.MACBYTES, XORMAC.MACBYTES));
                    }
                    for (int j = 0; j < doc_size; j++) {
                        Bits in_bits = res_bits.get(j * num_bits_per_slots, (j + 1) * num_bits_per_slots);
                        tmp = ByteUtils.xor(tmp, mac[z].create_without_iv(in_bits));
                    }
//                GF2_128 out = new GF2_128();
//                GF2_128.add(out, new GF2_128(tmp), new GF2_128(randomness));
//                System.out.println();
                    byte[] test_bytes = ByteUtils.xor(tmp, randomness);
                    if (!Arrays.equals(test_bytes, query_mac)) {

                    }
//                    System.out.println("Query Results Do not Pass Integrity Check");
                }
            }

            Bits col_keys = new Bits(key_len);
            Bits row_keys = new Bits(key_len);
            for (int i = 0; i < pos.length; i++) {
                col_keys.xor(ext_key_col[pos[i]]);
//                    Utils.read_keys(col_key_path, key_len, pos[i]);
                row_keys.xor(ext_key_row[pos[i]]);
//                    Utils.read_keys(row_key_path, key_len, pos[i]);
            }
            res_bits.xor(col_keys);
            res_bits.xor(row_keys);

            Bits testbits = new Bits(num_bits_per_slots);
            for (int i = 0; i < search_key_words.length; i++) {
                testbits.xor(Murmur3Hash.fingerprint(search_key_words[i].getBytes(StandardCharsets.UTF_8), num_bits_per_slots));
            }

            for (int i = 0; i < doc_size; i++) {
                if (testbits.equals(res_bits.get(i * num_bits_per_slots, (i + 1) * num_bits_per_slots))) {
//                System.out.println("Doc Identifier:" + i);
                }
            }
            end = System.nanoTime();
//            System.out.println("Client - search query decryption latency:" + (end - start) / 1000000);
            client_dec_delay = client_dec_delay + (end - start) / 1000000;
        }
        System.out.println(client_gen_delay / times);
        System.out.println(search_delay / times);
        System.out.println(client_dec_delay / times);
        System.out.println("total:"+((client_gen_delay+search_delay+client_dec_delay)/times));
        System.out.println(uplink / times);
        System.out.println(downlink / times);
        System.out.println("total comm:"+((uplink+downlink)/times));
    }
}
