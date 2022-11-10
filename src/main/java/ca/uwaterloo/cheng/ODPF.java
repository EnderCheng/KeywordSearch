package ca.uwaterloo.cheng;

import ca.uwaterloo.cheng.modules.*;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.checkerframework.checker.guieffect.qual.UI;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class ODPF {

    private final double[] fPP = new double[]{Math.pow(10, -3), Math.pow(10, -4), Math.pow(10, -5), Math.pow(10, -6)};
    private int ver = 0;
    private int lambda = 128;

    private String path;

    private String[] search_key_words;
    private int max_size, doc_size, mode;
    private PropertiesCache properties;
    private Bits mkey;
    private Bits source_key;
    private XORMAC[] mac;

    private String keyword_path, mac_path, col_key_path, row_key_path, row_key_original_path, iv_path;

    private double start, end;

    private int chi;

    private byte[][] mackeys;

    private Bits[] ivkeys;

    private int q, d;

    private CoverFamily cff;

    public ODPF(String path, int max_size, int doc_size, int mode, String[] search_key_words, int q, int d) {
        this.path = path;
        this.max_size = max_size;
        this.doc_size = doc_size;
        this.search_key_words = search_key_words.clone();
        this.mode = mode;
        properties = new PropertiesCache();
        this.q = q;
        this.d = d;
        source_key = Utils.base64ToBits(properties.read("MACKey_1"), lambda);
        mkey = Utils.base64ToBits(properties.read("Key1"), lambda);
        System.out.println("\nGenerating Cover Family Matrix...");
        cff = new CoverFamily(q, d);
        this.chi = cff.getLines();
        System.out.println("Number of MACs per column: " + chi);
        mac = new XORMAC[chi];
        mackeys = new byte[chi][];
        ivkeys = new Bits[chi];
        for (int i = 0; i < chi; i++) {
            mackeys[i] = Utils.prf_to_len(source_key, Utils.stringToBits("MAC" + i, lambda), lambda).toByteArray();
            ivkeys[i] = Utils.prf_to_len(mkey, Utils.byteArrayToBits(mackeys[i], lambda), lambda);
            mac[i] = new XORMAC(mackeys[i]);
        }
        setName(max_size, doc_size);
    }

    public ODPF(String path, int max_size, int doc_size, int mode, String search_key_word, int q, int d) {
        this(path, max_size, doc_size, mode, new String[]{search_key_word}, q, d);
    }

    private void setName(int max_size, int doc_size) {
        keyword_path = "ODPF_keyword_index_" + max_size + "_" + doc_size +"_"+q+"_"+d+".csv";
        mac_path = "ODPF_mac_index_" + max_size + "_" + doc_size + "_"+q+"_"+d+".csv";
        col_key_path = "ODPF_col_keys_" + max_size + "_" + doc_size + "_"+q+"_"+d+".csv";
        row_key_path = "ODPF_row_keys_" + max_size + "_" + doc_size + "_"+q+"_"+d+".csv";
        row_key_original_path = "ODPF_row_keys_original_" + max_size + "_" + doc_size + "_"+q+"_"+d+".csv";
        iv_path = "ODPF_iv_" + max_size + "_" + doc_size + "_"+q+"_"+d+".csv";
    }

    public void BuildIndex(boolean isMAC) {
        ProgressBar progressbar = new ProgressBar();
        GarbledBloomFilter gbfilter = new GarbledBloomFilter(max_size, fPP[mode]);
        int num_slots = gbfilter.getNumSlots();
        int num_bits_per_slots = gbfilter.getNumBitsPerSlot();
        System.out.println("Length of GBF-encoded Keywords: " + num_slots * num_bits_per_slots);

        System.out.println("Starting Encoding and Encryption...");
        Bits[] ext_key_row = new Bits[doc_size];
        Bits[] ext_key_col = new Bits[num_slots];

        String strLine;
        Bits enc_key_col = Utils.prf_to_len(mkey, Utils.stringToBits("KEYWORDS", lambda), lambda);
        Bits enc_key_row = Utils.prf_to_len(mkey, Utils.stringToBits("DOCUMENTS", lambda), lambda);

        System.out.println("Generating keys for encrypting columns...");
        progressbar.init();
        for (int j = 0; j < num_slots; j++) {
            ext_key_col[j] = PRFCipher.extend_key(PRFCipher.generateKey(enc_key_col, lambda, j, ver), lambda,
                    num_bits_per_slots * doc_size);
            progressbar.update(j, num_slots);
        }
        Utils.write(col_key_path, ext_key_col);
//            ext_key_col = null;

        System.out.println("\nGenerating keys for encrypting rows...");
        progressbar.init();
        for (int i = 0; i < doc_size; i++) {
            ext_key_row[i] = PRFCipher.extend_key(PRFCipher.generateKey(enc_key_row, lambda, i, ver), lambda,
                    num_bits_per_slots * num_slots);
            progressbar.update(i, doc_size);
        }

        Utils.write(row_key_original_path, ext_key_row);
//            enc_key_row = null;
//            Utils.write_row_large(row_key_path,ext_key_row,doc_size,num_slots,num_bits_per_slots);
        Utils.write(row_key_path, Utils.conv(ext_key_row, num_slots, num_bits_per_slots));

        System.out.println("\nCreating outsourced keyword indexes...");
//        System.out.println("Encrypting outsourced keyword indexes by rows...");
//            int col_len = num_bits_per_slots * doc_size;
//            int row_len = num_bits_per_slots * num_slots;
        int real_num_docs = 0;
        Bits[] enc_ctx = new Bits[doc_size];
        progressbar.init();
        try {
            FileInputStream fstream = new FileInputStream(path);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            start = System.nanoTime();
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
////                    ctx[j].xor(Utils.read_keys(col_key_path,col_len,j).get(start_col, end_col));
                    ctx[j].xor(ext_key_col[j].get(start_col, end_col));
                }
                enc_ctx[real_num_docs] = Utils.concatenate(ctx);
                enc_ctx[real_num_docs].xor(ext_key_row[real_num_docs]);
//                enc_ctx[real_num_docs].xor(Utils.read_keys(row_key_original_path,row_len,real_num_docs));
                progressbar.update(real_num_docs, doc_size);
                real_num_docs++;
            }
            end = System.nanoTime();
            if (real_num_docs != doc_size) {
                System.out.println("Error: The number of documents is not correct: " + real_num_docs);
                System.exit(-1);
            }
            fstream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Bits[] index = Utils.conv(enc_ctx, num_slots, num_bits_per_slots);
        Utils.write(keyword_path, index);

        System.out.println("\nSetup latency:" + (end - start) / 1000000);
    }

    public void CreateMAC(boolean isMAC)
    {
        if (isMAC) {
            ProgressBar progressbar = new ProgressBar();
            GarbledBloomFilter gbfilter = new GarbledBloomFilter(max_size, fPP[mode]);
            int num_slots = gbfilter.getNumSlots();
            int num_bits_per_slots = gbfilter.getNumBitsPerSlot();
            Bits[] index = Utils.readODPFIndex(keyword_path, num_slots, num_bits_per_slots, doc_size);

            start = System.nanoTime();
            byte[][] all_doc_rnds = new byte[chi][num_slots * XORMAC.MACBYTES];
            System.out.println("\nStart generating MACs:"+chi);
            progressbar.init();
            int total = chi * doc_size;
            int count = 0;
            for (int i = 0; i < chi; i++) {
                for (int j = 0; j < doc_size; j++) {
                    byte[] Gamma = Utils.prf_iv_doc(ivkeys[i], "random" + j + ver + "iv", lambda, num_slots);
                    all_doc_rnds[i] = ByteUtils.xor(all_doc_rnds[i], Gamma);
                    progressbar.update(count, total);
                    count++;
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

            Utils.write_mac_baseline(iv_path, all_doc_rnds, chi);
            Utils.write_mac_baseline(mac_path, macs, chi);
            System.out.println("\nMAC latency:" + (end - start) / 1000000);
        }
    }

    public void SearchTest(boolean isMAC, int M) {
        GarbledBloomFilter gbfilter = new GarbledBloomFilter(max_size, fPP[mode]);
        int num_slots = gbfilter.getNumSlots();
        int num_bits_per_slots = gbfilter.getNumBitsPerSlot();
//        if (M > num_slots) {
//            System.out.println("M should not be larger than the number of slots decided by GBF!");
//            System.exit(-1);
//        }
        int slot_len = (int) Math.ceil((num_slots + 0.0) / M);
        System.out.println("Each segment' length:" + slot_len);
        Bits[] index = Utils.readODPFIndex(keyword_path, num_slots, num_bits_per_slots, doc_size);
        int key_len = num_bits_per_slots * doc_size;
        Bits[] ext_key_col = Utils.read_all_keys(col_key_path, key_len, num_slots);
        Bits[] ext_key_row = Utils.read_all_keys(row_key_path, key_len, num_slots);
        byte[][] ivs = null;
        byte[][] MACs = null;
        if(isMAC) {
            ivs = Utils.read_mac(iv_path, chi);
            MACs = Utils.read_mac(mac_path, chi);
        }

        // Client - Search
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
        DMPFValue dpmfv = new DMPFValue(lambda, M, lambda, slot_len);
        Bits[] queries = dpmfv.Gen(alpha, beta, gbfilter.getNumHashes() * search_key_words.length);
        end = System.nanoTime();
        System.out.println("Client - search query generation latency:" + (end - start) / 1000000);

        int num_of_servers = 2;
        CountDownLatch countDownLatch = new CountDownLatch(num_of_servers);

        //Server_0 - Search
        System.out.println("Start Searching...");
        start = System.nanoTime();
        var ref_0 = new Object() {
            byte[][] mac_0 = new byte[chi][XORMAC.MACBYTES];
        };

        Bits res_bits_0 = new Bits(num_bits_per_slots * doc_size);
        byte[][] finalMacs_0 = MACs;
        Thread worker_0 = new Thread(() -> {
            try {
                int num = 0;
                System.out.println("Server_0 Searching...");
//                System.out.println(M*slot_len);
//                System.out.println(num_slots);
                for (int i = 0; i < M; i++) {
                    Bits tmp = dpmfv.Eval(false, queries[0], i, gbfilter.getNumHashes() * search_key_words.length, slot_len);
//                    System.out.println("Server 0_(" + i + "):" + tmp);
                    for (int j = 0; j < slot_len; j++) {
                        if (tmp.get(j)) {
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
                System.out.println("Server_1 Searching...");
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
        System.out.println("Server - search latency:" + (end - start) / 1000000);


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
                if (!Arrays.equals(test_bytes, query_mac))
                    System.out.println("Query Results Do not Pass Integrity Check");
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
                System.out.println("Doc Identifier:" + i);
            }
        }
        end = System.nanoTime();
        System.out.println("Client - search query decryption latency:" + (end - start) / 1000000);
    }

    public void UpdateSim(boolean isMAC) {
        System.out.println("Client Updating...");
        start = System.nanoTime();
        GarbledBloomFilter gbfilter = new GarbledBloomFilter(max_size, fPP[mode]);
        int num_slots = gbfilter.getNumSlots();
        int num_bits_per_slots = gbfilter.getNumBitsPerSlot();
        Bits[] ext_key_col = new Bits[num_slots];
        System.out.println("Generating keys for updating...");
        Bits enc_key_col = Utils.prf_to_len(mkey, Utils.stringToBits("KEYWORDS", lambda), lambda);
        Bits enc_key_row = Utils.prf_to_len(mkey, Utils.stringToBits("DOCUMENTS", lambda), lambda);
        Bits new_ext_key_row = PRFCipher.extend_key(PRFCipher.generateKey(enc_key_row, lambda, doc_size + 1, ver), lambda,
                num_bits_per_slots * num_slots);
        byte[][] MACs = null;
        if(isMAC) {
            MACs = Utils.read_mac(mac_path, chi);
        }

        ProgressBar pb = new ProgressBar();
        for (int j = 0; j < num_slots; j++) {
            ext_key_col[j] = PRFCipher.extend_key(PRFCipher.generateKey(enc_key_col, lambda, j, ver), lambda,
                    num_bits_per_slots * (doc_size + 1));
            pb.update(j, num_slots);
        }

        start = System.nanoTime();
        GarbledVector gv = new GarbledVector(num_slots,num_bits_per_slots);
        for (int i = 0; i < max_size; i++) {
            gv = gbfilter.insert("update"+i, gv);
        }
        Bits[] ctx = new Bits[num_slots];
        int start_col = doc_size * num_bits_per_slots;
        int end_col = (doc_size + 1) * num_bits_per_slots;
        for (int j = 0; j < num_slots; j++) {
            ctx[j] = gv.get(j);
//                    ctx[j].xor(Utils.read_keys(col_key_path,col_len,j).get(start_col, end_col));
            ctx[j].xor(ext_key_col[j].get(start_col, end_col));
        }
        Bits enc_ctx = Utils.concatenate(ctx);
        enc_ctx.xor(new_ext_key_row);

        byte[][] update_macs = new byte[chi][num_slots * XORMAC.MACBYTES];
        if(isMAC)
        {

            byte[][] doc_rnds = new byte[chi][num_slots * XORMAC.MACBYTES];
            pb.init();
            for (int i = 0; i < chi; i++) {
                byte[] Gamma = Utils.prf_iv_doc(ivkeys[i], "random" + (doc_size+1) + ver + "iv", lambda, num_slots);
                doc_rnds[i] = ByteUtils.xor(doc_rnds[i], Gamma);
                pb.update(i, chi);
            }

            for (int i = 0; i < chi; i++) {
                byte[][] macs_tmp = new byte[num_slots][XORMAC.MACBYTES];
                for (int z = 0; z < num_slots; z++) {
                    Bits data = new Bits(num_bits_per_slots);
                    data.xor(enc_ctx.get(z*num_bits_per_slots,(z+1)*num_bits_per_slots));
                    macs_tmp[z] = mac[i].create_without_iv(data);
                }
                update_macs[i] = Utils.flatten2DArray(macs_tmp);
                update_macs[i] = ByteUtils.xor(update_macs[i], doc_rnds[i]);
            }
        }
        end = System.nanoTime();
        System.out.println("\nClient - update query generation latency:"+(end-start)/1000000);
        System.out.println("update length:"+(enc_ctx.length()+num_slots * XORMAC.MACBYTES * chi *8));

        System.out.println("Server Updating...");
        start = System.nanoTime();
        if(isMAC) {
            for (int i = 0; i < chi; i++) {
                MACs[i] = ByteUtils.xor(MACs[i], update_macs[i]);
            }
        }
        end = System.nanoTime();
        System.out.println("Server - update latency:"+(end-start)/1000000);
    }


    public static void main(String[] args) {

        String path;
        int max_size, doc_size, mode;
        String[] search_keywords;

        /*
        path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/synthetic_keywords_1000_100.csv";
        max_size = 100;
        doc_size = 1000;
        mode = 3;
        search_keywords = new String[]{"explorer","quotation","divorce"};

        path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/test.csv";
        max_size = 3;
        doc_size = 3;
        mode = 3;
        search_keywords = new String[]{"quotation"};

        path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/sample_keywords.csv";
        max_size = 2835;
        doc_size = 11753;
        mode = 3;
        search_keywords = new String[]{"english"};

        path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/synthetic_keywords_15000_3000.csv";
        max_size = 3000;
        doc_size = 15000;
        mode = 3;
        search_keywords = new String[]{"explorer","quotation","divorce"};

        path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/synthetic_keywords_1000_5000.csv";
        max_size = 5000;
        doc_size = 1000;
        mode = 3;
        search_keywords = new String[]{"quotation"};

        path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/synthetic_keywords_5000_100.csv";
        max_size = 100;
        doc_size = 5000;
        mode = 0;
        search_keywords = new String[]{"english"};
        */

//        path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/test.csv";
//        max_size = 3;
//        doc_size = 3;
//        mode = 3;
//        search_keywords = new String[]{"quotation"};

//        path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/sample_keywords.csv";
//        max_size = 1000;
//        doc_size = 4000;
//        mode = 2;
//        search_keywords = new String[]{"english"};

//        path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/test.csv";
//        max_size = 3;
//        doc_size = 3;
//        mode = 3;
//        search_keywords = new String[]{"quotation"};

        path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/synthetic_keywords_1_1000.csv";
        max_size = 1000;
        doc_size = 1;
        mode = 2;
        search_keywords = new String[]{"english"};

        int q = 1;
        int d = 1;
        ODPF odpf = new ODPF(path, max_size, doc_size, mode, search_keywords, q, d);
        boolean isMAC = true;
        odpf.BuildIndex(isMAC);
        odpf.CreateMAC(isMAC);
        int M = 100;
        odpf.SearchTest(isMAC, M);
        odpf.UpdateSim(isMAC);
    }
}
