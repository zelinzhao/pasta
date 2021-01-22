package dsu.pasta.utils;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ZFileUtils {
    public static String getPurePath(String path) {
        return new File(path).getAbsolutePath();
    }

    public static File[] findFileRecursivelyWithPattern(String root, String pattern) {
        if (!new File(root).isDirectory())
            return new File[0];
        List<File> targets = new ArrayList<>();
        try {
            Files.walk(Paths.get(root))
                    .filter(Files::isRegularFile)
                    .forEach(p -> {
                        if (p.toFile().getName().endsWith(pattern))
                            targets.add(p.toFile());
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return targets.toArray(new File[0]);
    }

    public static File createFullDirectory(String dir) {
        File result = null;
        try {
            Path path = Paths.get(dir);
            if (Files.exists(path)) {
                result = path.toFile();
            } else {
                Files.createDirectories(path);
                result = path.toFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static File createFileAndParents(String file) {
        File result = null;
        try {
            Path path = Paths.get(file);
            if (Files.exists(path)) {
                result = path.toFile();
            } else {
                Files.createDirectories(path.getParent());
                result = Files.createFile(path).toFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String[] readFileToArray(String filepath) {
        List<String> result = readFileToList(filepath);
        return result.toArray(new String[result.size()]);
    }

    public static List<String> readFileToList(String filepath) {
        List<String> results = new ArrayList<String>();
        if (filepath == null)
            return results;
        if (!new File(filepath).exists())
            return results;
        try {
            results = Files.readAllLines(new File(filepath).toPath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return results;
        }
    }

    public static boolean fileExistNotEmpty(String path) {
        File f = new File(path);
        if (!f.exists())
            return false;
        if (f.length() == 0)
            return false;
        return true;
    }

    public static boolean directoryExistNotEmpty(String directory) {
        File f = new File(directory);
        if (!f.exists())
            return false;
        if (!f.isDirectory())
            return false;
        if (f.list().length == 0)
            return false;
        return true;
    }

    public static void write(String file, Set<String> set) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            for (String str : set)
                bw.write(str + "\n");
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteAllFilesInDir(String dir) {
        File root = new File(dir);
        if (!root.isDirectory())
            return;
//		for(File f: root.listFiles())
//			f.delete();
        Path directory = Paths.get(dir);
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static String findAllJarToCP(String... dir) {
        File[] jars = findAllJars(dir);
        List<String> paths = new ArrayList<String>();
        Arrays.asList(jars).forEach(f -> paths.add(f.getAbsolutePath()));
        return String.join(File.pathSeparator, paths);
    }

    public static File[] findAllJars(String... dirs) {
        File[] allJars = new File[0];
        for (String dir : dirs) {
            File[] jars = new File(dir).listFiles(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    if (f.isDirectory())
                        return false;
                    if (f.getName().endsWith(".jar"))
                        return true;
                    return false;
                }
            });
            allJars = ZCollectionUtils.concatenate(allJars, jars);
        }
        return allJars;
    }

}
