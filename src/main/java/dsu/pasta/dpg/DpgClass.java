package dsu.pasta.dpg;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class DpgClass {
    private String nameWith$;
    private String nameWithout$;
    private HashSet<DpgField> sameFields = new HashSet<>();
    private HashSet<DpgField> changedFields = new HashSet<>();
    private HashSet<DpgMethod> changedMethods = new HashSet<>();

    public DpgClass(String name) {
        this.nameWith$ = name;
        this.nameWithout$ = name.replaceAll("\\$", "\\.");
    }

    public String getNameWith$() {
        return this.nameWith$;
    }

    public String getNameWithout$() {
        return this.nameWithout$;
    }

    public void addSameField(String name, String declaringClass, String desc) {
        sameFields.add(new DpgField(name, declaringClass, desc, null));
    }

    public void addChangedField(String name, String declaringClass, String desc, String changeType) {
        changedFields.add(new DpgField(name, declaringClass, desc, changeType));
    }

    public void addChangedMethod(String name, String declaringClass, String desc, String changeType) {
        changedMethods.add(new DpgMethod(name, declaringClass, desc, changeType));
    }

    public HashSet<DpgField> getChangedFields() {
        return this.changedFields;
    }

    public HashSet<DpgField> getSameFields() {
        return this.sameFields;
    }

    public Set<String> getSameFieldsTypes() {
        return this.sameFields.stream().map(f -> f.getFullQualifiedType()).collect(Collectors.toSet());
    }

    public Set<String> getSameFieldsNames() {
        return this.sameFields.stream().map(f -> f.getName()).collect(Collectors.toSet());
    }

    public Set<String> getChangedFieldsTypes() {
        return this.changedFields.stream().map(f -> f.getFullQualifiedType()).collect(Collectors.toSet());
    }

    public Set<String> getChangedFieldsNames() {
        return this.changedFields.stream().map(f -> f.getName()).collect(Collectors.toSet());
    }
}
