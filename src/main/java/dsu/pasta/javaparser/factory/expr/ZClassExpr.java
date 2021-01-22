package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.expr.ClassExpr;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZClassExpr extends ZExpression {
    private ClassExpr classExpr;

    public ZClassExpr(ZCode parent, ClassExpr cla) {
        super(parent);
        this.classExpr = cla;
    }

    public static List<ZCode> visit(ClassExpr ce, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZClassExpr zce = new ZClassExpr(parent, ce);
        SketchGadget sketch = new SketchGadget();

        sketch.setRange(ce.getRange().get());
        sketch.setOriginalCode(zce.toString());
        sketch.setType(zce.getClass().getSimpleName());

        sketch.addElements(ce.getType().accept(visitor, zce));
        sketch.addElement(new Element(".class"));

        zce.setSketch(sketch);
        GadgetsCollections.addParsedNode(ce, zce);
        return new ArrayList<ZCode>(Arrays.asList(zce));
    }

    public String toString() {
        return this.classExpr.toString();
    }

}
