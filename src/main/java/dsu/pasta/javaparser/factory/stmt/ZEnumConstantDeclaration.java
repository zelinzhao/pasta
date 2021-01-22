package dsu.pasta.javaparser.factory.stmt;

import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedEnumConstantDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.Hole;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZEnumConstantDeclaration extends ZStatement {
    private EnumConstantDeclaration enumConstantDeclaration;
    private String str = "";

    public ZEnumConstantDeclaration(ZCode parent, EnumConstantDeclaration ecd) {
        super(parent);
        this.enumConstantDeclaration = ecd;
        this.sizes = 1;
    }

    public static List<ZCode> visit(EnumConstantDeclaration ecd, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZEnumConstantDeclaration zecd = new ZEnumConstantDeclaration(parent, ecd);
        SketchGadget sketch = new SketchGadget();

        sketch.setRange(ecd.getRange().get());
        sketch.setOriginalCode(zecd.toString());
        sketch.setType(zecd.getClass().getSimpleName());

        ResolvedEnumConstantDeclaration recd = ecd.resolve();
        ResolvedType type = recd.getType();

        //arguments
        ecd.getArguments().accept(visitor, zecd);
        //class body
        ecd.getClassBody().accept(visitor, zecd);
        //name
        ecd.getName().accept(visitor, zecd);

        String fieldName = recd.getName();
        sketch.addElement(new Element(type));
        sketch.addElement(new Element(" "));
        sketch.addElement(new Hole(type, false, Hole.HoleFrom.target));
        sketch.addElement(new Element("="));
        sketch.addElement(new Element(type));
        sketch.addElement(new Element("."));
        sketch.addElement(new Element(fieldName));
        sketch.addElement(new Element(";"));

        zecd.setSketch(sketch);
        GadgetsCollections.addParsedNode(ecd, zecd);
        GadgetsCollections.addTempSketch(sketch);
        return new ArrayList<>(Arrays.asList(zecd));
    }

    @Override
    public String toString() {
        return this.str;
    }
}
