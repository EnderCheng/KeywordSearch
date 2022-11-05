package ca.uwaterloo.cheng.modules;

import java.math.BigInteger;

public class Polynomial {
    private BigInteger[] coef;  // coefficients

    private BigInteger p;
    private int deg;     // degree of polynomial (0 for the zero polynomial)

    // a * x^b
    public Polynomial(BigInteger a, int b, BigInteger p) {
        coef = new BigInteger[b + 1];
        for (int i = 0; i < b + 1; i++) {
            coef[i] = BigInteger.valueOf(0);
        }
        coef[b] = a;
        this.p = p;
        deg = degree();
    }

    public Polynomial(String str, BigInteger p)
    {
        int len = str.length();
        coef = new BigInteger[len];
        for (int i = 0; i<len; i++) {
            coef[len-1-i] = new BigInteger(String.valueOf(str.charAt(i)));
        }
        this.p = p;
        deg = degree();
    }

    // test client
    public static void main(String[] args) {

        BigInteger p = BigInteger.valueOf(6);

        Polynomial p1 = new Polynomial(BigInteger.valueOf(4), 3, p);
        Polynomial p2 = new Polynomial(BigInteger.valueOf(3), 2, p);
        Polynomial p3 = new Polynomial(BigInteger.valueOf(1), 0, p);
        Polynomial poly1 = p1.plus(p2).plus(p3);   // 4x^3 + 3x^2 + 1

        Polynomial q1 = new Polynomial(BigInteger.valueOf(1), 1, p);
        Polynomial q2 = new Polynomial(BigInteger.valueOf(5), 0, p);
        Polynomial poly2 = q1.plus(q2);                     // x^1 + 5


        Polynomial r = poly1.plus(poly2);
        Polynomial t = poly1.minus(poly2);
        Polynomial s = poly1.times(poly2);
        Polynomial[] w = poly1.divides(poly2);

        System.out.println("p(x) =        " + poly1);
        System.out.println("q(x) =        " + poly2);
        System.out.println("p(x) + q(x) = " + r);
        System.out.println("p(x) - q(x) = " + t);
        System.out.println("p(x) * q(x) = " + s);
        System.out.println("p(x) / q(x) = " + w[0]);
        System.out.println("p(x) mod q(x) = " + w[1]);

        System.out.println("Eval p(2):"+poly1.evaluate(BigInteger.valueOf(2)));
    }

    // return the degree of this polynomial (0 for the zero polynomial)
    public int degree() {
        int d = 0;
        for (int i = 0; i < coef.length; i++) {
            if (coef[i].compareTo(BigInteger.ZERO) != 0) {
                d = i;
            }
        }
        return d;
    }

    // return c = a + b
    public Polynomial plus(Polynomial b) {
        Polynomial a = this;
        Polynomial c = new Polynomial(BigInteger.valueOf(0), Math.max(a.deg, b.deg), p);
        for (int i = 0; i <= a.deg; i++) c.coef[i] = c.coef[i].add(a.coef[i]).mod(p);
        for (int i = 0; i <= b.deg; i++) c.coef[i] = c.coef[i].add(b.coef[i]).mod(p);
        c.deg = c.degree();
        return c;
    }

    // return (a - b)
    public Polynomial minus(Polynomial b) {
        Polynomial a = this;
        Polynomial c = new Polynomial(BigInteger.ZERO, Math.max(a.deg, b.deg), p);
        for (int i = 0; i <= a.deg; i++) c.coef[i] = c.coef[i].add(a.coef[i]).mod(p);
        for (int i = 0; i <= b.deg; i++) c.coef[i] = c.coef[i].subtract(b.coef[i]).mod(p);
        c.deg = c.degree();
        return c;
    }

    // return (a * b)
    public Polynomial times(Polynomial b) {
        Polynomial a = this;
        Polynomial c = new Polynomial(BigInteger.ZERO, a.deg + b.deg, p);
        for (int i = 0; i <= a.deg; i++)
            for (int j = 0; j <= b.deg; j++) {
                BigInteger tmp = a.coef[i].multiply(b.coef[j]).mod(p);
                c.coef[i + j] = c.coef[i + j].add(tmp).mod(p);
            }
        c.deg = c.degree();
        return c;
    }

    // convert to string representation
    public String toString() {
        if (deg == 0) return "" + coef[0];
        if (deg == 1) return coef[1] + "x + " + coef[0];
        String s = coef[deg] + "x^" + deg;
        for (int i = deg - 1; i >= 0; i--) {
            if (coef[i].compareTo(BigInteger.ZERO) == 0) continue;
            else if (coef[i].compareTo(BigInteger.ZERO) != 0) s = s + " + " + (coef[i].toString());
            if (i == 1) s = s + "x";
            else if (i > 1) s = s + "x^" + i;
        }
        return s;
    }

    public Polynomial[] divides(Polynomial b) {
        Polynomial a = this;
        if ((b.deg == 0) && (b.coef[0].compareTo(BigInteger.ZERO)==0))
            throw new RuntimeException("Divide by zero polynomial"); //Zero polynomial is the one having coeff and degree both zero.

        if (a.deg < b.deg) {
            Polynomial[] polys = new Polynomial[2];
            polys[0] = new Polynomial(BigInteger.ZERO, 0, p);
            polys[1] = a;
            return polys;
        }

        BigInteger coefficient = a.coef[a.deg];
        coefficient = coefficient.divide(b.coef[b.deg]);
        int exponent = a.deg - b.deg;
        Polynomial c = new Polynomial(coefficient, exponent, p);
        Polynomial[] polys = new Polynomial[2];
        Polynomial[] tmp = (a.minus(b.times(c)).divides(b));
        polys[0] = c.plus(tmp[0]);
        polys[1] = tmp[1];
        return polys;
    }

    public BigInteger evaluate(BigInteger x) {
        BigInteger p_tmp = BigInteger.ZERO;
        for (int i = deg; i >= 0; i--)
            p_tmp = coef[i].add(x.multiply(p_tmp).mod(p)).mod(p);
        return p_tmp;
    }

    public BigInteger[] getCoef() {
        return coef;
    }

    public String getCoeffStr( )
    {
        String res = "";
        for(int i=coef.length-1;i>=0;i--)
        {
            res = res + coef[i];
        }
        return res;
    }

}
