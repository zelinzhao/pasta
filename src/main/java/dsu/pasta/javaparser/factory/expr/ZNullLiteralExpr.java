package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.expr.NullLiteralExpr;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZNullLiteralExpr extends ZExpression {
    private NullLiteralExpr nullLiteralExpr;

    public ZNullLiteralExpr(ZCode parent, NullLiteralExpr nullLiteralExpr) {
        super(parent);
        this.nullLiteralExpr = nullLiteralExpr;
    }

    public static List<ZCode> visit(NullLiteralExpr nle, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZNullLiteralExpr znle = new ZNullLiteralExpr(parent, nle);
        SketchGadget sketch = new SketchGadget();

        sketch.setRange(nle.getRange().get());
        sketch.setOriginalCode(znle.toString());
        sketch.setType(znle.getClass().getSimpleName());

        sketch.addElement(new Element(nle.toString(), nle.calculateResolvedType()));

        znle.setSketch(sketch);
        GadgetsCollections.addParsedNode(nle, znle);
        return new ArrayList<ZCode>(Arrays.asList(znle));
    }

    @Override
    public String toString() {
        return this.nullLiteralExpr.toString();
    }
}
