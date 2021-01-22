package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.model.typesystem.ReferenceTypeImpl;
import dsu.pasta.javaparser.factory.analyzer.JavaparserSolver;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZObjectCreationExpr extends ZExpression {
    private ObjectCreationExpr objectCreatExpr;

    public ZObjectCreationExpr(ZCode parent, ObjectCreationExpr objCreExpr) {
        super(parent);
        this.objectCreatExpr = objCreExpr;
    }

    public static List<ZCode> visit(ObjectCreationExpr oce, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZObjectCreationExpr zoce = new ZObjectCreationExpr(parent, oce);
        SketchGadget sketch = new SketchGadget();

        sketch.setRange(oce.getRange().get());
        sketch.setOriginalCode(zoce.toString());
        sketch.setType(zoce.getClass().getSimpleName());

        try {
            ResolvedConstructorDeclaration consDec = oce.resolve();
            ResolvedReferenceTypeDeclaration rrtd = consDec.declaringType();
            ReferenceTypeImpl rt = new ReferenceTypeImpl(rrtd, JavaparserSolver.combinedTypeSolver);
            //scope
            if (oce.getScope().isPresent()) {
                zoce.nowSource = true;
                sketch.addElements(oce.getScope().get().accept(visitor, zoce));
                sketch.addElement(new Element("."));
            }
            //new
            sketch.addElement(new Element("new "));
            zoce.nowSource = true;
            sketch.addElements(oce.getType().accept(visitor, zoce));
            //type arguments
            if (oce.getTypeArguments().isPresent()) {
                sketch.addElement(new Element("<"));
                sketch.addElements(oce.getTypeArguments().get().accept(visitor, zoce));
                sketch.addElement(new Element(">"));
            }
            //argument
            zoce.nowSource = true;
            sketch.addElement(new Element("("));
            sketch.addElements(oce.getArguments().accept(visitor, zoce), ",");
            sketch.addElement(new Element(")"));
            if (oce.getAnonymousClassBody().isPresent()) {
                zoce.nowSource = false;
                oce.getAnonymousClassBody().get().accept(visitor, zoce);
            }
            zoce.setSketch(sketch);
            GadgetsCollections.addParsedNode(oce, zoce);
        } catch (Exception e) {
        } finally {
            return new ArrayList<ZCode>(Arrays.asList(zoce));
        }
    }

    @Override
    public String toString() {
        return this.objectCreatExpr.toString();
    }
}
