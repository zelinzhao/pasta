package dsu.pasta.javaparser.factory.analyzer;

import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

/**
 * Used to store typed solved by all JarTypeSolver
 */
public class ZJarTypeSolver extends JarTypeSolver {
    public static HashSet<String> solvedTypes = new HashSet<>();

    public ZJarTypeSolver(String pathToJar) throws IOException {
        super(pathToJar);
    }

    public ZJarTypeSolver(File pathToJar) throws IOException {
        super(pathToJar);
    }

    @Override
    public ResolvedReferenceTypeDeclaration solveType(String name) throws UnsolvedSymbolException {
        SymbolReference<ResolvedReferenceTypeDeclaration> ref = super.tryToSolveType(name);
        if (ref.isSolved()) {
            solvedTypes.add(name);
            return ref.getCorrespondingDeclaration();
        } else {
            throw new UnsolvedSymbolException(name);
        }
    }

}
