package dsu.pasta.javaparser.gadget.program;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.printer.PrettyPrinter;
import com.github.javaparser.printer.PrettyPrinterConfiguration;

import java.text.DecimalFormat;
import java.util.Optional;

public class PrettyCodePrinter {
    public static final String STALE_OBJ_SPECIAL = "_stale_";
    public static final String TAR_FIELD_SPECIAL = "_object_";
    public static final String TAR_FIELD_NAME = "_dsu_target_field_";
    private static PrettyPrinter prettyPrinter = null;
    private static JavaParser parser = null;

    static {
        parser = new JavaParser();

        PrettyPrinterConfiguration conf = new PrettyPrinterConfiguration();
        conf.setIndentSize(2);
        conf.setIndentType(PrettyPrinterConfiguration.IndentType.SPACES);
        conf.setPrintComments(true);
        prettyPrinter = new PrettyPrinter(conf);
    }

    /**
     * Replace $0-->$$stale; var_1-->$$object
     *
     * @param method
     * @return
     */
    public static String replaceNameForPrint(String method) {
        method = method.replace("$0", STALE_OBJ_SPECIAL);
        method = method.replace(Variable.getNewFieldInstance().getName(), TAR_FIELD_SPECIAL);
        return method;
    }

    /**
     * Replace $$stale-->$0; $$object-->dsu_tar_field
     *
     * @param method
     * @return
     */
    public static String replaceSpecialForTest(String method) {
        method = method.replace(STALE_OBJ_SPECIAL, "$0");
        method = method.replace(TAR_FIELD_SPECIAL, TAR_FIELD_NAME);
        return method;
    }


    public static String makeCodePretty(String methodBody) {
        ParseResult<MethodDeclaration> parseResult = parser.parseMethodDeclaration(
                "void dsuTransformer(){" + methodBody + "}"
        );
        Optional<MethodDeclaration> method = parseResult.getResult();
        if (method == null || !method.isPresent()) {
            return methodBody;
        }

        MethodDeclaration md = method.get();
        Optional<BlockStmt> block = md.getBody();
        if (block == null || !block.isPresent()) {
            return methodBody;
        }
        String result = prettyPrinter.print(block.get());

        int length = result.length();
        int leftBrace = result.indexOf("{");
        int rightBrace = result.lastIndexOf("}");

        if (leftBrace <= 1 && (length - rightBrace) <= 1) {
            result = result.substring(leftBrace + 1, rightBrace);
        }
        return result;
    }

    public static String makeCodePrettyWithoutLinebreaks(String methodBody) {
        String result = makeCodePretty(methodBody);
        return result.replaceAll("\n\\s*\\/\\*", "  /*");
    }

    public static void main(String[] args) {
//        String method = "org.apache.tomcat.util.net.Nio2Endpoint var_0 = $0;\n" +
//                "java.nio.channels.AsynchronousChannelGroup var_1 = null;\n" +
//                "var_1 = java.nio.channels.AsynchronousChannelGroup.withThreadPool(((java.util.concurrent.ExecutorService) var_0.getExecutor()));\n" +
//                "/* []=AsynchronousChannelGroup.withThreadPool(((ExecutorService)[].getExecutor())); */\n" +
//                "var_1.shutdownNow();\n" +
//                "/* [].shutdownNow(); */";
//        System.out.println(method);
//        System.out.println("\n===============================\n");
//        method = replaceNameForPrint(method);
//        System.out.println(method);
//        System.out.println("\n===============================\n");
//        method = replaceSpecialForTest(method);
//        System.out.println(method);
//
//        System.out.println("/* ---------- Transformer #1 (cost = 2.36842105) ---------- */");
//        System.out.println(makeCodePrettyWithoutLinebreaks("" +
//                "  /* Target field is var_1 */\n" +
//                "  Example var_0 = $0;\n" +
//                "  java.io.File var_1 = null;\n" +
//                "  java.lang.String var_2 = var_0.file; /* g: [1] = [2].file; */\n" +
//                "  $$obj = new java.io.File(var_2); /* g: [1] = new java.io.File([2]); */"));
    }
}
