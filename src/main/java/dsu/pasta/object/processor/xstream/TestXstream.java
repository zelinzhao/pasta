package dsu.pasta.object.processor.xstream;

import com.thoughtworks.xstream.IgnoreTypes;
import dsu.pasta.object.processor.ObjectCreater;
import dsu.pasta.object.processor.ObjectDumper;

/**
 * Just for testing xstream
 */
public class TestXstream {
    String[] strarr = {"a", "b"};
    inner[] innarr = {new inner(), new inner()};
    inner tt = new inner();
    String x = "x";
    int ii = 2;

    public static inner getInner() {
        return new inner();
    }

    public static void main(String[] args) {
//		IgnoreTypes.addIgnoreName("dsu.pasta.object.processor.xstream.TestXstream.inner");
//		IgnoreTypes.addIgnoreName("dsu.pasta.object.processor.xstream.TestXstream.inner[]");

        IgnoreTypes.addIgnoreName("int");
        IgnoreTypes.addIgnoreName("java.lang.Integer");
        IgnoreTypes.setMaxDepth(0);
        TestXstream txs = new TestXstream();
        System.out.println("Dump object");

        String xml = ObjectDumper.dumpObjToXml(txs);
        System.out.println(xml);
        System.out.println("Create object");
        TestXstream a = (TestXstream) ObjectCreater.readObjFromXml(xml);
        xml = ObjectDumper.dumpObjToXml(a);
        System.out.println(xml);
    }

    public static class inner {
        private static final String t = new String("t");
        public final String a = new String("a");
        private int i = 10;

        public static String getFinal() {
            return t;
        }

        public void init() {
        }

        public void add(String str) {
        }
    }
}
