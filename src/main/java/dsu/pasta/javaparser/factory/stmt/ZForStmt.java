package dsu.pasta.javaparser.factory.stmt;

import com.github.javaparser.Range;
import com.github.javaparser.ast.stmt.ForStmt;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Context;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.SketchForCondition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZForStmt extends ZStatement {
    private ForStmt forStmt;

    public ZForStmt(ZCode parent, ForStmt forStmt) {
        super(parent);
        this.forStmt = forStmt;
    }

    public static List<ZCode> visit(ForStmt fs, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZForStmt zfs = new ZForStmt(parent, fs);
        SketchForCondition fcs = new SketchForCondition();
        fcs.setRange(new Range(fs.getRange().get().begin, fs.getRange().get().begin));
        fcs.setType(zfs.getClass().getSimpleName());
        zfs.setSketch(fcs);

        List<ZCode> body = new ArrayList<>();
        String oriConCode = "";
        //condition initialization
        zfs.nowSource = true;
        List<ZCode> tmp = fs.getInitialization().accept(visitor, zfs);
        fcs.addElements(tmp, ",");
        oriConCode += fs.getInitialization().toString();
        //;
        fcs.addElement(new Element(";"));
        oriConCode += ";";
        //condition compare
        if (fs.getCompare().isPresent()) {
            zfs.nowSource = true;
            tmp = fs.getCompare().get().accept(visitor, zfs);
            fcs.addElements(tmp, "");
            oriConCode += fs.getCompare().get().toString();
        }
        //;
        fcs.addElement(new Element(";"));
        oriConCode += ";";
        //condition update
        zfs.nowSource = true;
        tmp = fs.getUpdate().accept(visitor, zfs);
        fcs.addElements(tmp, ",");
        oriConCode += fs.getUpdate().toString();

        fcs.setOriginalCode(oriConCode);

        //body
        zfs.nowSource = false;
        tmp = fs.getBody().accept(visitor, zfs);
        if (tmp != null) {
            body.addAll(tmp);
        }
        Context context = new Context(fcs, fs.getRange().get());
        fcs.set_thisAsContext(context);
        addContextTo(context, body);


        GadgetsCollections.addTempSketch(fcs);
        GadgetsCollections.addParsedNode(fs, zfs);
        GadgetsCollections.addTempContext(context);
        return new ArrayList<ZCode>(Arrays.asList(zfs));
    }

    @Override
    public String toString() {
        return this.forStmt.toString();
    }

}
