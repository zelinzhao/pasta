package dsu.pasta.javaparser.factory.stmt;

import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.resolution.types.ResolvedType;
import dsu.pasta.javaparser.factory.analyzer.JavaparserSolver;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Context;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.SketchForeachCondition;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZForEachStmt extends ZStatement {
    public static final String _dsu_array_index_ = "_dsu_array_index_";
    public static final String _dsu_iterator_ = "_dsu_iterator_";
    private ForEachStmt forEachStmt;

    public ZForEachStmt(ZCode parent, ForEachStmt forEachStmt) {
        super(parent);
        this.forEachStmt = forEachStmt;
    }

    /**
     * This method keeps original foreach loop structure.
     *
     * @param fes
     * @param parent
     * @param visitor
     * @return
     */
    public static List<ZCode> visitOriginalForeach(ForEachStmt fes, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZForEachStmt zfes = new ZForEachStmt(parent, fes);

        SketchForeachCondition fcs = new SketchForeachCondition();
        fcs.setType(zfes.getClass().getSimpleName());

        List<ZCode> body = new ArrayList<>();
        String oriCondStr = "";
        //condition variable
        zfes.nowSource = false;
        List<ZCode> tmp = fes.getVariable().accept(visitor, zfes);
        if (tmp != null) {
            fcs.addElements(tmp);
        }
        oriCondStr += fes.getVariable().toString();
        //:
        fcs.addElement(new Element(":"));
        oriCondStr += ":";
        //condition iterable
        zfes.nowSource = true;
        tmp = fes.getIterable().accept(visitor, zfes);
        if (tmp != null) {
            fcs.addElements(tmp);
        }
        oriCondStr += fes.getIterable().toString();
        fcs.setOriginalCode(oriCondStr);
        //body
        zfes.nowSource = false;
        tmp = fes.getBody().accept(visitor, zfes);
        if (tmp != null) {
            body.addAll(tmp);
        }
        fcs.setRange(new Range(fes.getVariable().getBegin().get(), fes.getIterable().getEnd().get()));

        Context context = new Context(fcs, fes.getRange().get());
        fcs.set_thisAsContext(context);
        addContextTo(context, body);

        zfes.setSketch(fcs);

        GadgetsCollections.addTempSketch(fcs);
        GadgetsCollections.addParsedNode(fes, zfes);
        GadgetsCollections.addTempContext(context);
        return new ArrayList<ZCode>(Arrays.asList(zfes));
    }

    /**
     * This method transforms original foreach loop structure into while loop structure.
     *
     * @param fes
     * @param parent
     * @param visitor
     * @return
     */
    public static List<ZCode> visit(ForEachStmt fes, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZForEachStmt zfes = new ZForEachStmt(parent, fes);

        SketchForeachCondition fcs = new SketchForeachCondition();
        fcs.setType(zfes.getClass().getSimpleName());

        String oriCondStr = "";

        oriCondStr += fes.getVariable().toString();
        oriCondStr += ":";
        oriCondStr += fes.getIterable().toString();
        fcs.setOriginalCode(oriCondStr);

        Position originalBegin = fes.getVariable().getBegin().get();
        Position originalEnd = fes.getIterable().getEnd().get();
        if (originalBegin.column == 0)
            originalBegin.withColumn(3);
        Range originalRange = new Range(originalBegin, originalEnd);

        Position outBegin = new Position(originalBegin.line, originalBegin.column - 2);
        Position outEnd = new Position(originalBegin.line, originalBegin.column - 1);
        Position inBegin = new Position(originalEnd.line, originalEnd.column + 1);
        Position inEnd = new Position(originalEnd.line, originalEnd.column + 2);
        Position plusBegin = new Position(originalEnd.line, originalEnd.column + 3);
        Position plusEnd = new Position(originalEnd.line, originalEnd.column + 4);
        //Type variable: iterable
        //get variable
        zfes.nowSource = false;
        List<ZCode> varTmp = fes.getVariable().accept(visitor, zfes);
        ResolvedType varType = fes.getVariable().getVariable(0).resolve().getType();
        //get iterable
        zfes.nowSource = true;
        List<ZCode> iteTmp = fes.getIterable().accept(visitor, zfes);
        ResolvedType iteType = fes.getIterable().calculateResolvedType();

        if (iteType.isArray()) {
            String indexName = _dsu_array_index_;
            SketchGadget outerSg = new SketchGadget();
            //int _dsu_array_index_=0;
            outerSg.setRange(new Range(outBegin, outEnd));
            outerSg.addElement(new Element("int", JavaparserSolver.getType("int")));
            outerSg.addElement(Element.op_space);
            outerSg.addElement(new Element(indexName));
            outerSg.addElement(Element.op_equal);
            outerSg.addElement(new Element("0"));
            outerSg.addElement(Element.op_semicolon);
            //while( _dsu_array_index_ < ite.length  )
            fcs.addElement(new Element(indexName));
            fcs.addElement(Element.op_lesser);
            fcs.addElements(iteTmp);
            fcs.addElement(Element.op_dot);
            fcs.addElement(new Element("length"));
            //Object obj = ite[_dsu_array_index_];
            SketchGadget innerSg = new SketchGadget();
            innerSg.setRange(new Range(inBegin, inEnd));
            innerSg.addElements(varTmp);
            innerSg.addElement(Element.op_equal);
            innerSg.addElements(iteTmp);
            innerSg.addElement(Element.op_lsquare);
            innerSg.addElement(new Element(indexName));
            innerSg.addElement(Element.op_rsquare);
            innerSg.addElement(Element.op_semicolon);
            //_dsu_array_index_++;
            SketchGadget plusOne = new SketchGadget();
            plusOne.setRange(new Range(plusBegin, plusEnd));
            plusOne.addElement(new Element(indexName));
            plusOne.addElement(Element.op_plusplus);
            plusOne.addElement(Element.op_semicolon);

            outerSg.setOriginalCode(oriCondStr);
            innerSg.setOriginalCode(oriCondStr);
            plusOne.setOriginalCode(oriCondStr);

            fcs.addOuter(outerSg);
            fcs.addInner(innerSg);
            fcs.addInner(plusOne);
            GadgetsCollections.addTempSketch(outerSg);
            GadgetsCollections.addTempSketch(innerSg);
            GadgetsCollections.addTempSketch(plusOne);
        } else {
            String iteratorName = _dsu_iterator_;
            String iteratorClassName = "java.util.Iterator<" + JavaparserSolver.myDescribe(varType) + ">";
            //java.util.Iterator<type> _dsu_iterator_ = ite.iterator();
            SketchGadget outerSg = new SketchGadget();
            outerSg.setRange(new Range(outBegin, outEnd));
            outerSg.addElement(new Element(JavaparserSolver.getType(iteratorClassName)));
            outerSg.addElement(Element.op_space);
            outerSg.addElement(new Element(iteratorName));
            outerSg.addElement(Element.op_equal);
            outerSg.addElements(iteTmp);
            outerSg.addElement(Element.op_dot);
            outerSg.addElement(new Element("iterator()"));
            outerSg.addElement(Element.op_semicolon);
            //while(  _dsu_iterator_.hasNext()  )
            fcs.addElements(iteTmp);
            fcs.addElement(Element.op_dot);
            fcs.addElement(new Element("hasNext()"));
            //Object obj = _dsu_iterator_.next();
            SketchGadget innerSg = new SketchGadget();
            innerSg.setRange(new Range(inBegin, inEnd));
            innerSg.addElements(varTmp);
            innerSg.addElement(Element.op_equal);
            innerSg.addElement(new Element(iteratorName));
            innerSg.addElement(Element.op_dot);
            innerSg.addElement(new Element("next()"));
            innerSg.addElement(Element.op_semicolon);

            outerSg.setOriginalCode(oriCondStr);
            innerSg.setOriginalCode(oriCondStr);

            fcs.addOuter(outerSg);
            fcs.addInner(innerSg);
            GadgetsCollections.addTempSketch(outerSg);
            GadgetsCollections.addTempSketch(innerSg);
        }
        fcs.setRange(originalRange);
        zfes.setSketch(fcs);

        //body
        zfes.nowSource = false;
        List<ZCode> body = fes.getBody().accept(visitor, zfes);

        Context context = new Context(fcs, fes.getRange().get());
        fcs.set_thisAsContext(context);
        addContextTo(context, body);

        GadgetsCollections.addTempSketch(fcs);
        GadgetsCollections.addParsedNode(fes, zfes);
        GadgetsCollections.addTempContext(context);
        return new ArrayList<ZCode>(Arrays.asList(zfes));
    }

    @Override
    public String toString() {
        return this.forEachStmt.toString();
    }

}
