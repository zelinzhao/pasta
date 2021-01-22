package dsu.pasta.javaparser.factory.stmt;

import com.github.javaparser.ast.stmt.DoStmt;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Context;
import dsu.pasta.javaparser.gadget.sketch.SketchWhileCondition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZDoStmt extends ZStatement {
    private DoStmt doStmt;

    public ZDoStmt(ZCode parent, DoStmt doStmt) {
        super(parent);
        this.doStmt = doStmt;
    }

    public static List<ZCode> visit(DoStmt ds, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZDoStmt zds = new ZDoStmt(parent, ds);
        SketchWhileCondition wcs = new SketchWhileCondition();
        wcs.setRange(ds.getCondition().getRange().get());
        wcs.setOriginalCode(ds.getCondition().toString());
        wcs.setType(zds.getClass().getSimpleName());

        List<ZCode> body = new ArrayList<>();
        //body
        zds.nowSource = false;
        List<ZCode> tmp = ds.getBody().accept(visitor, zds);
        if (tmp != null) {
            body.addAll(tmp);
        }
        //while
        //condition
        zds.nowSource = true;
        tmp = ds.getCondition().accept(visitor, zds);
        if (tmp != null) {
            wcs.addElements(tmp, "");
        }
        zds.setSketch(wcs);

        Context context = new Context(wcs, ds.getRange().get());
        wcs.set_thisAsContext(context);

        addContextTo(context, body);

        GadgetsCollections.addTempSketch(wcs);
        GadgetsCollections.addTempContext(context);
        GadgetsCollections.addParsedNode(ds, zds);
        return new ArrayList<ZCode>(Arrays.asList(zds));
    }

    @Override
    public String toString() {
        return this.doStmt.toString();
    }

}
