package dsu.pasta;

public class Main {
    public static void main(String[] args) {
        if (args.length >= 1) {
            if (args[0].equals("distiller")) {
                Distiller.distiller(args);
            } else if (args[0].equals("synthesizer")) {
                Synthesizer.synthesizer(args);
            } else if (args[0].equals("verifier")) {
                Verifier.verifier(args);
            } else {
                helpAndExit();
            }
        } else {
            helpAndExit();
        }
    }

    private static void helpAndExit() {
        System.out.println("Usage pasta:");
        System.out.println("  pasta distiller <args>\t\tDistill gadgets from programs.");
        System.out.println("  pasta synthesizer <args>\t\tSynthesize transformers.");
        System.out.println("  pasta verifier <args>\t\t\tVerify a transformer against test cases.");
        System.exit(0);
    }
}
