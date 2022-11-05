package ca.uwaterloo.cheng.modules;

public class Cuckoo {


    public static long hash(long x, int len, long i, long n, long B, Bits key, long range) {
        long input = x+n*(i-1);
        if(input >= range)
        {
            System.out.println("Error: Cuckoo Hash Input is incorrect!");
            System.exit(-1);
        }
        Bits bit_in = Utils.long_to_bits(input,len);
        Bits tmp = Utils.prp_range(key,bit_in,range);
        double out = Utils.convert(tmp);
        return (long) Math.floor(out/B);
    }

    public static long index(long x, int len, long i, long n, long B, Bits key, long range) {
        long input = x+n*(i-1);
        if(input >= range)
        {
            System.out.println("Error: Cuckoo Hash Input is incorrect!");
            System.exit(-1);
        }
        Bits bit_in = Utils.long_to_bits(input,len);
        Bits tmp = Utils.prp_range(key,bit_in,range);
        long out = Utils.convert(tmp);
        return out % B;
    }

    /*
    public static long[] chcompact(long[] alpha, int len, long n, long B, int kappa, int m, Bits key, long range) {
        long[] table = new long[m];
        for(int i=0;i<m;i++)
        {
            table[i] = -1;
        }
        int t = alpha.length;
        for(int i=0;i<t;i++)
        {
            long beta = alpha[i];
            boolean success = false;
            int times = 0;
            do{
                int k = Utils.getRandomNumber(0,kappa);
                int pos = (int) Cuckoo.hash(beta,len,k,n,B,key,range);
                if(table[pos] < 0)
                {
                    table[pos] = beta;
                    success = true;
                }
                else{
                    long tmp = beta;
                    beta = table[pos];
                    table[pos] = tmp;
                }

            }while (success == false);
        }
        return table;
    }
    */

}
