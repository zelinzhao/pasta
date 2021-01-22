package org.javelus.dpg.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;


public class DSUClassStore {

    protected final DSUClassStore superStore;

    protected final List<String> classPathString;
    protected final Map<String, DSUClass> classes = new HashMap<String, DSUClass>();

    public static DSUClassStore buildFromClassPathString(String pathString){
        String[] pathArr = pathString.split(File.pathSeparator);
        List<String> pathStrings = new ArrayList<String>(pathArr.length);

        parseClassPathFromPathListString(pathStrings, pathString);

        DSUClassStore store = new DSUClassStore(
                pathStrings,
                DSUBootstrapClassStore.getInstance());
        store.loadClasses();
        return store;
    }

    protected DSUClassStore(
            List<String> classPathString,
            DSUClassStore superStore){
        this.classPathString = classPathString;
        this.superStore = superStore;
    }

    public List<String> getClassPathString(){
        return this.classPathString;
    }

    public static boolean isClassFile(File pathname) {
        return pathname.isFile() && pathname.toString().endsWith(".class");
    }

    public static boolean isClassFile(String filename) {
        return filename.endsWith(".class");
    }

    static void walk(List<File> results, File root){
        File[] files = root.listFiles();
        for(File file:files){
            if (file.isDirectory()){
                walk(results, file);
            }else if(isClassFile(file)){
                results.add(file);
            }
        }
    }

    static List<File> collectClassFilesInDirectory(File root){
        List<File> files =new ArrayList<File>();
        walk(files, root);
        return files;
    }

    static void collectClassFiles(String path, List<URL> results) throws IOException{
        File container = new File(path);
        if(container.isDirectory()){
            List<File> files = collectClassFilesInDirectory(container);
            for(File file:files){
                results.add(file.toURI().toURL());
            }
        }else {
            ZipInputStream input = null;
            ZipEntry entry = null;
            
            try {
                URL containerURL = container.toURI().toURL();
                String externalForm = containerURL.toExternalForm();
                input = new ZipInputStream(containerURL.openStream());
                while((entry = input.getNextEntry()) != null){
                    if(!entry.isDirectory() && entry.getName().endsWith(".class")){
                        URL url = new URL(
                                String.format("jar:%s!/%s", externalForm,
                                entry.getName()));
                        results.add(url);
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } finally{
                if(input != null){
                    input.close();
                }
            }
        }
    }
    
    public static void collectClassNodes(String pathString, final Map<String, ClassNode> classNodes) throws IOException{
        List<String> pathStrings = new ArrayList<String>();
        parseClassPathFromPathListString(pathStrings, pathString);
        List<URL> results = new ArrayList<URL>();
        for(String path:pathStrings){
            collectClassFiles(path, results);
        }
        for(URL url:results){
            InputStream stream = null;
            try {
                stream = url.openStream();
                ClassReader reader = new ClassReader(stream);
                ClassNode cn = new ClassNode();
                reader.accept(cn, 0);
                classNodes.put(cn.name, cn);
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
        }
    }

    private static void parseClassPathFromPathListString(
            List<String> pathStrings,
            String pathString) {
        String[] pathArr = pathString.split(File.pathSeparator);
        for (String pathStr : pathArr) {
            pathStr = pathStr.trim();

            // skip null string caused by redundant path separator
            if (pathStr == null || pathStr.length() == 0) {
                continue;
            }

            File file = new File(pathStr);

            if (file.isDirectory()) {
                // add an directory to the path list
                pathStrings.add(pathStr);
            } else if (file.isFile() 
                    && (pathStr.endsWith(".jar")
                    || pathStr.endsWith(".zip"))) {
                pathStrings.add(pathStr);
            } else {
                System.err.println("Ignore path str: " + pathStr + " in path list string " + pathString);
            }
        }
    }

    private void loadClasses(){
        try{
            List<URL> results = new ArrayList<URL>();
            for(String path: classPathString){
                collectClassFiles(path, results);
            }
            for(URL url:results){
                addClass(url);
            }
        }catch(Exception e){
            throw new RuntimeException("load all classes failed", e);
        }
    }

    protected DSUClass addClass(URL classFile) throws IOException {
        InputStream stream = null;
        try {
            stream = classFile.openStream();
            ClassReader reader = new ClassReader(stream);
            ClassNode cn = new ClassNode();
            reader.accept(cn, 0);
            return addClass(classFile, cn);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    /**
     * Find a class local in this store
     * @param className
     * @return
     */
    public DSUClass findClass(String className){
        return this.classes.get(className);
    }

    public DSUClass lookupClass(String className){
        DSUClass klass = this.classes.get(className);
        if(klass != null){
            return klass;
        }

        if(superStore != null){
            return superStore.lookupClass(className);
        }

        return null;
    }

    /**
     * look up the class in the hierarchy of class store,
     * create a DSUClass place holder if nothing is found.
     * @param className
     * @return
     */
    public DSUClass getOrCreate(String className){
        DSUClass dsuClass = lookupClass(className);

        if(dsuClass == null){
            dsuClass = new DSUClass(this, className);
            classes.put(className, dsuClass);
        }

        return dsuClass;
    }

    /**
     * add class to the store
     * @param classFile
     * @param cn
     * @return
     */
    protected DSUClass addClass(URL classFile, ClassNode cn) {
        DSUClass dsuClass = findClass(cn.name);

        if(dsuClass != null && dsuClass.isLoaded()){
            throw new RuntimeException("duplicated local class " + dsuClass.getName()
                    + ", existing URL=" + dsuClass.getClassFile()
                    + ", new URL=" + classFile);
        }

        if(dsuClass == null){
            dsuClass = lookupClass(cn.name);
            if(dsuClass != null){
                //throw new RuntimeException("duplicated non-local class");
                System.err.println("duplicated non-local class " + cn.name);
                dsuClass = null;
            }
        }

        if(dsuClass == null){
            dsuClass = new DSUClass(this, cn.name, classFile, cn);
            classes.put(cn.name, dsuClass);
        }
        
        if(!dsuClass.isLoaded()){
            dsuClass.setClassNode(cn);
            dsuClass.setClassFile(classFile);
        }

        return dsuClass;
    }

    public boolean isBootstrapStore(){
        return false;
    }

    public Iterator<DSUClass> getClassIterator(){
        return this.classes.values().iterator();
    }

    public String getPathString(){
        StringBuilder sb = new StringBuilder();
        for(String path:classPathString){
            sb.append(path);
            sb.append(File.pathSeparator);
        }
        return sb.toString();
    }
}
