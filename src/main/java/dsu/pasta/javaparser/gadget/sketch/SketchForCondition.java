package dsu.pasta.javaparser.gadget.sketch;

import com.github.javaparser.resolution.types.ResolvedType;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class SketchForCondition extends SketchGadget {
    private static final String forConditionCategoryName = "forCondition";

    public SketchForCondition() {
        super();
    }

    public SketchForCondition(JsonObject json) {
        super(json);
    }

    public static boolean is(JsonObject json) {
        return json.getString(categoryName) != null && json.getString(categoryName).equals(forConditionCategoryName);
    }

    public static SketchForCondition create(JsonObject json) {
        if (is(json)) {
            return new SketchForCondition(json);
        }
        return null;
    }

    @Override
    protected void createJsonObjectBuilder() {
        super.createJsonObjectBuilder();
        JsonValue categoryValue = Json.createValue(forConditionCategoryName);
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
    public ResolvedType generate() {
        return null;
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
    public SketchForCondition clone() {
        SketchForCondition sketch = new SketchForCondition();
        copy(this, sketch);
        organizeClones(sketch);
        return sketch;
    }

    @Override
    public boolean isIfStmt() {
        return false;
    }

    @Override
    public boolean isForStmt() {
        return true;
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