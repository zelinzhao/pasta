package dsu.pasta.object.processor.xstream;

import com.thoughtworks.xstream.IgnoreTypes;
import com.thoughtworks.xstream.converters.reflection.SunUnsafeReflectionProvider;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * We override {@code SunUnsafeReflectionProvider} to add the following
 * functionality:
 * <p>
 * 1, override {@code fieldModifiersSupported}, so that we can serialize static
 * and transient fields.
 * </p>
 * <p>
 * 2, override {@code writeField}, so that we can deserialize static, final and
 * static final fields.
 * </p>
 * <p>
 * 3, override {@code validateFieldAccess}. We add support for access to
 * static-final field and transient field. During serialization and
 * de-serialization, we can process static-final and transient field.
 * </p>
 */
public class EnableStaticPureJavaReflectionProvider extends SunUnsafeReflectionProvider {

    /**
     * Override this method, so that we can serialize static and transient fields.
     */
    @Override
    protected boolean fieldModifiersSupported(Field field) {
        return true;
    }

    /**
     * Override this method, so that we can deserialize static, final and static
     * final fields.
     * <p>
     * WARNNING: There is one point about deserialize static field. Deserialization
     * will override the current value of the static field. For example, there is a
     * static List type field {@code F}, which contains [1,2]. When you serialize an
     * object {@code O} now, the value of {@code F} is [1,2]. Then you may add more
     * element to {@code F}, and it becomes [1,2,3]. If you deserialize object
     * {@code O} now, the [1,2,3] will be override by [1,2], the {@code 3} will be
     * ignored forever unless you add {@code 3} to {@code F} again.
     * </p>
     * <p>
     * For static final field, we need to set the modifier of this field to
     * un-final. Orelse the {@code ObjectAccessException} will be thrown.
     * </p>
     */
    @Override
    public void writeField(Object object, String fieldName, Object value, Class definedIn) {
        if (IgnoreTypes.ignore(value, 0)) {
            return;
        }
        Field field = fieldDictionary.field(object.getClass(), fieldName, definedIn);
        validateFieldAccess(field);
        try {
            if (Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                Field modifiersField;
                modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                field.set(object, value);
                return;
            }
            if (Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                if (Modifier.isFinal(field.getModifiers())) {
                    Field modifiersField = Field.class.getDeclaredField("modifiers");
                    modifiersField.setAccessible(true);
                    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                }
                field.set(null, value);
//                super.writeField(object, fieldName, value, definedIn);
                return;
            } else {
                super.writeField(object, fieldName, value, definedIn);
                return;
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    /**
     * We add support for access to static-final field and transient field. During
     * serialization and de-serialization, we can process static-final and transient
     * field.
     */
    @Override
    protected void validateFieldAccess(Field field) {
        try {
            if (Modifier.isFinal(field.getModifiers())) {
                field.setAccessible(true);
                Field modifiersField;
                modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                return;
            }
            if (Modifier.isTransient(field.getModifiers())) {
                field.setAccessible(true);
                Field modifiersField;
                modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() & ~Modifier.TRANSIENT);
                return;
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
