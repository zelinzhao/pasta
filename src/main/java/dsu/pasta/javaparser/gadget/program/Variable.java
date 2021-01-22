package dsu.pasta.javaparser.gadget.program;

import com.github.javaparser.resolution.types.ResolvedType;
import dsu.pasta.config.UpdateConfig;
import dsu.pasta.javaparser.factory.analyzer.JavaparserSolver;

import java.util.concurrent.atomic.AtomicInteger;

public class Variable {

    private static final String var = "_var";
    private static AtomicInteger index = new AtomicInteger(0);

    private static ResolvedType targetType = null;
    private static ResolvedType newFieldType = null;

    private static Variable oldInstance = new Variable();
    private static Variable newField = new Variable();

    private int id;
    /**
     * Name of this variable: <tt>var_id</tt>
     */
    private String name;
    /**
     * Type of this variable;
     */
    private ResolvedType type;

    public Variable() {
        id = index.getAndIncrement();
        name = var + id + "_";
    }

    public Variable(ResolvedType type) {
        this();
        this.type = type;
    }

    public static ResolvedType getOldInstanceType() {
        if (targetType == null)
            targetType = JavaparserSolver.getType(UpdateConfig.one().targetClass);
        return targetType;
    }

    public static Variable getOldInstance() {
        if (oldInstance.type == null)
            oldInstance.setType(getOldInstanceType());
        return oldInstance;
    }

    public static ResolvedType getNewFieldType() {
        if (newFieldType == null)
            newFieldType = UpdateConfig.one().getNewFieldType();
        return newFieldType;
    }

    public static Variable getNewFieldInstance() {
        if (newField.type == null)
            newField.setType(getNewFieldType());
        return newField;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public ResolvedType getType() {
        return this.type;
    }

    public void setType(ResolvedType type) {
        this.type = type;
    }

    /**
     * like:
     * int a
     *
     * @return
     */
    public String getTypeName() {
        return type == null ? name : JavaparserSolver.myDescribe(type) + " " + name;
    }

    @Override
    public String toString() {
        return type == null ? name : JavaparserSolver.myDescribe(type) + " " + name;
    }

}
