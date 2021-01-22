package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.expr.SwitchExpr;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZSwitchExpr extends ZExpression {
    private SwitchExpr switchExpr;

    public ZSwitchExpr(ZCode parent, SwitchExpr switchExpr) {
        super(parent);
        this.switchExpr = switchExpr;
    }

    public static List<ZCode> visit(SwitchExpr se, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZSwitchExpr zse = new ZSwitchExpr(parent, se);
        List<ZCode> result = new ArrayList<>();
        List<ZCode> tmp;
        tmp = se.getEntries().accept(visitor, zse);
        if (tmp != null)
            result.addAll(tmp);
        tmp = se.getSelector().accept(visitor, zse);
        if (tmp != null)
            result.addAll(tmp);

        GadgetsCollections.addParsedNode(se, zse);
        return new ArrayList<ZCode>(Arrays.asList(zse));
    }

    @Override
    public String toString() {
        return this.switchExpr.toString();
    }
}
