package dsu.pasta.javaparser.factory.type;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.factory.expr.ZSimpleName;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZClassOrInterfaceType extends ZCode {
    private ClassOrInterfaceType classOrInterfaceType;

    public ZClassOrInterfaceType(ZCode parent, ClassOrInterfaceType coi) {
        super(parent);
        this.classOrInterfaceType = coi;
    }

    public static List<ZCode> visit(ClassOrInterfaceType n, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZClassOrInterfaceType zcoi = new ZClassOrInterfaceType(parent, n);
        SketchGadget sketch = new SketchGadget();
        sketch.setRange(n.getRange().get());
        sketch.setOriginalCode(zcoi.toString());
        sketch.setType(zcoi.getClass().getSimpleName());

        ResolvedReferenceType resolvedType = null;
        if (n.getTypeArguments().isPresent()) {
            NodeList<Type> tas = n.getTypeArguments().get();
            resolvedType = n.removeTypeArguments().resolve();
            n.setTypeArguments(tas);
        } else {
            try {
                resolvedType = n.resolve();
            } catch (Exception e) {
                return new ArrayList<ZCode>();
            }
        }

        boolean hasScope = false;
        if (n.getScope().isPresent()) {
            zcoi.nowSource = true;
            ClassOrInterfaceType cit = n.getScope().get();
            sketch.addElements(cit.accept(visitor, zcoi));
            sketch.addElement(new Element("."));
            hasScope = true;
        }
        //name
        zcoi.nowSource = false;
        ZSimpleName typeName = new ZSimpleName(zcoi, n.getName());
        if (!hasScope)
            sketch.addElement(new Element(resolvedType));
        else
            sketch.addElement(new Element(typeName.toString()));

        if (n.getTypeArguments().isPresent()) {
            zcoi.nowSource = false;
            sketch.addElement(new Element("<"));
            sketch.addElements(n.getTypeArguments().get().accept(visitor, zcoi), ",");
            sketch.addElement(new Element(">"));
        }
        zcoi.setSketch(sketch);
        GadgetsCollections.addParsedNode(n, zcoi);
        return new ArrayList<ZCode>(Arrays.asList(zcoi));
    }

    @Override
    public String toString() {
        return this.classOrInterfaceType.toString();
    }

}
