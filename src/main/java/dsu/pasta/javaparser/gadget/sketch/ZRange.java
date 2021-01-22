package dsu.pasta.javaparser.gadget.sketch;

import com.github.javaparser.Position;
import com.github.javaparser.Range;

public abstract class ZRange {
    protected Range range;

    public Range getRange() {
        return range;
    }

    public void setRange(String positions) {
        if (positions == null)
            return;
        positions = positions.trim();
        String[] s = positions.split(",");
        if (s.length != 4)
            return;
        this.range = new Range(
                new Position(Integer.valueOf(s[0]), Integer.valueOf(s[1])),
                new Position(Integer.valueOf(s[2]), Integer.valueOf(s[3])));
    }

    public void setRange(Range range) {
        this.range = range;
    }

    public void setRange(int beginLine, int beginColumn, int endLine, int endColumn) {
        this.range = new Range(
                new Position(beginLine, beginColumn),
                new Position(endLine, endColumn));
    }

    public String getRangeAsString() {
        if (range == null)
            return "";
        return " " + range.begin.line + "," + range.begin.column + "," + range.end.line + "," + range.end.column;
    }

    public boolean isAfter(Range ran) {
        if (this.range == null)
            return false;
        if (ran == null)
            return false;
        return this.range.begin.isAfter(ran.end);
    }

    public boolean contains(Range ran) {
        if (this.range == null || ran == null)
            return false;
        return this.range.contains(ran);
    }

    public Position getBegin() {
        return range == null ? null : range.begin;
    }

    public Position getEnd() {
        return range == null ? null : range.end;
    }
}
