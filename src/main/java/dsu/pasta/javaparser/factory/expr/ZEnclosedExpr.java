package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.expr.EnclosedExpr;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZEnclosedExpr extends ZExpression {
    private EnclosedExpr enclosedExpr;

    public ZEnclosedExpr(ZCode parent, EnclosedExpr ee) {
        super(parent);
        this.enclosedExpr = ee;
    }

    public static List<ZCode> visit(EnclosedExpr ee, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZEnclosedExpr zee = new ZEnclosedExpr(parent, ee);
        SketchGadget sketch = new SketchGadget();

        sketch.setRange(ee.getRange().get());
        sketch.setOriginalCode(zee.toString());
        sketch.setType(zee.getClass().getSimpleName());

        sketch.addElement(new Element("("));
        zee.nowSource = parent.nowSource;
        sketch.addElements(ee.getInner().accept(visitor, zee));
        sketch.addElement(new Element(")"));

        zee.setSketch(sketch);
        GadgetsCollections.addParsedNode(ee, zee);
        return new ArrayList<ZCode>(Arrays.asList(zee));
    }

    @Override
    public String toString() {
        return this.enclosedExpr.toString();
    }
}
