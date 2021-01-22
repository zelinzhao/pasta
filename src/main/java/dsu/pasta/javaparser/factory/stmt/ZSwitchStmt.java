package dsu.pasta.javaparser.factory.stmt;

import com.github.javaparser.ast.stmt.SwitchStmt;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TODO Should we make switch to multiple ifs?
 */
public class ZSwitchStmt extends ZStatement {
    private SwitchStmt switchStmt;

    public ZSwitchStmt(ZCode parent, SwitchStmt switchStmt) {
        super(parent);
        this.switchStmt = switchStmt;
    }

    public static List<ZCode> visit(SwitchStmt ss, ZCode parent, ZGenericListVisitorAdapter visitor) {
        List<ZCode> ifStmts = new ArrayList<>();

        ZSwitchStmt zss = new ZSwitchStmt(parent, ss);
        List<ZCode> result = new ArrayList<>();
        List<ZCode> tmp = new ArrayList<>();

        tmp = ss.getSelector().accept(visitor, zss);
        if (tmp != null)
            result.addAll(tmp);

        tmp = ss.getEntries().accept(visitor, zss);
        if (tmp != null)
            result.addAll(tmp);

        GadgetsCollections.addParsedNode(ss, zss);
        return new ArrayList<ZCode>(Arrays.asList(zss));
    }

    public String toString() {
        return this.switchStmt.toString();
    }

}
