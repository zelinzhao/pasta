package dsu.pasta.javaparser.gadget.program;

import java.util.ArrayList;
import java.util.List;

public class PenaltyFunction {
    public static double penalty(Program program) {
        return penalty(program.getStmtRankIndexes(), program.getLcsSimilarity());
    }

    /**
     * (score)^(-1)*Sum(rank(gi)*i)
     *
     * @param indexes
     * @return
     */
    private static double penalty(int[] indexes, double score) {
        double sum = 0.0;
        for (int i = 0; i < indexes.length; i++) {
            sum += rank(indexes[i]) * (i + 1);
        }

        if (score <= 0) {
            score = 0.0001;
        }
        double ahead = (double) 1 / score;
        return ahead * sum;

//        double ahead= Math.pow(score, -1);
//        return ahead*sum;

//        double m =0;
//        if(Math.exp(1) < score)
//            m = 0.1;
//        else
//            m = Math.pow (Math.exp(1)-score, indexes.length);
//        return m*sum;
    }

    /**
     * 2*rank i
     *
     * @param index
     * @return
     */
    private static double rank(int index) {
        if (index <= 10)
//            return index;
//            return Math.pow(index, 2);
            return index;
        else {
            return index;
//            return Math.pow(index, 2);
        }
    }

    public static void main(String[] args) {
        List<int[]> vecs = new ArrayList<>();
        vecs.add(new int[]{0, 50, 5});
        vecs.add(new int[]{2, 0, 25});
        vecs.add(new int[]{1, 0, 0});
        vecs.add(new int[]{1, 1});
        vecs.add(new int[]{1, 87});
        vecs.add(new int[]{5, 10, 21});
        vecs.add(new int[]{3, 41});
        vecs.add(new int[]{2, 14});
        vecs.add(new int[]{0});
        vecs.add(new int[]{0, 0});
        vecs.add(new int[]{28, 0});
        vecs.add(new int[]{1});
        vecs.add(new int[]{1, 0, 4});
        vecs.add(new int[]{2, 3});
        vecs.add(new int[]{2, 0});

        for (int[] vec : vecs) {
            double p = penalty(vec, 0);
            int less = 0;
            int[] c = new int[5];
            for (int i1 = 0; i1 < 100; i1++) {
                c[0] = i1;
                for (int i2 = 0; i2 < 100; i2++) {
                    c[1] = i2;
                    for (int i3 = 0; i3 < 100; i3++) {
                        c[2] = i3;
                        for (int i4 = 0; i4 < 100; i4++) {
                            c[3] = i4;
                            for (int i5 = 0; i5 < 100; i5++) {
                                c[4] = i5;
                                if (penalty(c, 0) < p)
                                    less++;
                                else
                                    break;
                            }
                        }
                    }
                }
            }
            System.out.println(less);
//            System.out.println("Penalty for indexes "+ Arrays.toString(vec)+ " is "+ p+".|||||||"+ less+" is better then it");
        }
    }
}
