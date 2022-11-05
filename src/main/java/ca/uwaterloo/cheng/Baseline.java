package ca.uwaterloo.cheng;

import ca.uwaterloo.cheng.modules.*;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class Baseline {

    private final double[] fPP = new double[]{Math.pow(10, -3), Math.pow(10, -4), Math.pow(10, -5), Math.pow(10, -6)};
    private int ver = 0;
    private int lambda = 128;
    private String path;

    private String[] search_key_words;
    private int max_size, doc_size, mode;
    private PropertiesCache properties;
    private Bits mkey;
    private byte[] mackey;
    private MAC mac;

    private String keyword_path, mac_path, key_path, key_original_path;

    private double start, end;

    public Baseline(String path, int max_size, int doc_size, int mode, String[] search_key_words) {
        this.path = path;
        this.max_size = max_size;
        this.doc_size = doc_size;
        this.search_key_words = search_key_words.clone();
        this.mode = mode;
        properties = new PropertiesCache();
        keyword_path = "baseline_keyword_index_" + max_size + "_" + doc_size + ".csv";
        mac_path = "baseline_mac_index_" + max_size + "_" + doc_size + ".csv";
        key_path = "baseline_keys_" + max_size + "_" + doc_size + ".csv";
        key_original_path = "baseline_keys_original_" + max_size + "_" + doc_size + ".csv";
        mkey = Utils.base64ToBits(properties.read("Key1"), lambda);
        mackey = PRFCipher.extend_key(Utils.base64ToBits(properties.read("MACKey_1"), lambda), lambda, lambda * 2).toByteArray();
        mac = new MAC(mackey);
//        time_eval = new double[5];
    }

    public Baseline(String path, int max_size, int doc_size, int mode, String search_key_word) {
        this(path, max_size, doc_size, mode, new String[]{search_key_word});
    }

    public void BuildIndex(boolean isMAC) {
        ProgressBar progressbar = new ProgressBar();
        BloomFilter bfilter = new BloomFilter(max_size, fPP[mode]);
        int num_bits_per_row = bfilter.getNumSlots();
        System.out.println("Length of BF-encoded Keywords: " + num_bits_per_row);

        System.out.println("Starting Encoding and Encryption...");
        Bits[] ext_key = new Bits[doc_size];

        System.out.println("Generating keys for encrypting rows...");
        progressbar.init();
        for (int i = 0; i < doc_size; i++) {
            Bits enc_key = PRFCipher.generateKey(mkey, lambda, i, ver);
            ext_key[i] = PRFCipher.extend_key(enc_key, lambda, num_bits_per_row);
            progressbar.update(i, doc_size);
        }

        Utils.write(key_original_path, ext_key);
        Utils.write(key_path, Utils.transform(ext_key));

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
//                double test2 = System.nanoTime();
//                System.out.println("\ntest:"+(test2-test1)/1000000);
//                Bits enc_key = PRFCipher.generateKey(mkey, lambda, real_num_docs, ver);
//                ext_key[real_num_docs] = PRFCipher.extend_key(enc_key, lambda, num_bits_per_row);
                ciphers[real_num_docs] = Utils.boolarray_to_bits(bvector.getBitVector());

                ciphers[real_num_docs].xor(ext_key[real_num_docs]);

//                ciphers[real_num_docs] = PRFCipher.cipher(Utils.read_keys(key_original_path,num_bits_per_row,real_num_docs), plain);
                progressbar.update(real_num_docs, doc_size);
                real_num_docs++;
//                Utils.updateProgress(real_num_docs / doc_size);
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
//        System.out.println(" ");
        Bits[] index = Utils.transform(ciphers);
        Utils.write(keyword_path, index);
//        Utils.write(key_path, Utils.transform(ext_key));
        System.out.println("\nSetup latency:" + (end - start) / 1000000);
//        time_eval[0] = (end - start) / 1000000;
    }

    public void CreateMAC(boolean isMAC) {
        if (isMAC) {
            BloomFilter bfilter = new BloomFilter(max_size, fPP[mode]);
            int num_bits_per_row = bfilter.getNumSlots();
            ProgressBar progressbar = new ProgressBar();
            Bits[] index = Utils.readBaselineIndex(keyword_path, doc_size, max_size, num_bits_per_row);

            System.out.println("\nStart generating MACs...");
            start = System.nanoTime();
            byte[][] MACs = new byte[num_bits_per_row][];
            for (int i = 0; i < num_bits_per_row; i++) {
                progressbar.update(i + 1, num_bits_per_row);
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
            Utils.write_mac_baseline(mac_path, MACs, num_bits_per_row);
            System.out.println("\nMAC latency:" + (end - start) / 1000000);
//            time_eval[1] = (end - start) / 1000000;
        }
    }

    public void SearchTest(boolean isMAC) {
        BloomFilter bfilter = new BloomFilter(max_size, fPP[mode]);
        int num_bits_per_row = bfilter.getNumSlots();
        Bits[] index = Utils.readBaselineIndex(keyword_path, doc_size, max_size, num_bits_per_row);
        Bits[] all_dec_keys = Utils.read_all_keys(key_path, doc_size, num_bits_per_row);
        byte[][] MACs = null;
        if (isMAC) {
            MACs = Utils.read_mac(mac_path, num_bits_per_row);
        }

        // Client - Search
        start = System.nanoTime();
        Set<Integer> joinedSet = new HashSet<>();
        for (int i = 0; i < search_key_words.length; i++) {
            joinedSet.addAll(bfilter.getHashPositions(search_key_words[i]));
        }
        ArrayList<Integer> positions = new ArrayList<>(joinedSet);
        int bit_len = Utils.len_long(num_bits_per_row);
        DPF dpf = new DPF(lambda, bit_len);
        Bits[] inputs = new Bits[positions.size()];
        Bits[][] queries = new Bits[inputs.length][];
        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = Utils.long_to_bits(positions.get(i), bit_len);
            queries[i] = dpf.Gen(inputs[i]);
        }
        end = System.nanoTime();
        System.out.println("Client - search query generation latency:" + (end - start) / 1000000);
//        time_eval[2] = time_eval[2] + (end - start) / 1000000;

        int num_of_servers = 2;
        CountDownLatch countDownLatch = new CountDownLatch(num_of_servers);

        //Server_0 - Search
        System.out.println("Start Searching...");
        start = System.nanoTime();
        byte[][] mac_0 = new byte[inputs.length][];
        if (isMAC) {
            for (int i = 0; i < inputs.length; i++) {
                mac_0[i] = new byte[lambda / 8];
            }
        }

        Bits[] res_bits_0 = new Bits[inputs.length];
        byte[][] finalMACs_0 = MACs;
        Thread worker_0 = new Thread(() -> {
            try {
                System.out.println("Server_0 Searching...");
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
                System.out.println("Server_1 Searching...");
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
        System.out.println("Server - search latency:" + (end - start) / 1000000);
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
                    System.out.println("Query Results Do not Pass Integrity Check");
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
                System.out.println("Doc Identifier:" + i);
            }
        }
        end = System.nanoTime();
        System.out.println("Client - search query decryption latency:" + (end - start) / 1000000);
//        time_eval[2] = time_eval[2] + (end - start) / 1000000;
    }

    public void UpdateSim(boolean isMAC) {

        //Client
        System.out.println("Client Updating...");
        BloomFilter bfilter = new BloomFilter(max_size, fPP[mode]);
        int num_bits_per_row = bfilter.getNumSlots();

        Bits enc_key = PRFCipher.generateKey(mkey, lambda, doc_size + 1, ver);
        Bits ext_key = PRFCipher.extend_key(enc_key, lambda, num_bits_per_row);
        BloomVector bv = new BloomVector(num_bits_per_row);
        byte[] original_MACs = null;
        byte[] send_macs = null;
        if (isMAC) {
            original_MACs = ByteUtils.concatenate(Utils.read_mac(mac_path, num_bits_per_row));
        }

        start = System.nanoTime();
        for(int i=0;i<max_size;i++)
        {
            bv = bfilter.insert("update"+i,bv);
        }
        Bits cipher = Utils.boolarray_to_bits(bv.getBitVector());
        cipher.xor(ext_key);
        byte[][] update_macs = new byte[num_bits_per_row][];
        if (isMAC) {
            for (int j = 0; j < num_bits_per_row; j++) {

                update_macs[j] = new byte[lambda / 8];
                boolean value = cipher.get(j);
                String data;
                if (value) {
                    data = "1" + (doc_size + 1) + j + 0;
                } else {
                    data = "0" + (doc_size + 1) + j + 0;
                }
                update_macs[j] = ByteUtils.xor(update_macs[j], mac.create(Utils.stringToBits(data)));
            }
            send_macs = ByteUtils.concatenate(update_macs);
        }

        end = System.nanoTime();
        System.out.println("Client - update query generation latency:" + (end - start) / 1000000);

        //Server
        System.out.println("Server Updating...");
        start = System.nanoTime();
        if (isMAC) {
            original_MACs = ByteUtils.xor(original_MACs, send_macs);
        }
        end = System.nanoTime();
        System.out.println("Server - update latency:" + (end - start) / 1000000);
//        time_eval[4] = (end - start) / 1000000;
    }

    public static void main(String[] args) {
        String path;
        int max_size, doc_size, mode;
        String[] search_keywords;

        /*
        path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/sample_keywords.csv";
        max_size = 2835;
        doc_size = 11753;
        mode = 3;
        search_keywords = new String[]{"english"};

        path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/synthetic_keywords_1000_100.csv";
        max_size = 100;
        doc_size = 1000;
        mode = 3;
        search_keywords = new String[]{"explorer","paste"};

        path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/synthetic_keywords_20000_4000.csv";
        max_size = 4000;
        doc_size = 20000;
        mode = 3;
        search_keywords = new String[]{"test1"};

        path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/test.csv";
        max_size = 3;
        doc_size = 3;
        mode = 3;
        search_keywords = new String[]{"quotation"};

        path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/synthetic_keywords_15000_3000.csv";
        max_size = 3000;
        doc_size = 15000;
        mode = 3;
        search_keywords = new String[]{"explorer","quotation","divorce"};

        */

//        path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/test.csv";
//        max_size = 3;
//        doc_size = 3;
//        mode = 3;
//        search_keywords = new String[]{"quotation"};

//        max_size = 1000;
//        doc_size = 100;
//        path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/synthetic_keywords_"+doc_size+"_"+max_size+".csv";
//        mode = 2;
//        search_keywords = new String[]{"explorer", "quotation", "divorce"};


        path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/sample_keywords.csv";
        max_size = 1000;
        doc_size = 4000;
        mode = 2;
        search_keywords = new String[]{"english"};


        Baseline bl_test = new Baseline(path, max_size, doc_size, mode, search_keywords);
        boolean isMAC = true;
        bl_test.BuildIndex(isMAC);
        bl_test.CreateMAC(isMAC);
//        bl_test.SearchTest(isMAC);
//        bl_test.UpdateSim(isMAC);
    }
}
