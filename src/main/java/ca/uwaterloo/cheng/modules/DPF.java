package ca.uwaterloo.cheng.modules;

import java.util.ArrayList;
import java.util.Arrays;

public class DPF {
    private final int lambda;
    private final int num;

    public DPF(int lambda, int num) {
        this.lambda = lambda;
        this.num = num;
    }

    public Bits[] Gen(Bits alpha) {
        if (alpha.length() != num) {
            System.out.println("Input size is not correct!");
            System.exit(-1);
        }
        Bits s00 = Utils.get_random_rits(lambda);
        Bits s10 = Utils.get_random_rits(lambda);
//        System.out.println("s00:"+s00.toString());
//        System.out.println("s10:"+s10.toString());
        boolean t00 = false;
        boolean t10 = true;
        Bits[] s0 = new Bits[num];
        boolean[] t0 = new boolean[num];
        Bits[] s1 = new Bits[num];
        boolean[] t1 = new boolean[num];
        Bits[] CW = new Bits[num];
        for (int i = 0; i < num; i++) {
            s0[i] = new Bits(lambda);
            s1[i] = new Bits(lambda);
            CW[i] = new Bits(lambda+2);
        }

        for (int i = 0; i < num; i++) {
            Bits S0 = null;
            Bits S1 = null;
            if (i == 0) {
                S0 = Utils.map(lambda, s00);
                S1 = Utils.map(lambda, s10);
            } else {
                S0 = Utils.map(lambda, s0[i - 1]);
                S1 = Utils.map(lambda, s1[i - 1]);
            }
            Bits s0L = S0.get(0, lambda);
            boolean t0L = S0.get(lambda);
            Bits s0R = S0.get(lambda + 1, 2 * lambda + 1);
            boolean t0R = S0.get(2 * lambda + 1);

//            System.out.println("S0["+i+"]="+S0.toString());

            Bits s1L = S1.get(0, lambda);
            boolean t1L = S1.get(lambda);
            Bits s1R = S1.get(lambda + 1, 2 * lambda + 1);
            boolean t1R = S1.get(2 * lambda + 1);

//            System.out.println("S1["+i+"]="+S1.toString());

            char Keep = 'R';
            char Lose = 'L';
            if (alpha.get(i) == false) {
                Keep = 'L';
                Lose = 'R';
            }
            Bits SCW = null;
            boolean TCWL = t0L ^ t1L ^ alpha.get(i) ^ true;
            boolean TCWR = t0R ^ t1R ^ alpha.get(i);
            Bits TCW_tmp = Utils.boolarray_to_bits(new boolean[]{TCWL, TCWR});
            if (Lose == 'L') {
                SCW = (Bits) s0L.clone();
                SCW.xor(s1L);
            } else {
                SCW = (Bits) s0R.clone();
                SCW.xor(s1R);
            }
            CW[i] = Utils.concatenate(new Bits[]{SCW, TCW_tmp});
//            System.out.println("CW["+i+"]="+CW[i].toString());

            boolean t0_tmp;
            boolean t1_tmp;

            if (i == 0) {
                t0_tmp = t00;
                t1_tmp = t10;
            } else {
                t0_tmp = t0[i - 1];
                t1_tmp = t1[i - 1];
            }

            if (Keep == 'L') {
                s0[i] = (Bits) s0L.clone();
                s1[i] = (Bits) s1L.clone();
                t0[i] = t0L ^ (t0_tmp & TCWL);
                t1[i] = t1L ^ (t1_tmp & TCWL);
            } else {
                s0[i] = (Bits) s0R.clone();
                s1[i] = (Bits) s1R.clone();
                t0[i] = t0R ^ (t0_tmp & TCWR);
                t1[i] = t1R ^ (t1_tmp & TCWR);
            }

            if (t0_tmp) {
                s0[i].xor(SCW);
            }

            if (t1_tmp) {
                s1[i].xor(SCW);
            }

        }

        Bits[] sets_0 = new Bits[num + 1];
        Bits[] sets_1 = new Bits[num + 1];
        sets_0[0] = s00;
        sets_1[0] = s10;

        for (int i = 1; i < num + 1; i++) {
            sets_0[i] = CW[i - 1];
            sets_1[i] = CW[i - 1];
        }

        Bits k0 = Utils.concatenate(sets_0);
        Bits k1 = Utils.concatenate(sets_1);
        return new Bits[]{k0, k1};
    }

    public boolean Eval(boolean bit, Bits key, Bits x) {
        if (x.length() != num) {
            System.out.println("Input size is not correct!");
            System.exit(-1);
        }

        Bits s0 = key.get(0, lambda);
//        if(bit)
//        {
//            System.out.println("1:"+s0);
//        }
//        else{
//            System.out.println("0:"+s0);
//        }

        boolean t0 = bit;
        Bits[] s = new Bits[num];
        boolean[] t = new boolean[num];
        Bits[] CW = new Bits[num];
        for (int i = 0; i < num; i++) {
            CW[i] = new Bits(lambda + 2);
            CW[i] = key.get(lambda + i * (lambda + 2), lambda + (i + 1) * (lambda + 2));
            s[i] = new Bits(lambda);
        }
        for (int i = 0; i < num; i++) {
            Bits SCW = CW[i].get(0, lambda);
            boolean TCWL = CW[i].get(lambda);
            boolean TCWR = CW[i].get(lambda + 1);
            Bits tau = null;
            if (i == 0) {
                tau = Utils.map(lambda, s0);
//                System.out.println("original tau["+i+"]="+tau);
                if (t0) {
                    Bits TCWL_tmp = new Bits(1);
                    TCWL_tmp.set(0, TCWL);
                    Bits TCWR_tmp = new Bits(1);
                    TCWR_tmp.set(0, TCWR);
                    Bits tmp = Utils.concatenate(new Bits[]{SCW, TCWL_tmp, SCW, TCWR_tmp});
//                    System.out.println("test["+i+"]="+tmp);
                    tau.xor(tmp);
                }
            } else {
                tau = Utils.map(lambda, s[i - 1]);
//                System.out.println("original tau["+i+"]="+tau);
                if (t[i - 1]) {
                    Bits TCWL_tmp = new Bits(1);
                    TCWL_tmp.set(0, TCWL);
                    Bits TCWR_tmp = new Bits(1);
                    TCWR_tmp.set(0, TCWR);
                    Bits tmp = Utils.concatenate(new Bits[]{SCW, TCWL_tmp, SCW, TCWR_tmp});
//                    System.out.println("test["+i+"]="+tmp);
                    tau.xor(tmp);
                }
            }
            Bits SL = tau.get(0, lambda);
            boolean TL = tau.get(lambda);
            Bits SR = tau.get(lambda + 1, 2 * lambda + 1);
            boolean TR = tau.get(2 * lambda + 1);
//            System.out.println("tau["+i+"]="+tau);
            if (x.get(i)) {
                s[i] = (Bits) SR.clone();
                t[i] = TR;
            } else {
                s[i] = (Bits) SL.clone();
                t[i] = TL;
            }
        }
        return t[num-1];
    }

    public static void main(String[] args) {

        int n = 10;
        int input_size = 2;
        long[] alpha = new long[input_size];
        ArrayList<Long> vals = new ArrayList<Long>();
        for(int i = 0;i<input_size;i++)
        {
            do {
                alpha[i] = Utils.getRandomNumber(0, n - 1);
            }while (vals.contains(alpha[i]));
            vals.add(alpha[i]);
        }
        System.out.println(Arrays.toString(vals.toArray()));

        int bit_len = Utils.len_long(n);
        int lambda = 128;

        DPF DPF = new DPF(lambda, bit_len);
        Bits[][] keys = new Bits[alpha.length][];
        double start, end;
        for(int i=0;i< alpha.length;i++)
        {
            Bits bits_a = Utils.long_to_bits(alpha[i],bit_len);
            keys[i] = DPF.Gen(bits_a);
        }

        System.out.println("key length:"+keys[0][0].length()*alpha.length);

        start = System.nanoTime();
        boolean[][] res_0 = new boolean[alpha.length][n];
        for(int i=0;i<alpha.length;i++) {
            for (int test = 0; test < n; test++) {
                Bits bits_b = Utils.long_to_bits(test, bit_len);
                res_0[i][test] = DPF.Eval(false, keys[i][0], bits_b);
            }
        }
        end = System.nanoTime();
        System.out.printf("server-0 latency: %,.1f\n", (end-start)/1000000);

        start = System.nanoTime();
        boolean[][] res_1 = new boolean[alpha.length][n];
        for(int i=0;i<alpha.length;i++) {
            for (int test = 0; test < n; test++) {
                Bits bits_b = Utils.long_to_bits(test, bit_len);
                res_1[i][test] = DPF.Eval(true, keys[i][1], bits_b);
            }
        }
        end = System.nanoTime();
        System.out.printf("server-1 latency: %,.1f\n", (end-start)/1000000);
        for(int i=0;i<alpha.length;i++) {
            Bits res_0_bits = Utils.boolarray_to_bits(res_0[i]);
            Bits res_1_bits = Utils.boolarray_to_bits(res_1[i]);
            System.out.println(res_0_bits);
            System.out.println(res_1_bits);
            res_0_bits.xor(res_1_bits);
            System.out.println(res_0_bits);
            System.out.println(" ");
        }

    }
}
