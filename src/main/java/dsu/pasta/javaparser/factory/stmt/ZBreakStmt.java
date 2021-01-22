package dsu.pasta.javaparser.factory.stmt;

import com.github.javaparser.ast.stmt.BreakStmt;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZBreakStmt extends ZStatement {
    public ZBreakStmt(ZCode parent) {
        super(parent);
    }

    public static List<ZCode> visit(BreakStmt bs, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZBreakStmt zbs = new ZBreakStmt(parent);
        SketchGadget sketch = new SketchGadget();

        sketch.setRange(bs.getRange().get());
        sketch.setOriginalCode(zbs.toString());
        sketch.setType(zbs.getClass().getSimpleName());

        sketch.addElement(new Element("break;\n"));

        zbs.setSketch(sketch);
        return new ArrayList<ZCode>(Arrays.asList(zbs));
    }

    public String toString() {
        return "break;";
    }
}
