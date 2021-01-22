package dsu.pasta.javaparser.factory.expr;

import com.github.javaparser.ast.expr.SimpleName;
import dsu.pasta.javaparser.gadget.ZCode;

public class ZSimpleName extends ZExpression {
    private SimpleName simpleName;

    public ZSimpleName(ZCode parent, SimpleName simpleName) {
        super(parent);
        this.simpleName = simpleName;
    }

    public String getSimpleNameString() {
        return this.simpleName.asString();
    }

    public SimpleName getSimpleName() {
        return this.simpleName;
    }

    @Override
    public String toString() {
        return this.simpleName.toString();
    }

}
