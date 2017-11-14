package nl.futureedge.simple.jta;

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
