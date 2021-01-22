package dsu.pasta.javaparser.factory.stmt;

import com.github.javaparser.ast.stmt.ExpressionStmt;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZExpressionStmt extends ZStatement {

    private ExpressionStmt exprStmt = null;

    public ZExpressionStmt(ZCode parent, ExpressionStmt exprStmt) {
        super(parent);
        this.exprStmt = exprStmt;
    }

    public static List<ZCode> visit(ExpressionStmt es, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZExpressionStmt zes = new ZExpressionStmt(parent, es);
        SketchGadget sketch = new SketchGadget();

        sketch.setRange(es.getRange().get());
        sketch.setOriginalCode(zes.toString());
        sketch.setType(zes.getClass().getSimpleName());
        //expression
        sketch.addElements(es.getExpression().accept(visitor, zes));
        sketch.addElement(new Element(";\n"));

        zes.setSketch(sketch);

        GadgetsCollections.addParsedNode(es, zes);
        GadgetsCollections.addTempSketch(sketch);
        return new ArrayList<ZCode>(Arrays.asList(zes));
    }

    @Override
    public String toString() {
        return this.exprStmt.toString();
    }
}
