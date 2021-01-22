package dsu.pasta.javaparser.gadget.sketch;

import com.github.javaparser.resolution.types.ResolvedType;
import dsu.pasta.javaparser.factory.analyzer.JavaparserSolver;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.*;

public class SketchConstant extends Sketch {
    private static final String constantCategory = "constant";
    Element element;

    public SketchConstant(String original, ResolvedType rtype) {
        super();
        this.originalCode = original;
        this.type = JavaparserSolver.myDescribe(rtype);
        this.element = new Element(original, rtype);
    }

    public SketchConstant(JsonObject json) {
        super(json);
        this.element = new Element(originalCode, JavaparserSolver.getType(this.type));
    }

    public static boolean is(JsonObject json) {
        return json.getString(categoryName) != null && json.getString(categoryName).equals(constantCategory);
    }

    public static SketchConstant create(JsonObject json) {
        if (is(json)) {
            return new SketchConstant(json);
        }
        return null;
    }

    @Override
    public void decrementPriorityByHoles() {
    }

    @Override
    public JsonObject getJson() {
        if (json != null)
            return json;
        super.createJsonObjectBuilder();

        JsonValue categoryValue = Json.createValue(constantCategory);
        jsonObjectBuilder.add(categoryName, categoryValue);

        json = jsonObjectBuilder.build();
        return json;
    }

    @Override
    public boolean hasConstantHole() {
        return false;
    }

    /**
     * This method only can be called after two ZCode are built completely
     *
     * @return
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(this.originalCode)
                .append(this.range)
                .append(this.element)
                .toHashCode();
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
        SketchConstant temp = (SketchConstant) obj;
        return new EqualsBuilder()
                .append(this.originalCode, temp.originalCode)
                .append(this.range, temp.range)
                .append(this.element, temp.element)
                .isEquals();
    }

    @Override
    public String getSketchString() {
        if (this.sketchString != null)
            return this.sketchString;
        this.sketchString = this.type + " " + this.originalCode;
        return this.sketchString;
    }

    @Override
    public String getSimpleSketchString() {
        if (this.simpleSketchString != null)
            return this.simpleSketchString;
        this.simpleSketchString = this.type + " " + this.originalCode;
        return this.simpleSketchString;
    }

    @Override
    public void addMustHave(Sketch sketch) {
    }

    @Override
    public void addAllMustHave(Sketch sketch) {
    }

    @Override
    public LinkedHashSet<Sketch> getMustHave() {
        return new LinkedHashSet<>();
    }

    @Override
    public List<SketchGadget> getMustHaveMustHave() {
        return Collections.emptyList();
    }

    @Override
    public List<SketchGadget> getDataflowByHoles(List<SketchGadget> allSketch) {
        return new ArrayList<>();
    }

    @Override
    public Set<SketchGadget> getPossibleLeadToGenOrUse() {
        return Collections.emptySet();
    }

    @Override
    public boolean usable(boolean ignoreSpecial) {
        return true;
    }

    @Override
    public boolean usable(Collection<ResolvedType> variableTypes, boolean ignoreSpecial) {
        return true;
    }

    @Override
    public boolean canGenerate(ResolvedType type, boolean skipSpecial) {
        if (this.element != null && this.element.getType() != null)
            return JavaparserSolver.isAssignableBy(type, this.element.getType());
        return false;
    }

    @Override
    public ResolvedType generate() {
        if (this.element != null && this.element.getType() != null)
            return this.element.getType();
        return null;
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

    @Override
    public LinkedList<ResolvedType> getSourceHoleTypes() {
        return new LinkedList<ResolvedType>();
    }

    @Override
    public LinkedList<ResolvedType> getTargetHoleTypes() {
        LinkedList<ResolvedType> result = new LinkedList<>();
        if (this.element != null && this.element.getType() != null)
            result.add(this.element.getType());
        return result;
    }

    public ResolvedType getConstantType() {
        if (this.element != null && this.element.getType() != null)
            return this.element.getType();
        return null;
    }

    @Override
    public boolean needTheType(ResolvedType type) {
        return false;
    }

    @Override
    public boolean needTheTypes(Collection<ResolvedType> type) {
        return false;
    }

    @Override
    public HashSet<String> getAllTypesAsString() {
        HashSet<String> result = new HashSet<>();
        result.add(this.element.getTypeAsString());
        return result;
    }

    @Override
    public boolean isOnlyAssignNull() {
        return false;
    }
}
