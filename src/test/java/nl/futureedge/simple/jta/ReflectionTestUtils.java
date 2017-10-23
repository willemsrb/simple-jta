package nl.futureedge.simple.jta;

import java.lang.reflect.Field;

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
}
