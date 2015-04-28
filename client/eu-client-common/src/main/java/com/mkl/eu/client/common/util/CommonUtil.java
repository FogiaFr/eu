package com.mkl.eu.client.common.util;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Various utility method.
 *
 * @author MKL.
 */
public final class CommonUtil {
    /**
     * No instance.
     */
    private CommonUtil() {

    }

    /**
     * Find the first element of the collection that matches the predicate, or <code>null</code> if none matches.
     *
     * @param list      collection to parse.
     * @param predicate to use for matching purpose.
     * @param <T>       Type of the Collection.
     * @return the first element of the collection matching the predicate.
     */
    public static <T> T findFirst(Collection<T> list, Predicate<T> predicate) {
        T returnValue = null;
        Optional<T> opt = list.stream().filter(predicate).findFirst();
        if (opt.isPresent()) {
            returnValue = opt.get();
        }

        return returnValue;
    }
}