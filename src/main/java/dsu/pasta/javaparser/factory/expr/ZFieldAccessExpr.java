package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
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

public class ZFieldAccessExpr extends ZExpression {
    private FieldAccessExpr fieldAccessExpr;

    public ZFieldAccessExpr(ZCode parent, FieldAccessExpr fieldAccessExpr) {
        super(parent);
        this.fieldAccessExpr = fieldAccessExpr;
    }

    public static List<ZCode> visit(FieldAccessExpr fae, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZFieldAccessExpr zfae = new ZFieldAccessExpr(parent, fae);
        SketchGadget sketchWithScope = new SketchGadget();

        sketchWithScope.setRange(fae.getRange().get());
        sketchWithScope.setOriginalCode(zfae.toString());
        sketchWithScope.setType(zfae.getClass().getSimpleName());

        SketchGadget sketchWithoutScope = new SketchGadget();
        sketchWithoutScope.setRange(fae.getRange().get());
        sketchWithoutScope.setOriginalCode(zfae.toString());
        sketchWithoutScope.setType(zfae.getClass().getSimpleName());
        try {
            List<ZCode> tmp = null;
            ResolvedType realFieldType = fae.calculateResolvedType();
            ZSimpleName fieldName = new ZSimpleName(zfae, fae.getName());
            ResolvedValueDeclaration resolvedValueDec = fae.resolve();

            ResolvedType declaringType = null;
            if (resolvedValueDec instanceof ResolvedFieldDeclaration) {
                ResolvedFieldDeclaration temp = (ResolvedFieldDeclaration) resolvedValueDec;
                ResolvedTypeDeclaration declaringTypeDec = temp.declaringType();
                declaringType = JavaparserSolver.getType(declaringTypeDec.getQualifiedName());
            }
            if (fae.getTypeArguments().isPresent()) {
                fae.getTypeArguments().get().accept(visitor, zfae);
            }
            //scope
            Expression scopeExpr = fae.getScope();
            ZSimpleName scopeName = null;
            if (scopeExpr instanceof NameExpr) {
                if (resolvedValueDec.isEnumConstant() ||
                        (resolvedValueDec.isField() && resolvedValueDec.asField().isStatic())) {
                    scopeName = new ZSimpleName(zfae, scopeExpr.asNameExpr().getName());
                } else {
                    //instance field or the length field of array type
                    declaringType = scopeExpr.calculateResolvedType();
                }
            }
            if (parent != null && parent.nowSource) {
                if (scopeName != null) {
                    //field is static or enum, scope is class
                    if (resolvedValueDec.isEnumConstant()) {
                        sketchWithScope.addElement(new Element(realFieldType));
                    } else if (resolvedValueDec.isField() && resolvedValueDec.asField().isStatic()) {
                        sketchWithScope.addElement(new Element(declaringType));
                    }
                } else {
                    //field not static. scope is object/variable
                    zfae.nowSource = true;
                    tmp = fae.getScope().accept(visitor, zfae);
                    if (scopeExpr instanceof ThisExpr)
                        sketchWithScope.addElement(new Hole("this", declaringType, true, Hole.HoleFrom.instance));
                    else {
                        for (ZCode zc : tmp) {
                            for (Element e : zc.getSketch().getElements()) {
                                if (e instanceof Hole) {
                                    Hole h = (Hole) e;
                                    if (h.getOriginalString() != null && h.getOriginalString().equals("this") && h.getType() != null)
                                        h.setSource(true);
                                }
                            }
                        }
                        sketchWithScope.addElements(tmp, "");
                    }
                }
                sketchWithoutScope.addElement(new Hole(realFieldType, true, Hole.HoleFrom.field));
            } else if (!parent.nowSource) {
                if (scopeName != null) {
                    //field is static, scope is class
                    if (resolvedValueDec.isEnumConstant()) {
                        sketchWithScope.addElement(new Element(realFieldType));
                    } else if (resolvedValueDec.isField() && resolvedValueDec.asField().isStatic()) {
                        sketchWithScope.addElement(new Element(declaringType));
                    }
                } else {
                    //field not static. scope is object/variable
                    zfae.nowSource = false;
                    tmp = fae.getScope().accept(visitor, zfae);
                    if (scopeExpr instanceof ThisExpr) {
                        //the original code is like: this.field = xxx;
                        //the instance is a hole:  [].field = xxx;
                        sketchWithScope.addElement(new Hole("this", declaringType, true, Hole.HoleFrom.instance));
                    } else {
                        for (ZCode zc : tmp) {
                            for (Element e : zc.getSketch().getElements()) {
                                if (e instanceof Hole) {
                                    Hole h = (Hole) e;
                                    if (h.getOriginalString() != null && h.getOriginalString().equals("this") && h.getType() != null)
                                        h.setSource(true);
                                }
                            }
                        }
                        sketchWithScope.addElements(tmp, "");
                    }
                }
                sketchWithoutScope.addElement(new Hole(realFieldType, false, Hole.HoleFrom.field));
            }
            sketchWithScope.addElement(new Element("."));
            sketchWithScope.addElement(new Element(fieldName.toString()));

            sketchWithScope.addReplace(sketchWithoutScope);

            zfae.setSketch(sketchWithScope);
            GadgetsCollections.addParsedNode(fae, zfae);
        } catch (Exception e) {
//			System.err.println("Fail to visit " +fae.toString());
//			e.printStackTrace();
        } finally {
            return new ArrayList<ZCode>(Arrays.asList(zfae));
        }

    }

    @Override
    public String toString() {
        return this.fieldAccessExpr.toString();
    }

}
