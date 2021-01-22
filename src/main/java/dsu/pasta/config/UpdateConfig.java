package dsu.pasta.config;

import com.github.javaparser.resolution.types.ResolvedType;
import dsu.pasta.javaparser.factory.analyzer.JavaparserSolver;
import org.apache.commons.configuration2.HierarchicalConfiguration;

public class UpdateConfig extends Config {
    public static String javelusXml;
    private static UpdateConfig one = null;
    public String targetClass;
    /**
     * We assume new fields is only one
     */
    public Field newField;

    public static UpdateConfig one() {
        if (one == null) {
            one = new UpdateConfig();
            one.parseConfig();
        }
        return one;
    }

    protected void parseConfig() {
        targetClass = config.getString("update.class");

        HierarchicalConfiguration field = config.configurationsAt("update.newField").get(0);
        newField = new Field();
        newField.realTypeAsString = field.getString("type").trim().replaceAll(" +", " ");
        newField.name = field.getString("name").trim().replaceAll(" +", " ");
        newField.new_name = "new_" + newField.name;
    }

    /**
     * Not contain $.
     *
     * @return
     */
    public String getNewFieldTypeDesc() {
        return newField.realResolvedType.describe();
    }

    /**
     * Contain $ if necessary
     *
     * @return
     */
    public String getNewFieldTypeRealString() {
        return newField.realTypeAsString;
    }

    /**
     * @return like new_ka
     */
    public String getNewFieldNewName() {
        return newField.new_name;
    }

    public String getNewFieldOriginalName() {
        return newField.name;
    }

    public String getCompareTag() {
        return newField.name;
    }

    public void solveFieldType() {
        newField.realResolvedType = JavaparserSolver.getType(newField.realTypeAsString);
    }

    public ResolvedType getNewFieldType() {
        if (newField.realResolvedType == null)
            newField.realResolvedType = JavaparserSolver.getType(newField.realTypeAsString);
        return newField.realResolvedType;
    }

    /**
     * For old and new fields.
     */
    public static class Field {
        public String name;
        public String new_name;
        public String realTypeAsString;
        private ResolvedType realResolvedType;
    }
}
