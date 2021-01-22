package dsu.pasta.javaparser.gadget.sketch;

import com.github.javaparser.resolution.types.ResolvedType;
import dsu.pasta.javaparser.factory.stmt.ZApiConstructor;
import dsu.pasta.javaparser.factory.stmt.ZApiMethod;
import dsu.pasta.javaparser.factory.stmt.ZConstructorDeclaration;
import dsu.pasta.javaparser.factory.stmt.ZMethodDeclaration;
import dsu.pasta.javaparser.gadget.PriorityDefinition;
import dsu.pasta.utils.ZFileUtils;

import javax.json.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Sketch extends ZRange {
    public static final int DISTANCE_MAX = 99999;
    protected static final String typeName = "type";
    protected static final String categoryName = "category";
    /**
     * The id of this sketch
     */
    private static final String idName = "id";
    private static final String oriCodeName = "oriCode";
    private static final String outerContextIdName = "outerContextId";
    private static final String rangeName = "range";
    public static Comparator<Sketch> rangeComparator = new Comparator<Sketch>() {
        @Override
        public int compare(Sketch o1, Sketch o2) {
            if (o1.getRange() == null && o2.getRange() == null)
                return 0;
            else if (o1.getRange() == null)
                return -1;
            else if (o2.getRange() == null)
                return 1;

            if (o1.getRange().begin.isBefore(o2.getRange().begin))
                return -1;
            else if (o2.getRange().begin.isBefore(o1.getRange().begin))
                return 1;
            else
                return 0;
        }
    };
    public static Comparator<Sketch> distanceComparator = new Comparator<Sketch>() {
        @Override
        public int compare(Sketch o1, Sketch o2) {
            return o1.distanceToGenerateNewFieldType - o2.distanceToGenerateNewFieldType;
        }
    };
    /**
     * The higher the better
     */
    public static Comparator<Sketch> priorityComparator = new Comparator<Sketch>() {
        @Override
        public int compare(Sketch o1, Sketch o2) {
            if (o2.priority == o1.priority)
                return o1.distanceToGenerateNewFieldType - o2.distanceToGenerateNewFieldType;
            else return o2.priority - o1.priority;
        }
    };
    protected static AtomicInteger globalId = new AtomicInteger(0);
    public boolean isDiffCode = false;
    public boolean useRemovedField = false;
    public boolean hasTargetFieldName = false;
    public boolean useApiConstructor = false;
    public boolean useApiChangeAllMethod = false;

    //    protected RankProperty rank = new RankProperty();
    protected int id = -1;
    protected JsonObject json;
    /**
     * internal use
     */
    protected JsonObjectBuilder jsonObjectBuilder;
    protected String originalCode;
    /**
     * The type of this sketch, e.g. ZFieldAccessExpr
     */
    protected String type;
    protected String sketchString;
    protected String simpleSketchString;
    protected int priority = 0;
    /**
     * Can be null, e.g. for ZApiConstructor, ZCastStmt
     */
    protected Context outerContext;
    /**
     * This field is for if/loop sketch. they are also used as context
     */
    protected Context _thisAsContext;
    protected SketchForeachCondition thisBelongsToForeach = null;
    private int _outerContextid;
    /**
     * Mind int overflow
     */
    private int distanceToGenerateNewFieldType = DISTANCE_MAX;

    protected Sketch() {
        id = globalId.incrementAndGet();
    }

    /**
     * This constructor is for reading json file and creating sketch
     *
     * @param json
     */
    protected Sketch(JsonObject json) {
        //not create the id here.
        this.json = json;
        if (json.get(idName) != null && !json.get(idName).equals(JsonValue.NULL))
            this.id = json.getInt(idName);
        if (json.get(typeName) != null && !json.get(typeName).equals(JsonValue.NULL))
            this.type = json.getString(typeName);
        if (json.get(oriCodeName) != null && !json.get(oriCodeName).equals(JsonValue.NULL))
            this.originalCode = json.getString(oriCodeName);
        if (json.get(rangeName) != null && !json.get(rangeName).equals(JsonValue.NULL)) {
            this.setRange(json.getString(rangeName));
        }
        if (json.get(outerContextIdName) != null && !json.get(outerContextIdName).equals(JsonValue.NULL))
            this._outerContextid = json.getInt(outerContextIdName);
    }

    public static Sketch create(JsonObject json) {
        if (SketchConstant.is(json))
            return SketchConstant.create(json);
        if (SketchForCondition.is(json))
            return SketchForCondition.create(json);
        if (SketchForeachCondition.is(json))
            return SketchForeachCondition.create(json);
        if (SketchGadget.is(json)) {
            return SketchGadget.create(json);
        }
        if (SketchIfCondition.is(json))
            return SketchIfCondition.create(json);
        if (SketchWhileCondition.is(json))
            return SketchWhileCondition.create(json);
        return null;
    }

    public static void readFile(String file, HashMap<Integer, Sketch> idSketch, HashMap<Integer, Context> idContext) {
        if (!ZFileUtils.fileExistNotEmpty(file) || idSketch == null || idContext == null)
            return;
        JsonReader jsonReader = null;
        try {
            jsonReader = Json.createReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        JsonArray array = jsonReader.readArray();
        jsonReader.close();
        for (int i = 0; i < array.size(); i++) {
            JsonObject json = array.getJsonObject(i);
            Sketch sketch = create(json);
            if (sketch != null)
                idSketch.put(sketch.id, sketch);
            else {
                Context context = Context.create(json);
                if (context != null)
                    idContext.put(context.getId(), context);
            }
        }
        for (Sketch s : idSketch.values()) {
            if (s instanceof SketchGadget) {
                ((SketchGadget) s).resolveMust(idSketch);
                ((SketchGadget) s).resolveClones(idSketch);
            }
            if (s instanceof SketchIfCondition) {
                ((SketchIfCondition) s).resolveOriThenElse(idSketch);
            }
            if (s instanceof SketchForeachCondition) {
                ((SketchForeachCondition) s).resolveOutInner(idSketch);
            }
            s.resolveOuterContext(idContext);
        }
        for (Context c : idContext.values()) {
            c.resolveOuterContextSpecialCondition(idContext, idSketch);
        }

        return;
    }

    public void resolveOuterContext(HashMap<Integer, Context> idContext) {
        Context context = idContext.get(this._outerContextid);
        if (context != null) {
            this.setOuterContext(context);
        }
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    protected void createJsonObjectBuilder() {
        JsonValue idValue = Json.createValue(id);
        JsonValue typeValue = type == null ? JsonValue.NULL : Json.createValue(type);
        JsonValue oriValue = (originalCode == null) ? JsonValue.NULL : Json.createValue(originalCode);
        JsonValue contextValue = outerContext == null ? JsonValue.NULL : Json.createValue(outerContext.getId());
        JsonValue rangeValue = range == null
                ? JsonValue.NULL
                : Json.createValue(this.getRangeAsString());

        jsonObjectBuilder = Json.createObjectBuilder()
                .add(idName, idValue)
                .add(typeName, typeValue)
                .add(oriCodeName, oriValue)
                .add(outerContextIdName, contextValue)
                .add(rangeName, rangeValue);
    }

    public boolean isConstructor() {
        return this.type != null &&
                (this.type.equals(ZConstructorDeclaration.class.getSimpleName()) || this.type.equals(ZApiConstructor.class.getSimpleName()));
    }

    public boolean isChangeAllMethod() {
        return this.type != null &&
                (this.type.equals(ZMethodDeclaration.class.getSimpleName()) || this.type.equals(ZApiMethod.class.getSimpleName())) &&
                (this.getSketchString().contains("putAll") || this.getSketchString().contains("addAll"));
    }

    public String getOriginalCode() {
        return originalCode;
    }

    public void setOriginalCode(String oriCode) {
        this.originalCode = oriCode;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void incrementPriority(PriorityDefinition priorityDefinition) {
        this.priority += priorityDefinition.getPriority();
    }

    public int getPriority() {
        return this.priority;
    }

    public void incrementPriority(int priority) {
        this.priority += priority;
    }

    public abstract void decrementPriorityByHoles();

    @Override
    public String toString() {
        return this.id + " " + this.originalCode + getRangeAsString();
    }

    protected Context getContextFromClone(HashSet<Sketch> visited) {
        if (visited.contains(this))
            return this.outerContext;
        visited.add(this);
        if (this.outerContext != null)
            return this.outerContext;
        if (this instanceof SketchConstant)
            return this.outerContext;
        for (SketchGadget sg : ((SketchGadget) this).getClones()) {
            Context ct = sg.getContextFromClone(visited);
            if (ct != null) {
                this.outerContext = ct;
                return ct;
            }
        }
        return this.outerContext;
    }

    public Context getOuterContext() {
        if (this.outerContext == null) {
            HashSet<Sketch> vs = new HashSet<>();
            this.outerContext = getContextFromClone(vs);
        }
        return this.outerContext;
    }

    public void setOuterContext(Context outerContext) {
        if (outerContext == null)
            return;
        this.outerContext = outerContext;
        this.outerContext.addInsideSketch(this);
    }

    public Context get_thisAsContext() {
        return _thisAsContext;
    }

    /**
     * This method is only for if/loop sketches. They are also used as context.
     * <tt>_thisAsContext</tt> is only used when parsing source code.
     *
     * @param _thisAsContext
     */
    public void set_thisAsContext(Context _thisAsContext) {
        this._thisAsContext = _thisAsContext;
    }

    /**
     * Is this gadget after another one?
     * Valid for both source factory gadgets.
     *
     * @param another
     * @return
     */
    public boolean isAfter(Sketch another) {
        if (another == null)
            return false;
        return this.isAfter(another.range);
    }

    /**
     * Is this gadget contains another one?
     * Valid for source factory gadget.
     *
     * @param sk
     * @return
     */
    public boolean contains(Sketch sk) {
        if (sk == null)
            return false;
        return this.contains(sk.range);
    }

    public int getDistanceToGenerateNewFieldType() {
        return distanceToGenerateNewFieldType;
    }

    public boolean setDistanceToGenerateNewFieldType(int distanceToGenerateNewFieldType) {
        if (this.distanceToGenerateNewFieldType != DISTANCE_MAX)
            return false;
        if (distanceToGenerateNewFieldType < this.distanceToGenerateNewFieldType) {
            this.distanceToGenerateNewFieldType = distanceToGenerateNewFieldType;
            if (this instanceof SketchGadget) {
                for (SketchGadget sg : ((SketchGadget) this).getClones())
                    sg.setDistanceToGenerateNewFieldType(distanceToGenerateNewFieldType);
            }
            return true;
        }
        return false;
    }

    public abstract boolean isIfStmt();

    public abstract boolean isForStmt();

    public abstract boolean isForeachStmt();

    public abstract boolean isWhileStmt();

    public boolean isLoop() {
        return this.isForeachStmt() || this.isForStmt() || this.isWhileStmt();
    }

    public abstract LinkedList<ResolvedType> getSourceHoleTypes();

    public abstract LinkedList<ResolvedType> getTargetHoleTypes();

    public abstract JsonObject getJson();

    public abstract boolean hasConstantHole();

    public abstract String getSketchString();

    /**
     * @return \/**[] = [].m2*\/
     */
    public abstract String getSimpleSketchString();

    public String getSketchStringWithRankScore() {
        return this.originalCode + "\n" + this.getSketchString()
                + "\n[priority]" + this.priority;
    }

    /**
     * <p>
     * Add <tt>sketch</tt> as must have.
     * </p>
     * <p>
     * Add <tt>sketch</tt>'s must have to this must have.
     * </p>
     *
     * @param sketch
     */
    public abstract void addAllMustHave(Sketch sketch);

    /**
     * <p>
     * Only add <tt>sketch</tt> as must have.
     * </p>
     *
     * @param sketch
     */
    public abstract void addMustHave(Sketch sketch);

    public abstract LinkedHashSet<Sketch> getMustHave();

    public abstract List<SketchGadget> getMustHaveMustHave();

    public abstract List<SketchGadget> getDataflowByHoles(List<SketchGadget> allSketch);

    public abstract Set<SketchGadget> getPossibleLeadToGenOrUse();

    /**
     * Is this gadget can be used directly?
     * <p>only-assign-null: Type foo = null; </p>
     * <p>variable-declarator-without-initializer: int a; </p>
     * <p>is-added-field: gadget uses a new field access expr, will be always skipped</p>
     *
     * @param ignoreSpecial if <tt>true</tt>, only-assign-null, variable-declarator-without-initializer gadgets are not usable; if <tt>false</tt> only assign null gadgets are usable.
     * @return
     */
    public abstract boolean usable(boolean ignoreSpecial);

    /**
     * Is this gadget can be used directly?
     * <p>only-assign-null: Type foo = null; </p>
     * <p>variable-declarator-without-initializer: int a; </p>
     * <p>is-added-field: gadget uses a new field access expr, will be always skipped</p>
     *
     * @param ignoreSpecial if <tt>true</tt>, only-assign-null, variable-declarator-without-initializer gadgets are not usable; if <tt>false</tt> only assign null gadgets are usable.
     * @return
     */
    public abstract boolean usable(Collection<ResolvedType> variableTypes, boolean ignoreSpecial);

    /**
     * Is this gadget can be used, given the one type <tt>variableType</tt>
     *
     * @param variableType
     * @return
     */
    public boolean usable(ResolvedType variableType) {
        return this.usable(new ArrayList<ResolvedType>(Arrays.asList(variableType)), false);
    }

    /**
     * Can this gadget generate a variable of <tt>type</tt>?
     *
     * @param type
     * @return
     */
    public abstract boolean canGenerate(ResolvedType type, boolean skipSpecial);

    /**
     * The type of the variable this gadget can generate.
     *
     * @return, type or null.
     */
    public abstract ResolvedType generate();

    /**
     * Does this gadget need this <tt>type</tt>?
     *
     * @param type
     * @return
     */
    public abstract boolean needTheType(ResolvedType type);

    /**
     * Does this gadget need those <tt>types</tt>?
     *
     * @param types
     * @return
     */
    public abstract boolean needTheTypes(Collection<ResolvedType> types);

    public abstract HashSet<String> getAllTypesAsString();

    public abstract boolean isOnlyAssignNull();

}
