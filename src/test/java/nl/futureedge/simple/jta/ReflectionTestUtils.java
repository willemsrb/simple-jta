package nl.futureedge.simple.jta;

/*-
 * #%L
 * Simple JTA
 * %%
 * Copyright (C) 2017 Future Edge IT
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import org.junit.Assert;

public class ReflectionTestUtils {

    public static <T> T getField(Object instance, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        final Field field = findField(instance.getClass(), fieldName);
        field.setAccessible(true);
        return (T) field.get(instance);
    }

    public static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            final Class<?> superclass = clazz.getSuperclass();
            if (superclass == null) {
                throw new NoSuchFieldException("Field '" + fieldName + "' not found");
            } else {
                return findField(superclass, fieldName);
            }
        }
    }

    public static void testNotInstantiable(Class<?> clazz) throws ReflectiveOperationException {
        final Constructor<?> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            Assert.fail("Constructor invocation should throw IllegalStateException");
        } catch (final InvocationTargetException e) {
            Assert.assertEquals("Constructor invocation should throw IllegalStateException", IllegalStateException.class, e.getCause().getClass());
        }
    }
}
