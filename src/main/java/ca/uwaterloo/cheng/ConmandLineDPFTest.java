package ca.uwaterloo.cheng;

import ca.uwaterloo.cheng.modules.*;
import org.checkerframework.checker.guieffect.qual.UI;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public class ConmandLineDPFTest {

    // /Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/synthetic_keywords_1000_100.csv 100 1000 1 divorce

    public static boolean isInteger(String s) {
        return isInteger(s, 10);
    }

    public static boolean isInteger(String s, int radix) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (i == 0 && s.charAt(i) == '-') {
                if (s.length() == 1) return false;
                else continue;
            }
            if (Character.digit(s.charAt(i), radix) < 0) return false;
        }
        return true;
    }

    public static void printError()
    {
        System.out.println("Must inputs five args:");
        System.out.println("args[0] (String) - File Path to keywords.csv, e.g., \\user\\cheng\\keywords.csv;");
        System.out.println("args[1] (Int) - Maximum Keywords, e.g, 2000, should be >=1;");
        System.out.println("args[2] (Int) - Number of Documents, e.g., 10000, should be >=1");
        System.out.println("args[3] (Int [1,2,3,4]) - False Positive Rate - {1: 10^-3}, {2: 10^-4}, {3: 10^-5}, {4: 10^-6}");
        System.out.println("args[4] (String) - Search Keyword.");
    }

    public static void main(String[] args) {
        if (args.length == 5) {
            String path = args[0].trim();
            if (!isInteger(args[1].trim()) || !isInteger(args[2].trim()) || !isInteger(args[3].trim())) {
                System.out.println("Input parameters' formats are not correct!");
                printError();
                System.exit(-1);
            }
            int max_size = Integer.parseInt(args[1].trim());
            int doc_size = Integer.parseInt(args[2].trim());
            int mode = Integer.parseInt(args[3].trim());
            if (mode < 1 || mode > 5 || max_size < 2 || doc_size < 1) {
                System.out.println("Input parameters' contents are not correct!");
                printError();
                System.exit(-1);
            }

            String search_key_word = args[4].trim();

        } else {
            System.out.println("The number of parameters is not correct!");
            printError();
        }
    }

}
