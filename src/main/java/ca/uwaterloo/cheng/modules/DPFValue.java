package ca.uwaterloo.cheng.modules;

public class DPFValue {
    private final int lambda;
    private final int input_len;
    private final int output_len;

    public DPFValue(int lambda, int input_len, int output_len) {
        this.lambda = lambda;
        this.input_len = input_len;
        this.output_len = output_len;
    }

    public Bits convert_value(Bits input, int bits_len)
    {
        int input_len = input.length();
        if(bits_len <= input_len)
        {
            return input.get(0,bits_len);
        }
        else {
            try {
                Bits input_ext = Utils.dprg(input_len, input);
                return convert_value(input_ext, bits_len);
            }catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return null;
    }

    public Bits[] Gen(Bits alpha, Bits beta) {
        if (alpha.length() != input_len || beta.length() != output_len) {
            System.out.println("Input size and Output size are not correct!");
            System.exit(-1);
        }
        Bits s00 = Utils.get_random_rits(lambda);
        Bits s10 = Utils.get_random_rits(lambda);
//        System.out.println("s00:"+s00.toString());
//        System.out.println("s10:"+s10.toString());
        boolean t00 = false;
        boolean t10 = true;
        Bits[] s0 = new Bits[input_len];
        boolean[] t0 = new boolean[input_len];
        Bits[] s1 = new Bits[input_len];
        boolean[] t1 = new boolean[input_len];
        Bits[] CW = new Bits[input_len+1];
        for (int i = 0; i < input_len; i++) {
            s0[i] = new Bits(lambda);
            s1[i] = new Bits(lambda);
            CW[i] = new Bits(lambda+2);
        }

        CW[input_len] = new Bits(output_len);

        for (int i = 0; i < input_len; i++) {
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

        Bits[] sets_0 = new Bits[input_len + 2];
        Bits[] sets_1 = new Bits[input_len + 2];
        sets_0[0] = s00;
        sets_1[0] = s10;

        CW[input_len].xor(beta);
        CW[input_len].xor(convert_value(s0[input_len-1],output_len));
        CW[input_len].xor(convert_value(s1[input_len-1],output_len));
//        System.out.println("final_s0:"+convert_value(s0[input_len-1],output_len));
//        System.out.println("final_s1:"+convert_value(s1[input_len-1],output_len));
//        System.out.println("CW_final:"+CW[input_len]);
        for (int i = 1; i < input_len + 2; i++) {
            sets_0[i] = CW[i - 1];
            sets_1[i] = CW[i - 1];
        }

        Bits k0 = Utils.concatenate(sets_0);
        Bits k1 = Utils.concatenate(sets_1);
        return new Bits[]{k0, k1};
    }

    public Bits Eval(boolean bit, Bits key, Bits x) {
        if (x.length() != input_len) {
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
        Bits[] s = new Bits[input_len];
        boolean[] t = new boolean[input_len];
        Bits[] CW = new Bits[input_len+1];
        for (int i = 0; i < input_len; i++) {
            CW[i] = new Bits(lambda + 2);
            CW[i] = key.get(lambda + i * (lambda + 2), lambda + (i + 1) * (lambda + 2));
            s[i] = new Bits(lambda);
        }
        CW[input_len] = key.get(key.length()-output_len,key.length());
//        System.out.println("CW_final:"+CW[input_len]);
        for (int i = 0; i < input_len; i++) {
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
        Bits res = CW[input_len];
//        System.out.println("final_s_"+bit+":"+convert_value(s[input_len-1],output_len));
//        System.out.println("bit_"+bit+":"+t[input_len-1]);
        if(t[input_len-1])
        {
            res.xor(convert_value(s[input_len-1],output_len));
            return res;
        }
        return convert_value(s[input_len-1],output_len);
    }

    public static void main(String[] args) {

        int lambda = 128;
        int input_len = 10;
        int output_len = 10;

        Bits input = Utils.long_to_bits(215,input_len);
        Bits output = Utils.get_random_rits(output_len);
        System.out.println("output:"+output);

        DPFValue fssvalue = new DPFValue(lambda,input_len,output_len);
        Bits[] key = fssvalue.Gen(input,output);

        Bits bits_0 = Utils.long_to_bits(215, input_len);
        Bits res_0 = fssvalue.Eval(false, key[0], bits_0);

        Bits bits_1 = Utils.long_to_bits(215, input_len);
        Bits res_1 = fssvalue.Eval(true, key[1], bits_1);

        System.out.println("res_0:"+res_0);
        System.out.println("res_1:"+res_1);

        Bits res = new Bits(output_len);
        res.xor(res_0);
        res.xor(res_1);
        System.out.println("res:"+res);
    }
}
