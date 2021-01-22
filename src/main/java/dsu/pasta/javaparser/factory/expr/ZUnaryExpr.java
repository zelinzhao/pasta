package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.expr.UnaryExpr;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZUnaryExpr extends ZExpression {
    private UnaryExpr unaryExpr;

    public ZUnaryExpr(ZCode parent, UnaryExpr unaryExpr) {
        super(parent);
        this.unaryExpr = unaryExpr;
    }

    public static List<ZCode> visit(UnaryExpr ue, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZUnaryExpr zue = new ZUnaryExpr(parent, ue);
        SketchGadget sketch = new SketchGadget();

        sketch.setRange(ue.getRange().get());
        sketch.setOriginalCode(zue.toString());
        sketch.setType(zue.getClass().getSimpleName());

        if (parent.nowSource) {
            zue.isSource = true;
            zue.nowSource = true;
        }
        List<ZCode> tmp = ue.getExpression().accept(visitor, zue);
        if (tmp != null) {
            if (ue.isPrefix()) {
                sketch.addElement(new Element(ue.getOperator().asString()));
                sketch.addElements(tmp, "");
            } else {
                sketch.addElements(tmp, "");
                sketch.addElement(new Element(ue.getOperator().asString()));
            }
        }

        zue.setSketch(sketch);

        GadgetsCollections.addParsedNode(ue, zue);
        return new ArrayList<ZCode>(Arrays.asList(zue));
    }

    @Override
    public String toString() {
        return this.unaryExpr.toString();
    }

}
