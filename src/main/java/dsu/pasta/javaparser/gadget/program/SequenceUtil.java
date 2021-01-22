package dsu.pasta.javaparser.gadget.program;

import dsu.pasta.javaparser.gadget.sketch.Dataflow;
import dsu.pasta.javaparser.gadget.sketch.Sketch;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SequenceUtil {

    public static double getAverageLcsSimilarity(Program program, List<Dataflow> dataflows) {
        double sum = 0;
        for (Dataflow df : dataflows) {
            sum += getLcsSimilarity(program, df);
        }
        if (dataflows.size() == 0)
            return 0;
        return sum / dataflows.size();
    }

    public static double getLcsSimilarity(Program program, Dataflow dataflow) {
        List<SketchGadget> p = new ArrayList<>(program.getAllStatementsNoSpecialForeach());
        List<SketchGadget> d = dataflow.getFlow();

        int psize = p.size();
        int diff = 0;

        List<SketchGadget> lcs = getLCS(p, d);
        for (int i = lcs.size() - 1; i >= 0; i--) {
            int indexInP = p.indexOf(lcs.get(i));

            if (indexInP < 0)
                continue;

            assert indexInP >= 0;

            diff += psize - indexInP - 1;
            psize = indexInP;
        }

        int sameMinusDiff = 2 * lcs.size() - diff;
        if (sameMinusDiff <= 0)
            sameMinusDiff = lcs.size();

        return (double) sameMinusDiff / (program.size() + d.size());

    }

    public static List<SketchGadget> getLCS(List<SketchGadget> one, List<SketchGadget> two) {
        List<SketchGadget> lcs = new ArrayList<>();

        int pre2 = -1;
        for (Sketch sk1 : one) {
            if (!(sk1 instanceof SketchGadget))
                continue;
            if (sk1.getSketchString().contains("dsu") && !sk1.isForeachStmt())
                continue;
            if (!two.contains(sk1))
                continue;
            int index2 = two.indexOf(sk1);
            if (index2 <= pre2) {
                lcs.clear();
                break;
            } else {
                lcs.add((SketchGadget) sk1);
            }
        }
        return lcs;
    }

    /**
     * <tt>NOTE:</tt><p>
     * If <tt>newSk</tt> is not in <tt>followThisDataflow</tt>, it actually follows LCS if we append <tt>newSk</tt> to <tt>existingSketches</tt>.
     * But we return <tt>false</tt> for that case.
     * So make sure <tt>newSk</tt> is inside <tt>followThisDataflow</tt> is better.
     *
     * @param newSk
     * @param existingSketches
     * @param followThisDataflow
     * @return <tt>false</tt>, if <tt>newSk</tt>: is not <tt>SketchGadget</tt>, or in <tt>existingSketchs</tt>, or not in <tt>followThisDataflow</tt>, or is before last of <tt>existingSketchs</tt>
     */
    public static boolean followLCS(Sketch newSk, List<SketchGadget> existingSketches, Dataflow followThisDataflow) {
        if (!(newSk instanceof SketchGadget))
            return false;
        if (existingSketches.contains(newSk))
            return false;
        if (!followThisDataflow.getFlow().contains(newSk))
            return false;
        //dataflow has newSk
        if (existingSketches.size() == 0) //newSk can be put in existingSketches any where.
            return true;

        SketchGadget last = existingSketches.get(existingSketches.size() - 1);
        int shouldBefore = followThisDataflow.getFlow().indexOf(last);
        if (shouldBefore < 0) {
//            System.err.println("May be an error here.");
            return false;
        }
        int shouldAfter = followThisDataflow.getFlow().indexOf(newSk);
        if (shouldAfter < 0) {
            return false;
        }
        return shouldAfter > shouldBefore;
    }

    public static List<SketchGadget> getSketchAfterExistingSketches(List<SketchGadget> existingSketches, Dataflow followThisDataflow) {
        List<SketchGadget> df = followThisDataflow.getFlow();
        if (df == null || df.size() == 0)
            return Collections.emptyList();

        if (existingSketches.size() == 0)
            return df;
        SketchGadget last = existingSketches.get(existingSketches.size() - 1);
        int index = df.indexOf(last);
        if (index < 0) {
//            System.err.println("Error here.");
            return Collections.emptyList();
        }
        if (index + 1 >= df.size())
            return Collections.emptyList();
        return df.subList(index + 1, df.size());

    }
}
