package dsu.pasta.javaparser.factory.stmt;

import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import dsu.pasta.javaparser.factory.analyzer.JavaparserSolver;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.Hole;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

/**
 * Cast super class to sub class;
 * Sub class to super class is directly assignable.
 */
public class ZCastStmt extends ZStatement {
    private ResolvedType superType;
    private ResolvedType sonType;
    private String cast;

    public ZCastStmt(ZCode parent) {
        super(parent);
    }

    public ZCastStmt(ResolvedType superType, ResolvedType sonType) {
        this(null);
        this.superType = superType;
        this.sonType = sonType;
    }

    public static void visitSuperToSubCast(String subClass) {
        ResolvedType sonType = JavaparserSolver.getType(subClass);
        if (sonType == null)
            return;
        if (sonType.isPrimitive() || sonType.isNull()
                || sonType.isVoid() || sonType.isArray()
                || !sonType.isReferenceType())
            return;
        try {
            for (ResolvedReferenceType superType : sonType.asReferenceType().getAllAncestors()) {
                if (superType.describe().equals(Object.class.getName()))
                    continue;
                ZCastStmt zcs = new ZCastStmt(superType, sonType);
                SketchGadget sketch = new SketchGadget();

                sketch.setType(zcs.getClass().getSimpleName());

                sketch.addElement(new Element(sonType));
                sketch.addElement(new Element(" "));
                sketch.addElement(new Hole(sonType, false, Hole.HoleFrom.target));
                sketch.addElement(new Element("="));
                sketch.addElement(new Element("(" + sonType.describe() + ")"));
                sketch.addElement(new Hole(superType, true, Hole.HoleFrom.definition));
                sketch.addElement(new Element(";\n"));

                zcs.cast = sonType.describe() + "=(" + sonType.describe() + ")" + superType.describe() + ";";

                sketch.setOriginalCode(zcs.toString());
                zcs.setSketch(sketch);

                GadgetsCollections.addTempSketch(sketch);
            }
        } catch (Exception e) {

        }
    }

    @Override
    public String toString() {
        return cast;
    }
}
