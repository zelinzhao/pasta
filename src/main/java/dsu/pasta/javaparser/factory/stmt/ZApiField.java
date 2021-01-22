package dsu.pasta.javaparser.factory.stmt;

import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.Hole;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

/**
 * Byte factory field access
 */
public class ZApiField extends ZStatement {
    private ResolvedFieldDeclaration rfd;
    private String oriString;

    public ZApiField(ZCode parent, ResolvedFieldDeclaration rfd) {
        super(parent);
        this.rfd = rfd;
        this.sizes = 1;
    }

    public static void visit(ResolvedFieldDeclaration fd, ResolvedType inType) {
        ZApiField zaf = new ZApiField(null, fd);
        SketchGadget sketch = new SketchGadget();
        sketch.setType(zaf.getClass().getSimpleName());

        ResolvedType fieldType = fd.getType();
        String fieldName = fd.getName();

        sketch.addElement(new Element(fieldType));
        sketch.addElement(new Element(" "));
        sketch.addElement(new Hole(fieldType, false, Hole.HoleFrom.target));
        sketch.addElement(new Element("="));

        zaf.oriString = fieldType.describe() + " v = ";
        if (fd.isStatic()) {
            sketch.addElement(new Element(inType));
        } else {
            sketch.addElement(new Hole(inType, true, Hole.HoleFrom.definition));
        }
        sketch.addElement(new Element("."));
        sketch.addElement(new Element(fieldName));
        sketch.addElement(new Element(";\n"));

        zaf.oriString += inType.describe() + "." + fieldName + ";";

        sketch.setOriginalCode(zaf.toString());

        zaf.setSketch(sketch);
        GadgetsCollections.addTempSketch(sketch);
    }

    @Override
    public String toString() {
        if (oriString != null)
            return this.oriString;
        else
            return this.rfd.toString();
    }
}
