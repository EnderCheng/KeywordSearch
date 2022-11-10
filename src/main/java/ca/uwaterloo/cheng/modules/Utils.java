package ca.uwaterloo.cheng.modules;

import ca.uwaterloo.cheng.Baseline;
import com.google.common.hash.Funnels;
import com.google.common.math.DoubleMath;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.awt.*;
import java.io.*;
import java.math.RoundingMode;
import java.nio.Buffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.BitSet;
import java.util.stream.Stream;

public class Utils {

    public static SecureRandom random = new SecureRandom();

    public static Bits concatenate(Bits[] sets) {
//        int totalBitsEach = 0;
//        for (int i = 0; i < sets.length; i++) {
//            totalBitsEach = totalBitsEach + sets[i].length();
//        }
//        int len = sets.length;
//        try {
//            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//            for (int i = 0; i < len; i++) {
//                outputStream.write(sets[i].toByteArray());
//            }
//            byte[] concat = outputStream.toByteArray();
//            return Utils.byteArrayToBits(concat,totalBitsEach);
//        }catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//        return null;
        int totalBitsEach = 0;
        for (int i = 0; i < sets.length; i++) {
            totalBitsEach = totalBitsEach + sets[i].length();
        }
        Bits result = new Bits(totalBitsEach);
        int index = 0;
        for (int j = 0; j < sets.length; j++) {
            int set_length = sets[j].length();
            for (int k = 0; k < set_length; k++) {
                if(sets[j].get(k)) {
                    result.set(index, sets[j].get(k));
                }
                index++;
            }
        }
        return result;
    }

    public static Bits long_to_bits(long input, int num)
    {
        BitSet bits_a = BitSet.valueOf(new long[]{input});
        if(bits_a.length()>num) {
            System.out.println("Error: the input's length is incorrect!");
            return null;
        }
        return Utils.bitset_to_bits(bits_a, num);
    }

    public static Bits bitset_to_bits(BitSet set, int len) {
        Bits bits = new Bits(len);
        bits.or(set);
//        for (int i = 0; i < len; i++) {
//            bits.or(set);
//        }
        return bits;
    }

    public static byte[] hmac256(String secretKey, String message) {
        try {
            return hmac256(secretKey.getBytes("UTF-8"), message.getBytes("UTF-8"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMACSHA256", e);
        }
    }

    public static byte[] hmac256(byte[] secretKey, byte[] message) {
        byte[] hmac256 = null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec sks = new SecretKeySpec(secretKey, "HmacSHA256");
            mac.init(sks);
            hmac256 = mac.doFinal(message);
            return hmac256;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMACSHA256 encrypt ");
        }
    }

    public static Bits boolarray_to_bits(boolean[] in) {
        int n = in.length;
        Bits bits = new Bits(n);
        for (int i = 0; i < n; i++) {
            bits.set(i, in[i]);
        }
        return bits;
    }

//    public static Bits get_random_rits(int n) {
//        SecureRandom random = new SecureRandom();
//        random.setSeed(System.nanoTime());
//        Bits bits = new Bits(n);
//        for (int i = 0; i < n-1; i++) {
//            bits.set(i, random.nextBoolean());
//        }
//        return bits;
//    }

    public static Bits get_random_rits(int n) {
        return get_random_rits(n,random);
    }


    public static Bits get_random_rits(int n, SecureRandom random) {
        int byte_num = (int) Math.ceil(n/8);
        byte[] bytes = new byte[byte_num];
        random.nextBytes(bytes);
        return byteArrayToBits(bytes,n);
    }

    public static Bits zero_bit_set(int n) {
        Bits bits = new Bits(n);
        for (int i = 0; i < n; i++) {
            bits.set(i, false);
        }
        return bits;
    }

    public static Bits map(int lambda, Bits in) {
        try {
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");

            random.setSeed(in.toByteArray());
            int n = lambda * 2 + 2;
            int byte_len = n/8;
            byte[] randomness = new byte[byte_len];
            random.nextBytes(randomness);
            Bits bits = Utils.byteArrayToBits(randomness,n);
//        Bits bits = new Bits(n);
//        for (int i = 0; i < n; i++) {
//            bits.set(i, random.nextBoolean());
//        }
            return bits;
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

//    public static Bits dprg(int len, Bits in) {
//        try {
//            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
//        random.setSeed(in.toByteArray());
//        int n = len * 2;
//        Bits bits = new Bits(n);
//        for (int i = 0; i < n; i++) {
//            bits.set(i, random.nextBoolean());
//        }
//        return bits;
//        }catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//        return null;
//    }

    public static Bits dprg(int len, Bits in) {
        try {
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(in.toByteArray());
            int n = len * 2;
            int byte_num = n/8;
            byte[] randomess = new byte[byte_num];
            random.nextBytes(randomess);
            Bits bits = Utils.byteArrayToBits(randomess,n);
            return bits;
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static Bits prg(Bits in, int ex_len) {
        try {
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(in.toByteArray());
            int byte_num = ex_len/8;
            byte[] randomess = new byte[byte_num];
            random.nextBytes(randomess);
            Bits bits = Utils.byteArrayToBits(randomess,ex_len);
            return bits;
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static Bits prg_col(Bits in, int ex_len, int doc_step, int num_bits_per_slot) {
        try {
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(in.toByteArray());
            int num = ex_len/(doc_step*num_bits_per_slot)+1;
            int byte_num = (doc_step*num_bits_per_slot)/8;
            byte[][] randomess = new byte[num][byte_num];
            for(int i=0;i<num;i++) {
                random.nextBytes(randomess[i]);
            }
            byte[] res = ByteUtils.concatenate(randomess);
            Bits bits = Utils.byteArrayToBits(res,ex_len);
            return bits;
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static Bits prp_range(Bits key, Bits in, long range) {
        Bits out = (Bits) in.clone();
//        int key_len = key.length();
        do {
            out = prp(key,out);
        }while (convert(out)>=range);
        return out;
    }

    public static Bits prf_to_len(Bits key, Bits in, int out_len) {
        int key_len = key.length();
        if(out_len > key_len) {
            System.out.println("Error: PRF Output length should be equal to or smaller than Key length");
            System.exit(-1);
        }
        Bits res = prf(key,in);
        return res.get(0,out_len);
    }

    public static byte[] prf_iv(Bits key, String data)
    {
        byte[] Gamma = Utils.prf(key,Utils.stringToBits(data)).toByteArray();
        byte[] res = new byte[MAC.MACBYTES];
        System.arraycopy(Gamma,0,res,0,Gamma.length);
        return res;
    }

    public static byte[] prf_iv_doc(Bits key, String data, int lambda, int num_slots)
    {
        byte[] Gamma = PRFCipher.extend_key(Utils.prf(key,Utils.stringToBits(data)),lambda,num_slots*XORMAC.MACBYTES*8).toByteArray();
        byte[] res = new byte[num_slots*XORMAC.MACBYTES];
        System.arraycopy(Gamma,0,res,0,Gamma.length);
        return res;
    }

    public static Bits prf(Bits key, Bits in) {
        int key_len = key.length();
        int in_len = in.length();
        Bits cur = (Bits) key.clone();
        for(int i=0;i<in_len;i++)
        {
            boolean bit = in.get(i);
            if(bit)
            {
                cur = dprg(key_len, cur).get(2*key_len - key_len, 2*key_len);
            }
            else {
                cur = dprg(key_len, cur).get(0, key_len);
            }
        }
        return cur;
    }

    public static boolean xor(boolean[] array) {
        return BooleanUtils.xor(array);
    }

    public static Bits prp(Bits key, Bits in) {
        int key_len = key.length();
        int in_len = in.length();
        if(key_len % 4 !=0 || in_len % 2 !=0) {
            System.out.println("Error: PRP Input length and Key length are incorrect! key_len % 4 !=0 || in_len % 2 !=0");
            System.exit(-1);
        }
        int quarter = key_len/4;
        int half = key_len/2;
        Bits[] keys_f = new Bits[4];
        keys_f[0] = key.get(0,quarter);
        keys_f[1] = key.get(quarter,half);
        keys_f[2] = key.get(half,half+quarter);
        keys_f[3] = key.get(half+quarter,key_len);

        Bits outputs = (Bits) in.clone();
        for(int i=0;i<4;i++)
        {
            outputs = feistel(keys_f[i], outputs);
        }
        return outputs;
    }

    public static Bits feistel(Bits key, Bits in) {
        int key_len = key.length();
        int in_len = in.length();
        int half = in_len/2;
        if(half*2 != in_len)
        {
            System.out.println("Error: Input length is not multiples of 2!");
            System.exit(-1);
        }

        Bits left = in.get(0,half);
        Bits right = in.get(half,in_len);
        Bits out_left = (Bits) right.clone();
        Bits out_right = prf_to_len(key, right, half);
        out_right.xor(left);
        return concatenate(new Bits[]{out_left,out_right});
    }

    static String sumBaseB(String a, String b, int base)
    {
        int len_a, len_b;

        len_a = a.length();
        len_b = b.length();

        String sum, s;
        s = "";
        sum = "";

        int diff;
        diff = Math.abs(len_a - len_b);

        for (int i = 1; i <= diff; i++)
            s += "0";

        if (len_a < len_b)
            a = s + a;
        else
            b = s + b;

        int curr, carry = 0;


        for (int i = Math.max(len_a, len_b) - 1;
             i > -1; i--) {

            curr = carry + (a.charAt(i) - '0') +
                    (b.charAt(i) - '0');

            carry = curr / base;
            curr = curr % base;

            sum = (char)(curr + '0') + sum;
        }
        if (carry > 0)
            sum = (char)(carry + '0') + sum;
        return sum;
    }

    public static double log2(double x)
    {
        double result = (Math.log(x) / Math.log(2));
        return result;
    }

    public static double log_q(double x, double q)
    {
        double result = (Math.log(x) / Math.log(q));
        return result;
    }

    public static int cuckoo_params_gen(int t, int lambda)
    {
        if(t<4)
        {
            System.out.println("\nt should be larger than 4");
            System.exit(-1);
        }
        double at = 123.5* Gaussian.cdf(t,6.3,2.3);
        double bt = 130*Gaussian.cdf(t,6.45,2.18);
        int e = (int) Math.ceil((lambda+bt+log2(t))/at);
        return e;
    }

    public static BitSet convert(long value) {
        BitSet bits = new BitSet();
        int index = 0;
        while (value != 0L) {
            if (value % 2L != 0) {
                bits.set(index);
            }
            ++index;
            value = value >>> 1;
        }
        return bits;
    }

    public static long convert(Bits bits) {
        long value = 0L;
        for (int i = 0; i < bits.length(); ++i) {
            value += bits.get(i) ? (1L << i) : 0L;
        }
        return value;
    }

    public static int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    public static int len_long(long x)
    {
        int len = convert(x).length();
        if((len & 1) != 0)
            return len+1;
        return len;
    }

    public static void write(String path, Bits[] data)
    {
        System.out.println("\nStart writing "+path+"...");
        ProgressBar pb = new ProgressBar();
        PrintWriter pw = null;
        int len = data.length;
        try {
            pw = new PrintWriter(new FileWriter(path));
            for(int i=0;i<len;i++)
            {
                pw.println(Utils.bitsToBase64(data[i]));
                pb.update(i,len);
            }
            pw.close();
        }catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("File writer fails!");
            System.exit(-1);
        }
    }

    public static void write_row_large(String path, Bits[] data, int doc_size, int num_slot, int num_bits_per_slot)
    {
        System.out.println("\nStart writing "+path+"...");
        ProgressBar pb = new ProgressBar();
        PrintWriter pw = null;
//        int row_len = num_slot*num_bits_per_slot;
        int total = num_slot*doc_size;
        System.out.println("test:"+total);
        int count = 0;
        try {
            pw = new PrintWriter(new FileWriter(path));
            for(int i=0;i<num_slot;i++)
            {
                int start = i*num_bits_per_slot;
                int end = (i+1)*num_bits_per_slot;
                Bits[] tmp = new Bits[doc_size];
                for(int j=0;j<doc_size;j++)
                {
//                    double test1 = System.nanoTime();
                    tmp[j] = data[j].get(start,end);
                    pb.update(count,total);
                    count++;
//                    double test2 = System.nanoTime();
//                    System.out.println("test:"+(test2-test1)/1000000);
//                    System.out.println("count:"+count);
                }
                System.out.println("test:"+count);
                pw.println(Utils.bitsToBase64(Utils.concatenate(tmp)));
            }
            pw.close();
        }catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("File writer fails!");
            System.exit(-1);
        }
    }

    public static Bits[] readODPFIndex(String path, int num_slots, int num_bits_per_slot, int doc_size)
    {
        Bits[] dataindex = new Bits[num_slots];
        int i = 0;
        try {
            FileInputStream fstream = new FileInputStream(path);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                dataindex[i] = Utils.base64ToBits(strLine, num_bits_per_slot*doc_size);
                i++;
            }
            fstream.close();
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return dataindex;
    }

    public static Bits[] readBaselineIndex(String path, int num_docs, int num_keywords, int num_bits_per_row)
    {
        Bits[] dataindex = new Bits[num_bits_per_row];
        int i = 0;
        try {
            FileInputStream fstream = new FileInputStream(path);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                dataindex[i] = Utils.base64ToBits(strLine,num_docs);
                i++;
            }
            fstream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dataindex;
    }

    public static void write_mac_baseline(String path, byte[][] MACs, int num)
    {
        System.out.println("\nStart writing "+path+"...");
        ProgressBar pb = new ProgressBar();
        PrintWriter pw_mac = null;
        try {
            pw_mac = new PrintWriter(new FileWriter(path));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("MAC File writer fails!");
            System.exit(-1);
        }
        for(int i=0;i<num;i++)
        {
            pw_mac.println(Base64.getEncoder().encodeToString(MACs[i]));
            pb.update(i,num);
        }
        pw_mac.close();
    }

    public static byte[] flatten2DArray(byte[][] array) {
        byte[] flattenedArray = new byte[array.length * array[0].length];
        for (int i = 0; i < array.length; i++) {
            System.arraycopy(array[i], 0, flattenedArray, i * array[0].length, array[0].length);
        }
        return flattenedArray;
    }

    public static byte[][] reverse1DArray(byte[] arr, int num, int len) {
        if(arr.length != num*len)
        {
            return null;
        }
        byte[][] arrays = new byte[num][len];
        for (int i = 0; i < num; i++) {
            System.arraycopy(arr,i*len, arrays[i],0, len);
        }
        return arrays;
    }

//    public static void write_mac_odpf(String path, byte[][][] MACs, int num)
//    {
//        byte[][] flat_macs = new byte[num][];
//        for(int i=0;i<num;i++) {
//            flat_macs[i] = flatten2DArray(MACs[i]);
//        }
//        write_mac_baseline(path,flat_macs,num);
//    }

    public static byte[] read_mac_baseline(String path, int index)
    {
        try (Stream<String> lines = Files.lines(Paths.get(path))) {
            String line = lines.skip(index).findFirst().get();
            return Base64.getDecoder().decode(line);
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[][] read_mac(String path, int total_lines)
    {
        byte[][] data = new byte[total_lines][];
        int i=0;
        try {
            FileInputStream fstream = new FileInputStream(path);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                data[i] = Base64.getDecoder().decode(strLine);
                i++;
            }
            return data;
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] get_range(byte[][] macs, int index, int start, int len)
    {
        byte[] res = new byte[len];
        System.arraycopy(macs[index],start,res,0,len);
        return res;
    }

    public static byte[] read_mac_range(String path, int index, int start, int len)
    {

        byte[] macs =  read_mac_baseline(path,index);
        byte[] res = new byte[len];
        System.arraycopy(macs,start,res,0,len);
        return res;
    }

//    public static byte[][] read_mac_odpf(String path, int index, int num, int len)
//    {
//        byte[] macs = read_mac_baseline(path,index);
//        return reverse1DArray(macs,num,len);
//    }

    public static Bits stringToBits(String data, int len)
    {
        byte[] bytes_data = data.getBytes();
        return byteArrayToBits(bytes_data,len);
    }

    public static Bits stringToBits(String data)
    {
        byte[] bytes_data = data.getBytes();
        return byteArrayToBits(bytes_data,bytes_data.length*8);
    }

    public static void writeObjectToFile(String file_path, Object object)
    {
        File f = new File(file_path);
        try {
            FileOutputStream fos = new FileOutputStream(f);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(object);
            oos.close();
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

//    public static Bits binaryToBits(String in) {
//        if (in.isEmpty()) {
//            return new Bits(0);
//        }
//        StringBuilder inBuilder = new StringBuilder(in);
//        String reversedIn = inBuilder.reverse().toString();
//
//        Bits bits = new Bits(in.length());
//
//        for (int i = 0; i < in.length(); i++) {
//            if (reversedIn.charAt(i) == '1') {
//                bits.set(i);
//            }
//        }
//
//        return bits;
//    }

    public static Object readObjectfromFile(String file_path) {
        File f = new File(file_path);
        Object obj = null;
        if (f.isFile()) {
            try {
                FileInputStream fis = new FileInputStream(f);
                ObjectInputStream ois = new ObjectInputStream(fis);
                obj = ois.readObject();
                ois.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return obj;
    }

    public static Bits base64ToBits(String base64, int bit_len)
    {
        byte[] data = Base64.getDecoder().decode(base64);
        return byteArrayToBits(data,bit_len);
    }

    public static String bitsToBase64(Bits in)
    {
        byte[] data = bitsToByteArray(in);
        return Base64.getEncoder().encodeToString(data);
    }

    public static byte[] bitsToByteArray(Bits in)
    {
        return in.toByteArray();
    }

    public static Bits byteArrayToBits(byte[] data, int bit_len)
    {
        BitSet bitset = BitSet.valueOf(data);
        return bitset_to_bits(bitset,bit_len);
    }

    public static Bits read_keys(String path, int bit_len, int index)
    {
        try (Stream<String> lines = Files.lines(Paths.get(path))) {
            String line = lines.skip(index).findFirst().get();
            return Utils.base64ToBits(line, bit_len);
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static Bits[] read_all_keys(String path, int bit_len, int total)
    {
        Bits[] keys = new Bits[total];
        int i=0;
        try {
            FileInputStream fstream = new FileInputStream(path);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                keys[i] = Utils.base64ToBits(strLine, bit_len);
                i++;
            }
            fstream.close();
            return keys;
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

//    public static void write_keys_transform(Bits[] keys, String path)
//    {
//        Bits[] new_keys = transform(keys);
//        write_keys_raw(new_keys, path);
//    }

    public static Bits[] transform(Bits[] data)
    {
        System.out.println("\nStarting transforming...");
        ProgressBar pb = new ProgressBar();
        int size = data[0].length();
        Bits[] new_data = new Bits[size];
        int len = data.length;
        for(int i=0;i<size;i++)
        {
            new_data[i] = new Bits(len);
            for(int j=0;j<len;j++)
            {
                if(data[j].get(i)) {
                    new_data[i].set(j);
                }
            }
            pb.update(i,size);
        }
        return new_data;
    }

    public static Bits[] conv(Bits[] data, int num_slot, int num_bits_per_slot)
    {
        System.out.println("\nStarting converting...");
        ProgressBar pb = new ProgressBar();
        Bits[] new_data = new Bits[num_slot];
        for(int i=0;i<num_slot;i++)
        {
            Bits[] tmp = new Bits[data.length];
            for(int j=0;j<data.length;j++)
            {
                tmp[j] = data[j].get(i*num_bits_per_slot,(i+1)*num_bits_per_slot);
            }
            new_data[i] = Utils.concatenate(tmp);
            pb.update(i,num_slot);
        }
        return new_data;
    }

    public static byte[][] conv_bytes(byte[][] data, int num_slot)
    {
        System.out.println("\nStarting converting bytes...");
        ProgressBar pb = new ProgressBar();
        byte[][] new_data = new byte[num_slot][];
        for(int i=0;i<num_slot;i++)
        {

            byte[][] tmp = new byte[data.length][];
            for(int j=0;j<data.length;j++)
            {
                tmp[j] = ByteUtils.subArray(data[j],i*XORMAC.MACBYTES,(i+1)*XORMAC.MACBYTES);
            }
            new_data[i] = ByteUtils.concatenate(tmp);
            pb.update(i,num_slot);
        }
        return new_data;
    }


    public static void main(String[] args) {
//        Bits keys = long_to_bits(12354354645645l,128);
//        Bits out1 = prg(keys,128);
//        Bits out2 = prg(keys,160);
//        Bits out3 = prg(keys,180);
        int num = 8;
        System.out.println(len_long(8));
//        System.out.println(out1);
//        System.out.println(out2);
//        System.out.println(out3);
//        System.out.println(sumBaseB("44","1",5));
//        byte[][] xxx = new byte[10][16];
//        for(int i=0;i<10;i++)
//        {
//            xxx[i] = Utils.get_random_rits(128).toByteArray();
//        }
//        byte[] yyy = Utils.flatten2DArray(xxx);
//        byte[][] zzz = Utils.reverse1DArray(yyy,10,16);
//        System.out.println("test");
//
//        Bits ivkeys = Utils.get_random_rits(128);
//        byte[] y = Utils.prf_iv_doc(ivkeys, "random iv"+2+1, 128, 115021);
//        double start = System.nanoTime();
//        byte[] x = Utils.prf_iv_doc(ivkeys, "random iv"+4+0, 128, 115021);
//        double end = System.nanoTime();
//        System.out.println((end-start)/1000000);
//        start = System.nanoTime();
//        byte[] z = ByteUtils.xor(x,y);
//        end = System.nanoTime();
//        System.out.println((end-start)/1000000);

//        Bits test1 = Utils.get_random_rits(12);
//        Bits test2 = Utils.get_random_rits(45);
//        Bits test3 = Utils.get_random_rits(23);
//        System.out.println(test1);
//        System.out.println(test2);
//        System.out.println(test3);
//        Bits[] test = new Bits[]{test1,test2,test3};
//        Bits new_test = concatenate(test);
//        System.out.println(new_test);
    }


//    public static void write_keys_conv(Bits[] keys, int num_slot, int num_bits_per_slot, String path)
//    {
//        Bits[] new_keys = conv(keys,num_slot,num_bits_per_slot);
//        write_keys_raw(new_keys, path);
//    }

}
