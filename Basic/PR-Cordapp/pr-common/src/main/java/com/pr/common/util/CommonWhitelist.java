package com.pr.common.util;

import net.corda.core.serialization.SerializationWhitelist;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.*;

/**
 * @author Ajinkya Pande & Rishi Kundu
 */

public class CommonWhitelist implements SerializationWhitelist {
    @NotNull
    @Override
    public List<Class<?>> getWhitelist() {
        return getWhiteListedClasses();
    }

    @NotNull
    public static List<Class<?>> getWhiteListedClasses() {
        return Arrays.asList(HashSet.class, LinkedHashSet.class, LinkedHashMap.class, HashMap.class, Map.class,
                Date.class, LocalDateTime.class);
    }
}
