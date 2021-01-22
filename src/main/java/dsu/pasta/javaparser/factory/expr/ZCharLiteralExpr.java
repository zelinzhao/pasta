package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.expr.CharLiteralExpr;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Hole;
import dsu.pasta.javaparser.gadget.sketch.SketchConstant;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZCharLiteralExpr extends ZExpression {
    private CharLiteralExpr charLiteralExpr;

    public ZCharLiteralExpr(ZCode parent, CharLiteralExpr charLiteralExpr) {
        super(parent);
        this.charLiteralExpr = charLiteralExpr;

    }

    public static List<ZCode> visit(CharLiteralExpr cle, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZCharLiteralExpr zcle = new ZCharLiteralExpr(parent, cle);
        SketchGadget sketch = new SketchGadget();
        sketch.setRange(cle.getRange().get());
        sketch.setOriginalCode(zcle.toString());
        sketch.setType(zcle.getClass().getSimpleName());

        sketch.addElement(new Hole(cle.toString(), cle.calculateResolvedType(), true, Hole.HoleFrom.constant));

        SketchConstant cs = new SketchConstant(cle.toString(), cle.calculateResolvedType());
        cs.setRange(cle.getRange().get());

        sketch.addMustHave(cs);

        zcle.setSketch(sketch);

        GadgetsCollections.addTempSketch(cs);
        GadgetsCollections.addParsedNode(cle, zcle);
        return new ArrayList<ZCode>(Arrays.asList(zcle));
    }

    @Override
    public String toString() {
        return this.charLiteralExpr.toString();
    }
}
