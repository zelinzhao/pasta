package dsu.pasta.javaparser.factory.stmt;

import com.github.javaparser.ast.stmt.WhileStmt;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Context;
import dsu.pasta.javaparser.gadget.sketch.SketchWhileCondition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZWhileStmt extends ZStatement {
    private WhileStmt whileStmt;

    public ZWhileStmt(ZCode parent, WhileStmt whileStmt) {
        super(parent);
        this.whileStmt = whileStmt;
    }

    public static List<ZCode> visit(WhileStmt ws, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZWhileStmt zws = new ZWhileStmt(parent, ws);

        SketchWhileCondition wcs = new SketchWhileCondition();
        wcs.setRange(ws.getCondition().getRange().get());
        wcs.setOriginalCode(ws.getCondition().toString());
        wcs.setType(zws.getClass().getSimpleName());
        zws.setSketch(wcs);

        List<ZCode> body = new ArrayList<>();
        //condition
        zws.nowSource = true;
        List<ZCode> tmp = ws.getCondition().accept(visitor, zws);
        if (tmp != null) {
            wcs.addElements(tmp);
        }
        //body
        zws.nowSource = false;
        tmp = ws.getBody().accept(visitor, zws);
        if (tmp != null) {
            body.addAll(tmp);
        }
        Context context = new Context(wcs, ws.getRange().get());
        wcs.set_thisAsContext(context);
        addContextTo(context, body);

        GadgetsCollections.addTempSketch(wcs);
        GadgetsCollections.addParsedNode(ws, zws);
        GadgetsCollections.addTempContext(context);
        return new ArrayList<ZCode>(Arrays.asList(zws));
    }

    @Override
    public String toString() {
        return this.whileStmt.toString();
    }
}
