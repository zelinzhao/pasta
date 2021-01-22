package dsu.pasta.javaparser.gadget.sketch;

import com.github.javaparser.resolution.types.ResolvedType;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class SketchWhileCondition extends SketchGadget {
    private static final String whileConditionCategoryName = "whileCondition";

    public SketchWhileCondition() {
        super();
    }

    public SketchWhileCondition(JsonObject json) {
        super(json);
    }

    public static boolean is(JsonObject json) {
        return json.getString(categoryName) != null && json.getString(categoryName).equals(whileConditionCategoryName);
    }

    public static SketchWhileCondition create(JsonObject json) {
        if (is(json)) {
            return new SketchWhileCondition(json);
        }
        return null;
    }

    @Override
    protected void createJsonObjectBuilder() {
        super.createJsonObjectBuilder();
        JsonValue categoryValue = Json.createValue(whileConditionCategoryName);
        jsonObjectBuilder.add(categoryName, categoryValue);
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
    public boolean canGenerate(ResolvedType type, boolean skipSpecial) {
        return false;
    }

    @Override
    public boolean isOnlyAssignNull() {
        return false;
    }

    @Override
    public SketchWhileCondition clone() {
        SketchWhileCondition sketch = new SketchWhileCondition();
        copy(this, sketch);
        organizeClones(sketch);
        return sketch;
    }

    @Override
    public ResolvedType generate() {
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
        return true;
    }
}
