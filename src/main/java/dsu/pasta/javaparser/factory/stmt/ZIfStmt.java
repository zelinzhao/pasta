package dsu.pasta.javaparser.factory.stmt;

import com.github.javaparser.Range;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Context;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.SketchIfCondition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZIfStmt extends ZStatement {
    private static SketchIfCondition oriThenCond = null;
    private IfStmt ifStmt;

    public ZIfStmt(ZCode parent, IfStmt ifStmt) {
        super(parent);
        this.ifStmt = ifStmt;
    }

    private static ZIfStmt visitThen(IfStmt is, ZCode parent, ZGenericListVisitorAdapter visitor, List<ZCode> condition) {
        ZIfStmt zthen = new ZIfStmt(parent, is);
        Range thenRange = new Range(is.getBegin().get(), is.getThenStmt().getEnd().get());

        SketchIfCondition thenCondition = new SketchIfCondition();
        thenCondition.setRange(is.getCondition().getRange().get());
        thenCondition.setOriginalCode(is.getCondition().toString());
        thenCondition.setType(zthen.getClass().getSimpleName());

        List<ZCode> tmp;
        List<ZCode> thenBody = new ArrayList<>();
        //condition
        zthen.nowSource = true;
        tmp = is.getCondition().accept(visitor, zthen);
        if (tmp != null) {
            thenCondition.addElements(tmp);
            condition.addAll(tmp);
        }
        //then statement
        zthen.nowSource = false;
        tmp = is.getThenStmt().accept(visitor, zthen);
        if (tmp != null) {
            thenBody.addAll(tmp);
        }
        Context thenContext = new Context(thenCondition, thenRange);
        thenCondition.set_thisAsContext(thenContext);
        addContextTo(thenContext, thenBody);
        zthen.setSketch(thenCondition);

        GadgetsCollections.addTempContext(thenContext);
        GadgetsCollections.addTempSketch(thenCondition);
        oriThenCond = thenCondition;
        return zthen;
    }

    private static ZIfStmt visitElse(IfStmt is, ZCode parent, ZGenericListVisitorAdapter visitor, List<ZCode> condition) {
        if (!is.getElseStmt().isPresent())
            return null;
        //TODO, we should negate the original if condition
        ZIfStmt zelse = new ZIfStmt(parent, is);
        Statement elseStmt = is.getElseStmt().get();

        SketchIfCondition elseCon = new SketchIfCondition();
        elseCon.setRange(new Range(is.getElseStmt().get().getBegin().get(), is.getElseStmt().get().getBegin().get()));
        elseCon.setOriginalCode("!(" + is.getCondition().toString() + ")");
        elseCon.setType(zelse.getClass().getSimpleName());

        List<ZCode> tmp;
        List<ZCode> elseBody = new ArrayList<>();

        elseCon.addElement(new Element("!("));
        //condition
        zelse.nowSource = true;
        elseCon.addElements(condition, "");
        elseCon.addElement(new Element(")"));
        tmp = elseStmt.accept(visitor, zelse);
        if (tmp != null) {
            elseBody.addAll(tmp);
        }
        Context context = new Context(elseCon, elseStmt.getRange().get());
        elseCon.set_thisAsContext(context);
        addContextTo(context, elseBody);

        zelse.setSketch(elseCon);

        if (oriThenCond != null) {
            oriThenCond.setOriElseCond(elseCon);
            elseCon.setOriThenCond(oriThenCond);
        }
        GadgetsCollections.addTempSketch(elseCon);
        GadgetsCollections.addTempContext(context);
        return zelse;
    }

    /**
     * if(con){} else {}
     * we build two sketches:
     * <p>1: if(con){} else {} </p>
     * <p>2: if(!con) {} else {} </p>
     *
     * @param is
     * @param parent
     * @param visitor
     * @return
     */
    public static List<ZCode> visit(IfStmt is, ZCode parent, ZGenericListVisitorAdapter visitor) {
        oriThenCond = null;
        List<ZCode> condition = new ArrayList<>();
        ZIfStmt thenIf = visitThen(is, parent, visitor, condition);
        ZIfStmt elseIf = visitElse(is, parent, visitor, condition);

        if (elseIf == null)
            return new ArrayList<ZCode>(Arrays.asList(thenIf));
        else
            return new ArrayList<ZCode>(Arrays.asList(thenIf, elseIf));
    }

    @Override
    public String toString() {
        return this.ifStmt.toString();
    }
}
