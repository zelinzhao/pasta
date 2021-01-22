package dsu.pasta.javaparser.gadget.program;

import com.github.javaparser.resolution.types.ResolvedType;
import dsu.pasta.javaparser.factory.analyzer.JavaparserSolver;
import dsu.pasta.javaparser.gadget.sketch.Hole;
import dsu.pasta.utils.ZPrint;

import java.util.*;

public class AvailableVariables {

    /**
     * How many times the new field instance are written to, after initialization
     */
    int latestRWNewField = 0;
    int preRWNewField = 0;
    private HashMap<Hole, Variable> holeToVar = new HashMap<>();
    private HashMap<ResolvedType, ArrayList<Variable>> typeToVars = new HashMap<>();

    @Override
    public AvailableVariables clone() {
        AvailableVariables vars = new AvailableVariables();
        vars.holeToVar = new HashMap<>(this.holeToVar);
        for (ResolvedType type : this.typeToVars.keySet()) {
            ArrayList<Variable> target = new ArrayList<>(this.typeToVars.get(type));
            vars.typeToVars.put(type, target);
        }
        vars.latestRWNewField = this.latestRWNewField;
        vars.preRWNewField = this.preRWNewField;
        return vars;
    }

    /**
     * Add variable for a type.
     * There maybe multiple variables for one type.
     *
     * @param type
     * @param var
     */
    public void add(ResolvedType type, Variable var) {
        if (type == null)
            return;
        ArrayList<Variable> list = typeToVars.get(type);
        if (list == null)
            list = new ArrayList<>();
        list.add(var);
        typeToVars.put(type, list);
    }

    /**
     * Add variable for a hole.
     * If there is an existing variable for hole, we don't put var in.
     *
     * @param hole
     * @param var
     */
    protected void add(Hole hole, Variable var) {
        if (holeToVar.containsKey(hole)) {
            ZPrint.verbose("Already has variable " + holeToVar.get(hole) + " for " + hole.toString());
        } else
            holeToVar.put(hole, var);
        add(hole.getType(), var);
    }

    private List<Variable> complexGetVariableForType(ResolvedType type, boolean initField) {
        List<Variable> variables = new ArrayList<>();
        for (ResolvedType rt : this.typeToVars.keySet()) {
            if (rt.equals(Variable.getNewFieldType()) && latestRWNewField == 0)
                continue;
            if (rt.equals(type)) {
                variables.addAll(0, this.typeToVars.get(rt));
            } else if (JavaparserSolver.isAssignableBy(type, rt)) {
                variables.addAll(this.typeToVars.get(rt));
            }
        }
        if (initField && JavaparserSolver.isAssignableBy(Variable.getNewFieldType(), type) && !variables.contains(Variable.getNewFieldInstance())) {
            variables.add(0, Variable.getNewFieldInstance());

        }
        return variables;
    }

    public Variable getAccurateVariableForHole(Hole hole, boolean forTarget, Program nowProgram) {
        boolean initField = false;
        if (JavaparserSolver.isAssignableBy(Variable.getNewFieldType(), hole.getType()) && forTarget) {
            initField = true;
            latestRWNewField++;
            return Variable.getNewFieldInstance();
        }
        if (JavaparserSolver.isAssignableBy(hole.getType(), Variable.getNewFieldType()) && !forTarget)
            latestRWNewField++;
        List<Variable> vars = complexGetVariableForType(hole.getType(), initField);
        if (vars.size() == 0)
            return null;
        return vars.get(0);
    }

    public List<Variable> getVariables(Hole hole, boolean forTarget, Program nowProgram) {
        boolean initField = false;
        if (JavaparserSolver.isAssignableBy(Variable.getNewFieldType(), hole.getType()) && forTarget) {
            latestRWNewField++;
            initField = true;
        }
        if (JavaparserSolver.isAssignableBy(hole.getType(), Variable.getNewFieldType()) && !forTarget)
            latestRWNewField++;
        return complexGetVariableForType(hole.getType(), initField);
    }

    public Variable createVariableForHole(Hole hole) {
        Variable var = new Variable(hole.getType());
        add(hole, var);
        return var;
    }

    public boolean needTestNewField() {
        return latestRWNewField > preRWNewField;
    }

    public void setAlreadyTestField() {
        this.preRWNewField = this.latestRWNewField;
    }

    public String getVariableNameForNewField() {
        if (latestRWNewField > 0)
            return Variable.getNewFieldInstance().getName();
        return null;
    }

    public Collection<ResolvedType> getKnownTypes() {
        if (latestRWNewField > 0)
            return this.typeToVars.keySet();
        else {
            HashSet<ResolvedType> knowns = new HashSet<>(this.typeToVars.keySet());
            knowns.remove(Variable.getNewFieldType());
            return knowns;
        }
    }
}
