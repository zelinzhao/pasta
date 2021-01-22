package dsu.pasta.javaparser.gadget.sketch;

import com.github.javaparser.resolution.types.ResolvedType;
import dsu.pasta.javaparser.factory.stmt.ZForEachStmt;

import javax.json.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class SketchForeachCondition extends SketchGadget {
    private static final String foreachConditionCategoryName = "foreachCondition";
    private static final String outerAsWhile = "outerAsWhile";
    private static final String innerAsWhile = "innerAsWhile";

    private List<SketchGadget> outer = new ArrayList<>();
    private List<SketchGadget> inner = new ArrayList<>();

    private List<Integer> _outer_id = new ArrayList<>();
    private List<Integer> _inner_id = new ArrayList<>();
    private List<SketchGadget> outSelfIn = new ArrayList<>();

    public SketchForeachCondition() {
        super();
    }

    public SketchForeachCondition(JsonObject json) {
        super(json);
        if (json.get(outerAsWhile) != null && !json.get(outerAsWhile).equals(JsonValue.NULL)) {
            JsonArray array = json.getJsonArray(outerAsWhile);
            for (int i = 0; i < array.size(); i++) {
                JsonNumber j = array.getJsonNumber(i);
                int jid = j.intValue();
                this._outer_id.add(jid);
            }
        }
        if (json.get(innerAsWhile) != null && !json.get(innerAsWhile).equals(JsonValue.NULL)) {
            JsonArray array = json.getJsonArray(innerAsWhile);
            for (int i = 0; i < array.size(); i++) {
                JsonNumber j = array.getJsonNumber(i);
                int jid = j.intValue();
                this._inner_id.add(jid);
            }
        }
    }

    public static boolean is(JsonObject json) {
        return json.getString(categoryName) != null && json.getString(categoryName).equals(foreachConditionCategoryName);
    }

    public static SketchForeachCondition create(JsonObject json) {
        if (is(json)) {
            return new SketchForeachCondition(json);
        }
        return null;
    }

    public void resolveOutInner(HashMap<Integer, Sketch> idSketch) {
        for (int i : _outer_id) {
            Sketch sg = idSketch.get(i);
            if (sg != null && sg instanceof SketchGadget)
                this.addOuter((SketchGadget) sg);
        }
        for (int i : _inner_id) {
            Sketch sg = idSketch.get(i);
            if (sg != null && sg instanceof SketchGadget) {
                if (sg.getSketchString().contains("++") && sg.getSketchString().contains(ZForEachStmt._dsu_array_index_)) {
                    //_dsu_iterator_++;
                    this.addInner((SketchGadget) sg);
                } else {
                    if (this.inner.size() == 0)
                        this.addInner((SketchGadget) sg);
                    else
                        this.addInner(0, (SketchGadget) sg);
                }
            }
        }

    }

    public int getExtraSize() {
        return this.outer.size() + this.inner.size();
    }

    public List<SketchGadget> getOutterSelfInnerOrder() {
        if (outSelfIn.size() > 0)
            return outSelfIn;
        outSelfIn.addAll(this.getOuter());
        outSelfIn.add(this);
        outSelfIn.addAll(this.getInner());
        return outSelfIn;
    }

    public List<SketchGadget> getOutterSelfInnerReverseOrder() {
        List<SketchGadget> reverse = new ArrayList<>(this.getOutterSelfInnerOrder());
        Collections.reverse(reverse);
        return reverse;
    }

    @Override
    protected void createJsonObjectBuilder() {
        super.createJsonObjectBuilder();
        JsonValue categoryValue = Json.createValue(foreachConditionCategoryName);

        JsonArrayBuilder arrayBuild = Json.createArrayBuilder();
        for (Sketch sk : this.outer) {
            arrayBuild.add(sk.id);
        }
        JsonArray outarr = arrayBuild.build();

        arrayBuild = Json.createArrayBuilder();
        for (Sketch sk : this.inner) {
            arrayBuild.add(sk.id);
        }
        JsonArray inarr = arrayBuild.build();

        jsonObjectBuilder.add(categoryName, categoryValue)
                .add(outerAsWhile, outarr)
                .add(innerAsWhile, inarr);
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
    public SketchForeachCondition clone() {
        SketchForeachCondition sketch = new SketchForeachCondition();
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
        return false;
    }

    @Override
    public boolean isForeachStmt() {
        return true;
    }

    @Override
    public boolean isWhileStmt() {
        return false;
    }

    public List<SketchGadget> getInner() {
        for (SketchGadget sg : inner)
            sg.setOuterContext(this.outerContext);
        return inner;
    }

    public void addInner(int index, SketchGadget in) {
        this.inner.add(index, in);
        in.thisBelongsToForeach = this;
        if (this.hasReplace()) {
            if (!in.hasReplace())
                ((SketchForeachCondition) this.getReplace()).addInner(index, in);
            else
                ((SketchForeachCondition) this.getReplace()).addInner(index, in.getReplace());
        }
    }

    public void addInner(SketchGadget in) {
        this.inner.add(in);
        in.thisBelongsToForeach = this;
        if (this.hasReplace()) {
            if (!in.hasReplace())
                ((SketchForeachCondition) this.getReplace()).addInner(in);
            else
                ((SketchForeachCondition) this.getReplace()).addInner(in.getReplace());
        }
    }

    public List<SketchGadget> getOuter() {
        for (SketchGadget sg : outer) {
            sg.setOuterContext(this.outerContext);
        }
        return outer;
    }

    public void addOuter(SketchGadget out) {
        this.outer.add(out);
        out.thisBelongsToForeach = this;
        if (this.hasReplace()) {
            if (!out.hasReplace())
                ((SketchForeachCondition) this.getReplace()).addOuter(out);
            else
                ((SketchForeachCondition) this.getReplace()).addOuter(out.getReplace());
        }
    }

    @Override
    public ResolvedType generate() {
        return null;
    }
//    @Override
//    public double calculateScoreBroughtByThisToProgram(Program program, double simi){
//        double result = 0.0;
//        for(SketchGadget sg: this.getOutterSelfInnerOrder()){
//            result += super.calculateScoreBroughtByThisToProgram(program, program.getDataflowGraphSimilarity());
//        }
//        return result;
//    }
//    @Override
//    public double getExtraScore(Program program){
//        double result = 0.0;
//        for(SketchGadget sg: this.getOutterSelfInnerOrder()){
//            result += super.getExtraScore(program);
//        }
//        return result;
//    }
}