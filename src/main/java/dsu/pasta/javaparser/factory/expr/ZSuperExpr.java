package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.expr.SuperExpr;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZSuperExpr extends ZExpression {
    private SuperExpr superExpr;

    public ZSuperExpr(ZCode parent, SuperExpr superExpr) {
        super(parent);
        this.superExpr = superExpr;
    }

    public static List<ZCode> visit(SuperExpr se, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZSuperExpr zse = new ZSuperExpr(parent, se);
        SketchGadget sketch = new SketchGadget();

        sketch.setRange(se.getRange().get());
        sketch.setOriginalCode(zse.toString());
        sketch.setType(zse.getClass().getSimpleName());

        if (se.getTypeName().isPresent()) {
            sketch.addElements(se.getTypeName().get().accept(visitor, zse));
            sketch.addElement(new Element("."));
        }
        sketch.addElement(new Element("super"));
        zse.setSketch(sketch);
        GadgetsCollections.addParsedNode(se, zse);
        return new ArrayList<ZCode>(Arrays.asList(zse));
    }

    @Override
    public String toString() {
        return this.superExpr.toString();
    }
}
