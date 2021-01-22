package dsu.pasta.object.processor;

import dsu.pasta.config.ProjectConfig;
import dsu.pasta.config.UpdateConfig;
import dsu.pasta.test.command.CommandExecutor;
import dsu.pasta.test.command.CommandThreadPool;
import dsu.pasta.utils.ZFileUtils;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CompareObjects {
    public static HashSet<Set<File>> CategorizeObjectsIntoKinds(String directory) {
        HashSet<Set<File>> objectKinds = new HashSet<>();
        HashSet<File> remainObjs = new HashSet<File>(
                Arrays.asList(ZFileUtils.findFileRecursivelyWithPattern(directory, ".xml")));
        int beforeSize = remainObjs.size();

        while (remainObjs.size() > 1) {
            File firstObj = (File) remainObjs.toArray()[0];
            remainObjs.remove(firstObj);

            CommandThreadPool pool = new CommandThreadPool();
            CompareThread.sameObjects.clear();
            CompareThread.sameObjects.add(firstObj);
            CompareThread.firstObj = firstObj;
            for (File f : remainObjs) {
                CompareThread t = new CompareThread(f);
                pool.execute(t);
            }
            pool.waitAll(CommandExecutor.timeoutSecond);

            // sameObjs and firstObj are same
            objectKinds.add(new HashSet<File>(CompareThread.sameObjects));
            remainObjs.removeAll(CompareThread.sameObjects);
        }
        if (remainObjs.size() == 1) {
            objectKinds.add(remainObjs.stream().collect(Collectors.toSet()));
        }

        int afterSize = objectKinds.size();
        return objectKinds;
    }

    public static HashSet<Set<String>> CategorizeTestsIntoKinds(HashSet<Set<File>> objectKinds) {
        HashSet<Set<String>> testKinds = new HashSet<>();
        for (Set<File> fs : objectKinds) {
            Set<String> kind = new HashSet<>();
            for (File f : fs) {
                String test = f.getName().replace("_object.xml", "");
                kind.add(test);
            }
            testKinds.add(kind);
        }
        return testKinds;
    }

    public static String cleanXmlFile(String oldpath, boolean removePrevious) {
        String newPath = oldpath + ".clean.xml";
        if (!new File(oldpath).exists()) {
            return null;
        }
        if (new File(newPath).exists()) {
            if (!removePrevious)
                return newPath;
            else
                new File(newPath).delete();
        }
        try {
            BufferedReader fr = new BufferedReader(new FileReader(oldpath));
            FileWriter fw = new FileWriter(newPath);
            for (String temp = fr.readLine(); temp != null; temp = fr.readLine()) {
                temp = temp.replace("&", "&amp;");
                fw.write(temp + "\n");
            }
            fr.close();
            fw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return newPath;
    }

    private static class CompareThread extends Thread {
        static Set<File> sameObjects = Collections.newSetFromMap(new ConcurrentHashMap<File, Boolean>());
        static File firstObj;

        private File thisFile;

        public CompareThread(File thisFile) {
            this.thisFile = thisFile;
        }

        @Override
        public void run() {
            synchronized (firstObj) {
                //TODO we assume new fields' length is 1 in many places
                ObjectComparator oc = new ObjectComparator(true, cleanXmlFile(firstObj.getAbsolutePath(), false),
                        cleanXmlFile(thisFile.getAbsolutePath(), false), UpdateConfig.one().newField.name,
                        ProjectConfig.one().onlyCompareTag, true);
                if (oc.isSame()) {
                    sameObjects.add(this.thisFile);
                }
            }
        }
    }
}
