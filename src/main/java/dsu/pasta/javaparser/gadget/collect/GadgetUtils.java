package dsu.pasta.javaparser.gadget.collect;

import com.github.javaparser.resolution.types.ResolvedType;
import dsu.pasta.config.ProjectConfig;
import dsu.pasta.config.UpdateConfig;
import dsu.pasta.dpg.ExtractProjectUpdatedInfoProcessor;
import dsu.pasta.javaparser.factory.analyzer.JavaparserSolver;
import dsu.pasta.javaparser.gadget.PriorityDefinition;
import dsu.pasta.javaparser.gadget.program.Program;
import dsu.pasta.javaparser.gadget.sketch.*;
import dsu.pasta.utils.ZFileUtils;
import dsu.pasta.utils.ZPrint;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class GadgetUtils {

    /**
     * Given partial <tt>program</tt>, get all usable sketches from <tt>noDataflowSketches</tt> and <tt>usefulDataflows</tt>.
     *
     * @param program       current partial program.
     * @param ignoreSpecial if <tt>true</tt>, skip special gadgets. See <tt>Sketch#usable</tt>.
     */
    public static List<SketchGadget> getNextUsableSketchesFromNarrowScope(
            List<Sketch> allSketch,
            Program program, boolean ignoreSpecial, boolean tryMore) {
        List<SketchGadget> usable = new ArrayList<>();
        Collection<ResolvedType> knownTypes = program.getKnownTypes();

        Iterator it = allSketch.iterator();
        while (it.hasNext()) {
            Sketch sk = (Sketch) it.next();
            if (!(sk instanceof SketchGadget))
                continue;
            if (sk.getSketchString().contains("dsu") && !(sk instanceof SketchForeachCondition))
                continue;

            SketchGadget sg = (SketchGadget) sk;
            if (sg.usable(ignoreSpecial) || sg.usable(knownTypes, ignoreSpecial)) {
                usable.add(sg);
            }
        }
        int left = 1000 - usable.size();
        for (int i = 0; i < GadgetsReader.uselessSketch.size() && i < left; i++) {
            Sketch sk = GadgetsReader.uselessSketch.get(i);
            if (!(sk instanceof SketchGadget))
                continue;
            if (sk.getSketchString().contains("dsu") && !(sk instanceof SketchForeachCondition))
                continue;
            if (sk.getPriority() <= PriorityDefinition.uselessGadget.getPriority() && !tryMore)
                continue;
            usable.add((SketchGadget) sk);
        }
        return usable;
    }

    /**
     * Read type distance information from the old and new <tt>BuildCallGraphFromCode.TypeDistanceName</tt> files.
     *
     * @return
     */
    public static HashMap<String, Integer> readDistancesOfType() {
        if (UpdateConfig.one().getNewFieldType().isPrimitive() || UpdateConfig.one().getNewFieldTypeRealString().equals(String.class.getName())) {
            ZPrint.verbose("New field type is " + UpdateConfig.one().getNewFieldTypeRealString() + ", read gadgets within distance 1");
            ProjectConfig.readGadgetRange = 1;
        }
        HashMap<String, Integer> typeDistance = new HashMap<>();
        for (File f : ZFileUtils.findFileRecursivelyWithPattern(ProjectConfig.one().logRoot, ProjectConfig.TypeDistanceName)) {
            ZPrint.verbose("Read type distance from " + f.getAbsolutePath());
            for (String s : ZFileUtils.readFileToArray(f.getAbsolutePath())) {
                int lastComma = s.lastIndexOf(',');
                String type = s.substring(0, lastComma);
                String distance = s.substring(lastComma + 1);
                int dis = Math.abs(Integer.valueOf(distance));
                if (dis >= ProjectConfig.readGadgetRange)
                    continue;
                if (typeDistance.containsKey(type)) {
                    dis = Math.min(dis, typeDistance.get(type));
                }
                typeDistance.put(type, dis);
            }
        }
        ZPrint.verbose("Add special API types as distance 0");
        for (String t : ProjectConfig.apiTypes) {
            if (!typeDistance.containsKey(t))
                typeDistance.put(t, 0);
        }
        return typeDistance;
    }

    /**
     * Read gadgets and contexts according to types' distance<tt>typeDistance</tt>.
     * <p>Gadgets are put into <tt>allSketch</tt></p>
     * <p>Contexts are put into allContexts</p>
     *
     * @param allSketch    gadgets are put into this and return;
     * @param allContext   contexts are put into this and return;
     * @param typeDistance
     */
    public static void readGadgetsFromTypes(
            List<Sketch> allSketch,
            List<Context> allContext,
            HashMap<String, Integer> typeDistance) {
        HashSet<String> done = new HashSet<>();
        HashSet<String> skip = new HashSet<>();
        for (String type : typeDistance.keySet()) {
            List<Sketch> tempSketch = new ArrayList<>();
            List<Context> tempContext = new ArrayList<>();

            int distance = typeDistance.get(type);
            boolean read = readTwoVersionGadgets(type, tempSketch, tempContext);
            if (read)
                done.add(type);
            else {
                ZPrint.verbose("Not read gadgets from " + type);
                skip.add(type);
                continue;
            }
            GadgetsReader.allReadSketchNumber += tempSketch.size();
            ZPrint.verbose("Before filter: " + tempSketch.size() + " sketches, " + tempContext.size() + " contexts");
            //setting
            //filtering useless gadgets if they are from types that are too far away
            setAndFilterSketchKeepUseless(tempSketch, tempContext, distance >= 0, distance == 0);
            ZPrint.verbose("After filter: " + tempSketch.size() + " sketches, " + tempContext.size() + " contexts");

            boolean isApi = ProjectConfig.apiTypes.contains(type);
            if (isApi)
                ZPrint.verbose("Gadgets from api class");

            setPriorityToGadget(tempSketch, isApi);

            ZPrint.verbose(type + " has gadgets " + tempSketch.size());
            allSketch.addAll(tempSketch);
            allContext.addAll(tempContext);
        }
        ZPrint.verbose("Read gadgets from " + done.size() + " types");
        ZPrint.verbose("All sketch size " + allSketch.size());
        ZPrint.verbose("All context size " + allContext.size());

    }

    private static void setPriorityToGadget(List<Sketch> allSketch, boolean isApi) {
        HashSet<String> types = ExtractProjectUpdatedInfoProcessor.getChangedFieldTypesOfChangedClass(UpdateConfig.one().targetClass);
        HashSet<String> names = ExtractProjectUpdatedInfoProcessor.getChangedFieldNamesOfChangedClass(UpdateConfig.one().targetClass);

        for (Sketch sk : allSketch) {
            if (sk == null)
                continue;
            if (!(sk instanceof SketchGadget))
                continue;
            SketchGadget sg = (SketchGadget) sk;
            HashSet<ResolvedType> allTypes = new HashSet<>(sg.getSourceHoleTypes());
            allTypes.addAll(sg.getTargetHoleTypes());
            Set<String> allTypeStrs = allTypes.stream().map(t -> JavaparserSolver.myDescribe(t)).collect(Collectors.toSet());

            //whether gadgets uses the type of removed field
            allTypeStrs.retainAll(types);
            if (allTypeStrs.size() > 0) {
                sg.incrementPriority(PriorityDefinition.useOtherChangedFieldInstance);
                sg.useRemovedField = true;
            }
            for (Element e : sg.getElements()) {
                if (e.getOriginalString() != null) {
                    if (e.getOriginalString().equals(UpdateConfig.one().getNewFieldOriginalName())) {
                        sg.incrementPriority(PriorityDefinition.nameSameWithNewField);
                        sg.hasTargetFieldName = true;
                        if (names.size() == 0) {
                            sg.incrementPriority(PriorityDefinition.useOtherChangedFieldInstance);
                            sg.useRemovedField = true;
                        }
                    }
                    if (names.contains(e.getOriginalString())) {
                        sg.incrementPriority(PriorityDefinition.useOtherChangedFieldInstance);
                        sg.useRemovedField = true;
                    }
                }
            }
            if (sg.isConstructor()) {
                sg.incrementPriority(PriorityDefinition.apiConstructor);
                sg.useApiConstructor = true;
            }
            if (sg.isChangeAllMethod()) {
                sg.incrementPriority(PriorityDefinition.apiChangeAllMethod);
                sg.useApiChangeAllMethod = true;
            }

        }
    }

    /**
     * <p>Mark gadgets that generate new fields.</p>
     * <p>For context, find dataflows about new field isntance. Remove context if it has no such dataflow.</p>
     *
     * @param tempSketch
     * @param tempContext
     * @param removeAllUseless
     */
    private static void setAndFilterSketchKeepUseless(List<Sketch> tempSketch, List<Context> tempContext, boolean removeAllUseless, boolean keepNoCtx) {
        //mark gadgets that generate new fields
        for (Sketch sk : tempSketch) {
            if (!(sk instanceof SketchGadget))
                continue;
            SketchGadget sg = (SketchGadget) sk;
            if (sg.canGenerate(UpdateConfig.one().getNewFieldType(), true))
                sg.setDistanceToGenerateNewFieldType(0);
        }
        //find dataflow that about new field instance.
        //removeAllUseless method context if it has no such dataflow
        //removeAllUseless sketch inside removed contexts
        //removeAllUseless context inside removed contexts
        List<Context> keepContext = new ArrayList<>();
        Iterator<Context> ctxIt = tempContext.iterator();
        while (ctxIt.hasNext()) {
            Context ctx = ctxIt.next();
            if (ctx.isMethodContext()) {
                ctx.resolveDistanceToTypeForMethodCtx(UpdateConfig.one().getNewFieldType());
                if (ctx.getDataflowsToTarget() == null || ctx.getDataflowsToTarget().size() == 0) {
                    if (removeAllUseless) {
                        GadgetsReader.uselessSketch.addAll(ctx.getAllInsideSketch());
                        ctxIt.remove();
                        tempSketch.removeAll(ctx.getAllInsideSketch());
                        continue;
                    }
                }
                keepContext.add(ctx);
            }
        }
        tempContext.clear();
        tempContext.addAll(keepContext);
        //setting gadgets without context
        HashSet<String> duplicate = new HashSet<>();

        Dataflow df = null;
        Iterator<Sketch> sgIt = tempSketch.iterator();
        while (sgIt.hasNext()) {
            Sketch sketch = sgIt.next();
            if (!(sketch instanceof SketchGadget)) {
                if (removeAllUseless) {
                    sgIt.remove();
                    GadgetsReader.uselessSketch.add(sketch);
                }
                continue;
            }
            if (sketch.canGenerate(UpdateConfig.one().getNewFieldType(), true)
                    || sketch.needTheType(UpdateConfig.one().getNewFieldType())) {
                sketch.setDistanceToGenerateNewFieldType(0);
            }

            if (((SketchGadget) sketch).getOuterDataflow() != null
                    && ((SketchGadget) sketch).getOuterDataflow().size() > 0) {
                continue;
            }
//            //removeAllUseless gadget that has context.
//            if(sketch.getOuterContext()!=null) {
//                if(removeAllUseless)
//                    sgIt.remove();
//                continue;
//            }
            //gadget has no context in following
            SketchGadget sg = (SketchGadget) sketch;
            if (sg.canGenerate(UpdateConfig.one().getNewFieldType(), true)) {
                df = new Dataflow();
                df.addGadgetToTail(sg);
                df.resolveDistanceGetNarrowFlow(UpdateConfig.one().getNewFieldType());
            } else if (sg.needTheType(UpdateConfig.one().getNewFieldType())) {
                df = new Dataflow();
                df.addGadgetToTail(sg);
                df.resolveDistanceGetNarrowFlow(UpdateConfig.one().getNewFieldType());
            } else if (keepNoCtx && sketch.getOuterContext() == null) { // field and method declaration
                if (duplicate.contains(Program.getCleanCode(sg.getSketchString())))
                    sgIt.remove();
                else
                    duplicate.add(Program.getCleanCode(sg.getSketchString()));
            } else if (removeAllUseless) {
                sgIt.remove();
                GadgetsReader.uselessSketch.add(sg);
            }
        }
    }

    /**
     * Read gadgets and context from the <tt>className</tt>'s file.
     * We will assign different priority to same/different gadgets between old and new gadgets.
     *
     * @param className
     * @param allSketch  gadgets are put into this.
     * @param allContext contexts are put into this.
     * @return <tt>true</tt> if any gadgets/contexts are read;
     * <p><tt>false</tt> if no gadgets/contexts are read. e.g. <tt>className == null</tt>, or we skip reading byte gadgets and this class has no source file.</p>
     */
    public static boolean readTwoVersionGadgets(String className,
                                                List<Sketch> allSketch,
                                                List<Context> allContext) {
        if (className == null)
            return false;
        //flag that control whether to read gadgets from byte code?
        if (!ProjectConfig.apiTypes.contains(className)
                && !JavaparserSolver.isSourceType(className)
//                && ProjectConfig.one().skip_read_byte_gadget
        ) {
            return false;
        }
        if (ExtractProjectUpdatedInfoProcessor.isChangedClass(className)) {
            HashMap<Integer, Sketch> idOldSketch = new HashMap<>();
            HashMap<Integer, Context> idOldContext = new HashMap<>();
            HashMap<Integer, Sketch> idNewSketch = new HashMap<>();
            HashMap<Integer, Context> idNewContext = new HashMap<>();
            read(className, "old", idOldSketch, idOldContext);
            read(className, "new", idNewSketch, idNewContext);

            HashSet<Sketch> oldSketches = new HashSet<>(idOldSketch.values());
            HashSet<Sketch> newSketches = new HashSet<>(idNewSketch.values());

            //mark gadgets that use added fields
            markAddedFieldsGadgets(newSketches, className);

            HashSet<Sketch> temp = new HashSet<>(oldSketches);

            temp.retainAll(newSketches);
            oldSketches.removeAll(temp);
            newSketches.removeAll(temp);
            //now, temp is the intersection set of old and new sketches
            //oldSketches contains only diff sketches in old version
            //newSketches contains only diff sketches in new version
            //now, purify old and new sketches
            Set<String> oldStrings = oldSketches.stream().map(s -> s.getSketchString()).collect(Collectors.toSet());
            Set<String> newStrings = newSketches.stream().map(s -> s.getSketchString()).collect(Collectors.toSet());
            Iterator<Sketch> oldIt = oldSketches.iterator();
            while (oldIt.hasNext()) {
                Sketch sk = oldIt.next();
                if (newStrings.contains(sk.getSketchString())) {
                    temp.add(sk);
                    oldIt.remove();
                }
            }
            Iterator<Sketch> newIt = newSketches.iterator();
            while (newIt.hasNext()) {
                Sketch sk = newIt.next();
                if (oldStrings.contains(sk.getSketchString())) {
                    temp.add(sk);
                    newIt.remove();
                }
            }
//            int prioritySame = PriorityDefinition.getPriorityAccordingToClass(className,true);
            int priorityDiff = PriorityDefinition.getPriorityAccordingToClass(className, false);
//            incrementPriorityToAll(temp, prioritySame);
            incrementPriorityToAll(oldSketches, priorityDiff);
            incrementPriorityToAll(newSketches, priorityDiff);
            for (Sketch sk : oldSketches) {
                sk.isDiffCode = true;
            }
            for (Sketch sk : newSketches)
                sk.isDiffCode = true;

            allSketch.addAll(temp);
            allSketch.addAll(oldSketches);
            allSketch.addAll(newSketches);

            allContext.addAll(idOldContext.values());
            allContext.addAll(idNewContext.values());
        } else {
//            int prioritySame = PriorityDefinition.getPriorityAccordingToClass(className,true);
            HashMap<Integer, Sketch> idOldSketch = new HashMap<>();
            HashMap<Integer, Context> idOldContext = new HashMap<>();
            read(className, "old", idOldSketch, idOldContext);
//            incrementPriorityToAll(idOldSketch.values(), prioritySame);
            allSketch.addAll(idOldSketch.values());
            allContext.addAll(idOldContext.values());
        }
        for (Sketch sk : allSketch) {
            sk.decrementPriorityByHoles();
        }
        return true;
    }

    public static void mergeConstantGadgets(Collection<Sketch> allSketch) {
        HashSet<SketchConstant> merge = new HashSet<>();
        Iterator<Sketch> iter = allSketch.iterator();
        int before = 0;
        while (iter.hasNext()) {
            Sketch sk = iter.next();
            if (sk instanceof SketchConstant) {
                SketchConstant temp = (SketchConstant) sk;
                merge.add(new SketchConstant(temp.getOriginalCode(), temp.getConstantType()));
                iter.remove();
                before++;
            }
        }
        ZPrint.verbose("Before merging constants, there are " + before + " constants.");
        ZPrint.verbose("After merging constants, there are " + merge.size() + " constants.");
        allSketch.addAll(merge);
    }

    public static HashMap<Sketch, ArrayList<Sketch>> mergeGadgets(Collection<Sketch> allSketch) {
        LinkedHashMap<String, ArrayList<Sketch>> temp = new LinkedHashMap<>();
        for (Sketch sk : allSketch) {
            String skStr = sk.getSketchString();
            ArrayList<Sketch> list = temp.get(skStr);
            if (list == null)
                list = new ArrayList<>();
            list.add(sk);
            temp.put(skStr, list);

        }
        HashMap<Sketch, ArrayList<Sketch>> mergedSketches = new HashMap<>();
        for (String sk : temp.keySet()) {
            ArrayList<Sketch> list = temp.get(sk);
            if (list == null || list.size() == 0)
                continue;
            list.sort(Sketch.distanceComparator);

            Sketch key = list.remove(0);
            mergedSketches.put(key, list);
        }
        return mergedSketches;
    }

    /**
     * While reading gadgets from new version class, there maybe added fields.
     * Some gadgets in new version class may have field access gadgets of added fields, we should mark these gadgets.
     */
    private static void markAddedFieldsGadgets(Collection<Sketch> allSketch, String className) {
        for (Sketch sk : allSketch) {
            if (sk instanceof SketchGadget) {
                ((SketchGadget) sk).resolveAddedFieldAccess(className);
            }
        }
    }

    private static Collection<Sketch> incrementPriorityToAll(Collection<Sketch> sketches, int priority) {
        for (Sketch sk : sketches)
            sk.incrementPriority(priority);
        return sketches;
    }

    public static void read(String file, HashMap<Integer, Sketch> idSketch, HashMap<Integer, Context> idContext) {
        Sketch.readFile(file, idSketch, idContext);
    }

    public static void read(String className, String version, HashMap<Integer, Sketch> idSketch, HashMap<Integer, Context> idContext) {
        //flag that control whether to read gadgets from byte code?
        if (!ProjectConfig.apiTypes.contains(className)
                && !JavaparserSolver.isSourceType(className)
//                && ProjectConfig.one().skip_read_byte_gadget
        ) {
            return;
        }
        read(JavaparserSolver.getGadgetsFile(className, version), idSketch, idContext);
    }

}
