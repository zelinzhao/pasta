package dsu.pasta.javaparser.gadget.program;

import com.github.javaparser.resolution.types.ResolvedPrimitiveType;
import com.github.javaparser.resolution.types.ResolvedType;
import dsu.pasta.config.ProjectConfig;
import dsu.pasta.config.UpdateConfig;
import dsu.pasta.javaparser.gadget.sketch.*;
import dsu.pasta.utils.ZCollectionUtils;
import dsu.pasta.utils.ZPrint;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Program {
    /**
     * The higher the better
     */
    public static Comparator<Program> similarityComparator = new Comparator<Program>() {
        @Override
        public int compare(Program o1, Program o2) {
            if (o1.getLcsSimilarity() == o2.getLcsSimilarity()) {
                return o1.estimateFutureDistance - o2.estimateFutureDistance;
//                return 0;
            } else if (o1.getLcsSimilarity() < o2.getLcsSimilarity())
                return 1;
            else
                return -1;
        }
    };
    public static Comparator<Program> penaltyComparator = new Comparator<Program>() {
        @Override
        public int compare(Program o1, Program o2) {
            if (o1.getPenalty() == o2.getPenalty()) {
                return o1.getEstimateFutureDistance() - o2.getEstimateFutureDistance();
            } else if (o1.getPenalty() < o2.getPenalty())
                return -1;
            else
                return 1;
        }
    };
    private static SketchGadget v0_oldInstance = new SketchGadget();
    private static Hole v0_hole = null;
    private static String v0_code = null;
    private static SketchGadget v1_newFieldInstance = new SketchGadget();
    private static Hole v1_hole = null;
    private static String v1_code = null;
    private static Pattern newRefStmtP = Pattern.compile(".*(var_[0-9])+\\s*=\\s*new.*");
    private static Pattern p = Pattern.compile("<[^<>]*>");

    static {
        v0_oldInstance.addElement(new Element(Variable.getOldInstanceType()));
        v0_oldInstance.addElement(Element.op_space);

        v0_hole = new Hole("this", Variable.getOldInstanceType(), false, Hole.HoleFrom.instance);
        v0_oldInstance.addElement(v0_hole);
        v0_oldInstance.addElement(Element.op_equal);
        v0_oldInstance.addElement(new Element("this"));
        v0_oldInstance.addElement(Element.op_semicolon);
        v0_code = UpdateConfig.one().targetClass + " " + Variable.getOldInstance().getName() + " = this;\n";


        v1_newFieldInstance.addElement(new Element(Variable.getNewFieldType()));
        v1_newFieldInstance.addElement(Element.op_space);
        v1_hole = new Hole(UpdateConfig.one().getNewFieldOriginalName(), Variable.getNewFieldType(), false, Hole.HoleFrom.variable);
        v1_newFieldInstance.addElement(v1_hole);
        v1_code = UpdateConfig.one().getNewFieldTypeRealString() + " " + Variable.getNewFieldInstance().getName();

        v1_newFieldInstance.addElement(Element.op_equal);
        String init = "";
        if (Variable.getNewFieldType().isPrimitive()) {
            ResolvedPrimitiveType pri = Variable.getNewFieldType().asPrimitive();
            if (pri.isNumeric())
                init = "0";
            else
                init = "true";
        } else if (Variable.getNewFieldType().isReferenceType()) {
            init = "null";
        }
        v1_newFieldInstance.addElement(new Element(init));
        v1_code += "=" + init + ";\n";

        v1_newFieldInstance.addElement(Element.op_semicolon);
    }

    public boolean ifIsOpen = false;
    public boolean loopIsOpen = false;
    public boolean isDiffCode = false;
    public boolean useRemovedField = false;
    public boolean hasTargetFieldName = false;
    public boolean useApiConstructor = false;
    public boolean useApiChangeAllMethod = false;
    public boolean hasIf = false;
    public boolean hasLoop = false;
    private AvailableVariables variables = new AvailableVariables();
    /**
     * Not contains the first two lines of base code
     */
    private LinkedList<SketchGadget> allStatements = new LinkedList<>();
    /**
     * Not contains the first two lines of base code
     */
    private HashMap<Dataflow, List<SketchGadget>> dataflowsToExistStmts = new HashMap<>();

    //    private DFGraph graph;
    private List<Dataflow> forEqualHashDataflow = new ArrayList<>();
    private List<String> codeListWithNullChek = new ArrayList<>();
    private List<String> codeListNoNullCheck = new ArrayList<>();
    private String _forEqualHash = "";
    private int completeFail = 1;
    private int[] stmtRankIndexes = new int[0];
    private int[] progRankIndexes = new int[0];
    private Double penalty = null;
    private boolean lastIsLoopFor = false;
    private HashSet<String> alreadyInitVar = new HashSet<>();
    /**
     * Don't clone this similarity field
     */
    private Double lcsSimilarity = null;
    private SketchGadget preIf = null;
    private SketchGadget preLoop = null;
    private int sketch_no_df;
    private int estimateFutureDistance = Integer.MAX_VALUE;
    private int size1Df = 0;

    public Program() {
        this(false);
    }

    public Program(boolean init) {
        if (init)
            this.initFirstStmt();
    }

    public static String getCleanCode(String trans) {
        String result = new String(trans);
        result = result.replace("this", "$0");

        String pre = result;
        while (result.contains("<") && result.contains(">")) {
            result = p.matcher(result).replaceAll(""); //result.replaceAll(, "");
            if (result.equals(pre))
                break;
            else
                pre = result;
        }
        return result;
    }

    public double getPenalty() {
        if (penalty != null && penalty != 0.0)
            return penalty;
        penalty = PenaltyFunction.penalty(this);
        return penalty;
    }

    public int getEstimateFutureDistance() {
        return this.estimateFutureDistance;
    }

    public void setAlreadyTestProgram() {
        this.variables.setAlreadyTestField();
    }

    public Program clone() {
        Program target = new Program();
        target.variables = this.variables.clone();
        target.allStatements = new LinkedList<>(this.allStatements);
        for (Dataflow df : dataflowsToExistStmts.keySet()) {
            target.dataflowsToExistStmts.put(df, new ArrayList<>(dataflowsToExistStmts.get(df)));
        }
        target.forEqualHashDataflow = new ArrayList<>(this.forEqualHashDataflow);
        target.codeListWithNullChek = new ArrayList<>(this.codeListWithNullChek);
        target.codeListNoNullCheck = new ArrayList<>(this.codeListNoNullCheck);
        target._forEqualHash = this._forEqualHash;
        target.completeFail = this.completeFail;
        target.preIf = this.preIf;
        target.preLoop = this.preLoop;
        target.sketch_no_df = this.sketch_no_df;
        target.ifIsOpen = this.ifIsOpen;
        target.loopIsOpen = this.loopIsOpen;
        target.isDiffCode = this.isDiffCode;
        target.useRemovedField = this.useRemovedField;
        target.hasTargetFieldName = this.hasTargetFieldName;
        target.useApiConstructor = this.useApiConstructor;
        target.useApiChangeAllMethod = this.useApiChangeAllMethod;
        target.hasLoop = this.hasLoop;
        target.hasIf = this.hasIf;
        target.size1Df = this.size1Df;
        target.alreadyInitVar = new HashSet<>(this.alreadyInitVar);

        target.stmtRankIndexes = Arrays.copyOf(this.stmtRankIndexes, this.stmtRankIndexes.length);
        target.progRankIndexes = Arrays.copyOf(this.progRankIndexes, this.progRankIndexes.length);
        return target;
    }

    public List<SketchGadget> getAllStatementsNoSpecialForeach() {
        List<SketchGadget> result = new ArrayList<>(this.allStatements);
        Iterator<SketchGadget> pit = result.iterator();
        while (pit.hasNext()) {
            SketchGadget psg = pit.next();
            if (psg.getSketchString().contains("dsu") && !psg.isForeachStmt())
                pit.remove();
        }
        return result;
    }

    public double getLcsSimilarity() {
        if (lcsSimilarity != null)
            return lcsSimilarity;
        lcsSimilarity = SequenceUtil.getAverageLcsSimilarity(this, this.forEqualHashDataflow);

        if (this.isDiffCode)
            lcsSimilarity += 0.2;
        if (this.useRemovedField)
            lcsSimilarity += 0.2;
        if (this.hasTargetFieldName)
            lcsSimilarity += 0.2;
//        if( this.useApiConstructor)
//            lcsSimilarity += 0.2;
//        if(this.useApiChangeAllMethod)
//            lcsSimilarity += 0.4;

        return lcsSimilarity;
    }

    public Set<Dataflow> getAllDataflows() {
        return dataflowsToExistStmts.keySet();
    }

    public boolean accept(SketchGadget sketch, Dataflow fromDf) {
//        if(sketch.getPriority()<= PriorityDefinition.uselessGadget.getPriority()){
//            Main.verbose("Priority is too low");
//            return false;
//        }
        if (this.size() >= ProjectConfig.maxTransLen) {
            ZPrint.verbose("Size overflow");
            return false;
        }
        if (this.allStatements.contains(sketch)) {
            ZPrint.verbose("Already has");
            return false;
        }
        if (_forEqualHash.contains(getCleanCode(sketch.getSketchString()))) {
            ZPrint.verbose("Already has");
            return false;
        }
        if (fromDf == null && sketch_no_df >= 2) {
//            Main.verbose("No dataflow statement number overflow");
            return false;
        }
        //dataflow is not null in following

        if (fromDf != null && fromDf.size() > 1
                && (dataflowsToExistStmts.size() - size1Df) >= 1
                && !dataflowsToExistStmts.keySet().contains(fromDf)) {
            return false;
        }
        //dataflow is inside dataflowsToExistStmts in following, or dataflowsToExistStmts size is acceptable
        if (this.ifIsOpen && sketch.isIfStmt()) {
            ZPrint.verbose("Already has if");
            return false;
        }
        if (this.loopIsOpen && sketch.isLoop()) {
            ZPrint.verbose("Already has loop");
            return false;
        }
        if (fromDf != null) {
            for (Dataflow df : this.dataflowsToExistStmts.keySet()) {
                if (df.equals(fromDf))
                    continue;
                if (df.overlapWith(fromDf)) {
                    ZPrint.verbose("Dataflows overlap");
                    return false;
                }
            }
            List<SketchGadget> existSketches = this.dataflowsToExistStmts.get(fromDf);
            if (existSketches != null && !SequenceUtil.followLCS(sketch, existSketches, fromDf)) {
                ZPrint.verbose("Not follow lcs rule");
                return false;
            }
        }
        return true;
    }

    public boolean hasHopeToGenOrUpdateNewField() {
        if (this.dataflowsToExistStmts.size() == 0)
            return true;
        if (this.allStatements.getLast().canGenerate(UpdateConfig.one().getNewFieldType(), true)
                || this.allStatements.getLast().needTheType(UpdateConfig.one().getNewFieldType()))
            return true;

        boolean all1 = true;
        boolean canGen = false;
        boolean canUse = false;
        for (Dataflow df : this.dataflowsToExistStmts.keySet()) {
            if (df.size() == 1) {
                continue;
            }
            all1 = false;
            List<SketchGadget> existing = this.dataflowsToExistStmts.get(df);
            List<SketchGadget> left = SequenceUtil.getSketchAfterExistingSketches(existing, df);
            for (SketchGadget sg : left) {
                if (sg.needTheType(UpdateConfig.one().getNewFieldType()))
                    canUse = true;
                if (sg.canGenerate(UpdateConfig.one().getNewFieldType(), true))
                    canGen = true;
                if (canGen || canUse)
                    return true;
            }
        }
        if (all1)
            return true;
        return false;
    }

    private boolean closeIf(SketchGadget sketchGadget) {
        if (!this.ifIsOpen || this.preIf == null)
            return false;
        //TODO test get_thisAsContext
        if (this.preIf.get_thisAsContext() == null) {
            this.ifIsOpen = false;
            return true;
        }
        if (!this.preIf.get_thisAsContext().getAllInsideSketch().contains(sketchGadget)) {
            this.ifIsOpen = false;
            return true;
        }
        return false;
    }

    private boolean closeLoop(SketchGadget sketchGadget) {
        if (!this.loopIsOpen || this.preLoop == null)
            return false;
        //TODO test get_thisAsContext
        if (this.preLoop.get_thisAsContext() == null)
            return true;
        if (!this.preLoop.get_thisAsContext().getAllInsideSketch().contains(sketchGadget)) {
            this.loopIsOpen = false;
            return true;
        }
        return false;
    }

    public List<String> filterBadCode(List<String> codeList) {
        Iterator<String> it = codeList.iterator();
        while (it.hasNext()) {
            String code = it.next();
            code = code.trim();
            //remove const for same var
            Matcher m = newRefStmtP.matcher(code);
            if (m.find()) {
                String varName = m.group(1);
                ZPrint.verbose("Var name " + varName);
                if (alreadyInitVar.contains(varName)) {
                    it.remove();
                    ZPrint.verbose("Already initialized");
                    continue;
                }
                alreadyInitVar.add(varName);
            }
            //a=a; skip
            if (code.contains("=") && !code.contains("==")) {
                code = code.replace(";", "");
                code = code.replace("\n", "");
                code = code.replaceAll("\\s+", "");
                String[] sp = code.split("=");
                if (sp.length == 2 && sp[0].equals(sp[1])) {
                    it.remove();
                } else if (!code.contains("==") && sp[0].contains(Variable.getOldInstance().getName())) {
                    //var_o.xx=xxx;
                    it.remove();
                }
                ZPrint.verbose("sp: " + code);
            }


        }
        return codeList;
    }

    public void addStmtRankIndex(int index) {
        int[] newArr = Arrays.copyOf(this.stmtRankIndexes, this.stmtRankIndexes.length + 1);
        newArr[newArr.length - 1] = index;
        this.stmtRankIndexes = newArr;
    }

    public int[] getStmtRankIndexes() {
        return this.stmtRankIndexes;
    }

    public void addProgRankIndexes(int index) {
        int[] newArr = Arrays.copyOf(this.progRankIndexes, this.progRankIndexes.length + 1);
        newArr[newArr.length - 1] = index;
        this.progRankIndexes = newArr;
    }

    public boolean addStmtToTail(SketchGadget sketchGadget, Dataflow fromDf) {
        if (!sketchGadget.getSketchString().contains("dsu") && fromDf != null && !sketchGadget.getOuterDataflow().contains(fromDf)) {
            return false;
        }
        if (accept(sketchGadget, fromDf)) {
            ZPrint.verbose("Accept stmt");
            Pair<List<String>, List<String>> noNull2Null = toCode(sketchGadget);
            List<String> noNull = noNull2Null.getKey();
            List<String> hasNull = noNull2Null.getValue();
            ZPrint.verbose("No null code size " + noNull.size());
            ZPrint.verbose("has null code size " + hasNull.size());

            noNull = filterBadCode(noNull);
            hasNull = filterBadCode(hasNull);
            Iterator<String> nnit = noNull.iterator();
            while (nnit.hasNext()) {
                String nn = nnit.next();
                for (String str : this.codeListNoNullCheck) {
                    if (str.contains(nn)) {
                        ZPrint.verbose("Already generated such code");
                        nnit.remove();
                        break;
                    }
                }
            }
            Iterator<String> hnit = hasNull.iterator();
            while (hnit.hasNext()) {
                String hn = hnit.next();
                for (String str : this.codeListWithNullChek) {
                    if (str.contains(hn)) {
                        ZPrint.verbose("Already generated such code");
                        hnit.remove();
                        break;
                    }
                }
            }

            if (noNull.size() == 0 && hasNull.size() == 0)
                return false;

            _forEqualHash += getCleanCode(sketchGadget.getSketchString());
            this.allStatements.addLast(sketchGadget);

            if (fromDf != null) {
                if (fromDf.size() == 1)
                    size1Df++;
                List<SketchGadget> exists = this.dataflowsToExistStmts.get(fromDf);
                if (exists == null)
                    exists = new ArrayList<>();
                exists.add(sketchGadget);
                this.dataflowsToExistStmts.put(fromDf, exists);
                if (!this.forEqualHashDataflow.contains(fromDf))
                    this.forEqualHashDataflow.add(fromDf);
            } else {
                sketch_no_df++;
            }

            if (noNull.size() > 0) {
                noNull.replaceAll(s -> getCleanCode(s));
//                noNull.replaceAll(s -> s + sketchGadget.getSimpleSketchString());
            }
            if (hasNull.size() > 0) {
                hasNull.replaceAll(s -> getCleanCode(s));
//                hasNull.replaceAll(s -> s + sketchGadget.getSimpleSketchString());
            }

            if (sketchGadget.isIfStmt()) {
                this.hasIf = true;
                this.ifIsOpen = true;
                this.preIf = sketchGadget;
                _forEqualHash += "@if@";
                this.lastIsLoopFor = true;
            } else if (sketchGadget.isLoop()) {
                this.hasLoop = true;
                _forEqualHash += "@loop@";
                this.loopIsOpen = true;
                this.preLoop = sketchGadget;
                this.lastIsLoopFor = true;
            } else {
                if (closeIf(sketchGadget)) {
                    if (noNull.size() > 0)
                        noNull.replaceAll(s -> s + "}\n");
                    if (hasNull.size() > 0)
                        hasNull.replaceAll(s -> s + "}\n");
                    _forEqualHash += "@endif@";
                }
                if (preLoop instanceof SketchForeachCondition && sketchGadget.getSketchString().contains("dsu")) {
                    ZPrint.verbose("Wait for foreach end.");
                } else if (closeLoop(sketchGadget)) {
                    if (noNull.size() > 0)
                        noNull.replaceAll(s -> s + "}\n");
                    if (hasNull.size() > 0)
                        hasNull.replaceAll(s -> s + "}\n");
                    _forEqualHash += "@endloop@";
                }
            }

            if (noNull.size() > 0 && hasNull.size() > 0) {
                this.codeListWithNullChek = ZCollectionUtils.cartesianAppendString(this.codeListWithNullChek, hasNull);
                this.codeListNoNullCheck = ZCollectionUtils.cartesianAppendString(this.codeListNoNullCheck, noNull);
            } else if (noNull.size() > 0) {
                this.codeListWithNullChek = ZCollectionUtils.cartesianAppendString(this.codeListWithNullChek, noNull);
                this.codeListNoNullCheck = ZCollectionUtils.cartesianAppendString(this.codeListNoNullCheck, noNull);
            } else if (hasNull.size() > 0) {
                this.codeListWithNullChek = ZCollectionUtils.cartesianAppendString(this.codeListWithNullChek, hasNull);
                this.codeListNoNullCheck = ZCollectionUtils.cartesianAppendString(this.codeListNoNullCheck, hasNull);
            }

            if (!sketchGadget.getSketchString().contains("dsu") || sketchGadget.isForeachStmt()) {
                int sketchDistance = sketchGadget.getDistanceToGenerateNewFieldType();
                int typeDistance = Integer.MAX_VALUE;
                ResolvedType type = sketchGadget.generate();
                if (type == null) {
                    type = sketchGadget.getSourceHoleTypes().size() > 0 ? sketchGadget.getSourceHoleTypes().get(0) : null;
                }
                if (type != null) {
                    Integer temp = Dataflow.distanceBetweenSourceTarget.get(type);
                    if (temp != null)
                        typeDistance = temp;
                }
                int min = Math.min(sketchDistance, typeDistance);
                if (min < Integer.MAX_VALUE && min < Sketch.DISTANCE_MAX)
                    this.estimateFutureDistance = min;
            }
            if (sketchGadget.isDiffCode)
                this.isDiffCode = true;
            if (sketchGadget.useRemovedField)
                this.useRemovedField = true;
            if (sketchGadget.hasTargetFieldName)
                this.hasTargetFieldName = true;
            if (sketchGadget.useApiConstructor)
                this.useApiConstructor = true;
            if (sketchGadget.useApiChangeAllMethod)
                this.useApiChangeAllMethod = true;
            return true;
        }
        return false;
    }

    public int size() {
        if (preLoop != null && preLoop instanceof SketchForeachCondition) {
            SketchForeachCondition temp = (SketchForeachCondition) preLoop;
            return this.allStatements.size() - temp.getExtraSize();
        }
        return this.allStatements.size();
    }

    /**
     * Put <tt>Type v0 = old;</tt> in the first statement.
     * Put <tt>Field v1 = default;</tt> in the second statement.
     */
    public void initFirstStmt() {
        variables.add(v0_hole, Variable.getOldInstance());
        variables.add(v1_hole, Variable.getNewFieldInstance());
//        allStatements.addLast(v0_oldInstance);
//        allStatements.addLast(v1_newFieldInstance);
        String baseCode = v0_code;
        baseCode += v1_code;
        codeListWithNullChek.add(getCleanCode(baseCode));
        codeListNoNullCheck.add(getCleanCode(baseCode));
        _forEqualHash += getCleanCode(baseCode);
//        _forEqualHash += v0_oldInstance.getSketchString();
//        _forEqualHash += v1_newFieldInstance.getSketchString();
    }

    public int getDataflowSize() {
        int i = 0;
        for (Dataflow df : this.forEqualHashDataflow)
            i += df.size();
        return i;
    }

    private Pair<List<String>, List<String>> toCodeIfLoop(SketchGadget sketchGadget) {
        if (!sketchGadget.isIfStmt() && !sketchGadget.isLoop())
            return new MutablePair<List<String>, List<String>>(Collections.emptyList(), Collections.emptyList());
        List<Pair<String, String>> noNull2HasNull = new ArrayList<>();

        List<String> codeNoNull = new ArrayList<>();
        String result = "";
        if (sketchGadget.isIfStmt())
            result += "if(";
        else if (sketchGadget.isForStmt())
            result += "for(";
        else if (sketchGadget.isForeachStmt()) {
            result += "while(";
        } else if (sketchGadget.isWhileStmt())
            result += "while(";
        final String temp = result;
        codeNoNull = toCodeForElements(sketchGadget.getElements());
        codeNoNull.replaceAll(s -> temp + s + "){\n" + sketchGadget.getSimpleSketchString());

        return new MutablePair<List<String>, List<String>>(codeNoNull, Collections.emptyList());
    }

    private List<String> toCodeForElements(List<Element> elements) {
        List<String> resultList = new ArrayList<>();
        resultList.add("");
        for (Element e : elements) {
            if (e instanceof Hole) {
                resultList = ZCollectionUtils.cartesianAppendString(
                        resultList,
                        variables.getVariables((Hole) e, false, this)
                                .stream().map(v -> v.getName()).collect(Collectors.toList()));

            } else {
                resultList.replaceAll(s -> s + e.getSketchString(false));
            }
        }
        return resultList;
    }

    //codeNoNull, codeWithNull
    private Pair<List<String>, List<String>> toCode(SketchGadget sketch) {
        if (sketch.isLoop() || sketch.isIfStmt())
            return toCodeIfLoop(sketch);
        String targetType = null;
        String targetName = null;
        String init = null;
        boolean alreadyHasTarget = false;

        //generate new variable for target hole, if no available one
        int startIndex = 0;
        if (sketch.getTargetHole() != null) {
            Hole target = sketch.getTargetHole();
            startIndex = sketch.getElements().indexOf(target) + 1;
            ResolvedType resolvedType = target.getType();
            targetType = target.getTypeAsString();
            List<Variable> vars = variables.getVariables(target, true, this);
            if (vars != null && vars.size() > 0) {
                targetName = variables.getAccurateVariableForHole(target, true, this).getName();
                alreadyHasTarget = true;
            } else {
                Variable v = variables.createVariableForHole(target);
                targetName = v.getName();
            }
            if (resolvedType != null && resolvedType.isPrimitive()) {
                ResolvedPrimitiveType pri = resolvedType.asPrimitive();
                if (pri.isNumeric())
                    init = "0";
                else
                    init = "true";
            } else if (resolvedType != null && resolvedType.isReferenceType()) {
                init = "null";
            } else {
                init = "null";
            }
        }
        //after this, the first index is not target hole
        //key is code, value is null checker
        HashMap<String, String> codeToNullCheck = new HashMap<>();

        for (int i = startIndex; i < sketch.getElements().size(); i++) {
            Element e = sketch.getElements().get(i);
            if (e instanceof Hole) {
                Hole h = (Hole) e;
                List<Variable> vars = variables.getVariables(h, false, this);
                if (vars == null || vars.size() == 0) {
//                    System.err.println("Error here. No available variables for source hole " + h);
                    continue;
                }
                boolean addNullcheckForHole = false;
                if (i + 1 < sketch.getElements().size()
                        && sketch.getElements().get(i + 1) != null
                        && !(sketch.getElements().get(i + 1) instanceof Hole)
                        && sketch.getElements().get(i + 1).getOriginalString() != null
                        && sketch.getElements().get(i + 1).getOriginalString().equals("."))
                    addNullcheckForHole = true;

                HashMap<String, String> temp = new HashMap<>();
                for (Variable v : vars) {
                    if (codeToNullCheck.keySet().size() == 0) {
                        if (addNullcheckForHole) {
                            temp.put(v.getName(), v.getName() + " != null");
                        } else
                            temp.put(v.getName(), "");
                    } else {
                        for (String code : codeToNullCheck.keySet()) {
                            String nullCheck = codeToNullCheck.get(code);
                            if (nullCheck == null || nullCheck.length() == 0) {
                                if (addNullcheckForHole)
                                    temp.put(code + v.getName(), v.getName() + " != null");
                                else
                                    temp.put(code + v.getName(), "");
                            } else {
                                if (addNullcheckForHole) {
                                    if (!nullCheck.contains(v.getName()))
                                        temp.put(code + v.getName(), nullCheck + " && " + v.getName() + " != null");
                                    else
                                        temp.put(code + v.getName(), nullCheck);
                                } else
                                    temp.put(code + v.getName(), nullCheck);
                            }
                        }
                    }
                }
                codeToNullCheck = temp;
            } else {
                String inside = e.getSketchString(false);
                HashMap<String, String> temp = new HashMap<>();
                if (codeToNullCheck.size() == 0) {
                    temp.put(inside, "");
                } else {
                    for (String code : codeToNullCheck.keySet()) {
                        String nullCheck = codeToNullCheck.get(code);
                        temp.put(code + inside, nullCheck);
                    }
                }
                codeToNullCheck = temp;
            }
        }

        List<String> codeWithNull = new ArrayList<>();
        List<String> codeNoNull = new ArrayList<>();
        for (String code : codeToNullCheck.keySet()) {
            String nullCheck = codeToNullCheck.get(code);

            if (alreadyHasTarget) {
                if (targetName != null && targetName.length() != 0) {
                    code = targetName + code;
                }
                code = code + sketch.getSimpleSketchString();
                if (nullCheck != null && nullCheck.length() > 0) {
                    codeNoNull.add(code);
                    if (!nullCheck.contains(Variable.getOldInstance().getName()))
                        codeWithNull.add("if(" + nullCheck + "){\n" + " /* g: [] != null */\n"
                                + code
                                + "}\n");
                } else {
                    codeNoNull.add(code);
                }
            } else if (targetType != null && targetName != null) {
                code = targetName + code + sketch.getSimpleSketchString();
                if (nullCheck != null && nullCheck.length() > 0) {
                    codeNoNull.add(targetType + " " + code);
                    if (!nullCheck.contains(Variable.getOldInstance().getName()))
                        codeWithNull.add(targetType + " " + targetName + "=" + init + ";\n" +
                                "if(" + nullCheck + "){\n" + " /* g: [] != null */\n"
                                + code
                                + "}\n");
                } else {
                    codeNoNull.add(targetType + " " + code);
                }
            } else {
                code = code + sketch.getSimpleSketchString();
                if (nullCheck != null && nullCheck.length() > 0) {
                    codeNoNull.add(code);
                    if (!nullCheck.contains(Variable.getOldInstance().getName()))
                        codeWithNull.add("if(" + nullCheck + "){\n" + " /* g: [] != null */\n"
                                + code
                                + "}\n");
                } else {
                    codeNoNull.add(code);
                }
            }
        }
        return new MutablePair<List<String>, List<String>>(codeNoNull, codeWithNull);
    }

    ////////////////////////the following method is fine
    public boolean newFieldCreated() {
        return variables.needTestNewField() && !this.lastIsLoopFor;
    }

    @Override
    public String toString() {
        return this._forEqualHash;
    }

    public String toCleanString() {
        return getCleanCode(this._forEqualHash);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        Program target = (Program) obj;
        if (this.forEqualHashDataflow.size() != target.forEqualHashDataflow.size())
            return false;
        EqualsBuilder eq = new EqualsBuilder()
                .append(this._forEqualHash, target._forEqualHash);
        for (int i = 0; i < this.forEqualHashDataflow.size(); i++)
            eq.append(this.forEqualHashDataflow.get(i), target.forEqualHashDataflow.get(i));
        return eq.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hash = new HashCodeBuilder(19, 43)
                .append(this._forEqualHash);
        for (Dataflow df : this.forEqualHashDataflow)
            hash.append(df);
        return hash.toHashCode();
    }

    public Collection<ResolvedType> getKnownTypes() {
        return this.variables.getKnownTypes();
    }

    public List<String> toCleanCodeNoNull() {
        List<String> code = new ArrayList<>(this.codeListNoNullCheck);
        if (this.ifIsOpen)
            code.replaceAll(s -> s + "}\n");
        if (this.loopIsOpen)
            code.replaceAll(s -> s + "}\n");
        return code;
    }

    public List<String> toCleanCodeWithNull() {
        List<String> code = new ArrayList<>(this.codeListWithNullChek);
        code.removeAll(this.codeListNoNullCheck);
        if (code == null)
            return code;
        if (this.ifIsOpen)
            code.replaceAll(s -> s + "}\n");
        if (this.loopIsOpen)
            code.replaceAll(s -> s + "}\n");
        return code;
    }

    public int programSize() {
        return this.codeListNoNullCheck.size() + this.codeListWithNullChek.size();
    }
}
