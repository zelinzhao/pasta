package dsu.pasta.object.processor;

import dsu.pasta.object.processor.xstream.BuildXStream;

import java.io.File;

/**
 * Sometimes, we need different reflection procider to init XStream instances,
 * so we separate {@code ObjectDumper} and {@ObjectCreater}. The default
 * implementation use one XStream, with
 * {@code EnableStaticPureJavaReflectionProvider}, which is initialized in
 * {@code ObjectDumper}.
 */
public class ObjectCreater {
    public static Object readObjFromXml(String xml) {
        return BuildXStream.one().fromXML(xml);
    }

    public static Object readObjFromFile(File file) {
        return BuildXStream.one().fromXML(file);
    }

    public static Object readObjFromFile(String file) {
        return readObjFromFile(new File(file));
    }
}
