package ca.uwaterloo.cheng.simulator;

import ca.uwaterloo.cheng.modules.*;

import java.util.*;

public class ClientKeySimulator {

    public static int countNum(boolean[] array)
    {
        int trueCount = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i]) {
                trueCount++;
            }
            if (trueCount >= 6) {
                break;
            }
        }
        return trueCount;
    }

    public static void main(String[] args) {
        Bits MK = Utils.get_random_rits(128);

        int keyword_nums = 3000;
        BloomFilter bf = new BloomFilter(keyword_nums,Math.pow(10,-5));
        int keywords_nodes = bf.getNumSlots();
        GGMTree tree_keywords = new GGMTree(keywords_nodes);

        int num_nodes = 50000;
        GGMTree tree = new GGMTree(num_nodes);

        double start,end;
        start = System.nanoTime();
        int document_nums = 30000;
        int keyword_auth_nums = 2000;
        Set<Integer> indexes = new HashSet<>();
        for(int i=0;i<document_nums;i++)
        {
            indexes.add(Utils.getRandomNumber(0,num_nodes));
        }
        List<GGMNode> nodes = new ArrayList<>();
        for(int index : indexes)
        {
            nodes.add(new GGMNode(index,tree.getFull_level()));
        }
        List<GGMNode> min_nodes = tree.min_coverage(nodes);

        Set<Integer> indexes_keywords = new HashSet<>();
        for(int i=0;i<keyword_auth_nums*bf.getNumHashes();i++)
        {
            indexes_keywords.add(Utils.getRandomNumber(0,num_nodes));
        }
        List<GGMNode> nodes_keywords = new ArrayList<>();
        for(int index : indexes_keywords)
        {
            nodes_keywords.add(new GGMNode(index,tree_keywords.getFull_level()));
        }
        List<GGMNode> min_nodes_keywords = tree_keywords.min_coverage(nodes_keywords);
        end = System.nanoTime();
        System.out.println("time:"+(end-start)/1000000);


        int key_size = 0;
        for (int i = 0; i < min_nodes.size(); i++)
        {
//            System.out.println("index:"+min_nodes.get(i).getIndex()+","+"level:"+min_nodes.get(i).getLevel());
//            System.out.println("bits:"+min_nodes.get(i).toBits());
            key_size = key_size + min_nodes.get(i).getKey(MK).length();
        }

        int key_size_keyword = 0;
        for (int i = 0; i < min_nodes_keywords.size(); i++)
        {
//            System.out.println("index:"+min_nodes.get(i).getIndex()+","+"level:"+min_nodes.get(i).getLevel());
//            System.out.println("bits:"+min_nodes.get(i).toBits());
            key_size_keyword = key_size_keyword + min_nodes_keywords.get(i).getKey(MK).length();
        }
        System.out.println("keysize:"+((key_size_keyword+key_size)/(8*1024)));
//        System.out.println(key_size);


        CoverFamily cff = new CoverFamily(5, 2);
        System.out.println("number of shared users:"+cff.getNum());
        System.out.println("number of mac size:"+cff.getLines());
        int num = 0;
        for(int i=0;i< cff.getNum();i++)
        {
            num = num + countNum(cff.getColumn(i));
        }
        System.out.println(num/cff.getNum());
    }
}
