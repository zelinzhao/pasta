package dsu.pasta.javaparser.factory.stmt;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.model.typesystem.ReferenceTypeImpl;
import dsu.pasta.javaparser.factory.analyzer.JavaparserSolver;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.Hole;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Create special field creating stmt from a field declaration
 */
public class ZFieldDeclaration extends ZStatement {
    private FieldDeclaration fieldDeclaration;
    private String fieldName;
    private String fieldCreateString = "";

    public ZFieldDeclaration(ZCode parent, FieldDeclaration fieldDeclaration) {
        super(parent);
        this.fieldDeclaration = fieldDeclaration;
        this.sizes = 1;
    }

    public static List<ZCode> visit(FieldDeclaration n, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZFieldDeclaration zfd = new ZFieldDeclaration(parent, n);

        SketchGadget sketchFieldAccess = new SketchGadget();
        sketchFieldAccess.setRange(n.getRange().get());
        sketchFieldAccess.setType(zfd.getClass().getSimpleName());

        SketchGadget sketchFieldCreate = new SketchGadget();
        sketchFieldCreate.setRange(n.getRange().get());

        ResolvedFieldDeclaration rfd = n.resolve();
        ResolvedReferenceTypeDeclaration inDec = (ResolvedReferenceTypeDeclaration) rfd.declaringType();
        ReferenceTypeImpl inType = new ReferenceTypeImpl(inDec, JavaparserSolver.combinedTypeSolver);
        ResolvedType fieldType = rfd.getType();
        zfd.fieldName = rfd.getName();

        sketchFieldAccess.addElement(new Element(fieldType));
        sketchFieldAccess.addElement(new Element(" "));
        sketchFieldAccess.addElement(new Hole(fieldType, false, Hole.HoleFrom.target));
        sketchFieldAccess.addElement(new Element("="));
        if (rfd.isStatic())
            sketchFieldAccess.addElement(new Element(inType));
        else
            sketchFieldAccess.addElement(new Hole(inType, true, Hole.HoleFrom.definition));
        sketchFieldAccess.addElement(new Element("."));
        sketchFieldAccess.addElement(new Element(zfd.fieldName));
        sketchFieldAccess.addElement(new Element(";\n"));

        List<ZCode> tmp = n.getVariables().accept(visitor, zfd);
        if (tmp != null) {
            zfd.fieldCreateString = tmp.get(0).toString();
            sketchFieldCreate.addElements(tmp, "");
            sketchFieldCreate.addElement(new Element(";\n"));
        }

        sketchFieldAccess.setOriginalCode(zfd.toString());
        sketchFieldCreate.setOriginalCode(zfd.toString());

        if (sketchFieldCreate.getElements().size() > 0) {
            sketchFieldCreate.setType(ZExpressionStmt.class.getSimpleName());
            sketchFieldAccess.addReplace(sketchFieldCreate);
        }
        zfd.setSketch(sketchFieldAccess);

        GadgetsCollections.addParsedNode(n, zfd);
        GadgetsCollections.addTempSketch(sketchFieldAccess);
        return new ArrayList<ZCode>(Arrays.asList(zfd));
    }

    public String getFieldName() {
        return this.fieldName;
    }

    @Override
    public String toString() {
        return this.fieldCreateString;
    }
}
