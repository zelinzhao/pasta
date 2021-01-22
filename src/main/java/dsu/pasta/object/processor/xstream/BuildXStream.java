package dsu.pasta.object.processor.xstream;

import com.thoughtworks.xstream.IgnoreTypes;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.AnyTypePermission;

public class BuildXStream {
    private static XStream one;

    public static XStream one() {
        if (one == null)
            init();
        return one;
    }

    private static void init() {
        one = new XStream(new EnableStaticPureJavaReflectionProvider());

//		one.setMode(XStream.ID_REFERENCES);
//		one.setMode(XStream.NO_REFERENCES);
        one.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        one.ignoreUnknownElements();
        one.addPermission(AnyTypePermission.ANY);

        // arrayname could be [java.lang.String; or java.lang.String[]
        IgnoreTypes.setMaxDepth(5);

        IgnoreTypes.addIgnoreName("sun.misc.Unsafe");
        IgnoreTypes.addIgnoreName("java.security.CodeSource");
        IgnoreTypes.addIgnoreName("java.security.ProtectionDomain");
        IgnoreTypes.addIgnoreName("java.util.Vector");
        IgnoreTypes.addIgnoreName("java.util.concurrent.CopyOnWriteArrayList");
        IgnoreTypes.addIgnoreName("org.apache.juli.AsyncFileHandler");
        IgnoreTypes.addIgnoreName("java.util.concurrent.ThreadPoolExecutor$Worker");
        IgnoreTypes.addIgnoreName("javax.management.MBeanAttributeInfo");
        IgnoreTypes.addIgnoreName("com.sun.jmx.mbeanserver.MBeanServerDelegateImpl");
        IgnoreTypes.addIgnoreName("com.sun.jmx.interceptor.DefaultMBeanServerInterceptor");
        IgnoreTypes.addIgnoreName("org.apache.catalina.mapper.MapperListener");
        IgnoreTypes.addIgnoreName("org.apache.catalina.core.StandardEngine");
        IgnoreTypes.addIgnoreName("sun.misc.Launcher$ExtClassLoader");
        IgnoreTypes.addIgnoreName("sun.misc.Launcher$AppClassLoader");
        IgnoreTypes.addIgnoreName("org.apache.tomcat.dbcp.pool2.impl.SoftReferenceObjectPool");
        IgnoreTypes.addIgnoreName("org.apache.tomcat.dbcp.pool2.PooledObjectFactory");
        IgnoreTypes.addIgnoreName("sun.net.www.http.ClientVector");
        IgnoreTypes.addIgnoreName("sun.net.www.http.KeepAliveCache");

        IgnoreTypes.addIgnorePattern("sun.net.www.http.*");
        IgnoreTypes.addIgnorePattern("sun.management.*");
        IgnoreTypes.addIgnorePattern("org.apache.juli.logging.*");
        IgnoreTypes.addIgnorePattern("com.sun.jmx.mbeanserver.*");
        IgnoreTypes.addIgnorePattern("org.apache.tomcat.dbcp.pool2.PooledObjectFactory$MockitoMock$*");
        IgnoreTypes.addIgnorePattern("org.apache.tomcat.dbcp.pool2.PooledObjectFactory.*");
    }
}
