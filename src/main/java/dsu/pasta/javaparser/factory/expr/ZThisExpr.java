package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.expr.ThisExpr;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZThisExpr extends ZExpression {
    private ThisExpr thisExpr;

    public ZThisExpr(ZCode parent, ThisExpr thisExpr) {
        super(parent);
        this.thisExpr = thisExpr;
    }

    public static List<ZCode> visit(ThisExpr te, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZThisExpr zte = new ZThisExpr(parent, te);
        SketchGadget sketch = new SketchGadget();

        try {
            sketch.setRange(te.getRange().get());
        } catch (Exception e) {
            sketch.setRange(parent.getSketch().getRange());
        }
        sketch.setOriginalCode(zte.toString());
        sketch.setType(zte.getClass().getSimpleName());

        if (te.getTypeName().isPresent()) {
            sketch.addElements(te.getTypeName().get().accept(visitor, zte));
            sketch.addElement(new Element("."));
        }
//		sketch.addElement(new Element("this"));

        zte.setSketch(sketch);

        GadgetsCollections.addParsedNode(te, zte);
        return new ArrayList<ZCode>(Arrays.asList(zte));
    }

    @Override
    public String toString() {
        return this.thisExpr.toString();
    }

}
