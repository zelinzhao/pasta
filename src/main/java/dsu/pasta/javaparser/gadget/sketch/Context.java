package dsu.pasta.javaparser.gadget.sketch;

import com.github.javaparser.Range;
import com.github.javaparser.resolution.types.ResolvedType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Context extends ZRange {
    protected static final String categoryName = "category";
    protected static final String idName = "id";
    protected static final String inClassName = "inClass";
    protected static final String inMethodName = "inMethod";
    protected static final String specCondIdname = "specialConditionId";
    protected static final String rangeName = "range";
    protected static final String outerContextIdName = "outerContextId";
    private static final String contextName = "context";
    private static final int distance_to_build_replace = 5;
    private static AtomicInteger globalId = new AtomicInteger(0);

    private int id = -1;
    private String declaringClass;
    private String declaringMethod;

    /**
     * While reading json to creat sketch and contexts, the <tt>setOuterSketch</tt> method in Sketch/Context will be called.
     * Inside that method, the corresponding sketch/context will be added to
     * <tt>insideSketches</tt> <tt>insideContexts</tt>.
     */
    private LinkedList<Sketch> insideSketches = new LinkedList<>();
    private LinkedList<Context> insideContexts = new LinkedList<>();

    /**
     * All sketches, including inner context's sketches.
     * Will not write to file. Only used in synthesis.
     */
    private LinkedList<Sketch> allInsideSketches = new LinkedList<>();

    /**
     * For, foreach, if, while blocks. We only save the condition here, not the body
     */
    private int _specialConditionId;
    private SketchGadget specialCondition;
    private int _outerContextid;
    private Context outerContext;
    /**
     * From index 0-->n, most outer context --> this;
     * The most outer context should have <tt>declaringClass</tt> and <tt>declaringMethod</tt> names.
     */
    private LinkedList<Context> contextChain;
    private JsonObject json;

    private List<Dataflow> dataflowsToTarget = null;

    public Context() {
        this.id = globalId.incrementAndGet();
    }

    public Context(String declaringClass, Range range) {
        this(declaringClass, null, range);
    }

    public Context(String declaringClass, String declaringMethod, Range range) {
        this();
        this.declaringClass = declaringClass;
        this.declaringMethod = declaringMethod;
        this.range = range;
    }

    public Context(SketchGadget specialCondition, Range range) {
        this();
        this.specialCondition = specialCondition;
        this.range = range;
    }

    public Context(JsonObject json) {
        //not create the id here.
        this.json = json;
        if (!json.get(idName).equals(JsonValue.NULL))
            this.id = json.getInt(idName);
        if (!json.get(inClassName).equals(JsonValue.NULL))
            this.declaringClass = json.getString(inClassName);
        if (!json.get(inMethodName).equals(JsonValue.NULL))
            this.declaringMethod = json.getString(inMethodName);
        if (!json.get(specCondIdname).equals(JsonValue.NULL))
            this._specialConditionId = json.getInt(specCondIdname);
        if (!json.get(rangeName).equals(JsonValue.NULL)) {
            this.setRange(json.getString(rangeName));
        }
        if (!json.get(outerContextIdName).equals(JsonValue.NULL))
            this._outerContextid = json.getInt(outerContextIdName);
    }

    /**
     * All sketches' rank properties should be calculated before this method being called.
     */
//    public void calculateRankProperty() {
//        if (this.isMethodContext())
//            return;
//        if (this.rank != null)
//            return;
//        int mergeCount = 0;
//        this.rank = new RankProperty();
//        if (this.specialCondition != null) {
//            this.rank.mergeRank(this.specialCondition.getRank());
//            mergeCount++;
//        }
//        for (Sketch sk : insideSketches) {
//            this.rank.mergeRank(sk.getRank());
//            mergeCount++;
//        }
//
//        for (Context ct : insideContexts) {
//            if (ct.rank == null) {
//                ct.calculateRankProperty();
//            }
//            this.rank.mergeRank(ct.rank);
//            mergeCount++;
//        }
//        if (mergeCount != 0)
//            this.rank.setPriority(this.rank.getPriority() / mergeCount);
//    }
    private static LinkedList<SketchGadget> getGadgetInScope(LinkedList<SketchGadget> flow) {
        LinkedList<SketchGadget> result = new LinkedList<>();
        for (SketchGadget sg : flow) {
            if (sg.getDistanceToGenerateNewFieldType() < distance_to_build_replace)
                result.add(sg);
        }
        return result;
    }

    public static List<Dataflow> getDataflowsForType(List<Sketch> allSketch, ResolvedType target) {
        List<Dataflow> dataflows = new ArrayList<>();
        LinkedList<Sketch> temp = new LinkedList<>(allSketch);

        boolean hasTarget = true;
        while (temp.size() > 0 && hasTarget) {
            hasTarget = false;
            Dataflow df = new Dataflow();
            HashSet<Sketch> mustHave = new HashSet<>();
            HashSet<Hole> holes = new HashSet<>();
            Iterator<Sketch> it = temp.descendingIterator();
            while (it.hasNext()) {
                Sketch sk = it.next();
                if (!(sk instanceof SketchGadget))
                    continue;
                SketchGadget gs = (SketchGadget) sk;

                boolean add = false;
                //Find dataflow that generate or uses <tt>target</tt> type.
                if (gs.canGenerate(target, true) || gs.needTheType(target)) {
                    hasTarget = true;
                    if (df.size() == 0) {
                        add = true;
                    }
                }
                if (mustHave.size() > 0) {
                    for (Sketch interset : gs.getMustHave()) {
                        if (mustHave.contains(interset)) {
                            add = true;
                            break;
                        }
                    }
                }
                if (holes.size() > 0) {
                    for (Hole h : gs.getAllHoles()) {
                        if (holes.contains(h)) {
                            add = true;
                            break;
                        }
                    }
                }
                if (add) {
                    if (gs instanceof SketchForeachCondition) {
                        SketchForeachCondition fe = (SketchForeachCondition) gs;
                        df.addGadgetToHead(fe.getOutterSelfInnerReverseOrder());
                    } else {
                        if (gs.thisBelongsToForeach != null)
                            df.addGadgetToHead(gs.thisBelongsToForeach.getOutterSelfInnerOrder());
                        else
                            df.addGadgetToHead(gs);
                    }
                    mustHave.addAll(gs.getMustHave());
                    for (Hole h : gs.getAllHoles()) {
                        if (!h.isConstantHole())
                            holes.add(h);
                    }
                    it.remove();
                }
            }
            if (df.size() > 0 && !df.onlyHasControlFlow()) {
                List<Dataflow> narrows = df.resolveDistanceGetNarrowFlow(target);
                dataflows.add(df);
                dataflows.addAll(narrows);
            }
        }
        return dataflows;
    }

    private static List<LinkedList<Sketch>> mixAdd(List<LinkedList<Sketch>> old, Sketch tail) {
        for (LinkedList<Sketch> r : old)
            r.add(tail);
        return old;
    }

    private static List<LinkedList<Sketch>> mixAdd(List<LinkedList<Sketch>> old, List<SketchGadget> tails) {
        List<LinkedList<Sketch>> newlist = new ArrayList<>();
        for (LinkedList<Sketch> o : old) {
            for (Sketch s : tails) {
                LinkedList<Sketch> n = new LinkedList<>(o);
                n.add(s);
                newlist.add(n);
            }
        }
        return newlist;
    }

    public static boolean is(JsonObject json) {
        return json.getString(categoryName).equals(contextName);
    }

    public static Context create(JsonObject json) {
        if (is(json)) {
            return new Context(json);
        }
        return null;
    }

    /**
     * We resolve distance to generate target type inside a method context.
     * <p>A gadget, including control-allStatements gadget, is one step</p>
     *
     * @param target
     */
    public void resolveDistanceToTypeForMethodCtx(ResolvedType target) {
        if (!this.isMethodContext())
            return;
        getAllInsideSketch();
        resolveDataFlow(target);
    }

    /**
     * Find dataflow that generate or uses <tt>target</tt> type.
     *
     * @param target
     */
    public void resolveDataFlow(ResolvedType target) {
        if (dataflowsToTarget != null)
            return;
        dataflowsToTarget = new ArrayList<>();
        dataflowsToTarget.addAll(getDataflowsForType(getAllInsideSketch(), target));

        List<Dataflow> newDataflows = new ArrayList<>();
        for (Dataflow df : dataflowsToTarget) {
            if (df.isCanGenerateTargetInstance()) {
                for (List<Sketch> flow : getAllReplaceListsFor(getGadgetInScope(df.flow))) {
                    newDataflows.addAll(getDataflowsForType(flow, target));
                }
            }
        }
        this.dataflowsToTarget.addAll(newDataflows);
//        if(dataflowsToTarget.size()==0) {
//            for (List<Sketch> flow : getAllReplaceListsFor(helper(getAllInsideSketch()))){
//                newDataflows.addAll(getDataflowsForType(flow, target));
//            }
//        }
        for (Dataflow df : this.dataflowsToTarget) {
            df.setInMethodContext(this);
        }
    }

    public List<Dataflow> getDataflowsToTarget() {
        return dataflowsToTarget;
    }

    /**
     * Get all sketches, including control-allStatements sketches, inside a context.
     *
     * @return
     */
    public LinkedList<Sketch> getAllInsideSketch() {
        if (this.allInsideSketches.size() > 0)
            return this.allInsideSketches;

        if (this.insideContexts.size() == 0) {
            this.allInsideSketches.addAll(this.insideSketches);
            return this.allInsideSketches;
        }
        HashSet<Sketch> temp = new HashSet<>();

        temp.addAll(this.insideSketches);

        for (Context ct : this.insideContexts) {
//            if(ct.getSpecialCondition() !=null )
//                this.allInsideSketches.add(ct.getSpecialCondition());
            //insideSketches already contains special condition
            if (ct.isForeachContext()) {
                SketchForeachCondition fe = (SketchForeachCondition) ct.getSpecialCondition();
                temp.addAll(fe.getOutterSelfInnerOrder());
            }
            temp.addAll(ct.getAllInsideSketch());
        }
        this.allInsideSketches.addAll(temp);
        this.allInsideSketches.sort(Sketch.rangeComparator);
        return this.allInsideSketches;
    }

    public List<LinkedList<Sketch>> getAllReplaceListsFor(List<SketchGadget> flow) {
        List<LinkedList<Sketch>> allReplaceList = new ArrayList<>();
        LinkedList<Sketch> temp = new LinkedList<>();
        allReplaceList.add(temp);

        for (SketchGadget sk : flow) {
            if (sk.getClones().size() > 0)
                allReplaceList = mixAdd(allReplaceList, sk.getClones());
            else
                allReplaceList = mixAdd(allReplaceList, sk);
            if (allReplaceList.size() > 10)
                break;
        }
        return allReplaceList;
    }

    public void addInsideSketch(Sketch sketch) {
        this.insideSketches.add(sketch);
        this.insideSketches.sort(Sketch.rangeComparator);
    }

    public void addInsideContext(Context context) {
        this.insideContexts.add(context);
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
        Context temp = (Context) obj;
        return new EqualsBuilder()
                .append(this.declaringClass, temp.declaringClass)
                .append(this.declaringMethod, temp.declaringMethod)
                .append(this.specialCondition, temp.specialCondition)
                .append(this.outerContext, temp.outerContext)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(11, 31)
                .append(declaringClass)
                .append(declaringMethod)
                .append(specialCondition)
                .append(outerContext)
                .toHashCode();
    }

    public void resolveOuterContextSpecialCondition(HashMap<Integer, Context> idContext, HashMap<Integer, Sketch> idSketch) {
        Context context = idContext.get(this._outerContextid);
        if (context != null) {
            this.setOuterContext(context);
        }
        Sketch gadget = idSketch.get(this._specialConditionId);
        if (gadget == null || !(gadget instanceof SketchGadget))
            return;
        this.specialCondition = (SketchGadget) gadget;
        //TODO test this
        gadget.set_thisAsContext(this);
    }

    public int getId() {
        return this.id;
    }

    public JsonObject getJson() {
        if (json != null)
            return json;
        JsonValue categoryValue = Json.createValue(contextName);
        JsonValue idValue = Json.createValue(id);
        JsonValue classValue = this.declaringClass == null ? JsonValue.NULL : Json.createValue(declaringClass);
        JsonValue methodValue = this.declaringMethod == null ? JsonValue.NULL : Json.createValue(declaringMethod);
        JsonValue conditionValue = this.specialCondition == null
                ? JsonValue.NULL
                : Json.createValue(specialCondition.getId());
        JsonValue rangeValue = this.range == null
                ? JsonValue.NULL
                : Json.createValue(this.getRangeAsString());
        JsonValue contextValue = this.outerContext == null ? JsonValue.NULL : Json.createValue(outerContext.id);
        json = Json.createObjectBuilder()
                .add(categoryName, categoryValue)
                .add(idName, idValue)
                .add(inClassName, classValue)
                .add(inMethodName, methodValue)
                .add(specCondIdname, conditionValue)
                .add(rangeName, rangeValue)
                .add(outerContextIdName, contextValue).build();
        return json;
    }

    /**
     * Is this context a method context, i.e. it has <tt>declaringClass</tt> and <tt>declaringMethod</tt> values.
     *
     * @return
     */
    public boolean isMethodContext() {
        return this.declaringClass != null && this.declaringMethod != null
                && this.specialCondition == null;
    }

    public boolean isForeachContext() {
        return this.specialCondition != null && this.specialCondition.isForeachStmt();
    }

    public LinkedList<Context> getContextChain() {
        if (contextChain != null)
            return contextChain;
        contextChain = new LinkedList<>();
        Context outer = this;
        while (outer != null) {
            contextChain.addFirst(outer);
            outer = outer.outerContext;
        }
        return contextChain;
    }

    public SketchGadget getSpecialCondition() {
        return this.specialCondition;
    }

    public void setOuterContext(Context outerContext) {
        this.outerContext = outerContext;
        this.outerContext.addInsideContext(this);
    }
}
