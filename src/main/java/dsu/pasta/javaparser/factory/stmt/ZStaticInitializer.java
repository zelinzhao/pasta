package dsu.pasta.javaparser.factory.stmt;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZStaticInitializer extends ZStatement {
    private InitializerDeclaration initializerDeclaration;

    public ZStaticInitializer(ZCode parent, InitializerDeclaration init) {
        super(parent);
        this.initializerDeclaration = init;
    }

    public static List<ZCode> visit(InitializerDeclaration n, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZStaticInitializer zsi = new ZStaticInitializer(parent, n);
        //body
        List<ZCode> body = n.getBody().accept(visitor, zsi);

        List<ClassOrInterfaceDeclaration> classes = n.getParentNode().get().findAll(ClassOrInterfaceDeclaration.class);
        assert (classes.size() == 1);
        if (classes.size() == 1) {
            Context context = new Context(
                    classes.get(0).getFullyQualifiedName().get(),
                    zsi.toString(), n.getRange().get());
            addContextTo(context, body);
            GadgetsCollections.addTempContext(context);
        }
        GadgetsCollections.addParsedNode(n, zsi);
        return new ArrayList<ZCode>(Arrays.asList(zsi));
    }

    public String toString() {
        return "static{}";
    }
}
