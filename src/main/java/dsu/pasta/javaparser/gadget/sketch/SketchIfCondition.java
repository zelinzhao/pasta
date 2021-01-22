package dsu.pasta.javaparser.gadget.sketch;

import com.github.javaparser.resolution.types.ResolvedType;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.HashMap;

public class SketchIfCondition extends SketchGadget {
    private static final String ifConditionCategoryName = "ifCondition";
    private static final String oriThenName = "oriThen";
    private static final String oriElseName = "oriElse";

    private SketchIfCondition oriThenCond;
    private SketchIfCondition oriElseCond;
    private Integer _oriThenCondId;
    private Integer _oriElseCondId;

    public SketchIfCondition() {
        super();
    }

    public SketchIfCondition(JsonObject json) {
        super(json);
        if (json.get(oriThenName) != null && !json.get(oriThenName).equals(JsonValue.NULL))
            this._oriThenCondId = json.getInt(oriThenName);
        if (json.get(oriElseName) != null && !json.get(oriElseName).equals(JsonValue.NULL))
            this._oriElseCondId = json.getInt(oriElseName);
    }

    public static boolean is(JsonObject json) {
        return json.getString(categoryName) != null && json.getString(categoryName).equals(ifConditionCategoryName);
    }

    public static SketchIfCondition create(JsonObject json) {
        if (is(json)) {
            return new SketchIfCondition(json);
        }
        return null;
    }

    public void resolveOriThenElse(HashMap<Integer, Sketch> idSketch) {
        if (_oriElseCondId != null) {
            this.oriElseCond = (SketchIfCondition) idSketch.get(_oriElseCondId);
        }
        if (_oriThenCondId != null) {
            this.oriThenCond = (SketchIfCondition) idSketch.get(_oriThenCondId);
        }
    }

    public void setOriThenCond(SketchIfCondition sif) {
        this.oriThenCond = sif;
    }

    public void setOriElseCond(SketchIfCondition sel) {
        this.oriElseCond = sel;
    }

    @Override
    public SketchIfCondition clone() {
        SketchIfCondition sketch = new SketchIfCondition();
        copy(this, sketch);
        sketch.oriThenCond = this.oriThenCond;
        sketch.oriElseCond = this.oriElseCond;
        organizeClones(sketch);
        return sketch;
    }

    @Override
    protected void createJsonObjectBuilder() {
        super.createJsonObjectBuilder();
        JsonValue categoryValue = Json.createValue(ifConditionCategoryName);
        JsonValue oriThenValue = oriThenCond == null ? JsonValue.NULL : Json.createValue(oriThenCond.id);
        JsonValue oriElseValue = oriElseCond == null ? JsonValue.NULL : Json.createValue(oriElseCond.id);

        jsonObjectBuilder.add(categoryName, categoryValue)
                .add(oriThenName, oriThenValue)
                .add(oriElseName, oriElseValue);
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
    public boolean isIfStmt() {
        return true;
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
