package dsu.pasta.object.processor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import static dsu.pasta.utils.ZPrint.print;

public class ObjectApi {
    public static final String TRANS_OBJECT = "TRANS_OBJECT";
    public static final String TARGET_CLASS = "TARGET_CLASS";
    public static final String DUMP_FILE = "DUMP_FILE";

    public static final String DUMP_ID = "DUMP_ID";
    public static final String MAKE_NULL = "MAKE_NULL";
    public static final String FIELD_NAME = "FIELD_NAME";

    public static boolean make_null = false;
    public static String field_name = null;

    private static String targetClass = "";
    private static boolean trans = false;
    private static int dump_id = -999;

    private static AtomicBoolean doneAll = new AtomicBoolean(false);
    private static AtomicBoolean doneTrans = new AtomicBoolean(false);

    static {
        targetClass = System.getenv(TARGET_CLASS);
        print("Target class is " + targetClass);
        trans = Boolean.valueOf(System.getenv(TRANS_OBJECT));
        print("Transform object? " + trans);

        dump_id = Integer.valueOf(System.getProperty(DUMP_ID));
        print("Dump id is " + dump_id);
        make_null = Boolean.valueOf(System.getProperty(MAKE_NULL));
        print("Create null? " + make_null);
        field_name = System.getProperty(FIELD_NAME);
        print("Field name is " + field_name);
    }
    /**
     * TODO: Add more support for primitive.
     * @param b
     */
    /**
     * @param b
     * @param id dump when equals to dump_id, or negative
     */
    public static void processObject(boolean b, int id) {
        if (id != dump_id && id > 0)
            return;
        print("ID: " + id);
        print("Coming object is boolean, assume it's transformed field and dump directly");

        if (doneTrans.get() && !doneAll.get()) {
            print("Dump transformed boolean field");
            dump(b);
            doneAll.set(true);
            return;
        }
    }

    public static void processObject(int i, int id) {
        if (id != dump_id && id > 0)
            return;
        print("ID: " + id);
        print("Coming object is int, assume it's transformed field and dump directly");

        if (doneTrans.get() && !doneAll.get()) {
            print("Dump transformed int field");
            dump(i);
            doneAll.set(true);
            return;
        }
    }

    public synchronized static void processObject(Object obj, int id) {
        if (id != dump_id && id > 0)
            return;
        print("ID: " + id);
        if (doneAll.get()) {
            print("All done");
            return;
        }
        if (obj == null) {
            if (!doneTrans.get()) {
                print("Coming object is null. Old instance hasn't been transformed. Error here.");
                return;
            } else if (doneTrans.get() && !doneAll.get()) {
                print("Coming object is null and trans finished. Dump transformed field");
                dump(obj);
                doneAll.set(true);
                return;
            }
        } else if (obj != null) {
            String objcla = obj.getClass().getName();
//			print("Coming object class is " + objcla);
            if (!objcla.equals(targetClass)) {
                if (!doneTrans.get()) {
                    print("Coming object class is not as expected. Old instance hasn't been transformed. Error here.");
                    return;
                } else if (doneTrans.get() && !doneAll.get()) {
                    print("Coming object class is not as expected and trans finished. Dump transformed field");
                    dump(obj);
                    doneAll.set(true);
                    return;
                }
            } else {
                print("Coming object class is as expected. Process this");
                process(obj);
            }
        }
    }

    private static void process(Object obj) {
        Class target = obj.getClass();
        if (make_null) {
            try {
                Field f = target.getDeclaredField(field_name);
                f.setAccessible(true);
                f.set(obj, null);
                print("Set " + field_name + " field of old instance to null");
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        if (trans && !doneTrans.get() && !doneAll.get() && sameClass(obj)) {
            print("Transform " + targetClass + " object");
            try {
                if (target == null) {
                    print("ERROR: Can't find target class " + targetClass + ", exit.");
                    return;
                }

                Method m = target.getMethod("DsuTransformDump", null);
                if (m == null) {
                    print("Transform method is null!!");
                }
                doneTrans.set(true);
                m.invoke(obj, null);
                print("Transform finished, wait transformed field.");
                if (make_null)
                    System.exit(0);
                return;
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException
                    | IllegalArgumentException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        // dump full
        if (!doneTrans.get() && !doneAll.get()) {
            print("Dump full object");
            dump(obj);
            doneAll.set(true);
        }
        if (make_null)
            System.exit(0);
    }

    private static void dump(Object obj) {
        String path = System.getenv(DUMP_FILE);
        if (path == null || path.length() == 0) {
            print("Dump file path is not valid");
            return;
        }
        print("Dumping to " + path);
        ObjectDumper.dumpObjToFile(obj, path);
    }

    private static boolean sameClass(Object obj) {
        return obj.getClass().getName().equals(targetClass);
    }
}
