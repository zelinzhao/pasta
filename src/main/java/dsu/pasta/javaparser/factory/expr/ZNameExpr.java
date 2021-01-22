package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserFieldDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserParameterDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserSymbolDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserVariableDeclaration;
import dsu.pasta.javaparser.factory.analyzer.JavaparserSolver;
import dsu.pasta.javaparser.factory.analyzer.ZGenericListVisitorAdapter;
import dsu.pasta.javaparser.factory.body.ZVariableDeclarator;
import dsu.pasta.javaparser.factory.stmt.ZStatement;
import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javaparser.gadget.sketch.Element;
import dsu.pasta.javaparser.gadget.sketch.Hole;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZNameExpr extends ZExpression {
    private NameExpr nameExpr;

    public ZNameExpr(ZCode parent, NameExpr nameExpr) {
        super(parent);
        this.nameExpr = nameExpr;
    }

    public static List<ZCode> visit(NameExpr ne, ZCode parent, ZGenericListVisitorAdapter visitor) {
        ZNameExpr zne = new ZNameExpr(parent, ne);
        SketchGadget sketch = new SketchGadget();

        sketch.setRange(ne.getRange().get());
        sketch.setOriginalCode(zne.toString());
        sketch.setType(zne.getClass().getSimpleName());

        boolean isField = false;
        ResolvedType declaringTypeOfField = null;

        ResolvedType resolveType = null;
        ResolvedValueDeclaration valueDec = null;
        try {
            resolveType = ne.calculateResolvedType();
        } catch (Exception e) {
        }
        try {
            valueDec = ne.resolve();
            /**
             * There is [scope.xxx] Enum.field (FieldAccessExpr), Class.static_field (FieldAccessExpr), Class.static_method (MethodCallExpr).<br>
             * Javassist assumes that the scope, which is a NameExpr, of FieldAccessExpr, MethodCallExpr is a value (local variable, object etc), but actually the scope is type!<br>
             * If we pass the non-value-type scope (NameExpr) to our NameExpr visit method and try to call {@code resolve} on it, an exception will be thrown.<br>
             *  It will take some time to resolve and throw exception, but we don't care here. :-( <br>
             */
        } catch (Exception e) {
        }
        if (parent != null && parent.nowSource) {
            if (valueDec != null) {
                if (valueDec.isEnumConstant()) {
                    //assume enum field is always accessed via class name, we don't need to add MustHave here.
                } else if (valueDec.isField()) {
                    //assume static field is accessed via class name, we don't need to add MustHave here
                    //if field is accessed via object, the object will add mustHave processed below
                    ZCode zfield = null;
                    if (valueDec instanceof JavaParserFieldDeclaration) {
                        JavaParserFieldDeclaration jpfd = (JavaParserFieldDeclaration) valueDec;
                        FieldDeclaration fd = jpfd.getWrappedNode();
                        zfield = GadgetsCollections.allParsedNode.get(fd);
                        isField = true;
                        ResolvedTypeDeclaration declaringTypeDec = fd.resolve().declaringType();
                        declaringTypeOfField = JavaparserSolver.getType(declaringTypeDec.getQualifiedName());
                    }
                } else if (valueDec.isMethod()) {
                    //assume static method is accessed via class name, we don't need to add MustHave here
                    //if method is accessed via object, the object will add mustHave processed below
                    // method name is avaliable
                } else if (valueDec.isParameter()) {
                    // real parameter doesn't reach here. why? TODO
                    ResolvedParameterDeclaration para = valueDec.asParameter();
                    if (valueDec instanceof JavaParserParameterDeclaration) {
                        JavaParserParameterDeclaration t = (JavaParserParameterDeclaration) valueDec;
                        Node wrapNode = t.getWrappedNode();
                        ZCode zc = GadgetsCollections.allParsedNode.get(wrapNode);
                    }
                } else if (valueDec.isType()) {
                } else if (valueDec.isVariable()) {
                    // TODO fix this? I can't get here, why?
                    System.out.print("This is a variable. check this " + ne.toString());
                    if (valueDec instanceof JavaParserVariableDeclaration) {
                        JavaParserVariableDeclaration t = (JavaParserVariableDeclaration) valueDec;
                        ZVariableDeclarator zvd = (ZVariableDeclarator) GadgetsCollections.allParsedNode
                                .get(t.getVariableDeclarator());
                    }
                } else if (valueDec instanceof JavaParserSymbolDeclaration) {
                    // This supposed to be a variable
                    JavaParserSymbolDeclaration t = (JavaParserSymbolDeclaration) valueDec;
                    // The wrapped node should be a variable decalator
                    Node wrapNode = t.getWrappedNode();
                    if (wrapNode instanceof VariableDeclarator) {
//						ZVariableDeclarator zvd = (ZVariableDeclarator) GadgetsCollections.allParsedNode.get(wrapNode);
                        ZStatement zs = getTopStatement(GadgetsCollections.allParsedNode.get(wrapNode));
                        if (zs != null)
                            sketch.addMustHave(zs.getSketch());
                    }
                }
            }
            if (resolveType != null) {
                if (isField)
                    sketch.addElement(new Hole(ne.getNameAsString(), resolveType, true, Hole.HoleFrom.field));
                else
                    sketch.addElement(new Hole(ne.getNameAsString(), resolveType, true, Hole.HoleFrom.variable));
            }
        } else if (parent != null && !parent.nowSource) {
            if (valueDec != null) {
                if (valueDec.isField()) {
                    //assume static field is accessed via class name, we don't need to add MustHave here
                    //if field is accessed via object, the object will add mustHave processed below
                    ZCode zfield = null;
                    if (valueDec instanceof JavaParserFieldDeclaration) {
                        JavaParserFieldDeclaration jpfd = (JavaParserFieldDeclaration) valueDec;
                        FieldDeclaration fd = jpfd.getWrappedNode();
                        zfield = GadgetsCollections.allParsedNode.get(fd);
                        isField = true;
                        ResolvedTypeDeclaration declaringTypeDec = fd.resolve().declaringType();
                        declaringTypeOfField = JavaparserSolver.getType(declaringTypeDec.getQualifiedName());
                    }
                } else if (valueDec instanceof JavaParserSymbolDeclaration) {
                    // This supposed to be a variable
                    JavaParserSymbolDeclaration t = (JavaParserSymbolDeclaration) valueDec;
                    // The wrapped node should be a variable decalator
                    Node wrapNode = t.getWrappedNode();
                    if (wrapNode instanceof VariableDeclarator) {
                        ZStatement zs = getTopStatement(GadgetsCollections.allParsedNode.get(wrapNode));
                        if (zs != null)
                            sketch.addMustHave(zs.getSketch());
                    }
                }
            }
            if (resolveType != null) {
                if (isField)
                    sketch.addElement(new Hole(ne.getNameAsString(), resolveType, false, Hole.HoleFrom.field));
                else
                    sketch.addElement(new Hole(ne.getNameAsString(), resolveType, false, Hole.HoleFrom.variable));
            }
        }
        if (isField && declaringTypeOfField != null) {
            SketchGadget fieldSketch = new SketchGadget();
            fieldSketch.setRange(ne.getRange().get());
            fieldSketch.setOriginalCode(zne.toString());
            fieldSketch.setType(ZFieldAccessExpr.class.getSimpleName());
            if (parent != null && parent.nowSource) {
                fieldSketch.addElement(new Hole("this", declaringTypeOfField, true, Hole.HoleFrom.instance));
            } else if (parent != null && !parent.nowSource) {
                //the original code is like: instance.field = xxx;
                //the instance is a hole:  [].field = xxx;
                fieldSketch.addElement(new Hole("this", declaringTypeOfField, true, Hole.HoleFrom.instance));
            }
            fieldSketch.addElement(new Element("."));
            fieldSketch.addElement(new Element(ne.getNameAsString()));
            fieldSketch.addReplace(sketch);
            zne.setSketch(fieldSketch);
        } else {
            zne.setSketch(sketch);
        }
        zne.nowSource = parent.nowSource;
        GadgetsCollections.addParsedNode(ne, zne);
        return new ArrayList<ZCode>(Arrays.asList(zne));
    }

    @Override
    public String toString() {
        return this.nameExpr.toString();
    }
}
