package dsu.pasta.javaparser.gadget.program;

import dsu.pasta.config.ProjectConfig;
import dsu.pasta.javaparser.gadget.PriorityDefinition;
import dsu.pasta.javaparser.gadget.collect.GadgetUtils;
import dsu.pasta.javaparser.gadget.collect.GadgetsReader;
import dsu.pasta.javaparser.gadget.sketch.Dataflow;
import dsu.pasta.javaparser.gadget.sketch.Sketch;
import dsu.pasta.javaparser.gadget.sketch.SketchForeachCondition;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;
import dsu.pasta.javassist.JavassistSolver;
import dsu.pasta.utils.ZPrint;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class PenaltySynthesizer {

    public static int allCodeStr = 0; //partial or complete
    public static int testCodeNum = 0;

    /**
     * Sorted programs, their next usable sketches, and how many sketches that are already consumed, the previous correct index inside next usable sketches
     */
    public static List<Program> allPrograms = new ArrayList<>();
    public static HashSet<String> allCodeStringInSet = new HashSet<>();

    //    public static HashSet<Program> passTestPrograms = new HashSet<>();
    public static HashSet<String> passTestCode = new HashSet<>();

    public static HashSet<Program> compileFailPrograms = new HashSet<>();
    public static HashSet<String> compileFailCode = new HashSet<>();

    public static HashSet<Program> skipProgramWithSameCodeButNoDataflow = new HashSet<>();

    public static HashMap<Dataflow, Integer> dataflowUsedTimes = new HashMap<>();
    public static HashSet<Dataflow> banDataflow = new HashSet<>();

    public static HashSet<String> oneSizeDfPrograms = new HashSet<>();

    public static HashMap<String, Integer> completeAppearTimes = new HashMap<>();
    private static DecimalFormat df = new DecimalFormat("0.0000");
    private static boolean end() {
        if (allPrograms.size() <= 0) {
            ZPrint.verbose("No more programs left. Exit");
            return true;
        }
        return false;
    }

    public static void process() {
        ZPrint.verbose("Penalty function synthesis");
        allPrograms.add(new Program(true));
        while (true) {
            if (end())
                break;
            allPrograms.sort(Program.penaltyComparator);
            ZPrint.verbose("All program size is: " + allPrograms.size());
            for (int i = 0; i < allPrograms.size() && i < 50; i++) {
                Program p = allPrograms.get(i);
                p.addProgRankIndexes(i + 1);
            }
            Program program = allPrograms.remove(0);
            synthesizeProgram(program);
        }
        ZPrint.verbose("All programs, including partial/complete: " + allCodeStr);
    }


    /**
     * Generate all new programs for nowU
     *
     * @param program
     */
    public static void synthesizeProgram(Program program) {
        //program is the current best program, check whether it is complete.
        isCompletePassToTest(program);

        boolean tryMore = false;
        int tryNegTimesLimit = 20;
        if (GadgetsReader.allSketch.size() < 50) {
            tryNegTimesLimit = 500;
            tryMore = true;
            ProjectConfig.TRY_TOP_N_NEXT_STMT = 1000;
        }

        List<SketchGadget> nextUsable = GadgetUtils.getNextUsableSketchesFromNarrowScope(
                GadgetsReader.allSketch,
                program, true, tryMore);
        if (nextUsable == null || nextUsable.size() == 0) {
            ZPrint.verbose("No more next usable sketches.");
            return;
        }
        nextUsable.sort(Sketch.priorityComparator);
        int consume = 1;
        int tryNegTimes = 1;
        List<Program> newPrograms = new ArrayList<>();
        HashMap<Program, Integer> programToConsum = new HashMap<>();
        ZPrint.verbose("Next usable size is " + nextUsable.size());

        for (int i = 0; i < nextUsable.size() && consume < ProjectConfig.TRY_TOP_N_NEXT_STMT; i++) {
            SketchGadget sg = nextUsable.get(i);
            List<Dataflow> sgdfs = new ArrayList<>(sg.getOuterDataflow());
            ZPrint.verbose(consume + ", try add sketch: " + sg.getRangeAsString() + "@" + sg.getSketchString() +
                    " useApiChangeAllMethod " + sg.useApiChangeAllMethod +
                    " useApiConstructor " + sg.useApiConstructor +
                    " useRemovedField " + sg.useRemovedField +
                    " hasTargetFieldName " + sg.hasTargetFieldName +
                    " isDiffCode " + sg.isDiffCode +
                    " priority " + sg.getPriority()
            );

            if (sg.getPriority() <= PriorityDefinition.uselessGadget.getPriority() && !tryMore) {
                ZPrint.verbose("Useless here. Skip");
                continue;
            }
            if (sg.getPriority() <= -20)
                tryNegTimes++;

            if (tryNegTimes >= tryNegTimesLimit)
                break;

            boolean inc = false;
            if (sgdfs == null || sgdfs.size() == 0) {
                ZPrint.verbose("Dataflow is null");

                Program p = program.clone();
                p = addNormalSketchTo(sg, p, null);
                if (p != null) {
                    inc = true;
                    newPrograms.add(p);
                    programToConsum.put(p, consume);
                }
            } else {
                for (Dataflow df : sgdfs) {
                    if (banDataflow.contains(df)) {
                        ZPrint.verbose("Banned dataflow");
                        continue;
                    }
                    if (df.size() > 10) {
                        continue;
                    }
                    Program p = program.clone();
                    if (sg.isForeachStmt()) {
                        p = addForeachSketchTo(sg, p, df);
                    } else {
                        p = addNormalSketchTo(sg, p, df);
                    }
                    if (p != null) {
                        inc = true;
                        newPrograms.add(p);
                        programToConsum.put(p, consume);
                    }
                }
            }
            if (inc) {
                consume++;
            }
        }
        ZPrint.verbose("Consume gadgets " + consume + ", generated " + newPrograms.size() + " new programs.");
        newPrograms.sort(Program.similarityComparator);
        for (int i = 0; i < newPrograms.size(); i++) {
            Program p = newPrograms.get(i);
            int consum = programToConsum.containsKey(p) ? programToConsum.get(p) : (i + 1);
            p.addStmtRankIndex(Math.min(i + 1, consum));
        }
    }

    /**
     * @param program
     * @return true, skip; false keep
     */
    private static boolean appearTooManyTimes(Program program) {
        int times = 20;
        if (!program.newFieldCreated())
            return false;
        boolean test = false;
        for (String str : program.toCleanCodeNoNull()) {
            Integer appear = completeAppearTimes.get(str);
            if (appear == null)
                appear = 0;
            appear++;
            if (appear > times)
                test = true;
            completeAppearTimes.put(str, appear);
        }
        for (String str : program.toCleanCodeWithNull()) {
            Integer appear = completeAppearTimes.get(str);
            if (appear == null)
                appear = 0;
            appear++;
            if (appear > times)
                test = true;
            completeAppearTimes.put(str, appear);
        }
        //if test is false, then it appear too many times a
        if (!test)
            return false;

        return isCompletePassToTest(program);
    }

    private static Program addNormalSketchTo(SketchGadget sg, Program program, Dataflow df) {
        if (program.addStmtToTail(sg, df)) {
            allCodeStr += program.programSize();
            if (program.getAllDataflows().size() == 0) {
                if (skipProgramWithSameCodeButNoDataflow.contains(program))
                    return null;
            }
            skipProgramWithSameCodeButNoDataflow.add(program);
            ZPrint.verbose(program.toCleanCodeNoNull().toString()
                    + "\ntest? " + program.newFieldCreated());
            if (!skipFilter(program)) {
                allPrograms.add(program);
                return program;
            }
        }
        return null;
    }

    private static Program addForeachSketchTo(SketchGadget sg, Program program, Dataflow df) {
        if (!(sg instanceof SketchForeachCondition)) {
            return null;
        }
        boolean add = true;
        SketchForeachCondition fe = (SketchForeachCondition) sg;
        for (SketchGadget temp : fe.getOutterSelfInnerOrder()) {
            if (!program.addStmtToTail(temp, df))
                add = false;
        }
        allCodeStr += program.programSize();
        if (add) {
            skipProgramWithSameCodeButNoDataflow.add(program);
            ZPrint.verbose(program.toCleanCodeNoNull().toString()
                    + "\ntest? " + program.newFieldCreated());
            if (!skipFilter(program)) {
                allPrograms.add(program);
                return program;
            }
        }
        return null;
    }

    /**
     * @param program
     * @return true, is complete program and passes all tests (or has npe), skip; false, not complete or fail some tests
     */
    private static boolean isCompletePassToTest(Program program) {
        if (program.newFieldCreated()) {
            program.setAlreadyTestProgram();
            for (String code : program.toCleanCodeNoNull()) {
                printer(code, program);
            }
            for (String code : program.toCleanCodeWithNull()) {
                printer(code, program);
            }
            return true;
        } else {
            ZPrint.verbose("Not generated new field, continue.");
            return false;
        }
    }

    private static boolean skipFilter(Program program) {
        if (program == null) {
            ZPrint.verbose("Found a program that is null. Skip");
            return true;
        }
        if (program.getDataflowSize() == program.getAllDataflows().size()) {
            String clean = Program.getCleanCode(program.toString());
            if (oneSizeDfPrograms.contains(clean)) {
                ZPrint.verbose("Duplicate to a 1-size dataflow program. Skip");
                return true;
            }
            oneSizeDfPrograms.add(clean);
        }
        if (program.getAllDataflows().size() > 0) {
            int allMax = 0;
            for (Dataflow df : program.getAllDataflows()) {
                if (df == null)
                    continue;
                Integer times = dataflowUsedTimes.get(df);
                if (times == null)
                    times = 0;
                times++;
                dataflowUsedTimes.put(df, times);
                if (times > ProjectConfig.LIMIT_PRO_NUM_FOR_DF) {
                    banDataflow.add(df);
                    allMax++;
                    ZPrint.verbose("Ban df \n" + df.toString());
                }
            }
            if (program.getAllDataflows().size() <= allMax) {
//                ZPrint.verbose("All dataflows reach max number " + ProjectConfig.LIMIT_PRO_NUM_FOR_DF + " for generating transformer");
                return true;
            }
        }
        if (program.size() > ProjectConfig.maxTransLen) {
            ZPrint.verbose("Reach max length of program. Skip");
            return true;
        }
        if (compileFailPrograms.contains(program) || compileFailCode.contains(program.toString())) {
            ZPrint.verbose("Duplicate to a non-compilable transformer. Skip");
//            Main.verbose(program.toJavaCode());
            return true;
        }
//        //not use passing test programs to generate new programs.
//        if (passTestPrograms.contains(program) || passTestCode.contains(program.toString())) {
//            ZPrint.verbose("Duplicated to a passing test transformer. Skip");
//            return true;
//        }
        if (!program.hasHopeToGenOrUpdateNewField()) {
            ZPrint.verbose("This program has very low chance to generate new field instance. Update score");
            return true;
        }

        //not use non-compilable programs to generate new programs.
        if (!JavassistSolver.compaliableTrans(program)) {
            ZPrint.verbose("Can't compile program. Skip");
            compileFailPrograms.add(program);
            compileFailCode.add(program.toCleanString());
            return true;
        }
//        if (passTestPrograms.contains(program) || passTestCode.contains(program.toString())) {
//            program.setAlreadyTestProgram();
//            return true;
//        }
        if (appearTooManyTimes(program)) {
            ZPrint.verbose("Complete program appear many times.");
            return true;
        }
        return false;
    }

    private static void printer(String method, Program program) {
        if (allCodeStringInSet.contains(method)) {
            ZPrint.verbose("The method is same as one previous generated. Skip");
            return;
        } else {
            allCodeStringInSet.add(method);

            testCodeNum++;
            System.out.println("/* -------- Transformer #" + testCodeNum + " (cost = " + df.format(program.getPenalty()) + ") -------- */");
            //Make our code pretty
            method = PrettyCodePrinter.replaceNameForPrint(method);
            method = PrettyCodePrinter.makeCodePretty(method);
            method = PrettyCodePrinter.makeCodePrettyWithoutLinebreaks(method);
            System.out.println(method);
        }
    }
}
