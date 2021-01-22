package dsu.pasta.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class ZCollectionUtils {
    public static <T> T[] concatenate(T[]... arrs) {
        HashSet<T> result = new HashSet<T>();
        for (T[] oneA : arrs) {
            result.addAll(Arrays.asList(oneA));
        }
        T[] c = (T[]) Array.newInstance(arrs[0].getClass().getComponentType(), result.size());
        return result.toArray(c);
    }

    public static List<String> cartesianAppendString(List<String> heads, List<String> tails) {
        if (heads.size() == 0)
            return tails;
        if (tails.size() == 0)
            return heads;

        List<String> result = new ArrayList<>();
        for (String h : heads) {
            for (String t : tails) {
                result.add(h + t);
            }
        }
        return result;
    }
}
