package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Hole;
import dsu.pasta.javaparser.gadget.sketch.SketchConstant;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZDoubleLiteralExpr extends ZExpression {
    private DoubleLiteralExpr doubleLiteralExpr;

    public ZDoubleLiteralExpr(ZCode parent, DoubleLiteralExpr doubleLiteralExpr) {
        super(parent);
        this.doubleLiteralExpr = doubleLiteralExpr;
    }

    public static List<ZCode> visit(DoubleLiteralExpr dle, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZDoubleLiteralExpr zdle = new ZDoubleLiteralExpr(parent, dle);
        SketchGadget sketch = new SketchGadget();

        sketch.setRange(dle.getRange().get());
        sketch.setOriginalCode(zdle.toString());
        sketch.setType(zdle.getClass().getSimpleName());

        sketch.addElement(new Hole(dle.toString(), dle.calculateResolvedType(), true, Hole.HoleFrom.constant));

        SketchConstant cs = new SketchConstant(dle.toString(), dle.calculateResolvedType());
        cs.setRange(dle.getRange().get());

        sketch.addMustHave(cs);

        zdle.setSketch(sketch);

        GadgetsCollections.addTempSketch(cs);
        GadgetsCollections.addParsedNode(dle, zdle);
        return new ArrayList<ZCode>(Arrays.asList(zdle));
    }

    @Override
    public String toString() {
        return this.doubleLiteralExpr.toString();
    }
}
