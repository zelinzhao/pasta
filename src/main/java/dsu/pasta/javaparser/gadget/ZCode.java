package dsu.pasta.javaparser.gadget;

import dsu.pasta.javaparser.factory.stmt.ZStatement;
import dsu.pasta.javaparser.gadget.sketch.SketchGadget;

public abstract class ZCode {
    /**
     * Useful while resolving children of a gadget.
     * The left-hand side is target and the right-hand side should be source.
     */
    public boolean nowSource = false;
    /**
     * Is this a source for other variables?
     * Valid for expressions.
     */
    protected boolean isSource = false;
    private SketchGadget sketch;
    /**
     * Parent of this. Null for a top statement.
     */
    private ZCode parent = null;

    public ZCode(ZCode parent) {
        this.parent = parent;
    }

    /**
     * Get the top statement for this. If this gadget is a statement, return itself.
     * If this gadget is an expression, return the statement that contains this.
     *
     * @param zc
     * @return
     */
    public static ZStatement getTopStatement(ZCode zc) {
        if (zc == null)
            return null;
        if (zc instanceof ZStatement)
            return (ZStatement) zc;
        else
            return getTopStatement(zc.parent);
    }

    public SketchGadget getSketch() {
        if (sketch == null) {
            sketch = new SketchGadget();
        }
        return this.sketch;
    }

    public void setSketch(SketchGadget sketch) {
        this.sketch = sketch;
    }

    /**
     * This method only can be called after two ZCode are built completely
     *
     * @return
     */
    @Override
    public int hashCode() {
        if (sketch == null)
            return 0;
        return sketch.hashCode();
    }

    /**
     * This method only can be called after two ZCode are built completely
     *
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ZCode))
            return false;
        ZCode temp = (ZCode) obj;
        if (this.sketch == null && temp.sketch == null)
            return true;
        if (this.sketch == null && temp.sketch != null)
            return false;
        return this.sketch.equals(temp.sketch);
    }

    public String getSketchString() {
        return this.sketch.getSketchString();
    }

    public ZCode getParent() {
        return this.parent;
    }
}
