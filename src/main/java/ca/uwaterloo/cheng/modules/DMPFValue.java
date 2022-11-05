package ca.uwaterloo.cheng.modules;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class DMPFValue {

    private final int tau;

    private final int lambda;

    private final int n;

    private final int slots;

    private final int kappa;

    public DMPFValue(int tau, int n, int lambda, int slots) {
        this.kappa = 3;
        this.lambda = lambda;
        this.n = n; // row
        this.slots = slots; //columns
        this.tau = tau;
    }

    public Bits[] Gen(long[] alpha, Bits[] outputs, int num_hashes) {
        long range = kappa * n;
        int t = alpha.length;
        if(t != outputs.length || num_hashes < alpha.length || t > n) {
            System.out.println("Error: Input Sizes are not matched!");
            System.exit(-1);
        }

        for(int i=0;i<t;i++)
        {
            if(outputs[i].length() != slots)
            {
                System.out.println("Error: Output Sizes are not matched!");
                System.exit(-1);
            }
        }

        int e = Utils.cuckoo_params_gen(num_hashes,lambda);
        double m = e * num_hashes;
        long B = (long) Math.ceil(range/m);
        int len = Utils.len_long(range);
        boolean redo;
        long[] table = new long[(int)m];
        Bits key;
        do{
            redo = false;
            key = Utils.get_random_rits(lambda);
            for(int i=0;i<m;i++)
            {
                table[i] = -1;
            }
            for(int i=0;i<t;i++)
            {
                long beta = alpha[i];
                boolean success = false;
                int times = 0;
                boolean sign = false;
                do{
                    int k = Utils.getRandomNumber(1,kappa+1);
                    int pos = (int) Cuckoo.hash(beta,len,k,n,B,key,range);
                    if(table[pos] < 0)
                    {
                        table[pos] = beta;
                        success = true;
                    }
                    else{
                        times++;
                        long tmp = beta;
                        beta = table[pos];
                        table[pos] = tmp;
                    }
                    if(times == 10*m)
                    {
                        sign = true;
                        break;
                    }

                }while (success == false);

                if(sign ==true)
                {
                    redo = true;
                    break;
                }
            }
//            System.out.println(Arrays.toString(table));
        }while(redo);

        int num = Utils.len_long(B);
        int num_out = slots;
        DPFValue fss = new DPFValue(tau,num,num_out);
        Bits key_0 = (Bits) key.clone();
        Bits key_1 = (Bits) key.clone();
        for(int i=0;i<m;i++)
        {
            Bits a;
            Bits b;
            if(table[i] == -1)
            {
                a = Utils.long_to_bits(B,num);
                b = new Bits(num_out);
            }
            else {
                int k_loc = -1;
                for(int k=1;k<=kappa;k++)
                {
                    if(Cuckoo.hash(table[i],len,k,n,B,key,range) == i)
                    {
                        k_loc = k;
                        break;
                    }
                }
                long val = Cuckoo.index(table[i],len,k_loc,n,B,key,range);
                int pos_loc = -1;
                for(int k = 0; k< t;k++)
                {
                    if(table[i] == alpha[k])
                    {
                        pos_loc = k;
                    }
                }
                a = Utils.long_to_bits(val,num);
                b = outputs[pos_loc];
//                //////
//                if(table[i] == 2) {
//                    System.out.println("hash:"+k_loc);
//                    System.out.println("index:"+a);
//                }
//                //////
            }
            Bits[] kk = fss.Gen(a,b);
//            //////
//            if(table[i] == 2) {
//                System.out.println("key for server-0:"+kk[0]);
//                System.out.println("key for server-1:"+kk[1]);
//                System.out.println("key length:"+kk[0].length());
//            }
//            //////
            key_0 = Utils.concatenate(new Bits[]{key_0, kk[0]});
//            System.out.println("key" +i+" = "+ kk[0]);
            key_1 = Utils.concatenate(new Bits[]{key_1, kk[1]});
        }
        return new Bits[]{key_0,key_1};
    }

    public Bits Eval(boolean bit, Bits key, long x, int num_hashes, int slots) {
        long range = kappa * n;
        int e = Utils.cuckoo_params_gen(num_hashes,lambda);
        double m = e * num_hashes;
        long B = (long) Math.ceil(range/m);
        int len = Utils.len_long(range);
        int num = Utils.len_long(B);
        int num_out = slots;
        DPFValue fss = new DPFValue(tau,num,num_out);

        Bits[] keys = new Bits[(int)m];
        Bits key_init = key.get(0,lambda);
        int key_len = num*(tau+2)+tau+num_out;
//        System.out.println("key len = "+ key_len);
        for(int i=0;i<m;i++)
        {
            keys[i] = key.get(lambda+key_len*i,lambda+key_len*(i+1));
//            System.out.println("key" +i+" = "+ keys[i]);
        }

        Bits res = new Bits(num_out);
        for(int i=1;i<=kappa;i++)
        {
            int key_pos = (int) Cuckoo.hash(x,len,i,n,B,key_init,range);
            Bits tmp_key = keys[key_pos];
            long index = Cuckoo.index(x,len,i,n,B,key_init,range);
            Bits bit_ind = Utils.long_to_bits(index,num);
//            //////
//            System.out.println("server["+bit+"]:"+fss.Eval(bit,tmp_key,bit_ind));
//            //////
            res.xor(fss.Eval(bit,tmp_key,bit_ind));
        }
        return res;
    }

    public static void main(String[] args) {


        int n = 100;
        int slots = 10000/n+1;

//        int n = (int) Math.sqrt(Setting.size_table);
//        int slots = n;

        int input_size = 10;
        long[] alpha = new long[input_size];
        Bits[] beta = new Bits[input_size];
        ArrayList<Long> vals = new ArrayList<Long>();
        alpha[0] = 21;
        beta[0] = Utils.long_to_bits(15,slots);
        vals.add(alpha[0]);
        System.out.print(" "+alpha[0]);
        for(int i = 1;i<input_size;i++)
        {
            do {
                alpha[i] = Utils.getRandomNumber(0, n - 1);
                beta[i] = Utils.long_to_bits(11,slots);
            }while (vals.contains(alpha[i]));
            vals.add(alpha[i]);
            System.out.print(" "+alpha[i]);
        }
        System.out.print("\n");

        int t = alpha.length;
        int num_hashes = t+2;
        int tau = 128;
        int lambda = 128;
        DMPFValue dmpf = new DMPFValue(tau, n, lambda, slots);
        Bits[] keys = dmpf.Gen(alpha,beta,num_hashes);

        System.out.println("key length:"+keys[0].length());

        double start,end;
//        start = System.nanoTime();
//        for(int test=0;test<n;test++)
//        {
//            Bits res = dmpf.Eval(false,keys[0],test,t);
//        }
//        end = System.nanoTime();
//        System.out.printf("server-0 latency: %,.1f\n", (end-start)/1000000);
//
//        start = System.nanoTime();
//        for(int test=0;test<n;test++)
//        {
//            Bits res = dmpf.Eval(true,keys[1],test,t);
//        }
//        end = System.nanoTime();
//        System.out.printf("server-1 latency: %,.1f\n", (end-start)/1000000);

//        System.out.println("k0:"+keys[0]);
//        System.out.println("k1:"+keys[1]);

        for(int test=0;test<n;test++)
        {
            Bits res_0 = dmpf.Eval(false,keys[0],test,num_hashes,slots);
            Bits res_1 = dmpf.Eval(true,keys[1],test,num_hashes, slots);
            res_0.xor(res_1);
            if(Utils.convert(res_0) == 15)
            {
                System.out.println(test);
                System.out.println(vals.contains((long)test));
            }

        }
    }


}

