package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.expr.LongLiteralExpr;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Hole;
import dsu.pasta.javaparser.gadget.sketch.SketchConstant;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZLongLiteralExpr extends ZExpression {
    private LongLiteralExpr longLiteralExpr;

    public ZLongLiteralExpr(ZCode parent, LongLiteralExpr longLiteralExpr) {
        super(parent);
        this.longLiteralExpr = longLiteralExpr;
    }

    public static List<ZCode> visit(LongLiteralExpr lle, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZLongLiteralExpr zlle = new ZLongLiteralExpr(parent, lle);
        SketchGadget sketch = new SketchGadget();

        sketch.setRange(lle.getRange().get());
        sketch.setOriginalCode(zlle.toString());
        sketch.setType(zlle.getClass().getSimpleName());

        sketch.addElement(new Hole(lle.toString(), lle.calculateResolvedType(), true, Hole.HoleFrom.constant));

        SketchConstant cs = new SketchConstant(lle.toString(), lle.calculateResolvedType());
        cs.setRange(lle.getRange().get());

        sketch.addMustHave(cs);

        zlle.setSketch(sketch);

        GadgetsCollections.addTempSketch(cs);
        GadgetsCollections.addParsedNode(lle, zlle);
        return new ArrayList<ZCode>(Arrays.asList(zlle));
    }

    @Override
    public String toString() {
        return this.longLiteralExpr.toString();
    }
}
