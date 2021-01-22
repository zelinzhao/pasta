package dsu.pasta.javaparser.gadget.sketch;

import com.github.javaparser.resolution.types.ResolvedType;
import dsu.pasta.config.UpdateConfig;
import dsu.pasta.dpg.ExtractProjectUpdatedInfoProcessor;
import dsu.pasta.javaparser.factory.analyzer.JavaparserSolver;
import dsu.pasta.javaparser.factory.stmt.ZReturnStmt;
import dsu.pasta.javaparser.gadget.ZCode;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.json.*;
import java.util.*;
import java.util.stream.Collectors;

public class SketchGadget extends Sketch {
    private static final String gadgetCategoryName = "gadget";
    private static final String holeNumName = "holes";
    private static final String elementsName = "elements";
    private static final String mustHaveName = "must";
    private static final String clonesName = "clones";
    /**
     * gadgets with typed placeholders
     */
    protected LinkedList<Element> elements = new LinkedList<Element>();
    /**
     * <tt>S</tt> statement must have <tt>P</tt>, when <tt>P</tt> declares a local variable that is used inside <tt>S</tt>.
     * This also means that <tt>P</tt>'s range is before <tt>S</tt>'range.
     * <p>
     * Notice: say a statement is <tt>int a = 10;</tt>.
     * </p>
     * <p>
     * The <tt>10</tt> is a constant, but it will not be added to the <tt>mustHave</tt> of <tt>int a = 10;</tt>,
     * because <tt>10</tt>'s range is inside <tt>int a = 10;</tt>.
     * </p>
     * However, the sketch for the statement is <tt>int @int@ = @int@;</tt> and the second <tt>@int@</tt>
     * will record the "original code (original value)", that is <tt>10</tt>.
     * <p>
     * Another thing that matters:
     * one must have sketch may have a replace, which is not placed in the mustHave field.
     * While we resolving mustHave, we should also use their replace.
     * </p>
     */
    protected LinkedHashSet<Sketch> mustHave = new LinkedHashSet<>();
    /**
     * number of source placeholders
     */
    protected Integer holeNumber = null;
    /**
     * If this gadget is from new version and uses an added field,
     * we mark <tt>isAddedFieldGadget</tt> to <tt>true</tt>
     */
    protected boolean isAddedFieldGadget = false;
    /**
     * If a variable declaration has an initializer, e.g. int a = 0;,
     * this statement can be extracted a useful gadget.
     * If a variable declaration has no initializer, e.g. int a;,
     * this statement is actually not valid gadget.
     */
    protected Boolean isVariableDeclaratorNoInitializer = null;
    private LinkedList<ResolvedType> sourceHoleTypes = null;
    private LinkedList<ResolvedType> targetHoleTypes = null;
    private Boolean isAssignNull = null;
    /**
     * This field is only used for reading gadgets from file
     */
    private LinkedHashSet<Integer> _mustHaveIds = new LinkedHashSet<>();
    private List<SketchGadget> mustHaveMustHave = null;
    private List<SketchGadget> mustHaveByHoles = null;
    /**
     * small dataflows for sketch that may lead to generate/use new field
     */
    private HashSet<SketchGadget> possibleLeadToGenOrUse = new HashSet<>();
    /**
     * This field is for field access expression.
     * We should extract two sketch for field access:
     * 1, @type@.fieldName
     * 2, @fieldType@
     */
    private SketchGadget replace = null;
    private ArrayList<SketchGadget> clones = new ArrayList<>();
    private ArrayList<Integer> _clonesId = new ArrayList<>();
    private HashSet<Hole> allHoles = null;
    private HashSet<Dataflow> outerDataflows = new HashSet<>();
    /**
     * Small dataflow for sketch that generate/use new field
     */
    private Dataflow narrowFlow = null;

    public SketchGadget() {
        super();
    }

    /**
     * This constructor is for reading json file and creating sketch
     *
     * @param json
     */
    public SketchGadget(JsonObject json) {
        super(json);
        if (json.get(holeNumName) != null && !json.get(holeNumName).equals(JsonValue.NULL))
            this.holeNumber = json.getInt(holeNumName);
        if (json.get(elementsName) != null && !json.get(elementsName).equals(JsonValue.NULL)) {
            JsonArray array = json.getJsonArray(elementsName);
            //build element array
            this.elements = new LinkedList<>();
            for (int i = 0; i < array.size(); i++) {
                JsonObject j = array.getJsonObject(i);
                Element e = Element.create(j);
                Hole h = Hole.create(j);
                if (e != null)
                    this.elements.add(e);
                else if (h != null)
                    this.elements.add(h);
            }
        }
        if (json.get(mustHaveName) != null && !json.get(mustHaveName).equals(JsonValue.NULL)) {
            JsonArray array = json.getJsonArray(mustHaveName);
            for (int i = 0; i < array.size(); i++) {
                JsonNumber j = array.getJsonNumber(i);
                int jid = j.intValue();
                this._mustHaveIds.add(jid);
//                this.mustHave.put(jid,new SketchGadget());
            }
        }
        if (json.get(clonesName) != null && !json.get(clonesName).equals(JsonValue.NULL)) {
            JsonArray array = json.getJsonArray((clonesName));
            for (int i = 0; i < array.size(); i++) {
                JsonNumber j = array.getJsonNumber(i);
                int jid = j.intValue();
                this._clonesId.add(jid);
            }
        }
    }

    public static boolean is(JsonObject json) {
        return json.getString(categoryName) != null && json.getString(categoryName).equals(gadgetCategoryName);
    }

    public static SketchGadget create(JsonObject json) {
        if (is(json)) {
            return new SketchGadget(json);
        }
        return null;
    }

    public ArrayList<SketchGadget> getClones() {
        return clones;
    }

    public HashSet<Dataflow> getOuterDataflow() {
        return outerDataflows;
    }

    public void addOuterDataflow(Dataflow outerDataflow) {
        if (outerDataflow != null && !outerDataflow.onlyHasControlFlow())
            this.outerDataflows.add(outerDataflow);
    }

    /**
     * If this gadget is from new version and uses an Added field,
     * we set <tt>isAddedFieldGadget</tt> to true
     *
     * @param className
     */
    public void resolveAddedFieldAccess(String className) {
        ResolvedType type = ExtractProjectUpdatedInfoProcessor.getChangedClassType(className);
        if (type == null)
            return;
        if (ExtractProjectUpdatedInfoProcessor.getChangedFieldTypesOfChangedClass(className).size() == 0)
            return;
        if (!this.needTheType(type))
            return;

        ListIterator<Element> it = this.elements.listIterator();
        while (it.hasNext()) {
            Element e = it.next();
            if (!(e instanceof Hole) || !JavaparserSolver.descEqual(type, e.getType())) {
                continue;
            }
            //find a hole uses type;
            //next should be dot .
            //next.next should has a field name
            Element dot = it.hasNext() ? it.next() : null;
            if (dot == null) // end of list
                return;
            if (dot instanceof Hole)
                it.previous(); // go back one step
            if (dot.getOriginalString() == null || !dot.getOriginalString().equals("."))
                continue;
            Element field = it.hasNext() ? it.next() : null;
            if (field == null)
                return; // end of list
            if (field instanceof Hole)
                it.previous();
            if (field.getOriginalString() == null ||
                    !ExtractProjectUpdatedInfoProcessor.isAddedFieldInChangedClass(className, field.getOriginalString()))
                continue;
            this.isAddedFieldGadget = true;
            break;
        }
    }

    public boolean isAddedFieldGadget() {
        return this.isAddedFieldGadget;
    }

    /**
     * this.ka = xxx;<p>
     * [1].ka = [2]<p>
     * [1] is the target hole, but originally a field access
     *
     * @return
     */
    public boolean isFieldAccessThisAsTarget() {
        Hole h = this.getTargetHole();
        if (h == null)
            return false;
        if (h.type != null && h.type != null && h.originalString != null && h.originalString.equals("this"))
            return true;
        return false;
    }

    public boolean isVariableDeclaratorNoInitializer() {
        if (isVariableDeclaratorNoInitializer != null)
            return isVariableDeclaratorNoInitializer;

        int size = this.elements.size();
        if (size < 4)
            return false;

        if (this.elements.get(size - 1).toString().contains(";")
                && this.elements.get(size - 2) instanceof Hole
                && this.elements.get(size - 3).toString().matches("\\s+")
                && !(this.elements.get(size - 4) instanceof Hole)
                && !this.getSketchString().contains("=")
                && this.getSourceHoleTypes().size() == 0
                && this.getTargetHoleTypes().size() == 1)
            isVariableDeclaratorNoInitializer = true;
        else
            isVariableDeclaratorNoInitializer = false;

        return isVariableDeclaratorNoInitializer;
    }

    @Override
    public HashSet<String> getAllTypesAsString() {
        HashSet<String> result = new HashSet<>();
        result.addAll(this.elements.stream().map(e -> e.getTypeAsString()).collect(Collectors.toSet()));
        return result;
    }

    @Override
    protected void createJsonObjectBuilder() {
        super.createJsonObjectBuilder();
        JsonValue categoryValue = Json.createValue(gadgetCategoryName);
        JsonValue holeValue = Json.createValue(getHoleNumber());
        JsonArrayBuilder arrayBuild = Json.createArrayBuilder();
        for (Element e : elements)
            arrayBuild.add(e.getJson());
        JsonArray elementValue = arrayBuild.build();

        arrayBuild = Json.createArrayBuilder();
        for (Sketch sk : this.mustHave) {
            arrayBuild.add(sk.id);
        }
        JsonArray mustHave = arrayBuild.build();

        arrayBuild = Json.createArrayBuilder();
        for (SketchGadget sk : this.clones) {
            arrayBuild.add(sk.id);
        }
        JsonArray clonesArray = arrayBuild.build();

        jsonObjectBuilder.add(categoryName, categoryValue)
                .add(holeNumName, holeValue)
                .add(clonesName, clonesArray)
                .add(mustHaveName, mustHave)
                .add(elementsName, elementValue);
    }

    @Override
    public boolean isOnlyAssignNull() {
        if (isAssignNull != null)
            return isAssignNull;
        if (getSourceHoleTypes().size() > 0)
            isAssignNull = false;
        if (this.getSketchString().matches("[^=]*=\\s*null\\s*;?\\s*"))
            isAssignNull = true;
        else
            isAssignNull = false;
        return isAssignNull;
    }

    private boolean isReturnStmt() {
        return this.type != null && this.type.equals(ZReturnStmt.class.getSimpleName());
    }

    @Override
    public JsonObject getJson() {
        if (json != null)
            return json;
        createJsonObjectBuilder();
        json = jsonObjectBuilder.build();
        return json;
    }

    @Override
    public boolean hasConstantHole() {
        for (Element e : this.elements) {
            if (e instanceof Hole) {
                Hole h = (Hole) e;
                if (h.getHoleFrom() == Hole.HoleFrom.constant)
                    return true;
            }
        }
        return false;
    }

    @Override
    public void setOriginalCode(String oriCode) {
        super.setOriginalCode(oriCode);
        if (this.hasReplace())
            this.replace.originalCode = oriCode;
    }

    @Override
    public String getSketchString() {
        if (sketchString != null)
            return sketchString;
        resolveHoleNumber();

        StringBuilder holeString = new StringBuilder();
        for (Element e : elements) {
            holeString.append(e.toString());
        }
        sketchString = holeString.toString();
        return sketchString;
    }

    @Override
    public String getSimpleSketchString() {
        if (simpleSketchString != null)
            return simpleSketchString;
        resolveHoleNumber();
        StringBuilder holeString = new StringBuilder();
        for (Element e : elements) {
            holeString.append(e.getSimpleSketchString());
        }
        simpleSketchString = holeString.toString();
        simpleSketchString = simpleSketchString.replaceAll("\\n", "");
        simpleSketchString = "/* g: " + simpleSketchString + " */\n";
        return simpleSketchString;
    }

    @Override
    public void decrementPriorityByHoles() {
        this.priority -= this.getHoleNumber();
    }

    /**
     * This method is for resolving must/may sketches, after creating sketches by reading file.
     *
     * @param allSketch
     */
    public void resolveMust(HashMap<Integer, Sketch> allSketch) {
        for (int i : this._mustHaveIds) {
            Sketch s = allSketch.get(i);
            if (s != null) {
                this.mustHave.add(s);
            }
        }
    }

    public void resolveClones(HashMap<Integer, Sketch> allSketch) {
        for (int i : this._clonesId) {
            Sketch s = allSketch.get(i);
            if (s != null && (s instanceof SketchGadget))
                this.clones.add((SketchGadget) s);
        }
    }

    public SketchGadget getReplace() {
        return replace;
    }

    @Override
    public void addMustHave(Sketch sketch) {
        if (this.isAfter(sketch) && !this.equals(sketch))
            this.mustHave.add(sketch);
    }

    @Override
    public void addAllMustHave(Sketch sketch) {
        for (Sketch m : sketch.getMustHave()) {
            this.addMustHave(m);
        }
        this.addMustHave(sketch);
        if (this.hasReplace()) {
            for (Sketch m : sketch.getMustHave()) {
                this.replace.addMustHave(m);
            }
            this.replace.addMustHave(sketch);
        }
    }

    @Override
    public LinkedHashSet<Sketch> getMustHave() {
        return this.mustHave;
    }

    /**
     * Only called for sketches that generate/use new field instance.
     *
     * @param allSketch
     * @return
     */
    public Dataflow getNarrowFlowSetDistance(List<SketchGadget> allSketch) {
        if (narrowFlow != null)
            return narrowFlow;
        List<SketchGadget> narrowList = new ArrayList<>();
        HashSet<SketchGadget> temp = new HashSet<>();
        temp.add(this);
        temp.addAll(this.getMustHaveMustHave());
        temp.addAll(this.getDataflowByHoles(allSketch));

        narrowList.addAll(temp);
        Collections.sort(narrowList, Sketch.rangeComparator);

        narrowFlow = new Dataflow();
        for (int i = 0; i < narrowList.size(); i++) {
            SketchGadget sg = narrowList.get(i);
            if (sg.setDistanceToGenerateNewFieldType(narrowList.size() - i - 1)) {
                sg.possibleLeadToGenOrUse.add(this);
            }
            if (sg.canGenerate(UpdateConfig.one().getNewFieldType(), true))
                narrowFlow.setCanGenerateTargetInstance(true);
        }
        if (narrowList.size() != allSketch.size()) {
            for (int i = 0; i < narrowList.size(); i++) {
                SketchGadget sg = narrowList.get(i);
                sg.setDistanceToGenerateNewFieldType(narrowList.size() - 1 - i);
            }
            narrowFlow.addGadgetToHead(narrowList);
            return narrowFlow;
        } else {
            return null;
        }
    }

    @Override
    public List<SketchGadget> getMustHaveMustHave() {
        if (this.mustHaveMustHave != null)
            return this.mustHaveMustHave;

        HashSet<SketchGadget> result = new HashSet<>();
        for (Sketch sk : this.mustHave) {
            if (!(sk instanceof SketchGadget))
                continue;
            if (result.contains(sk))
                continue;
            SketchGadget sg = (SketchGadget) sk;
            result.addAll(sg.getMustHaveMustHave());
            result.add(sg);
            if (sg instanceof SketchForeachCondition) {
                result.addAll(((SketchForeachCondition) sg).getOutterSelfInnerOrder());
            }
            if (sg.thisBelongsToForeach != null) {
                result.addAll(sg.thisBelongsToForeach.getOutterSelfInnerOrder());
            }
        }
        mustHaveMustHave = new ArrayList<>(result);
        Collections.sort(mustHaveMustHave, rangeComparator);
        return this.mustHaveMustHave;
    }

    @Override
    public List<SketchGadget> getDataflowByHoles(List<SketchGadget> allSketch) {
        if (this.mustHaveByHoles != null)
            return this.mustHaveByHoles;
        HashSet<SketchGadget> temp = new HashSet<>();
        int index = allSketch.indexOf(this);
        if (index <= 0)
            return Collections.emptyList();
        index--;
        HashSet<Sketch> mustHave = new HashSet<>(this.getMustHave());
        Set<Hole> holes = this.getAllHoles();
        for (; index >= 0; index--) {
            Sketch sk = allSketch.get(index);
            if (!(sk instanceof SketchGadget))
                continue;

            SketchGadget gs = (SketchGadget) sk;
            boolean add = false;
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
                    temp.addAll(fe.getOutterSelfInnerOrder());
                } else if (gs.thisBelongsToForeach != null) {
                    temp.addAll(gs.thisBelongsToForeach.getOutterSelfInnerOrder());
                } else {
                    temp.add(gs);
                }
                mustHave.addAll(gs.getMustHave());
                for (Hole h : gs.getAllHoles()) {
                    if (!h.isConstantHole() && h.getOriginalString() != null) // && !h.getOriginalString().equals("this")
                        holes.add(h);
                }
            }
        }
        this.mustHaveByHoles = new ArrayList<>(temp);
        Collections.sort(this.mustHaveByHoles, rangeComparator);
        return this.mustHaveByHoles;
    }

    @Override
    public Set<SketchGadget> getPossibleLeadToGenOrUse() {
        return this.possibleLeadToGenOrUse;
    }

    @Override
    public boolean usable(boolean ignoreSpecial) {
        if (this.isAddedFieldGadget || this.isReturnStmt() || this.isFieldAccessThisAsTarget())
            return false;
        if (ignoreSpecial && (this.isOnlyAssignNull() || this.isVariableDeclaratorNoInitializer()))
            return false;
        return getHoleNumber() == 0;
    }

    @Override
    public boolean usable(Collection<ResolvedType> variableTypes, boolean ignoreSpecial) {
        if (this.isAddedFieldGadget || this.isReturnStmt())
            return false;
        if (ignoreSpecial && (this.isOnlyAssignNull() || this.isVariableDeclaratorNoInitializer()))
            return false;

        HashSet<ResolvedType> temp = new HashSet<>(getSourceHoleTypes());
        if (this.isFieldAccessThisAsTarget()) {
            temp.add(this.getTargetHole().getType());
        }
        HashSet<ResolvedType> emptyHoles = new HashSet<>(temp);
        //intersection
        temp.retainAll(variableTypes);

        emptyHoles.removeAll(temp);

        for (ResolvedType t : emptyHoles) {
            boolean found = false;
            for (ResolvedType v : variableTypes) {
                try {
                    if (JavaparserSolver.isAssignableBy(t, v)) {
                        found = true;
                        break;
                    }
                } catch (Exception e) {
                }
            }
            if (!found)
                return false;
        }
        return true;
    }

    @Override
    public boolean canGenerate(ResolvedType type, boolean skipSpecial) {
        if (this.isAddedFieldGadget())
            return false;
        if (skipSpecial && (this.isOnlyAssignNull() || this.isVariableDeclaratorNoInitializer()))
            return false;
        for (ResolvedType t : getTargetHoleTypes()) {
            if (JavaparserSolver.isAssignableBy(type, t))
                return true;
        }
        return false;
    }

    @Override
    public ResolvedType generate() {
        if (getTargetHoleTypes().size() > 0)
            return getTargetHoleTypes().get(0);
        return null;
    }

    @Override
    public boolean needTheType(ResolvedType type) {
        for (ResolvedType t : getSourceHoleTypes()) {
            if (JavaparserSolver.isAssignableBy(t, type))
                return true;
        }
        return false;
    }

    @Override
    public boolean needTheTypes(Collection<ResolvedType> types) {
        for (ResolvedType t : getSourceHoleTypes()) {
            for (ResolvedType type : types) {
                if (JavaparserSolver.isAssignableBy(t, type))
                    return true;
            }
        }
        return false;
    }

    public void addReplace(SketchGadget replace) {
        this.replace = replace;
    }

    public boolean hasReplace() {
        return this.replace != null;
    }

    /**
     * This method only can be called after two ZCode are built completely
     *
     * @return
     */
    @Override
    public int hashCode() {
        HashCodeBuilder hash = new HashCodeBuilder(13, 33)
                .append(this.originalCode)
                .append(this.range);
        for (Element ele : this.elements)
            hash.append(ele);
        for (Sketch sk : this.mustHave)
            hash.append(sk);
        return hash.toHashCode();
    }

    /**
     * This method only can be called after two Sketch are built completely
     *
     * @return
     */
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
        SketchGadget temp = (SketchGadget) obj;
        if (this.elements.size() != temp.elements.size())
            return false;
        if (this.mustHave.size() != temp.mustHave.size())
            return false;
        EqualsBuilder eq = new EqualsBuilder()
                .append(this.originalCode, temp.originalCode)
                .append(this.range, temp.range);
        for (int i = 0; i < this.elements.size(); i++) {
            eq.append(this.elements.get(i), temp.elements.get(i));
        }
        Iterator<Sketch> thisIt = this.mustHave.iterator();
        Iterator<Sketch> tempIt = temp.mustHave.iterator();
        while (thisIt.hasNext() && tempIt.hasNext()) {
            eq.append(thisIt.next(), (tempIt.next()));
        }
        return eq.isEquals();
    }

    public LinkedList<ResolvedType> getSourceHoleTypes() {
        if (sourceHoleTypes != null)
            return sourceHoleTypes;
        sourceHoleTypes = new LinkedList<>();
        for (Element e : elements) {
            if (e.isSourceHole() && e.getType() != null) {
                if (e.getType().isArray()) {
                    if (e.getType().asArrayType().getComponentType() == null)
                        continue;
                    if (JavaparserSolver.myDescribe(e.getType().asArrayType().getComponentType()).contains("=null"))
                        continue;
                }
                sourceHoleTypes.add(e.getType());
            }
        }
        return sourceHoleTypes;
    }

    @Override
    public LinkedList<ResolvedType> getTargetHoleTypes() {
        if (targetHoleTypes != null)
            return targetHoleTypes;
        targetHoleTypes = new LinkedList<>();

        for (Element e : elements) {
            if (e.isTargetHole() && e.getType() != null) {
                if (e.getType().isArray()) {
                    if (e.getType().asArrayType().getComponentType() == null)
                        continue;
                    if (JavaparserSolver.myDescribe(e.getType().asArrayType().getComponentType()).contains("=null"))
                        continue;
                }
                targetHoleTypes.add(e.getType());
            }
        }
        return targetHoleTypes;
    }

    public Hole getTargetHole() {
        for (Element e : elements) {
            if (e.isTargetHole())
                return (Hole) e;
        }
        return null;
    }

    public HashSet<Hole> getSourceHoles() {
        HashSet<Hole> source = new HashSet<>();
        for (Element e : elements) {
            if (e.isSourceHole())
                source.add((Hole) e);
        }
        return source;
    }

    /**
     * Including target and source holes.
     *
     * @return
     */
    public HashSet<Hole> getAllHoles() {
        if (this.allHoles != null)
            return this.allHoles;
        this.allHoles = new HashSet<>();
        for (Element e : elements) {
            if (e instanceof Hole)
                this.allHoles.add((Hole) e);
        }
        return allHoles;
    }

    protected void copy(SketchGadget src, SketchGadget target) {
        target.type = src.type;
        target.elements.addAll(src.elements);
        target.mustHave.addAll(src.mustHave);
        target.originalCode = src.originalCode;
        target.range = src.range;
        target.outerContext = src.outerContext;
        target._thisAsContext = src._thisAsContext;
    }

    @Override
    public SketchGadget clone() {
        SketchGadget sketch = new SketchGadget();
        copy(this, sketch);
        organizeClones(sketch);
        return sketch;
    }

    protected void organizeClones(SketchGadget oneClone) {
        this.clones.add(oneClone);
        oneClone.clones.add(this);
    }

    /**
     * Make all sketch in this gadgets replaceable;
     * Mainly used while resolve parameters' types.
     * All parameters are placeholders
     */
    public void makeAllElementsReplacable(boolean isSource, Hole.HoleFrom holeFrom) {
        LinkedList<Element> temp = new LinkedList<>();
        for (Element ze : elements) {
            temp.add(new Hole(ze.originalString, ze.type, isSource, holeFrom));
        }
        this.elements = temp;
        if (this.hasReplace())
            this.replace.makeAllElementsReplacable(isSource, holeFrom);
    }

    public void clearElements() {
        this.elements.clear();
    }

    public SketchGadget removeConstantHoles() {
        SketchGadget sg = this.clone();
        sg.clearElements();
        boolean removed = false;
        for (Element e : this.elements) {
            if ((e instanceof Hole)
                    && ((Hole) e).getHoleFrom() == Hole.HoleFrom.constant) {
                sg.addElement(((Hole) e).fallBackToElement());
                removed = true;
            } else
                sg.addElement(e.clone());
        }
        if (removed)
            return sg;
        else
            return null;
    }

    public void addElement(Element e) {
        this.elements.add(e);
        if (this.hasReplace()) {
            this.replace.elements.add(e);
//            for(Sketch s : this.replace)
//                s.elements.add(e);
        }
    }

    public void addElement(Element... alle) {
        for (Element e : alle)
            this.addElement(e);
    }

    /**
     * If we cross them over, there could be too many replaces.
     * We add temp.elements to this.elements and add temp.replace.elements to this.replace.elements here.
     * Add all must have of <tt>sketch</tt> to this
     *
     * @param sketch
     */
    public void addElements(Sketch sketch) {
        if (!(sketch instanceof SketchGadget))
            return;
        SketchGadget temp = (SketchGadget) sketch;
        if (this.hasReplace()) {
            this.elements.addAll(temp.elements);
            if (temp.hasReplace()) {
                this.replace.elements.addAll(temp.replace.elements);
            } else {
                this.replace.elements.addAll(temp.elements);
            }
        } else {
            if (temp.hasReplace()) {
                this.replace = this.clone();
                this.replace.elements.addAll(temp.replace.elements);
            }
            this.elements.addAll(temp.elements);
        }
        for (Sketch s : temp.getMustHave()) {
            this.addMustHave(s);
        }
        /**
         if(this.hasReplace()){
         List<Sketch> newReplace = new ArrayList<>();
         List<Sketch> thisReplace = new ArrayList<>(this.replace);
         thisReplace.add(this.clone());
         if(temp.hasReplace()){
         List<Sketch> tempReplace = new ArrayList<>(temp.replace);
         tempReplace.add(temp.clone());
         for(Sketch s : thisReplace){
         for(Sketch t : tempReplace){
         Sketch sclone = s.clone();
         sclone.elements.addAll(t.elements);
         newReplace.add(sclone);
         }
         }
         this.replace = newReplace;
         }else{
         for(Sketch s: thisReplace){
         s.elements.addAll(temp.elements);
         newReplace.add(s);
         }
         this.replace = newReplace;
         }
         }else if(temp.hasReplace()){
         this.replace = new ArrayList<>();
         List<Sketch> tempReplace = new ArrayList<>(temp.replace);
         tempReplace.add(temp.clone());
         for(Sketch t : tempReplace){
         Sketch sclone = this.clone();
         sclone.elements.addAll(t.elements);
         this.replace.add(sclone);
         }
         }
         */
    }

    /**
     * add all elements of <tt>e</tt> gadgets into this gadget
     * add all must have of <tt>e</tt> to this
     *
     * @param e
     */
    public void addElements(ZCode e) {
        addElements(e.getSketch());
    }

    /**
     * add all elements of all gadgets in <tt>list</tt> into this gadget.
     * add <tt>sepa</tt> between gadgets
     * add all must have of <tt>list</tt> to this.
     *
     * @param list
     * @param sepa
     */
    public void addElements(List<ZCode> list, String sepa) {
        Element sepaE = new Element(sepa, null);
        boolean sp = false;
        for (ZCode zs : list) {
            if (sp) {
                addElement(sepaE);
            }
            addElements(zs);
            if (!sp)
                sp = true;
        }
    }

    public void addElements(List<ZCode> list) {
        addElements(list, "");
    }

    public LinkedList<Element> getElements() {
        return this.elements;
    }

    /**
     * count source placeholders
     */
    public void resolveHoleNumber() {
        if (this.holeNumber != null)
            return;
        int hole = 0;
        for (Element ze : elements) {
            if (ze.isReplacable() && ze.isSourceHole()) {
                hole++;
            }
        }
        this.holeNumber = hole;

        if (this.hasReplace())
            this.replace.resolveHoleNumber();
    }

    public int getHoleNumber() {
        if (this.holeNumber == null) {
            resolveHoleNumber();
        }
        return this.holeNumber;
    }

    @Override
    public boolean isIfStmt() {
        return false;
    }

    @Override
    public boolean isForStmt() {
        return false;
    }

    @Override
    public boolean isForeachStmt() {
        return false;
    }

    @Override
    public boolean isWhileStmt() {
        return false;
    }
}
