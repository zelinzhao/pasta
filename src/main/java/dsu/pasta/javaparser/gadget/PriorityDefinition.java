package dsu.pasta.javaparser.gadget;

import dsu.pasta.config.UpdateConfig;
import dsu.pasta.dpg.ExtractProjectUpdatedInfoProcessor;

import java.util.Set;

@Deprecated
public enum PriorityDefinition {
    ///////PriorityDefinition for gadgets
    /////////////////////////////
    /**
     * The gadget appears in different code of two version program
     */
    inDiffCodeOfTargetClass(10),
    /**
     * The gadget appears in the target (changed) class
     */
    inSameCodeOfTargetClass(8),
    /**
     * The gadget appears in different code of other changed classes
     */
    inDiffCodeOfOtherChangedClass(2),
    /**
     * The gadget appears in same code of other changed classes
     */
    inSameCodeOfOtherChangedClass(1),
    /**
     * The gadget appears in the target field's class (sometimes, the target field is unchanged)
     */
    inTargetFieldClass(6),
    /**
     * The gadget appears in other changed field's class (excludes target field's class)
     */
    inChangedFieldClass(5),
    /**
     * The gadget appears in unchanged field's class (of target class)
     */
    inSameFieldClass(2),

    /**
     * This gadget can generate instance of target class
     */
    canGenTargetInstance(0),
    /**
     * This gadget can generate instance for the new field
     */
    canGenNewFieldInstance(10),
    /**
     * This gadget can generate instance for other changed fields
     */
    canGenOtherChangedFieldInstance(6),

    /**
     * This gadget uses target class instance
     */
    useTargetInstance(8),
    /**
     * This gadget uses new field instance
     */
    useNewFieldInstance(4),
    /**
     * This gadget uses other changed fields (exclude target field)
     */
    useOtherChangedFieldInstance(10),
    /**
     * This gadget uses unchanged fields
     */
    useUnchangedFieldInstance(4),

    specialMethod(10),
    apiConstructor(10),
    apiChangeAllMethod(10),
    uselessGadget(-50),
    nameSameWithNewField(10),
    /**
     * This gadget only assign null to target, negative property
     */
    onlyAssignNull(0),

    /**
     * The base priority for similarity (0-1) with new field's name
     */
    basePriorityOfSimilarWithNewFieldName(100),
    /**
     * The base priority for similarity (0-1) with other changed fields' names
     */
    basePriorityOfSimilarWithOtherChangedFieldNames(60),
    /**
     * The base priority for similarity (0-1) with unchanged fields' names
     */
    basePriorityOfSimilaryWithSameFieldNames(40);

    public static int addScoreIfOverPriority = 15;

    private int priority;

    private PriorityDefinition(int priority) {
        this.priority = priority;
    }

    public static int getPriorityAccordingToClass(String className, boolean needSame) {
        int priority = 0;
        if (className.equals(UpdateConfig.one().targetClass)) {
            if (needSame)
                priority += inSameCodeOfTargetClass.getPriority();
            else
                priority += inDiffCodeOfTargetClass.getPriority();
        }
        Set<String> otherChangedClass = ExtractProjectUpdatedInfoProcessor.getAllChangedClasses();
        otherChangedClass.remove(UpdateConfig.one().targetClass);
        if (otherChangedClass.contains(className)) {
            if (needSame)
                priority += inSameCodeOfOtherChangedClass.getPriority();
            else
                priority += inDiffCodeOfOtherChangedClass.getPriority();
        }
        if (className.equals(UpdateConfig.one().getNewFieldTypeRealString())) {
            priority += inTargetFieldClass.getPriority();
        }
        Set<String> changedFieldClass = ExtractProjectUpdatedInfoProcessor.getChangedFieldTypesOfChangedClass(UpdateConfig.one().targetClass);
        changedFieldClass.remove(UpdateConfig.one().getNewFieldTypeRealString());
        if (changedFieldClass.contains(className)) {
            priority += inChangedFieldClass.getPriority();
        }
        Set<String> sameFieldClass = ExtractProjectUpdatedInfoProcessor.getSameFieldTypesOfChangedClass(UpdateConfig.one().targetClass);
        if (sameFieldClass.contains(className)) {
            priority += inSameFieldClass.getPriority();
        }
        return priority;
    }

    public int getPriority() {
        return priority;
    }

}
