package dsu.pasta.javaparser.gadget.collect;

import dsu.pasta.config.ProjectConfig;
import dsu.pasta.javaparser.gadget.PriorityDefinition;
import dsu.pasta.javaparser.gadget.sketch.Context;
import dsu.pasta.javaparser.gadget.sketch.Dataflow;
import dsu.pasta.javaparser.gadget.sketch.Sketch;
import dsu.pasta.utils.ZPrint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GadgetsReader {
    public static HashMap<String, Integer> typeDistance = new HashMap<>();
    public static List<Sketch> allSketch = new ArrayList<>();
    public static List<Sketch> uselessSketch = new ArrayList<>();

    public static List<Context> allContext = new ArrayList<>();

    //    public static HashMap<Sketch, ArrayList<Sketch>> mergedSketch = new HashMap<>();
    public static int allReadSketchNumber = 0;

    public static void process() {
        typeDistance = GadgetUtils.readDistancesOfType();
        ZPrint.verbose("There are " + typeDistance.size() + " types within the maximum distance " + ProjectConfig.readGadgetRange);
        ZPrint.verbose("Read gadgets from " + typeDistance.size() + " classes.");
        allSketch = new ArrayList<>();
        allContext = new ArrayList<>();

        ZPrint.verbose("Read gadgets and contexts from types");
        GadgetUtils.readGadgetsFromTypes(allSketch, allContext, typeDistance);
        ZPrint.verbose("There are " + Dataflow.allDataflows.size() + " dataflows about (generate/use) new field, and " + Dataflow.dataflowsCanGenerateNewField.size() + " dataflows can generate new field.");

        for (Sketch sk : uselessSketch) {
            if (sk.isIfStmt())
                sk.incrementPriority(PriorityDefinition.uselessGadget.getPriority() / 2);
            else
                sk.incrementPriority(PriorityDefinition.uselessGadget);
        }
        uselessSketch.sort(Sketch.priorityComparator);

        ZPrint.verbose(Dataflow.allDataflows.size() + " dataflows that generate/use new field type instance");
        ZPrint.verbose(Dataflow.dataflowsCanGenerateNewField.size() + " dataflows that generate new field type instance");

        ZPrint.verbose("Gadgets in dataflow are " + allSketch.size());
        ZPrint.verbose("Gadgets out dataflow are " + uselessSketch.size());
        ZPrint.verbose("All gadgets number is " + (allSketch.size() + uselessSketch.size()));
        List<Sketch> tempAll = new ArrayList<>(allSketch);
        tempAll.addAll(uselessSketch);


        ZPrint.verbose("Before merging constants, there are " + tempAll.size() + " gadgets");
        GadgetUtils.mergeConstantGadgets(tempAll);
        ZPrint.verbose("After merging constants, there are " + tempAll.size() + " gadgets");
        HashMap<Sketch, ArrayList<Sketch>> merged = GadgetUtils.mergeGadgets(tempAll);
        ZPrint.verbose("After merging gadgets, there are " + merged.size());
        ZPrint.verbose("All dataflows");
        for (Dataflow df : Dataflow.getSortedAllDfBySize()) {
            ZPrint.verbose("Can generate " + df.isCanGenerateTargetInstance() + "\n" + df.toString());
        }
    }
}
