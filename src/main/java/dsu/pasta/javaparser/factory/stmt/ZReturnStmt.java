package dsu.pasta.javaparser.factory.stmt;

import com.github.javaparser.ast.stmt.ReturnStmt;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZReturnStmt extends ZStatement {
    private ReturnStmt returnStmt;

    public ZReturnStmt(ZCode parent, ReturnStmt returnStmt) {
        super(parent);
        this.returnStmt = returnStmt;
    }

    public static List<ZCode> visit(ReturnStmt rs, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZReturnStmt zrs = new ZReturnStmt(parent, rs);
        SketchGadget sketch = new SketchGadget();

        sketch.setRange(rs.getRange().get());
        sketch.setOriginalCode(zrs.toString());
        sketch.setType(zrs.getClass().getSimpleName());

        if (rs.getExpression().isPresent()) {
            zrs.nowSource = true;
            sketch.addElements(rs.getExpression().get().accept(visitor, zrs));
        }
        sketch.addElement(new Element(";\n"));

        zrs.setSketch(sketch);
        GadgetsCollections.addTempSketch(sketch);
        GadgetsCollections.addParsedNode(rs, zrs);
        return new ArrayList<ZCode>(Arrays.asList(zrs));
    }

    @Override
    public String toString() {
        return this.returnStmt.toString();
    }
}
