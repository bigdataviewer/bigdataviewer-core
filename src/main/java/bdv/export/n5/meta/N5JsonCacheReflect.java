/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2025 BigDataViewer developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.export.n5.meta;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.janelia.saalfeldlab.n5.cache.N5JsonCache;

class N5JsonCacheReflect {

    private static final Method getCacheInfoMethod;
    private static final Field childrenField;
    private static final Field isDatasetField;
    private static final Field isGroupField;

    static {
        try {
            final Class<?> cacheClass = N5JsonCache.class;

            // Obtain protected getCacheInfo(String)
            getCacheInfoMethod = cacheClass.getDeclaredMethod("getCacheInfo", String.class);
            getCacheInfoMethod.setAccessible(true);

            // N5CacheInfo is the inner class
            final Class<?> infoClass = Class.forName(
                    "org.janelia.saalfeldlab.n5.cache.N5JsonCache$N5CacheInfo");

            childrenField = infoClass.getDeclaredField("children");
            childrenField.setAccessible(true);

            isDatasetField = infoClass.getDeclaredField("isDataset");
            isDatasetField.setAccessible(true);

            isGroupField = infoClass.getDeclaredField("isGroup");
            isGroupField.setAccessible(true);

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize reflection helpers", e);
        }
    }

    static Object getCacheInfo(final N5JsonCache cache, final String normalPathKey) throws InvocationTargetException, IllegalAccessException {
		return getCacheInfoMethod.invoke(cache, normalPathKey);
    }

    static void setChildren(final Object info, final String[] children) throws IllegalAccessException {
		final Set<String> set = new LinkedHashSet<>(Arrays.asList(children));
		childrenField.set(info, set);
	}

    static void setIsDataset(final Object info, final boolean isDataset) throws IllegalAccessException {
		isDatasetField.setBoolean(info, isDataset);
    }

    static void setIsGroup(final Object info, final boolean isGroup) throws IllegalAccessException {
		isGroupField.setBoolean(info, isGroup);
    }
}
