package dsu.pasta.dpg;

public class DpgField extends DpgMember {
    private String fullQualifiedType;

    public DpgField(String name, String declaringClass, String desc, String changeType) {
        super(name, declaringClass, desc, changeType);
    }

    public String getFullQualifiedType() {
        return this.fullQualifiedType;
    }

    public void setFullQualifiedType(String fullQualifiedType) {
        this.fullQualifiedType = fullQualifiedType;
    }
}
