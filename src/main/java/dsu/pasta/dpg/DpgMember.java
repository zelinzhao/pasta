package dsu.pasta.dpg;

public class DpgMember {
    private String name;
    private String declaringClass;
    private String desc;
    private String changeType; // DEL, ADD, BC

    public DpgMember() {
    }

    /**
     * @param name
     * @param declaringClass
     * @param desc
     * @param changeType,    DEL, ADD, BC
     */
    public DpgMember(String name, String declaringClass, String desc, String changeType) {
        this.name = name;
        this.declaringClass = declaringClass;
        this.desc = desc;
        this.changeType = changeType;
    }

    @Override
    public int hashCode() {
        return (name + declaringClass + desc).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DpgMember))
            return false;
        DpgMember temp = (DpgMember) obj;
        if (!this.name.equals(temp.name))
            return false;
        if (!this.declaringClass.equals(temp.declaringClass))
            return false;
        if (!this.desc.equals(temp.desc))
            return false;
        return true;
    }

    public String getName() {
        return this.name;
    }

    public boolean isADD() {
        return this.changeType != null && this.changeType.equalsIgnoreCase("ADD");
    }
}
