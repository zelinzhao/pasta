package dsu.pasta.object.processor;

import dsu.pasta.object.processor.xstream.BuildXStream;
import dsu.pasta.utils.ZFileUtils;

import java.io.FileWriter;
import java.io.IOException;

import static dsu.pasta.utils.ZPrint.print;

/**
 * Sometimes, we need different reflection procider to init XStream instances,
 * so we separate {@code ObjectDumper} and {@ObjectCreater}. The default
 * implementation use one XStream with
 * {@code EnableStaticPureJavaReflectionProvider}, which is initialized in
 * {@code ObjectDumper}.
 */
public class ObjectDumper {
    public static String dumpObjToXml(boolean b) {
        return String.valueOf(b);
    }

    public static String dumpObjToXml(int b) {
        return String.valueOf(b);
    }

    public static String dumpObjToXml(Object obj) {
        String xml = BuildXStream.one().toXML(obj);
        return xml;
    }

    public static boolean dumpObjToFile(Object obj, String file) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(ZFileUtils.createFileAndParents(file));
            String xml = dumpObjToXml(obj);
            fw.write(xml);
            print("Dump object to " + file);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public static void main(String[] args) {
        dumpObjToFile(null, "/home/BigData/subjects/icse21/tomcat80/f4451c/dumpobjects/dump.xml");
    }
}
