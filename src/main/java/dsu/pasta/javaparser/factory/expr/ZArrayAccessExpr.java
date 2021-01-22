package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.expr.ArrayAccessExpr;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZArrayAccessExpr extends ZExpression {
    private ArrayAccessExpr arrayAccessExpr;

    public ZArrayAccessExpr(ZCode parent, ArrayAccessExpr aae) {
        super(parent);
        this.arrayAccessExpr = aae;
    }

    public static List<ZCode> visit(ArrayAccessExpr aae, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZArrayAccessExpr zaae = new ZArrayAccessExpr(parent, aae);
        SketchGadget sketch = new SketchGadget();
        sketch.setRange(aae.getRange().get());
        sketch.setOriginalCode(zaae.toString());
        sketch.setType(zaae.getClass().getSimpleName());

        //name
        zaae.nowSource = parent.nowSource;
        sketch.addElements(aae.getName().accept(visitor, zaae));
        //index
        sketch.addElement(new Element("["));
        zaae.nowSource = true;
        sketch.addElements(aae.getIndex().accept(visitor, zaae));
        sketch.addElement(new Element("]"));

        zaae.setSketch(sketch);
        GadgetsCollections.addParsedNode(aae, zaae);
        return new ArrayList<ZCode>(Arrays.asList(zaae));
    }

    @Override
    public String toString() {
        return this.arrayAccessExpr.toString();
    }

}
