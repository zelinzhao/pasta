package dsu.pasta.javaparser.factory.stmt;

import com.github.javaparser.ast.stmt.ContinueStmt;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZContinueStmt extends ZStatement {

    public ZContinueStmt(ZCode parent) {
        super(parent);
    }

    public static List<ZCode> visit(ContinueStmt bs, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZContinueStmt zbs = new ZContinueStmt(parent);
        SketchGadget sketch = new SketchGadget();
        sketch.setRange(bs.getRange().get());
        sketch.setOriginalCode(zbs.toString());
        sketch.setType(zbs.getClass().getSimpleName());

        sketch.addElement(new Element("continue;\n"));

        zbs.setSketch(sketch);
        return new ArrayList<ZCode>(Arrays.asList(zbs));
    }

    public String toString() {
        return "continue;";
    }

}
