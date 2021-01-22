package org.javelus.dpg.io;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import org.javelus.dpg.model.DSUClass;
import org.javelus.dpg.model.DSUClassStore;
import org.javelus.dpg.model.DSU;

public class PlainTextDSUWriter {

    void appendDeletedClass(PrintWriter pw, DSUClass klass) {
        pw.append("delclass ");
        pw.append(klass.getName());
        pw.append('\n');
    }

    void appendAddedClass(PrintWriter pw, DSUClass klass) {
        pw.append("addclass ");
        pw.append(klass.getName());
        pw.append('\n');
    }

    void appendModifiedClass(PrintWriter pw, DSUClass klass) {
        pw.append("modclass ");
        pw.append(klass.getName());
        pw.append('\n');
    }

    void appendClassEntry(PrintWriter pw, String path) {
        pw.append("classpath ");
        pw.append(path);
        pw.append('\n');
    }

    public void write(DSU update, OutputStream output) {

        PrintWriter pw = new PrintWriter(output);

        DSUClassStore newStore = update.getNewStore();

        for (String path : newStore.getClassPathString()) {
            appendClassEntry(pw, path);
        }

        List<DSUClass> deletedClass = update.getDeletedClasses();
        for (DSUClass klass : deletedClass) {
            if (klass.isLoaded() && klass.isUpdated()) {
                appendDeletedClass(pw, klass);
            }
        }

        Iterator<DSUClass> it = update.getSortedNewClasses();
        while (it.hasNext()) {
            DSUClass klass = it.next();
            DSUClass old = klass.getOldVersion();
            if (old == null) {
                // this is a new added class
                appendAddedClass(pw, klass);
            } else if (old.isLoaded()) {
                if (old.needReloadClass()) {
                    appendModifiedClass(pw, old);
                }
            } else {
                throw new RuntimeException("Should not reach here.");
            }
        }

        pw.flush();
    }
}
