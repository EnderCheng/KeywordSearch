package ca.uwaterloo.cheng.modules;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

public class CoverFamily {

    private int k;
    private int num;

    private int lines;
    private Polynomial[] polys;

    private boolean[][] M;

    //d-CFF q >= d*k + 1
    public CoverFamily(int q, int d) {
        k = (q - 1) / d;
        BigInteger big_q = BigInteger.valueOf(q);
        num = (int) Math.pow(q, k + 1);
        lines = q * q;
        polys = new Polynomial[num];
        polys[0] = new Polynomial(BigInteger.ZERO, 0, big_q);
        String str[] = new String[num];
        str[0] = "0";
        for (int i = 1; i < num; i++) {
            str[i] = Utils.sumBaseB(str[i - 1], "1", 5);
            polys[i] = new Polynomial(str[i], big_q);
        }

        M = new boolean[lines][num];

        int index = 0;
        for (int x = 0; x < q; x++) {
            for (int y = 0; y < q; y++) {
//                System.out.println(x+","+y);
                for (int col = 0; col < num; col++) {
                    if (polys[col].evaluate(BigInteger.valueOf(x)).equals(BigInteger.valueOf(y))) {
                        M[index][col] = true;
                    } else {
                        M[index][col] = false;
                    }
                }
//                System.out.println(index);
                index++;
            }
        }
    }

    public boolean[][] getM() {
        return M;
    }

    public void print2D(boolean mat[][]) {
        // Loop through all rows
        for (int i = 0; i < mat.length; i++) {
            for (int j = 0; j < mat[i].length; j++) {
                System.out.print(mat[i][j] + " ");
            }
            System.out.print('\n');
        }
    }

    public int getLines() {
        return lines;
    }

    public int getNum() {
        return num;
    }

    public boolean[] getColumn(int index){
        boolean[] column = new boolean[lines];
        for(int i=0; i<column.length; i++){
            column[i] = M[i][index];
        }
        return column;
    }

    public Polynomial[] getPolys() {
        return polys;
    }

    public static void main(String[] args) {
        CoverFamily cff = new CoverFamily(5, 2);
        System.out.println("number of shared users:"+cff.getNum());
        System.out.println("number of mac size:"+cff.getLines());
        System.out.println(Arrays.toString(cff.getColumn(1)));
    }
}
