package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZVariableDeclarationExpr extends ZExpression {
    private VariableDeclarationExpr variableDeclarationExpr;

    public ZVariableDeclarationExpr(ZCode parent, VariableDeclarationExpr variableDec) {
        super(parent);
        this.variableDeclarationExpr = variableDec;
    }

    public static List<ZCode> visit(VariableDeclarationExpr vde, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZVariableDeclarationExpr zvde = new ZVariableDeclarationExpr(parent, vde);
        SketchGadget sketch = new SketchGadget();

        sketch.setRange(vde.getRange().get());
        sketch.setOriginalCode(zvde.toString());
        sketch.setType(zvde.getClass().getSimpleName());

        try {
            zvde.nowSource = false;
            sketch.addElements(vde.getVariables().accept(visitor, zvde));

            zvde.setSketch(sketch);
            GadgetsCollections.addParsedNode(vde, zvde);
        } catch (Exception e) {
            //todo there is a bug here, may need fix
//			e.printStackTrace();
        } finally {
            return new ArrayList<ZCode>(Arrays.asList(zvde));
        }
    }

    @Override
    public String toString() {
        return this.variableDeclarationExpr.toString();
    }
}
