package com.mengxinya.it;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MockUtil {

    // TODO 通过注解来获取需要被修改的字段会比较好。同理，通过注解获取要修改的方法，也是好选择。
    public static <T> T mock(Class<T> targetClass, Class<?> templateClass, Object... args) {
        if (targetClass.isAnnotation() || targetClass.isArray() || targetClass.isEnum()) {
            throw new RuntimeException("not support targetClass: " + targetClass);
        }


        try {
            Object templateObj = null;
            if (templateClass != null) {
                templateObj = templateClass.getConstructor().newInstance();
            }

            Class<? extends T> newClass = makeClass(targetClass, templateObj);
            T newObj = createInstance(newClass, args);

            if (templateClass == null || templateClass.getDeclaredFields().length > 0) {
                injectFieldValues(newObj, templateObj);
            }

            return newObj;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | NoSuchFieldException e) {
            throw new RuntimeException("Mock Error", e);
        }

    }

    private static <T> Class<? extends T> makeClass(Class<T> targetClass, Object templateObj) {
        ByteBuddy byteBuddy = new ByteBuddy();
        DynamicType.Builder<T> builder = byteBuddy.subclass(targetClass, ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR);

        if (targetClass.isInterface()) {
            builder = builder.method(ElementMatchers.isDeclaredBy(targetClass)).intercept(StubMethod.INSTANCE);
        }
        if (templateObj != null) {
            Class<?> templateClass = templateObj.getClass();
            List<MethodDescription.SignatureToken> methodsTokens = getTemplateMethods(templateClass).stream().map(method -> new MethodDescription.ForLoadedMethod(method).asSignatureToken()).collect(Collectors.toList());
            builder = builder.method(methodDescription -> methodsTokens.stream().anyMatch(token -> token.equals(methodDescription.asSignatureToken())))
                    .intercept(InvocationHandlerAdapter.of((proxy, method, args) ->
                            templateClass.getMethod(method.getName(), args == null ? null : Arrays.stream(args)
                                    .map(Object::getClass)
                                    .toArray(Class<?>[]::new))
                                    .invoke(templateObj, args))
                    );
        }

        return builder.make()
                .load(targetClass.getClassLoader())
                .getLoaded();
    }

    private static List<Method> getTemplateMethods(Class<?> templateClass) {
        return Arrays.asList(templateClass.getMethods());
    }

    public static <T> T mock(Class<T> targetClass) {
        return mock(targetClass, null);
    }

    private static void setFieldVal(Field field, Object obj, Object val) throws IllegalAccessException {
        boolean isModify = false;
        if (!field.canAccess(obj)) {
            field.setAccessible(true);
            isModify = true;
        }
        field.set(obj, val);
        if (isModify) {
            field.setAccessible(false);
        }
    }

    private static Object getFieldVal(Field field, Object obj) throws IllegalAccessException {
        boolean isModify = false;
        if (!field.canAccess(obj)) {
            field.setAccessible(true);
            isModify = true;
        }
        Object val = field.get(obj);
        if (isModify) {
            field.setAccessible(false);
        }
        return val;
    }

    private static <T> T createInstance(Class<T> tClass, Object... args) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return tClass.getConstructor(Arrays.stream(args).map(Object::getClass).toArray(Class<?>[]::new)).newInstance(args);
    }

    private static void injectFieldValues(Object obj, Object templateObj) throws NoSuchFieldException, IllegalAccessException {
        if (templateObj == null) {
            return;
        }
        Class<?> templateClass = templateObj.getClass();
        Class<?> objClass = obj.getClass();
        for (Field field : templateClass.getDeclaredFields()) {
            Field objField = objClass.getSuperclass().getDeclaredField(field.getName());
            setFieldVal(objField, obj, getFieldVal(field, templateObj));
        }
    }

}
