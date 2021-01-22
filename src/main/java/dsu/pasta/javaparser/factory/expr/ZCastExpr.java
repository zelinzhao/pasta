package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.expr.CastExpr;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZCastExpr extends ZExpression {
    private CastExpr castExpr;

    public ZCastExpr(ZCode parent, CastExpr castExpr) {
        super(parent);
        this.castExpr = castExpr;
    }

    public static List<ZCode> visit(CastExpr ce, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZCastExpr zce = new ZCastExpr(parent, ce);
        SketchGadget sketch = new SketchGadget();
        sketch.setRange(ce.getRange().get());
        sketch.setOriginalCode(zce.toString());
        sketch.setType(zce.getClass().getSimpleName());

        //type
        zce.nowSource = false;
        ce.getType().accept(visitor, zce);
        sketch.addElement(new Element("(("));
        sketch.addElement(new Element(ce.getType().resolve()));
        sketch.addElement(new Element(")"));
        //expression
        zce.nowSource = true;
        sketch.addElements(ce.getExpression().accept(visitor, zce));
        sketch.addElement(new Element(")"));

        zce.setSketch(sketch);
        GadgetsCollections.addParsedNode(ce, zce);
        return new ArrayList<ZCode>(Arrays.asList(zce));
    }

    @Override
    public String toString() {
        return this.castExpr.toString();
    }
}
