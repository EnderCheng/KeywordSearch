package ca.uwaterloo.cheng;

import ca.uwaterloo.cheng.modules.*;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class VDPF {

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

    private int M;

    private double loadFactor;

    public VDPF(String path, int max_size, int doc_size, int mode, String[] search_key_words, int q, int d, int M, double loadFactor) {
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
        this.M = M;
        this.loadFactor = loadFactor;
        setName(max_size, doc_size);
    }

    public VDPF(String path, int max_size, int doc_size, int mode, String search_key_word, int q, int d, int M, double loadFactor) {
        this(path, max_size, doc_size, mode, new String[]{search_key_word}, q, d, M, loadFactor);
    }

    private void setName(int max_size, int doc_size) {
        keyword_path = "VDPF_keyword_index_" + max_size + "_" + doc_size + "_"+ +M + "_"+q+"_"+d+".csv";
        mac_path = "VDPF_mac_index_" + max_size + "_" + doc_size + "_"+ +M + "_"+q+"_"+d+".csv";
        col_key_path = "VDPF_col_keys_" + max_size + "_" + doc_size + "_"+ +M + "_"+q+"_"+d+".csv";
        row_key_path = "VDPF_row_keys_" + max_size + "_" + doc_size + "_"+ +M + "_"+q+"_"+d+".csv";
        row_key_original_path = "VDPF_row_keys_original_" + max_size + "_" + doc_size + "_"+ +M + "_"+q+"_"+d+".csv";
        iv_path = "VDPF_iv_" + max_size + "_" + doc_size + "_"+ +M + "_"+q+"_"+d+".csv";
    }

    public void BuildIndex(boolean isMAC) {
        ProgressBar progressbar = new ProgressBar();
        CuckooFilter cf = new CuckooFilter(max_size, M, fPP[mode], loadFactor);
        int num_buckets = (int) cf.getNumBuckets();
        int slots_per_buckets = cf.getVectors()[0].getNumSlots();
        int num_slots = slots_per_buckets * num_buckets;
        int num_bits_per_slots = cf.getVectors()[0].getNumBitsPerSlot();
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
                cf = new CuckooFilter(max_size, M, fPP[mode], loadFactor);
                List<String> word = Arrays.asList(strLine.split(","));
                if (word.size() > max_size) {
                    System.out.println("Error: The maximum number of keywords is not correct: " + word.size());
                    System.exit(-1);
                }
                for (int i = 0; i < word.size(); i++) {
                    boolean state = cf.add(word.get(i));
                    if(!state)
                    {
                        System.out.println("Cannot build index! Please Retry with smaller LoadFactor (0-1)!");
                        System.exit(-1);
                    }
                }
                Bits[] ctx = new Bits[num_slots];
                int start_col = real_num_docs * num_bits_per_slots;
                int end_col = (real_num_docs + 1) * num_bits_per_slots;
                for (int j = 0; j < num_slots; j++) {
                    int bucket = j / slots_per_buckets;
                    int index = j % slots_per_buckets;
                    ctx[j] = cf.getVectors()[bucket].get(index);
//                    ctx[j].xor(Utils.read_keys(col_key_path,col_len,j).get(start_col, end_col));
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
        ProgressBar progressbar = new ProgressBar();
        CuckooFilter cf = new CuckooFilter(max_size, M, fPP[mode], loadFactor);
        int num_buckets = (int) cf.getNumBuckets();
        int slots_per_buckets = cf.getVectors()[0].getNumSlots();
        int num_slots = slots_per_buckets * num_buckets;
        int num_bits_per_slots = cf.getVectors()[0].getNumBitsPerSlot();
        Bits[] index = Utils.readODPFIndex(keyword_path, num_slots, num_bits_per_slots, doc_size);

        if (isMAC) {
            start = System.nanoTime();
            byte[][] all_doc_rnds = new byte[chi][num_slots * XORMAC.MACBYTES];
            System.out.println("\nStart generating MACs...");
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

    public void SearchTest(boolean isMAC) {
        CuckooFilter cf = new CuckooFilter(max_size, M, fPP[mode], loadFactor);
        GarbledBloomFilter gbf = cf.getGbf();
        int num_buckets = (int) cf.getNumBuckets();
        int slots_per_buckets = cf.getVectors()[0].getNumSlots();
        int num_slots = slots_per_buckets * num_buckets;
        int num_bits_per_slots = cf.getVectors()[0].getNumBitsPerSlot();

//        int slot_len = (int) Math.ceil((num_slots + 0.0) / M);
        System.out.println("Each segment' length:" + slots_per_buckets);
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
        int bit_len = Utils.len_long(M);
        DPFValue dpfValue = new DPFValue(lambda,bit_len,slots_per_buckets);
        Bits[][][] keys = new Bits[search_key_words.length][][];
        for (int i = 0; i < search_key_words.length; i++) {
            int[] indexes = cf.getBuckets(search_key_words[i]);
            System.out.println("indexes:"+Arrays.toString(indexes));
            keys[i] = new Bits[indexes.length][];
            for(int j=0;j<indexes.length;j++)
            {
                int bucket_num = indexes[j];
                Bits input = Utils.long_to_bits(bucket_num,bit_len);
                ArrayList<Integer> positions = gbf.getHashPositions(search_key_words[i]);
                Bits val = new Bits(slots_per_buckets);
                for(int z=0;z<positions.size();z++)
                {
                    val.set(positions.get(z));
                }
                keys[i][j] = dpfValue.Gen(input,val);
            }
        }
//        segs[indexes[0]] = null;
//        gbf.getHashPositions(search_key_words[i]);

        end = System.nanoTime();
        System.out.println("Client - search query generation latency:" + (end - start) / 1000000);

        int num_of_servers = 2;
        CountDownLatch countDownLatch = new CountDownLatch(num_of_servers);

        //Server_0 - Search
        System.out.println("Start Searching...");
        start = System.nanoTime();
        var ref_0 = new Object() {
            byte[][][][] mac_0 = new byte[search_key_words.length][2][chi][XORMAC.MACBYTES];
        };

        Bits[][] res_bits_0 = new Bits[search_key_words.length][2];
        for(int i=0;i<search_key_words.length;i++)
        {
            for (int j=0;j<2;j++)
            {
                res_bits_0[i][j] = new Bits(num_bits_per_slots * doc_size);
            }
        }

        byte[][] finalMacs_0 = MACs;
        Thread worker_0 = new Thread(() -> {
            try {
                System.out.println("Server_0 Searching...");
                for(int i=0;i<search_key_words.length;i++)
                {
                    for (int j=0;j<2;j++)
                    {
                        int num = 0;
                        for (int z = 0; z < M; z++) {
                            Bits tmp = dpfValue.Eval(false,keys[i][j][0],Utils.long_to_bits(z,bit_len));
//                            System.out.println(j+"-Server 0_(" + z + "):" + tmp);
                            for (int y = 0; y< slots_per_buckets; y++) {
                                if (tmp.get(y)) {
                                    res_bits_0[i][j].xor(index[num]);
                                    if(isMAC) {
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
        for(int i=0;i<search_key_words.length;i++)
        {
            for (int j=0;j<2;j++)
            {
                res_bits_1[i][j] = new Bits(num_bits_per_slots * doc_size);
            }
        }

        byte[][] finalMacs_1 = MACs;
        Thread worker_1 = new Thread(() -> {
            try {
                System.out.println("Server_1 Searching...");
                for(int i=0;i<search_key_words.length;i++)
                {
                    for (int j=0;j<2;j++)
                    {
                        int num = 0;
                        for (int z = 0; z < M; z++) {
                            Bits tmp = dpfValue.Eval(true,keys[i][j][1],Utils.long_to_bits(z,bit_len));
//                            System.out.println(j+"-Server 1_(" + z + "):" + tmp);
                            for (int y = 0; y< slots_per_buckets; y++) {
                                if (tmp.get(y)) {
                                    res_bits_1[i][j].xor(index[num]);
                                    if(isMAC) {
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
        System.out.println("Server - search latency:" + (end - start) / 1000000);


        //Client - Combine Query Results to Recover the Plaintext
        start = System.nanoTime();
        for (int i = 0; i < search_key_words.length; i++) {
            ArrayList<Integer> positions = gbf.getHashPositions(search_key_words[i]);
            int[] pos = positions.stream().mapToInt(l -> l).toArray();
            int[] indexes = cf.getBuckets(search_key_words[i]);
            Bits[] res_bits= new Bits[2];
            for(int j = 0; j< 2; j++) {
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
                            randomness = ByteUtils.xor(randomness, Utils.get_range(ivs, z, (indexes[j]*slots_per_buckets+pos[x]) * XORMAC.MACBYTES, XORMAC.MACBYTES));
                        }
                        for (int y = 0; y< doc_size; y++) {
                            Bits in_bits = res_bits[j].get(y * num_bits_per_slots, (y + 1) * num_bits_per_slots);
                            tmp = ByteUtils.xor(tmp, mac[z].create_without_iv(in_bits));
                        }
                        byte[] test_bytes = ByteUtils.xor(tmp, randomness);
                        if (!Arrays.equals(test_bytes, query_mac))
                            System.out.println("Query Results Do not Pass Integrity Check");
                    }
                }

                Bits col_keys = new Bits(key_len);
                Bits row_keys = new Bits(key_len);
                for (int x = 0; x < pos.length; x++) {
                    col_keys.xor(ext_key_col[indexes[j]*slots_per_buckets+pos[x]]);
//                    Utils.read_keys(col_key_path, key_len, pos[i]);
                    row_keys.xor(ext_key_row[indexes[j]*slots_per_buckets+pos[x]]);
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
                    System.out.println("Doc Identifier:" + x +", keyword:"+search_key_words[i]);
                }
            }
        }

        end = System.nanoTime();
        System.out.println("Client - search query decryption latency:" + (end - start) / 1000000);
    }

    public void UpdateSim(boolean isMAC) {
        System.out.println("Client Updating...");
        start = System.nanoTime();
        CuckooFilter cf = new CuckooFilter(max_size, M, fPP[mode], loadFactor);
        GarbledBloomFilter gbf = cf.getGbf();
        int num_buckets = (int) cf.getNumBuckets();
        int slots_per_buckets = cf.getVectors()[0].getNumSlots();
        int num_slots = slots_per_buckets * num_buckets;
        int num_bits_per_slots = cf.getVectors()[0].getNumBitsPerSlot();

//        Bits plain = Utils.get_random_rits(num_bits_per_slots * num_slots);
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
        CuckooFilter cf_update = new CuckooFilter(max_size, M, fPP[mode], loadFactor);
        for (int i = 0; i < max_size; i++) {
            boolean state = cf_update.add("update"+i);
            if(!state)
            {
                System.out.println("Cannot build index! Please Retry with smaller LoadFactor (0-1)!");
                System.exit(-1);
            }
        }
        Bits[] ctx = new Bits[num_slots];
        int start_col = doc_size * num_bits_per_slots;
        int end_col = (doc_size + 1) * num_bits_per_slots;
        for (int j = 0; j < num_slots; j++) {
            int bucket = j / slots_per_buckets;
            int index = j % slots_per_buckets;
            ctx[j] = cf.getVectors()[bucket].get(index);
//                    ctx[j].xor(Utils.read_keys(col_key_path,col_len,j).get(start_col, end_col));
            ctx[j].xor(ext_key_col[j].get(start_col, end_col));
//                    ctx[j].xor(Utils.read_keys(col_key_path,col_len,j).get(start_col, end_col));
        }
        Bits enc_ctx = Utils.concatenate(ctx);
        enc_ctx.xor(new_ext_key_row);

        byte[][] update_macs = new byte[chi][num_slots * XORMAC.MACBYTES];
        if (isMAC) {

            byte[][] doc_rnds = new byte[chi][num_slots * XORMAC.MACBYTES];
            pb.init();
            for (int i = 0; i < chi; i++) {
                byte[] Gamma = Utils.prf_iv_doc(ivkeys[i], "random" + (doc_size + 1) + ver + "iv", lambda, num_slots);
                doc_rnds[i] = ByteUtils.xor(doc_rnds[i], Gamma);
                pb.update(i, chi);
            }

            for (int i = 0; i < chi; i++) {
                byte[][] macs_tmp = new byte[num_slots][XORMAC.MACBYTES];
                for (int z = 0; z < num_slots; z++) {
                    Bits data = enc_ctx.get(z * num_bits_per_slots, (z + 1) * num_bits_per_slots);
                    macs_tmp[z] = mac[i].create_without_iv(data);
                }
                update_macs[i] = Utils.flatten2DArray(macs_tmp);
                update_macs[i] = ByteUtils.xor(update_macs[i], doc_rnds[i]);
            }
        }
        end = System.nanoTime();
        System.out.println("\nClient - update query generation latency:" + (end - start) / 1000000);

        System.out.println("Server Updating...");
        start = System.nanoTime();
        if (isMAC) {
            for (int i = 0; i < chi; i++) {
                MACs[i] = ByteUtils.xor(MACs[i], update_macs[i]);
            }
        }
        end = System.nanoTime();
        System.out.println("Server - update latency:" + (end - start) / 1000000);
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
        */

        path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/sample_keywords.csv";
        max_size = 1000;
        doc_size = 4000;
        mode = 2;
        search_keywords = new String[]{"english"};

//        max_size = 100;
//        doc_size = 1;
//        path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/synthetic_keywords_"+doc_size+"_"+max_size+".csv";
//        mode = 2;
//        search_keywords = new String[]{"explorer", "quotation", "divorce"};

        VDPF vdpf = new VDPF(path, max_size, doc_size, mode, search_keywords, 1, 1, 10, 0.9);
        boolean isMAC = true;
        vdpf.BuildIndex(isMAC);
        vdpf.CreateMAC(isMAC);
//        vdpf.SearchTest(isMAC);
//        vdpf.UpdateSim(isMAC);
    }
}
