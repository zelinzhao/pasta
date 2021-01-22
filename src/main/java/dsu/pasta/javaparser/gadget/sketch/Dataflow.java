package dsu.pasta.javaparser.gadget.sketch;

import com.github.javaparser.Position;
import com.github.javaparser.resolution.types.ResolvedType;
import dsu.pasta.config.ProjectConfig;
import dsu.pasta.javaparser.factory.analyzer.JavaparserSolver;

import java.util.*;

public class Dataflow {
    public static HashMap<ResolvedType, Integer> distanceBetweenSourceTarget = new HashMap<>();

    public static HashMap<Dataflow, Integer> allDataflows = new HashMap<>();
    public static HashSet<Dataflow> dataflowsCanGenerateNewField = new HashSet<>();
    public static int maxSize = 0;
    public static Comparator<Dataflow> appearTimesComparator = new Comparator<Dataflow>() {
        @Override
        public int compare(Dataflow o1, Dataflow o2) {
            return o1.size() - o2.size();
        }
    };
    private static HashSet<String> dataflowCompare = new HashSet<>();
    protected LinkedList<SketchGadget> flow = new LinkedList<>();
    private boolean canGenerateTargetInstance = false;
    private Context inMethodContext = null;
    private Position begin;
    private Position end;

    public static void addToAll(Dataflow df) {
        if (df.onlyHasControlFlow())
            return;
        if (dataflowCompare.contains(df.forCompare()))
            return;
        dataflowCompare.add(df.forCompare());
        if (allDataflows.containsKey(df)) {
            int times = allDataflows.get(df);
            times++;
            allDataflows.put(df, times);
        } else {
            allDataflows.put(df, 1);
        }
        if (df.canGenerateTargetInstance) {
            dataflowsCanGenerateNewField.add(df);
            if (df.size() > maxSize)
                maxSize = df.size();
        }
    }

    public static List<Dataflow> getSortedAllDfBySize() {
        List<Dataflow> sorted = new ArrayList<>();
        sorted.addAll(allDataflows.keySet());
        sorted.sort(appearTimesComparator);
        return sorted;
    }

    private static void putDistance(ResolvedType type, int newDistance) {
        if (type == null)
            return;
        if (type.isPrimitive() || JavaparserSolver.myDescribe(type).equals(String.class.getName()))
            return;
        try {
            Integer oldDis = distanceBetweenSourceTarget.get(type);
            if (oldDis == null || oldDis > newDistance) {
                distanceBetweenSourceTarget.put(type, newDistance);
            }
        } catch (Exception e) {

        }
    }

    public LinkedList<SketchGadget> getFlow() {
        return flow;
    }

    public boolean overlapWith(Dataflow target) {
        if (this.inMethodContext == null || target.inMethodContext == null)
            return false;
        if (!this.inMethodContext.equals(target.inMethodContext))
            return false;
        if (this.begin == null || target.end == null || this.end == null || target.begin == null)
            return false;
        if (this.begin.isAfter(target.end) || this.end.isBefore(target.begin))
            return false;
        return true;
    }

    public void setInMethodContext(Context inMethodContext) {
        this.inMethodContext = inMethodContext;
    }

    private void setPosition(SketchGadget sg) {
        Position b = sg.getBegin();
        Position e = sg.getEnd();
        if (this.begin == null || (b != null && b.isBefore(begin)))
            begin = b;
        if (this.end == null || (e != null && e.isAfter(end)))
            end = e;
    }

    public void addGadgetToHead(SketchGadget sketchGadget) {
        if (this.flow.contains(sketchGadget))
            return;
        this.flow.addFirst(sketchGadget);
        sketchGadget.addOuterDataflow(this);
        setPosition(sketchGadget);
        this.flow.sort(Sketch.rangeComparator);
    }

    public void addGadgetToHead(List<SketchGadget> sketchGadgets) {
        for (int i = sketchGadgets.size() - 1; i >= 0; i--) {
            SketchGadget sg = sketchGadgets.get(i);
            this.addGadgetToHead(sketchGadgets.get(i));
            sg.addOuterDataflow(this);
        }
        this.flow.sort(Sketch.rangeComparator);
    }

    public void addGadgetToTail(SketchGadget sketchGadget) {
        if (this.flow.contains(sketchGadget))
            return;
        this.flow.addLast(sketchGadget);
        sketchGadget.addOuterDataflow(this);
        setPosition(sketchGadget);
        this.flow.sort(Sketch.rangeComparator);
    }

    public int size() {
        return this.flow.size();
    }

    public boolean onlyHasControlFlow() {
        for (SketchGadget sg : this.flow) {
            if (!sg.isLoop() && !sg.isIfStmt())
                return false;
        }
        return true;
    }

    public List<Dataflow> resolveDistanceGetNarrowFlow(ResolvedType target) {
        List<Dataflow> narrows = new ArrayList<>();
        int preIndex = 0;
        for (int i = 0; i < this.flow.size(); i++) {
            SketchGadget gs = this.flow.get(i);
            if (gs.canGenerate(target, false)) {
                if (!gs.isFieldAccessThisAsTarget())
                    this.canGenerateTargetInstance = true;
                Dataflow narrow = gs.getNarrowFlowSetDistance(this.flow);
                if (narrow != null) {
                    Dataflow.addToAll(narrow);
                    narrows.add(narrow);
                }
                if (i - preIndex >= ProjectConfig.maxTransLen)
                    preIndex = i - ProjectConfig.maxTransLen;
                for (int r = preIndex; r <= i; r++) {
                    SketchGadget temp = this.flow.get(r);
                    putDistance(temp.generate(), temp.getDistanceToGenerateNewFieldType());
                }
                preIndex = i + 1;
                for (ResolvedType type : gs.getSourceHoleTypes())
                    putDistance(type, 1);
            } else if (gs.needTheType(target)) {
                Dataflow narrow = gs.getNarrowFlowSetDistance(this.flow);
                if (narrow != null) {
                    Dataflow.addToAll(narrow);
                    narrows.add(narrow);
                }
                if (i - preIndex >= ProjectConfig.maxTransLen)
                    preIndex = i - ProjectConfig.maxTransLen;
                for (int r = preIndex; r <= i; r++) {
                    SketchGadget temp = this.flow.get(r);
                    putDistance(temp.generate(), temp.getDistanceToGenerateNewFieldType());
                }
                preIndex = i + 1;
            }
        }
        Dataflow.addToAll(this);
        return narrows;
    }

    public boolean isCanGenerateTargetInstance() {
        return canGenerateTargetInstance;
    }

    public void setCanGenerateTargetInstance(boolean canGenerateTargetInstance) {
        this.canGenerateTargetInstance = canGenerateTargetInstance;
    }

    public String forCompare() {
        String result = "";
        for (SketchGadget sg : this.flow)
            result += sg.getSketchString();
        return result;
    }

    public String toString() {
        String result = "Data dependency size: " + this.flow.size() + "\n";
        for (SketchGadget sg : this.flow)
            result += sg.getRangeAsString() + "@" + sg.getSketchString();
        return result;
    }
//    public void addGadgetToIndex(SketchGadget sketchGadget, int index) {
//        if (index > this.flow.size())
//            this.flow.addLast(sketchGadget);
//        else
//            this.flow.add(index, sketchGadget);
//        sketchGadget.addOuterDataflow(this);
//        setPosition(sketchGadget);
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        if (obj == null) {
//            return false;
//        }
//        if (obj == this) {
//            return true;
//        }
//        if (obj.getClass() != getClass()) {
//            return false;
//        }
//        Dataflow temp = (Dataflow) obj;
//        if(this.flow.size()!=temp.flow.size())
//            return false;
//        for(int i=0; i<this.flow.size(); i++){
//            if(!this.flow.get(i).getSketchString().equals(temp.flow.get(i).getSketchString()))
//                return false;
//        }
//        return true;
//    }
//
//    @Override
//    public int hashCode() {
//        HashCodeBuilder hcb = new HashCodeBuilder(13, 23);
//        for(SketchGadget sg: this.flow)
//            hcb.append(sg.getSketchString());
//        return hcb.toHashCode();
//    }
}
