package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.expr.InstanceOfExpr;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZInstanceOfExpr extends ZExpression {
    private InstanceOfExpr instanceExpr;

    public ZInstanceOfExpr(ZCode parent, InstanceOfExpr ie) {
        super(parent);
        this.instanceExpr = ie;
    }

    public static List<ZCode> visit(InstanceOfExpr ie, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZInstanceOfExpr zie = new ZInstanceOfExpr(parent, ie);
        SketchGadget sketch = new SketchGadget();

        sketch.setRange(ie.getRange().get());
        sketch.setOriginalCode(zie.toString());
        sketch.setType(zie.getClass().getSimpleName());

        zie.nowSource = true;
        //expression
        sketch.addElements(ie.getExpression().accept(visitor, zie));
        //instanceof
        sketch.addElement(new Element(" "));
        sketch.addElement(new Element("instanceof"));
        sketch.addElement(new Element(" "));
        //type
        sketch.addElements(ie.getType().accept(visitor, zie));

        zie.setSketch(sketch);
        GadgetsCollections.addParsedNode(ie, zie);
        return new ArrayList<ZCode>(Arrays.asList(zie));

    }

    @Override
    public String toString() {
        return this.instanceExpr.toString();
    }
}
