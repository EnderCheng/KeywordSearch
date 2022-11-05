package ca.uwaterloo.cheng.modules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GGMTree {

    private int full_level;

    public GGMTree(int num_nodes)
    {
        this.full_level = (int) Math.ceil(Utils.log2(num_nodes));
    }

    public int getFull_level() {
        return full_level;
    }

    public List<GGMNode> min_coverage(List<GGMNode> node_list)
    {
        List<GGMNode> next_level_nodes = new ArrayList<>();

        for(int i=0;i<node_list.size();i++)
        {
            GGMNode node1 = node_list.get(i);
            if(i+1 == node_list.size())
            {
                next_level_nodes.add(node1);
            }
            else {
                GGMNode node2 = node_list.get(i+1);
                if((node1.getIndex() >> 1 == node2.getIndex() >> 1) && (node1.getLevel() == node2.getLevel()))
                {
                    next_level_nodes.add(new GGMNode(node1.getIndex() >> 1, node1.getLevel() - 1));
                    i++;
                }
                else {
                    next_level_nodes.add(node1);
                }
            }
        }

        if(next_level_nodes.size() == node_list.size() || next_level_nodes.isEmpty())
        {
            return node_list;
        }

        return min_coverage(next_level_nodes);
    }

    public static void main(String[] args) {
        int num_nodes = 28755212;
        GGMTree tree = new GGMTree(num_nodes);
        System.out.println(tree.getFull_level());
//        List<GGMNode> nodes = new ArrayList<>();
//        nodes.add(new GGMNode(0, tree.getFull_level()));
//        nodes.add(new GGMNode(1, tree.getFull_level()));
//        nodes.add(new GGMNode(4, tree.getFull_level()));
//        nodes.add(new GGMNode(5, tree.getFull_level()));
//
//        List<GGMNode> min_nodes = tree.min_coverage(nodes);
//        for (int i = 0; i < min_nodes.size(); i++)
//        {
//            System.out.println("index:"+min_nodes.get(i).getIndex()+","+"level:"+min_nodes.get(i).getLevel());
//            System.out.println("bits:"+min_nodes.get(i).toBits());
//        }

        int keyword_nums = 10000;
        int k = 20;
        Set<Integer> indexes = new HashSet<>();
        for(int i=0;i<keyword_nums*k;i++)
        {
            indexes.add(Utils.getRandomNumber(0,num_nodes));
        }
        List<GGMNode> nodes = new ArrayList<>();
        for(int index : indexes)
        {
            nodes.add(new GGMNode(index,tree.getFull_level()));
        }
        List<GGMNode> min_nodes = tree.min_coverage(nodes);
        Bits MK = Utils.get_random_rits(128);
        int key_size = 0;
        for (int i = 0; i < min_nodes.size(); i++)
        {
//            System.out.println("index:"+min_nodes.get(i).getIndex()+","+"level:"+min_nodes.get(i).getLevel());
//            System.out.println("bits:"+min_nodes.get(i).toBits());
            key_size = key_size + min_nodes.get(i).getKey(MK).length();
        }
        System.out.println(key_size);

    }
}
