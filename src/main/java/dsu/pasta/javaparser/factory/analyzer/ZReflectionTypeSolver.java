package dsu.pasta.javaparser.factory.analyzer;

import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.util.HashSet;

/**
 * Used to store types that are solved by ReflectionTypeSolver
 */
public class ZReflectionTypeSolver extends ReflectionTypeSolver {
    public static HashSet<String> solvedTypes = new HashSet<>();
    private static HashSet<String> unsolvedTypes = new HashSet<>();

    public ZReflectionTypeSolver(boolean jreOnly) {
        super(jreOnly);
    }

    public ZReflectionTypeSolver() {
        this(true);
    }

    @Override
    protected boolean filterName(String name) {
        if (solvedTypes.contains(name))
            return true;
        if (unsolvedTypes.contains(name))
            return false;
        if (super.filterName(name)) {
            solvedTypes.add(name);
            return true;
        } else {
            unsolvedTypes.add(name);
            return false;
        }
    }
}
