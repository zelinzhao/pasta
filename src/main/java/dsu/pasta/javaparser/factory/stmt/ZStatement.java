package dsu.pasta.javaparser.factory.stmt;

import dsu.pasta.javaparser.gadget.ZCode;
import dsu.pasta.javaparser.gadget.sketch.Context;

import java.util.List;

public abstract class ZStatement extends ZCode {
    //how many lines of this statement
    protected int sizes;

    public ZStatement(ZCode parent) {
        super(parent);
    }

    protected static void addContextTo(Context outerContext, List<ZCode> body) {
        for (ZCode zc : body) {
            zc.getSketch().setOuterContext(outerContext);
//			if(zc.getSketch().getReplace()!=null)
//				zc.getSketch().getReplace().setOuterContext(outerContext);
            if (zc.getSketch().get_thisAsContext() != null)
                zc.getSketch().get_thisAsContext().setOuterContext(outerContext);
        }
    }

    public int getSizes() {
        return sizes;
    }
}
