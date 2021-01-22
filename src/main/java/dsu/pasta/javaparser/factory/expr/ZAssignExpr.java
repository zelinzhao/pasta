package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZAssignExpr extends ZExpression {
    private AssignExpr assignExpr;

    public ZAssignExpr(ZCode parent, AssignExpr assignExpr) {
        super(parent);
        this.assignExpr = assignExpr;
    }

    public static List<ZCode> visit(AssignExpr ae, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZAssignExpr zae = new ZAssignExpr(parent, ae);
        SketchGadget sketch = new SketchGadget();
        sketch.setRange(ae.getRange().get());
        sketch.setOriginalCode(zae.toString());
        sketch.setType(zae.getClass().getSimpleName());
        try {
            Expression targetExpr = ae.getTarget();
            Expression valueExpr = ae.getValue();
            //target expr
            zae.nowSource = false;
            sketch.addElements(targetExpr.accept(visitor, zae));
            //operator
            sketch.addElement(new Element(ae.getOperator().asString()));
            //value
            zae.nowSource = true;
            sketch.addElements(valueExpr.accept(visitor, zae));

            zae.setSketch(sketch);

            GadgetsCollections.addParsedNode(ae, zae);
        } catch (Exception e) {
        } finally {
            return new ArrayList<ZCode>(Arrays.asList(zae));
        }
    }

    public AssignExpr getAssignExpr() {
        return this.assignExpr;
    }

    @Override
    public String toString() {
        return this.assignExpr.toString();
    }
}
