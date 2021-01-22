package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.expr.ConditionalExpr;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;
import dsu.pasta.javaparser.gadget.sketch.SketchIfCondition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZConditionalExpr extends ZExpression {
    private ConditionalExpr conditionalExpr;

    public ZConditionalExpr(ZCode parent, ConditionalExpr ce) {
        super(parent);
        this.conditionalExpr = ce;
    }

    public static List<ZCode> visit(ConditionalExpr ce, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZConditionalExpr zce = new ZConditionalExpr(parent, ce);
        SketchGadget sketch = new SketchGadget();

        sketch.setRange(ce.getRange().get());
        sketch.setOriginalCode(zce.toString());
        sketch.setType(zce.getClass().getSimpleName());

        SketchIfCondition ics = new SketchIfCondition();

        //condition
        zce.nowSource = true;
        List<ZCode> tmp = ce.getCondition().accept(visitor, zce);
        sketch.addElements(tmp);
        ics.addElements(tmp);
        ics.setRange(ce.getCondition().getRange().get());
        ics.setOriginalCode(ce.getCondition().toString());
        ics.setType(zce.getClass().getSimpleName());

        GadgetsCollections.addTempSketch(ics);
        //?
        sketch.addElement(new Element("?"));
        sketch.addElements(ce.getThenExpr().accept(visitor, zce));
        //:
        sketch.addElement(new Element(":"));
        sketch.addElements(ce.getElseExpr().accept(visitor, zce));

        zce.setSketch(sketch);
        GadgetsCollections.addParsedNode(ce, zce);
        return new ArrayList<ZCode>(Arrays.asList(zce));
    }

    @Override
    public String toString() {
        return this.conditionalExpr.toString();
    }
}
