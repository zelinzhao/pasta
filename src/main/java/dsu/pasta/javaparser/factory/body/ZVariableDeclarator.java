package dsu.pasta.javaparser.factory.body;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.factory.expr.ZSimpleName;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.Hole;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZVariableDeclarator extends ZBody {
    private VariableDeclarator variableDeclarator;

    public ZVariableDeclarator(ZCode parent, VariableDeclarator variableDeclarator) {
        super(parent);
        this.variableDeclarator = variableDeclarator;
    }

    public static List<ZCode> visit(VariableDeclarator vd, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZVariableDeclarator zvd = new ZVariableDeclarator(parent, vd);
        SketchGadget sketch = new SketchGadget();
        sketch.setRange(vd.getRange().get());
        sketch.setOriginalCode(zvd.toString());
        sketch.setType(zvd.getClass().getSimpleName());

        ResolvedValueDeclaration rvd = vd.resolve();

        ResolvedType type = vd.resolve().getType();
        //Type
        zvd.nowSource = false;
        vd.getType().accept(visitor, zvd);
        //type
        sketch.addElement(new Element(type));
        sketch.addElement(new Element(" "));
        //name
        ZSimpleName zsn = new ZSimpleName(zvd, vd.getName());
        sketch.addElement(new Hole(zsn.toString(), type, false, Hole.HoleFrom.variable));
        //initializer
        /**
         * if a variable declaration has an initializer, e.g. int a = 0;,
         * this statement can be extracted a useful gadget.
         * If a variable declaration has no initializer, e.g. int a;,
         * this statement is actually not valid gadget.
         */
        if (vd.getInitializer().isPresent()) {
            sketch.addElement(new Element("="));
            zvd.nowSource = true;
            sketch.addElements(vd.getInitializer().get().accept(visitor, zvd));
        }
        zvd.setSketch(sketch);
        GadgetsCollections.addParsedNode(vd, zvd);
        return new ArrayList<ZCode>(Arrays.asList(zvd));
    }

    @Override
    public String toString() {
        return this.variableDeclarator.toString();
    }
}
