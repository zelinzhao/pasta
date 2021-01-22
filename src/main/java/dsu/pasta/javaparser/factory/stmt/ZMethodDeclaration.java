package dsu.pasta.javaparser.factory.stmt;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.model.typesystem.ReferenceTypeImpl;
import dsu.pasta.javaparser.factory.analyzer.JavaparserSolver;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Context;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.Hole;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Create special method calling stmt from a method declaration.
 */
public class ZMethodDeclaration extends ZStatement {
    private MethodDeclaration methodDeclaration;

    public ZMethodDeclaration(ZCode parent, MethodDeclaration methodDeclaration) {
        super(parent);
        this.methodDeclaration = methodDeclaration;
        this.sizes = 1; // method calling stmt is 1 size
    }

    public static List<ZCode> visit(MethodDeclaration n, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZMethodDeclaration zmd = new ZMethodDeclaration(parent, n);
        SketchGadget sketch = new SketchGadget();

        sketch.setRange(n.getRange().get());
        sketch.setOriginalCode(zmd.toString());
        sketch.setType(zmd.getClass().getSimpleName());

        ResolvedMethodDeclaration rmd = n.resolve();
        ResolvedReferenceTypeDeclaration rrtd = rmd.declaringType();
        ReferenceTypeImpl rt = new ReferenceTypeImpl(rrtd, JavaparserSolver.combinedTypeSolver);

        List<ZCode> tmp;
        if (rmd.getReturnType().isVoid()) {
        } else if (rmd.getReturnType().isPrimitive()) {
            sketch.addElement(new Element(rmd.getReturnType()));
            sketch.addElement(new Element(" "));
            sketch.addElement(new Hole(rmd.getReturnType(), false, Hole.HoleFrom.target));
            sketch.addElement(new Element("="));
        } else {
            sketch.addElement(new Element(rmd.getReturnType()));
            sketch.addElement(new Element(" "));
            sketch.addElement(new Hole(rmd.getReturnType(), false, Hole.HoleFrom.target));
            sketch.addElement(new Element("="));
        }
        if (rmd.isStatic()) {
            sketch.addElement(new Element(rt));
            sketch.addElement(new Element("."));
        } else {
            sketch.addElement(new Hole(rt, true, Hole.HoleFrom.definition));
            sketch.addElement(new Element("."));
        }
        //type
        n.getType().accept(visitor, zmd);
        //name
        n.getName().accept(visitor, zmd);
        sketch.addElement(new Element(n.getNameAsString()));
        sketch.addElement(new Element("("));
        //parameters
        tmp = n.getParameters().accept(visitor, zmd);
        if (tmp != null) {
            for (ZCode p : tmp) {
                p.getSketch().makeAllElementsReplacable(true, Hole.HoleFrom.parameter);
            }
            sketch.addElements(tmp, ",");
        }
        sketch.addElement(new Element(");\n"));
        //type parameters
        n.getTypeParameters().accept(visitor, zmd);
        //body
        List<ZCode> body = new ArrayList<>();
        if (n.getBody().isPresent()) {
            body = n.getBody().get().accept(visitor, zmd);
        }
        Context context = new Context(rt.describe(), zmd.toString(), n.getRange().get());
        addContextTo(context, body);

        GadgetsCollections.addParsedNode(n, zmd);
        GadgetsCollections.addTempSketch(sketch);
        GadgetsCollections.addTempContext(context);

        return new ArrayList<ZCode>(Arrays.asList(zmd));
    }

    @Override
    public String toString() {
        return this.methodDeclaration.getDeclarationAsString(false, false);
    }
}
