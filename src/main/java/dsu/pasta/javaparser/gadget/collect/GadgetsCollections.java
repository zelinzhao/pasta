package dsu.pasta.javaparser.gadget.collect;

import com.github.javaparser.ast.Node;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.sketch.Context;
import dsu.pasta.javaparser.gadget.sketch.Sketch;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GadgetsCollections {

    /**
     * All visited (non-unique) resolved factory, including statements, expressions.
     */
    public static HashMap<Node, ZCode> allParsedNode = new HashMap<Node, ZCode>();
    public static List<Sketch> tempSketches = new ArrayList<>();
    public static List<Context> tempContext = new ArrayList<>();
    private static Map<String, Boolean> config = null;
    private static JsonWriterFactory jwf = null;

    public static void clearTemp() {
        tempSketches.clear();
        tempContext.clear();
    }

    public static void addTempSketch(Sketch sketch) {
        //if sketch contains constant or string, we make a new gadget.
        //Original sketch has constants as holes, the copy has constants as constants.
        tempSketches.add(sketch);
        if (sketch instanceof SketchGadget) {
            SketchGadget temp = (SketchGadget) sketch;
            if (temp.hasConstantHole()) {
                SketchGadget sk = temp.removeConstantHoles();
                if (sk != null)
                    tempSketches.add(sk);
            }
            if (temp.hasReplace()) {
                SketchGadget replace = temp.getReplace();
                tempSketches.add(replace);
                if (replace.hasConstantHole()) {
                    SketchGadget sk = replace.removeConstantHoles();
                    if (sk != null)
                        tempSketches.add(sk);
                }
            }
        }
    }

    public static void addTempContext(Context context) {
        tempContext.add(context);
    }

    public static void addParsedNode(Node node, ZCode zcode) {
        allParsedNode.put(node, zcode);
    }

    public static void writeTempSketchesContextsTo(String file) {
        if (config == null) {
            config = new HashMap<>();
            config.put(JsonGenerator.PRETTY_PRINTING, true);
            jwf = Json.createWriterFactory(config);
        }
        JsonArrayBuilder arrayBuild = Json.createArrayBuilder();
        for (Sketch s : tempSketches)
            arrayBuild.add(s.getJson());
        for (Context s : tempContext)
            arrayBuild.add(s.getJson());
        JsonArray elementValue = arrayBuild.build();
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            try (JsonWriter jsonWriter = jwf.createWriter(bw)) {
                jsonWriter.writeArray(elementValue);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
