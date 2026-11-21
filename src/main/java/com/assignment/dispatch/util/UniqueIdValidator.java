package com.assignment.dispatch.util;

import com.assignment.dispatch.exception.InvalidRequestException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class UniqueIdValidator {

    private UniqueIdValidator() {
    }

    /** Rejects a batch that contains the same ID more than once. */
    public static void requireUnique(Collection<String> ids, String idName) {
        Set<String> seen = new HashSet<>();
        for (String id : ids) {
            if (!seen.add(id)) {
                throw new InvalidRequestException("Duplicate " + idName + " in request: " + id);
            }
        }
    }
}
