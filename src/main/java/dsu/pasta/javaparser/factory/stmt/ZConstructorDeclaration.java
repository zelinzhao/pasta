package dsu.pasta.javaparser.factory.stmt;

import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
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

public class ZConstructorDeclaration extends ZStatement {
    private ConstructorDeclaration constructorDeclaration;

    public ZConstructorDeclaration(ZCode parent, ConstructorDeclaration constructorDeclaration) {
        super(parent);
        this.constructorDeclaration = constructorDeclaration;
        this.sizes = 1; // constructor calling stmt is 1 size
    }

    public static List<ZCode> visit(ConstructorDeclaration n, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZConstructorDeclaration zcd = new ZConstructorDeclaration(parent, n);
        SketchGadget sketch = new SketchGadget();

        sketch.setRange(n.getRange().get());
        sketch.setOriginalCode(zcd.toString());
        sketch.setType(zcd.getClass().getSimpleName());

        ResolvedConstructorDeclaration rcd = n.resolve();
        ResolvedReferenceTypeDeclaration rrtd = rcd.declaringType();
        ReferenceTypeImpl rft = new ReferenceTypeImpl(rrtd, JavaparserSolver.combinedTypeSolver);

        sketch.addElement(new Element(rft));
        sketch.addElement(new Element(" "));
        sketch.addElement(new Hole(rft, false, Hole.HoleFrom.target));
        sketch.addElement(new Element("="));

        List<ZCode> tmp;
        //name
        n.getName().accept(visitor, zcd);
        sketch.addElement(new Element("new "));
        sketch.addElement(new Element(rft));
        sketch.addElement(new Element("("));
        //parameters
        tmp = n.getParameters().accept(visitor, zcd);
        if (tmp != null) {
            for (ZCode p : tmp) {
                p.getSketch().makeAllElementsReplacable(true, Hole.HoleFrom.parameter);
            }
            sketch.addElements(tmp, ",");
        }
        sketch.addElement(new Element(");\n"));
        //body
        List<ZCode> body = n.getBody().accept(visitor, zcd);
        //type parameters
        n.getTypeParameters().accept(visitor, zcd);

        Context context = new Context(rft.describe(), zcd.toString(), n.getRange().get());
        addContextTo(context, body);

        GadgetsCollections.addTempSketch(sketch);
        GadgetsCollections.addTempContext(context);
        GadgetsCollections.addParsedNode(n, zcd);
        return new ArrayList<ZCode>(Arrays.asList(zcd));
    }

    @Override
    public String toString() {
        return this.constructorDeclaration.getDeclarationAsString(false, false);
    }
}
