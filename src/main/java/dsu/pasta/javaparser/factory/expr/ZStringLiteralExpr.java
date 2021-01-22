package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.expr.StringLiteralExpr;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Hole;
import dsu.pasta.javaparser.gadget.sketch.SketchConstant;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZStringLiteralExpr extends ZExpression {
    private StringLiteralExpr stringLiteralExpr;

    public ZStringLiteralExpr(ZCode parent, StringLiteralExpr stringLiteralExpr) {
        super(parent);
        this.stringLiteralExpr = stringLiteralExpr;
    }

    public static List<ZCode> visit(StringLiteralExpr sle, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZStringLiteralExpr zsle = new ZStringLiteralExpr(parent, sle);
        SketchGadget sketch = new SketchGadget();

        sketch.setRange(sle.getRange().get());
        sketch.setOriginalCode(zsle.toString());
        sketch.setType(zsle.getClass().getSimpleName());

        sketch.addElement(new Hole(sle.toString(), sle.calculateResolvedType(), true, Hole.HoleFrom.constant));

        SketchConstant cs = new SketchConstant(sle.toString(), sle.calculateResolvedType());
        cs.setRange(sle.getRange().get());

        sketch.addMustHave(cs);
        zsle.setSketch(sketch);

        GadgetsCollections.addTempSketch(cs);
        GadgetsCollections.addParsedNode(sle, zsle);
        return new ArrayList<ZCode>(Arrays.asList(zsle));
    }

    @Override
    public String toString() {
        return this.stringLiteralExpr.toString();
    }
}
