package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.model.typesystem.ReferenceTypeImpl;
import dsu.pasta.javaparser.factory.analyzer.JavaparserSolver;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.Hole;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZMethodCallExpr extends ZExpression {

    private MethodCallExpr methodCallExpr;

    public ZMethodCallExpr(ZCode parent, MethodCallExpr methodCallExpr) {
        super(parent);
        this.methodCallExpr = methodCallExpr;
    }

    public static List<ZCode> visit(MethodCallExpr mce, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZMethodCallExpr zme = new ZMethodCallExpr(parent, mce);
        SketchGadget sketch = new SketchGadget();

        sketch.setRange(mce.getRange().get());
        sketch.setOriginalCode(zme.toString());
        sketch.setType(zme.getClass().getSimpleName());

        try {
            ResolvedMethodDeclaration methodDec = mce.resolve();
            ResolvedType realReturnType = mce.calculateResolvedType();

            ResolvedType scopeType = null;
            if (mce.getScope().isPresent()) {
                Expression scopeExpr = null;
                ZSimpleName scopeName = null;
                scopeExpr = mce.getScope().get();
                scopeType = scopeExpr.calculateResolvedType();

                zme.nowSource = true;

                // static method
                if ((scopeExpr instanceof NameExpr) && methodDec.isStatic()) {
                    scopeName = new ZSimpleName(zme, scopeExpr.asNameExpr().getName());
                    //static method scope
                    sketch.addElement(new Element(scopeType));
                } else {//not static method
                    sketch.addElements(scopeExpr.accept(visitor, zme));
                }
            } else {
                scopeType = new ReferenceTypeImpl(
                        JavaparserSolver.combinedTypeSolver.solveType(methodDec.declaringType().getQualifiedName()),
                        JavaparserSolver.combinedTypeSolver);
                if (methodDec.isStatic()) {
                    // static scope
                    sketch.addElement(new Element(scopeType));
                } else {
                    Expression scopeExpr = new ThisExpr();
                    mce.setScope(scopeExpr);
                    ZThisExpr zte = new ZThisExpr(zme, (ThisExpr) scopeExpr);
                    // this
                    sketch.addElement(new Hole("this", scopeType, true, Hole.HoleFrom.instance));
                }
            }
            sketch.addElement(new Element("."));
            if (mce.getTypeArguments().isPresent()) {
                // TODO maybe wrong
                // scope.<
                sketch.addElement(new Element("<"));
                zme.nowSource = false;
                sketch.addElements(mce.getTypeArguments().get().accept(visitor, zme));
                // scope.<type parameter>
                sketch.addElement(new Element(">"));
            }
            //name
            ZSimpleName zsn = new ZSimpleName(zme, mce.getName());
            // scope.<>methodname(
            sketch.addElement(new Element(zsn.toString()));
            sketch.addElement(new Element("("));
            //parameters
            zme.nowSource = true;
            sketch.addElements(mce.getArguments().accept(visitor, zme), ",");
            sketch.addElement(new Element(")"));

            if (parent.nowSource) {
                zme.isSource = true;
            }
            zme.setSketch(sketch);
            GadgetsCollections.addParsedNode(mce, zme);
        } catch (Exception e) {
            // TODO
//			System.err.println("Fail to visit " + mce.toString());
        } finally {
            return new ArrayList<ZCode>(Arrays.asList(zme));
        }
    }

    @Override
    public String toString() {
        return this.methodCallExpr.toString();
    }

}