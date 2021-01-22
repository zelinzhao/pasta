package dsu.pasta.javaparser.factory.stmt;

import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.Hole;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

public class ZApiConstructor extends ZStatement {
    private ResolvedConstructorDeclaration constructor;
    private String oriString;

    public ZApiConstructor(ZCode parent, ResolvedConstructorDeclaration cons) {
        super(parent);
        this.constructor = cons;
        this.sizes = 1;
    }

    public static void visit(ResolvedConstructorDeclaration cons, ResolvedType inType) {
        ZApiConstructor zac = new ZApiConstructor(null, cons);
        SketchGadget sketch = new SketchGadget();

        sketch.setType(zac.getClass().getSimpleName());

        sketch.addElement(new Element(inType));
        sketch.addElement(new Element(" "));
        sketch.addElement(new Hole(inType, false, Hole.HoleFrom.target));
        sketch.addElement(new Element("="));
        sketch.addElement(new Element("new "));
        sketch.addElement(new Element(inType));
        sketch.addElement(new Element("("));

        zac.oriString = inType.describe() + " v = new " + inType.describe() + "(";
        // resolve parameters
        boolean sp = false;
        for (int i = 0; i < cons.getNumberOfParams(); i++) {
            ResolvedParameterDeclaration para = null;
            try {
                para = cons.getParam(i);
            } catch (java.lang.ArrayIndexOutOfBoundsException e) {
                //todo sometimes, the cons's parameters number is not 0, but can't call grtParam to obtain parameters.
                return;
            }
            ResolvedType paraType = inType.asReferenceType().useThisTypeParametersOnTheGivenType(para.getType());
            if (sp) {
                sketch.addElement(new Element(","));
                zac.oriString += ",";
            }
            sketch.addElement(new Hole(paraType, true, Hole.HoleFrom.parameter));
            zac.oriString += paraType.describe();
            if (!sp)
                sp = true;
        }
        sketch.addElement(new Element(");\n"));
        zac.oriString += ");";

        sketch.setOriginalCode(zac.toString());

        zac.setSketch(sketch);
        GadgetsCollections.addTempSketch(sketch);
    }

    @Override
    public String toString() {
        if (oriString != null)
            return oriString;
        else
            return this.constructor.toString();
    }

}
