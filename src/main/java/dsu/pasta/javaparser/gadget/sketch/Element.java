package dsu.pasta.javaparser.gadget.sketch;

import com.github.javaparser.resolution.types.ResolvedType;
import dsu.pasta.javaparser.factory.analyzer.JavaparserSolver;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.HashSet;

public class Element {
    /**
     * Type of this element. could be <tt>element</tt> or <tt>hole</tt>
     */
    protected static final String categoryName = "category";
    /**
     * The original code of this element, if any
     */
    protected static final String oriCodeName = "oriCode";
    /**
     * The fully qualified name of type, if any
     */
    protected static final String fullyName = "type";
    private static final String elementCategory = "element";
    public static Element op_plusplus = new Element("++");
    public static Element op_minusminus = new Element("--");
    /**
     * =
     */
    public static Element op_equal = new Element("=");
    /**
     * +
     */
    public static Element op_plus = new Element("+");
    /**
     * -
     */
    public static Element op_minus = new Element("-");
    /** * */
    public static Element op_times = new Element("*");
    /**
     * &&
     */
    public static Element op_and = new Element("&&");
    /**
     * ||
     */
    public static Element op_or = new Element("||");
    public static Element op_lesser = new Element("<");
    public static Element op_grater = new Element(">");
    /*/*/
    public static Element op_divide = new Element("/");
    /**
     * ;
     */
    public static Element op_semicolon = new Element(";\n");
    /**
     * :
     */
    public static Element op_colon = new Element(":");
    /**
     * ,
     */
    public static Element op_comma = new Element(",");
    /**
     * !
     */
    public static Element op_negate = new Element("!");
    /**
     * .
     */
    public static Element op_dot = new Element(".");
    /**
     * ?
     */
    public static Element op_question = new Element("?");
    public static Element op_space = new Element(" ");
    public static Element op_lbracket = new Element("(");
    public static Element op_rbracket = new Element(")");
    public static Element op_lsquare = new Element("[");
    public static Element op_rsquare = new Element("]");
    public static Element op_langle = new Element("<");
    public static Element op_rangle = new Element(">");
    public static Element op_lcurly = new Element("{\n");
    public static Element op_rcurly = new Element("}\n");
    /**
     * Variable name index.
     */
    protected static int vIndex = 0;
    protected JsonObject json = null;
    /**
     * The original string of this element could be used directly.
     * Could be method name, variable name, method call.
     */
    protected String originalString;
    /**
     * The type corresponding to this element
     */
    protected ResolvedType type;

    /**
     * Resolved string for this element, contains original string, possible variable names.
     */
    protected HashSet<String> resolves = null;

    /**
     * Fixed element
     *
     * @param original
     */
    public Element(String original) {
        this(original, null);
    }

    /**
     * Fixed element
     *
     * @param type
     */
    public Element(ResolvedType type) {
        this(null, type);
    }

    /**
     * Fixed element
     *
     * @param original
     * @param type
     */
    public Element(String original, ResolvedType type) {
        this.originalString = original;
        this.type = type;
    }

    public Element(JsonObject json) {
        this.json = json;
        JsonValue v1 = json.get(oriCodeName);
        if (!v1.equals(JsonValue.NULL))
            this.originalString = json.getString(oriCodeName);

        JsonValue v2 = json.get(fullyName);
        if (!v2.equals(JsonValue.NULL)) {
            String full = json.getString(fullyName);
            if (full != null)
                this.type = JavaparserSolver.getType(full);
        }
    }

    public static Element create(JsonObject json) {
        if (!json.getString(categoryName).equals(elementCategory))
            return null;
        return new Element(json);
    }

    public JsonObject getJson() {
        if (json != null)
            return json;

        JsonValue oriValue = (originalString == null) ? JsonValue.NULL : Json.createValue(originalString);
        JsonValue fullValue = (type == null) ? JsonValue.NULL : Json.createValue(JavaparserSolver.myDescribe(type));

        json = Json.createObjectBuilder()
                .add(categoryName, elementCategory)
                .add(oriCodeName, oriValue)
                .add(fullyName, fullValue)
                .build();
        return json;
    }

    public String getJsonString() {
        return getJson().toString();
    }

    /**
     * Is this a place holder?
     *
     * @return
     */
    public boolean isReplacable() {
        return false;
    }

    public boolean isSourceHole() {
        return false;
    }

    public boolean isTargetHole() {
        return false;
    }

    public String getOriginalString() {
        return originalString;
    }

    public ResolvedType getType() {
        if (this.type != null)
            return this.type;
        return null;
    }

    public String getTypeAsString() {
        if (this.type != null)
            return JavaparserSolver.myDescribe(this.type);
        return null;
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hash = new HashCodeBuilder(23, 33)
                .append(this.originalString);
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
        Element temp = (Element) obj;
        return new EqualsBuilder()
                .append(this.originalString, temp.originalString)
                .append(JavaparserSolver.myDescribe(this.type), JavaparserSolver.myDescribe(temp.type))
                .isEquals();
    }

    /**
     * Call {@link this.sketchString()}
     *
     * @return
     */
    @Override
    public String toString() {
        return getSketchString(true);
    }

    @Override
    public Element clone() {
        Element e = new Element(this.originalString, this.type);
        return e;
    }

    public String getSketchString(boolean useSimpleName) {
        String result = "";
        if (originalString != null)
            result = originalString;
        else if (type != null) {
            result = useSimpleName ? JavaparserSolver.myDescribeSimple(type)
                    : JavaparserSolver.myDescribe(type);
        }
        if (result.endsWith("{") || result.endsWith(";"))
            result += "\n";

        return result;
    }

    /**
     * @return \/**[] = [].m2*\/
     */
    public String getSimpleSketchString() {
        String result = "";
        if (originalString != null) {
            if (originalString.length() == 1 && originalString.equals("="))
                result = " = ";
            else
                result = originalString;
        } else if (type != null) {
            result = JavaparserSolver.myDescribeSimple(type);
        }
        return result;
    }

    public boolean isTypeNameElement() {
        return this.type != null && this.originalString == null;
    }

    /**
     * Only replace types for special blocks, such as for loop.
     *
     * @return
     */
    public String replaceTypeForBlocks() {
        if (type != null)
            return replaceE(JavaparserSolver.myDescribe(type));
        else if (originalString != null)
            return originalString;
        return originalString;
    }

    /**
     * Replace types and corresponding variable names for blocks, such as for loops.
     *
     * @param isSource
     * @return
     */
    public String replaceTypeAndNameForBlocks(boolean isSource) {
        if (type != null)
            return replaceE(JavaparserSolver.myDescribe(type));
        else if (originalString != null)
            return originalString;
        return originalString;
    }

    /**
     * Resolve string for this element.
     *
     * @param isSource
     * @return
     */
    public HashSet<String> resolveString(boolean isSource) {
        if (resolves != null)
            return resolves;
        HashSet<String> result = new HashSet<>();

        if (originalString != null)
            result.add(originalString);
        else if (type != null)
            result.add(replaceE(JavaparserSolver.myDescribe(type)));
        assert (result.size() > 0);
        resolves = result;
        return result;
    }

    /**
     * Javassist can't compile generic type parameters.
     * We replace element such as < xxx > to <>
     *
     * @param str
     * @return
     */
    private String replaceE(String str) {
        if (str.contains("<E>"))
            return str.replace("<E>", "<>");
        if (str.contains("<K, V>"))
            return str.replace("<K, V>", "<>");
        return str;
    }

}
