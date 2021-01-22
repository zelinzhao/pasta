package dsu.pasta.javaparser.gadget.sketch;

import com.github.javaparser.resolution.types.ResolvedType;
import dsu.pasta.javaparser.factory.analyzer.JavaparserSolver;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.HashMap;

public class Hole extends Element {
    private static final String holeCategory = "hole";
    private static final String isSourceName = "isSource";
    private static final String holeFromName = "holeFrom";
    private static final String SKETCH_HOLE = "@";
    /**
     * Only valid for holes
     */
    private boolean isSource = false;
    private HoleFrom holeFrom;

    /**
     * Replaceable holes
     *
     * @param type
     * @param isSource
     */
    public Hole(ResolvedType type, boolean isSource, HoleFrom holeFrom) {
        this(null, type, isSource, holeFrom);
    }

//    private Variable fill = null;

    public Hole(String original, ResolvedType type, boolean isSource, HoleFrom holeFrom) {
        super(original, type);
        this.isSource = isSource;
        this.holeFrom = holeFrom;
    }

    public Hole(JsonObject json) {
        super(json);
        JsonValue v1 = json.get(isSourceName);
        if (v1.equals(JsonValue.TRUE))
            this.isSource = true;
        else if (v1.equals(JsonValue.FALSE))
            this.isSource = false;
        if (!json.get(holeFromName).equals(JsonValue.NULL)) {
            String hf = json.getString(holeFromName);
            this.holeFrom = HoleFrom.get(hf);
        }
    }

    public static Hole create(JsonObject json) {
        if (!json.getString(categoryName).equals(holeCategory))
            return null;
        return new Hole(json);
    }

    @Override
    public JsonObject getJson() {
        if (json != null)
            return json;
        JsonValue oriValue = (originalString == null) ? JsonValue.NULL : Json.createValue(originalString);
        JsonValue fullyNameValue = (type == null) ? JsonValue.NULL : Json.createValue(JavaparserSolver.myDescribe(type));
        JsonValue srcValue = isSource ? JsonValue.TRUE : JsonValue.FALSE;
        JsonValue holeFromValue = Json.createValue(holeFrom.getName());
        json = Json.createObjectBuilder()
                .add(categoryName, holeCategory)
                .add(oriCodeName, oriValue)
                .add(fullyName, fullyNameValue)
                .add(isSourceName, srcValue)
                .add(holeFromName, holeFromValue)
                .build();
        return json;
    }

    /**
     * For internal use
     *
     * @param source
     */
    public void setSource(boolean source) {
        isSource = source;
    }

    @Override
    public boolean isReplacable() {
        return true;
    }

    @Override
    public boolean isSourceHole() {
        return this.isSource;
    }

    @Override
    public boolean isTargetHole() {
        return !this.isSource;
    }

    @Override
    public Hole clone() {
        Hole e = new Hole(this.originalString, this.type, this.isSource, this.holeFrom);
        return e;
    }

    public boolean isConstantHole() {
        return this.holeFrom == HoleFrom.constant;
    }

    public boolean isPrimitiveTypeHole() {
        return this.type != null && this.type.isPrimitive();
    }

    public Element fallBackToElement() {
        return new Element(this.originalString, this.type);
    }

    @Override
    public String toString() {
        return this.getSketchString(true);
    }

    @Override
    public String getSketchString(boolean useSimpleName) {
        String result = "";
        if (type != null)
            result = SKETCH_HOLE +
                    (useSimpleName ? JavaparserSolver.myDescribeSimple(type)
                            : JavaparserSolver.myDescribe(type))
                    + SKETCH_HOLE;
        else if (originalString != null)
            result = originalString;
        if (result.endsWith("{") || result.endsWith(";"))
            result += "\n";
        return result;
    }

    @Override
    public String getSimpleSketchString() {
        String result = "";
        if (type != null)
            result = "[]";
        else if (originalString != null)
            result = originalString;
        return result;
    }

    public HoleFrom getHoleFrom() {
        return holeFrom;
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hash = new HashCodeBuilder(23, 33)
                .append(this.originalString)
                .append(this.holeFrom);
        if (this.type != null)
            hash.append(JavaparserSolver.myDescribe(this.type));
        return hash.toHashCode();
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
        Hole temp = (Hole) obj;
        return new EqualsBuilder()
                .append(this.originalString, temp.originalString)
                .append(this.holeFrom, temp.holeFrom)
                .append(JavaparserSolver.myDescribe(this.type), JavaparserSolver.myDescribe(temp.type))
                .isEquals();
    }

    public static enum HoleFrom {
        constant("constant"),//original thing is a constant, e.g. 1+2
        variable("variable"),//original thing is a local variable, e.g. a+b
        field("field"),//original thing is a field, e.g. field access with implicit this
        instance("instance"),//original thing is an instance, e.g. the "this" in field access

        //the followings are resolved from declarations
        parameter("parameter"),//parameters in method/constructor declarations
        target("target"),//target hole resolved from declarations
        definition("definition");//holes created according to definitions, e.g. the "this" hole in resolved field access expr

        private static HashMap<String, HoleFrom> nameToHolefrom = new HashMap<>();

        static {
            nameToHolefrom.put(constant.getName(), constant);
            nameToHolefrom.put(variable.getName(), variable);
            nameToHolefrom.put(field.getName(), field);
            nameToHolefrom.put(instance.getName(), instance);
            nameToHolefrom.put(parameter.getName(), parameter);
            nameToHolefrom.put(target.getName(), target);
            nameToHolefrom.put(definition.getName(), definition);
        }

        private String name;

        private HoleFrom(String s) {
            name = s;
        }

        public static HoleFrom get(String name) {
            return nameToHolefrom.get(name);
        }

        public String getName() {
            return this.name;
        }
    }

}
