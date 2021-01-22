package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.expr.BinaryExpr;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZBinaryExpr extends ZExpression {
    private BinaryExpr binaryExpr;

    public ZBinaryExpr(ZCode parent, BinaryExpr binaryExpr) {
        super(parent);
        this.binaryExpr = binaryExpr;

    }

    public static List<ZCode> visit(BinaryExpr be, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZBinaryExpr zbe = new ZBinaryExpr(parent, be);
        SketchGadget sketch = new SketchGadget();
        sketch.setRange(be.getRange().get());
        sketch.setOriginalCode(zbe.toString());
        sketch.setType(zbe.getClass().getSimpleName());
        // a condition is source
        zbe.isSource = true;
        zbe.nowSource = true;
        //left
        sketch.addElements(be.getLeft().accept(visitor, zbe));
        sketch.addElement(new Element(" "));
        //operator
        sketch.addElement(new Element(be.getOperator().asString()));
        sketch.addElement(new Element(" "));
        //right
        sketch.addElements(be.getRight().accept(visitor, zbe));

        zbe.setSketch(sketch);
        GadgetsCollections.addParsedNode(be, zbe);
        return new ArrayList<ZCode>(Arrays.asList(zbe));
    }

    @Override
    public String toString() {
        return this.binaryExpr.toString();
    }
}
