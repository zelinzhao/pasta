package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Hole;
import dsu.pasta.javaparser.gadget.sketch.SketchConstant;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZIntegerLiteralExpr extends ZExpression {
    private IntegerLiteralExpr integerLiteralExpr;

    public ZIntegerLiteralExpr(ZCode parent, IntegerLiteralExpr integerLiteralExpr) {
        super(parent);
        this.integerLiteralExpr = integerLiteralExpr;
    }

    public static List<ZCode> visit(IntegerLiteralExpr ile, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZIntegerLiteralExpr zile = new ZIntegerLiteralExpr(parent, ile);
        SketchGadget sketch = new SketchGadget();

        sketch.setRange(ile.getRange().get());
        sketch.setOriginalCode(zile.toString());
        sketch.setType(zile.getClass().getSimpleName());

        sketch.addElement(new Hole(ile.toString(), ile.calculateResolvedType(), true, Hole.HoleFrom.constant));

        SketchConstant cs = new SketchConstant(ile.toString(), ile.calculateResolvedType());
        cs.setRange(ile.getRange().get());

        sketch.addMustHave(cs);

        zile.setSketch(sketch);

        GadgetsCollections.addTempSketch(cs);
        GadgetsCollections.addParsedNode(ile, zile);
        return new ArrayList<ZCode>(Arrays.asList(zile));
    }

    @Override
    public String toString() {
        return this.integerLiteralExpr.toString();
    }
}
