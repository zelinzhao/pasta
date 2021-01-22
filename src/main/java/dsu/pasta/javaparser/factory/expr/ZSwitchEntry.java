package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.stmt.SwitchEntry;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZSwitchEntry extends ZExpression {
    private SwitchEntry switchEntry;

    public ZSwitchEntry(ZCode parent, SwitchEntry switchEntry) {
        super(parent);
        this.switchEntry = switchEntry;
    }

    public static List<ZCode> visit(SwitchEntry se, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZSwitchEntry zse = new ZSwitchEntry(parent, se);
        List<ZCode> result = new ArrayList<>();
        List<ZCode> tmp;
        tmp = se.getLabels().accept(visitor, zse);
        if (tmp != null)
            result.addAll(tmp);

        tmp = se.getStatements().accept(visitor, zse);
        if (tmp != null)
            result.addAll(tmp);

        GadgetsCollections.addParsedNode(se, zse);
        return new ArrayList<ZCode>(Arrays.asList(zse));
    }

    public String toString() {
        return this.switchEntry.toString();
    }
}
