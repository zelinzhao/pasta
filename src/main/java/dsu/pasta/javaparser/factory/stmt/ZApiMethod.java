package dsu.pasta.javaparser.factory.stmt;

import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import dsu.pasta.javaparser.factory.analyzer.JavaparserSolver;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.Hole;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.List;

/**
 * Bytecode api methods.
 */
public class ZApiMethod extends ZStatement {
    private MethodUsage methodUsage;
    private String oriString;

    public ZApiMethod(ZCode parent, MethodUsage methodUsage) {
        super(parent);
        this.methodUsage = methodUsage;
        this.sizes = 1;
    }

    public static void visit(MethodUsage mu, ResolvedType inType) {
        ZApiMethod zam = new ZApiMethod(null, mu);
        SketchGadget sketch = new SketchGadget();
        sketch.setType(zam.getClass().getSimpleName());

        ResolvedMethodDeclaration rmd = mu.getDeclaration();
        if (rmd instanceof JavaParserMethodDeclaration) {
            try {
                List<ZCode> child = JavaparserSolver.visitor.visit(((JavaParserMethodDeclaration) rmd).getWrappedNode(),
                        zam);
            } catch (Exception e) {
            }
        }
        // resolve target type
        if (mu.returnType().isVoid()) {
        } else if (mu.returnType().isPrimitive()) {
            sketch.addElement(new Element(mu.returnType()));
            sketch.addElement(new Element(" "));
            sketch.addElement(new Hole(mu.returnType(), false, Hole.HoleFrom.target));
            sketch.addElement(new Element("="));
        } else {
            sketch.addElement(new Element(mu.returnType().asReferenceType()));
            sketch.addElement(new Element(" "));
            sketch.addElement(new Hole(mu.returnType().asReferenceType(), false, Hole.HoleFrom.target));
            sketch.addElement(new Element("="));
        }
        zam.oriString = mu.returnType().describe() + " v = ";

        // resolve source type
        if (!rmd.isStatic()) {
            sketch.addElement(new Hole(inType, true, Hole.HoleFrom.definition));
            sketch.addElement(new Element("."));
        } else {
            sketch.addElement(new Element(inType));
            sketch.addElement(new Element("."));
        }
        sketch.addElement(new Element(mu.getName()));
        sketch.addElement(new Element("("));

        zam.oriString += inType.describe() + "." + mu.getName() + "(";

        boolean sp = false;
        for (ResolvedType rt : mu.getParamTypes()) {
            ResolvedType paraType = inType.asReferenceType().useThisTypeParametersOnTheGivenType(rt);
            if (sp) {
                sketch.addElement(new Element(","));
                zam.oriString += ",";
            }
            sketch.addElement(new Hole(paraType, true, Hole.HoleFrom.parameter));
            zam.oriString += paraType.describe();
            if (!sp)
                sp = true;
        }
        sketch.addElement(new Element(");\n"));
        zam.oriString += ");";
        sketch.setOriginalCode(zam.toString());
        zam.setSketch(sketch);
        GadgetsCollections.addTempSketch(sketch);
    }

    @Override
    public String toString() {
        if (this.oriString != null)
            return this.oriString;
        else
            return this.methodUsage.toString();
    }
}
