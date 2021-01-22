package dsu.pasta.javaparser.factory.analyzer;

import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.GenericListVisitorAdapter;
import dsu.pasta.javaparser.factory.body.ZVariableDeclarator;
import dsu.pasta.javaparser.factory.expr.*;
import dsu.pasta.javaparser.factory.stmt.*;
import dsu.pasta.javaparser.factory.type.ZClassOrInterfaceType;
import dsu.pasta.javaparser.gadget.ZCode;

import java.util.List;

public class ZGenericListVisitorAdapter extends GenericListVisitorAdapter<ZCode, ZCode> {
    @Override
    public List<ZCode> visit(MethodDeclaration md, ZCode parent) {
        return ZMethodDeclaration.visit(md, parent, this);
    }

    @Override
    public List<ZCode> visit(ConstructorDeclaration cd, ZCode parent) {
        return ZConstructorDeclaration.visit(cd, parent, this);
    }

    @Override
    public List<ZCode> visit(FieldDeclaration fd, ZCode parent) {
        return ZFieldDeclaration.visit(fd, parent, this);
    }

    public List<ZCode> visit(InitializerDeclaration n, ZCode parent) {
        return ZStaticInitializer.visit(n, parent, this);
    }

    // statements
    @Override
    public List<ZCode> visit(ExpressionStmt es, ZCode parent) {
        return ZExpressionStmt.visit(es, parent, this);
    }

    @Override
    public List<ZCode> visit(DoStmt ds, ZCode parent) {
        return ZDoStmt.visit(ds, parent, this);
    }

    @Override
    public List<ZCode> visit(ForEachStmt fes, ZCode parent) {
        return ZForEachStmt.visit(fes, parent, this);
    }

    @Override
    public List<ZCode> visit(ForStmt fs, ZCode parent) {
        return ZForStmt.visit(fs, parent, this);
    }

    @Override
    public List<ZCode> visit(IfStmt is, ZCode parent) {
        return ZIfStmt.visit(is, parent, this);
    }

    @Override
    public List<ZCode> visit(ReturnStmt rs, ZCode parent) {
        return ZReturnStmt.visit(rs, parent, this);
    }

    @Override
    public List<ZCode> visit(ThrowStmt n, ZCode parent) {
        return ZThrowStmt.visit(n, parent, this);
    }

    @Override
    public List<ZCode> visit(WhileStmt ws, ZCode parent) {
        return ZWhileStmt.visit(ws, parent, this);
    }

    // expression
    @Override
    public List<ZCode> visit(InstanceOfExpr ie, ZCode parent) {
        return ZInstanceOfExpr.visit(ie, parent, this);
    }

    @Override
    public List<ZCode> visit(ArrayAccessExpr aae, ZCode parent) {
        return ZArrayAccessExpr.visit(aae, parent, this);
    }

    @Override
    public List<ZCode> visit(ConditionalExpr ce, ZCode parent) {
        return ZConditionalExpr.visit(ce, parent, this);

    }

    @Override
    public List<ZCode> visit(EnclosedExpr ee, ZCode parent) {
        return ZEnclosedExpr.visit(ee, parent, this);
    }

    @Override
    public List<ZCode> visit(BooleanLiteralExpr ble, ZCode parent) {
        return ZBooleanLiteralExpr.visit(ble, parent, this);
    }

    @Override
    public List<ZCode> visit(CharLiteralExpr cle, ZCode parent) {
        return ZCharLiteralExpr.visit(cle, parent, this);
    }

    @Override
    public List<ZCode> visit(DoubleLiteralExpr dle, ZCode parent) {
        return ZDoubleLiteralExpr.visit(dle, parent, this);
    }

    @Override
    public List<ZCode> visit(IntegerLiteralExpr ile, ZCode parent) {
        return ZIntegerLiteralExpr.visit(ile, parent, this);
    }

    @Override
    public List<ZCode> visit(LongLiteralExpr lle, ZCode parent) {
        return ZLongLiteralExpr.visit(lle, parent, this);
    }

    @Override
    public List<ZCode> visit(NullLiteralExpr nle, ZCode parent) {
        return ZNullLiteralExpr.visit(nle, parent, this);
    }

    @Override
    public List<ZCode> visit(StringLiteralExpr sle, ZCode parent) {
        return ZStringLiteralExpr.visit(sle, parent, this);
    }

    @Override
    public List<ZCode> visit(CastExpr ce, ZCode parent) {
        return ZCastExpr.visit(ce, parent, this);
    }

    @Override
    public List<ZCode> visit(BinaryExpr be, ZCode parent) {
        return ZBinaryExpr.visit(be, parent, this);
    }

    @Override
    public List<ZCode> visit(FieldAccessExpr fae, ZCode parent) {
        return ZFieldAccessExpr.visit(fae, parent, this);
    }

    @Override
    public List<ZCode> visit(NameExpr ne, ZCode parent) {
        return ZNameExpr.visit(ne, parent, this);
    }

    @Override
    public List<ZCode> visit(MethodCallExpr mce, ZCode parent) {
        return ZMethodCallExpr.visit(mce, parent, this);
    }

    @Override
    public List<ZCode> visit(VariableDeclarator vd, ZCode parent) {
        return ZVariableDeclarator.visit(vd, parent, this);
    }

    @Override
    public List<ZCode> visit(AssignExpr ae, ZCode parent) {
        return ZAssignExpr.visit(ae, parent, this);
    }

    @Override
    public List<ZCode> visit(VariableDeclarationExpr vde, ZCode parent) {
        return ZVariableDeclarationExpr.visit(vde, parent, this);
    }

    @Override
    public List<ZCode> visit(ObjectCreationExpr oce, ZCode parent) {
        return ZObjectCreationExpr.visit(oce, parent, this);
    }

    @Override
    public List<ZCode> visit(UnaryExpr ue, ZCode parent) {
        return ZUnaryExpr.visit(ue, parent, this);
    }

    @Override
    public List<ZCode> visit(ThisExpr te, ZCode parent) {
        return ZThisExpr.visit(te, parent, this);
    }

    @Override
    public List<ZCode> visit(ClassOrInterfaceType n, ZCode parent) {
        return ZClassOrInterfaceType.visit(n, parent, this);
    }

    @Override
    public List<ZCode> visit(ClassExpr ce, ZCode parent) {
        return ZClassExpr.visit(ce, parent, this);
    }

    public List<ZCode> visit(BreakStmt n, ZCode parent) {
        return ZBreakStmt.visit(n, parent, this);
    }

    public List<ZCode> visit(ContinueStmt n, ZCode parent) {
        return ZContinueStmt.visit(n, parent, this);
    }

    public List<ZCode> visit(EnumConstantDeclaration n, ZCode parent) {
        return ZEnumConstantDeclaration.visit(n, parent, this);
    }

    public List<ZCode> visit(SuperExpr n, ZCode parent) {
        return ZSuperExpr.visit(n, parent, this);
    }

    public List<ZCode> visit(SwitchStmt n, ZCode parent) {
        return ZSwitchStmt.visit(n, parent, this);
    }

    public List<ZCode> visit(SwitchEntry n, ZCode parent) {
        return ZSwitchEntry.visit(n, parent, this);
    }

    public List<ZCode> visit(SwitchExpr n, ZCode parent) {
        return ZSwitchExpr.visit(n, parent, this);
    }
}
