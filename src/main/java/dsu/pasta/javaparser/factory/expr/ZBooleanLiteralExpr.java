package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Hole;
import dsu.pasta.javaparser.gadget.sketch.SketchConstant;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZBooleanLiteralExpr extends ZExpression {
    private BooleanLiteralExpr booleanLiteralExpr;

    public ZBooleanLiteralExpr(ZCode parent, BooleanLiteralExpr booleanLiteralExpr) {
        super(parent);
        this.booleanLiteralExpr = booleanLiteralExpr;
    }

    public static List<ZCode> visit(BooleanLiteralExpr ble, ZCode parent, ZGenericListVisitorAdapter visitor) {
        // should always be source
        ZBooleanLiteralExpr zble = new ZBooleanLiteralExpr(parent, ble);
        SketchGadget sketch = new SketchGadget();
        sketch.setRange(ble.getRange().get());
        sketch.setOriginalCode(zble.toString());
        sketch.setType(zble.getClass().getSimpleName());

        sketch.addElement(new Hole(ble.toString(), ble.calculateResolvedType(), true, Hole.HoleFrom.constant));

        SketchConstant cs = new SketchConstant(ble.toString(), ble.calculateResolvedType());
        cs.setRange(ble.getRange().get());

        sketch.addMustHave(cs);

        zble.setSketch(sketch);

        GadgetsCollections.addTempSketch(cs);
        GadgetsCollections.addParsedNode(ble, zble);
        return new ArrayList<ZCode>(Arrays.asList(zble));
    }

    @Override
    public String toString() {
        return this.booleanLiteralExpr.toString();
    }
}
