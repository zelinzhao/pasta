package dsu.pasta.javaparser.factory.stmt;

import com.github.javaparser.ast.stmt.ThrowStmt;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZThrowStmt extends ZStatement {
    private ThrowStmt throwStmt;

    public ZThrowStmt(ZCode parent, ThrowStmt throwStmt) {
        super(parent);
        this.throwStmt = throwStmt;
    }

    public static List<ZCode> visit(ThrowStmt n, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZThrowStmt zts = new ZThrowStmt(parent, n);
        SketchGadget sketch = new SketchGadget();

        sketch.setRange(n.getRange().get());
        sketch.setOriginalCode(zts.toString());
        sketch.setType(zts.getClass().getSimpleName());

        sketch.addElement(new Element("throw "));
        zts.nowSource = true;
        sketch.addElements(n.getExpression().accept(visitor, zts));
        sketch.addElement(new Element(";\n"));

        zts.setSketch(sketch);
        GadgetsCollections.addParsedNode(n, zts);
        return new ArrayList<ZCode>(Arrays.asList(zts));
    }

    @Override
    public String toString() {
        return this.throwStmt.toString();
    }

}
