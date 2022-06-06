/*
 * Copyright (c) 2016 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.creation.bytebuddy;

import static java.lang.Thread.currentThread;
import static net.bytebuddy.description.modifier.Visibility.PRIVATE;
import static net.bytebuddy.dynamic.Transformer.ForMethod.withModifiers;
import static net.bytebuddy.implementation.MethodDelegation.to;
import static net.bytebuddy.implementation.attribute.MethodAttributeAppender.ForInstrumentedMethod.INCLUDING_RECEIVER;
import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.hasParameters;
import static net.bytebuddy.matcher.ElementMatchers.hasType;
import static net.bytebuddy.matcher.ElementMatchers.isEquals;
import static net.bytebuddy.matcher.ElementMatchers.isHashCode;
import static net.bytebuddy.matcher.ElementMatchers.isPackagePrivate;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.whereAny;
import static org.mockito.internal.util.StringUtil.join;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.SynchronizationState;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.MultipleParentClassLoader;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.GraalImageCode;
import net.bytebuddy.utility.RandomString;
import org.mockito.codegen.InjectionBase;
import org.mockito.exceptions.base.MockitoException;
import org.mockito.internal.creation.bytebuddy.ByteBuddyCrossClassLoaderSerializationSupport.CrossClassLoaderSerializableMock;
import org.mockito.internal.creation.bytebuddy.MockMethodInterceptor.DispatcherDefaultingToRealMethod;
import org.mockito.mock.SerializableMode;

class SubclassBytecodeGenerator implements BytecodeGenerator {

    private static final String CODEGEN_PACKAGE = "org.mockito.codegen.";

    private final SubclassLoader loader;
    private final ModuleHandler handler;
    private final ByteBuddy byteBuddy;
    private final Implementation readReplace;
    private final ElementMatcher<? super MethodDescription> matcher;

    private final Implementation dispatcher = to(DispatcherDefaultingToRealMethod.class);
    private final Implementation hashCode = to(MockMethodInterceptor.ForHashCode.class);
    private final Implementation equals = to(MockMethodInterceptor.ForEquals.class);
    private final Implementation writeReplace = to(MockMethodInterceptor.ForWriteReplace.class);

    public SubclassBytecodeGenerator() {
        this(new SubclassInjectionLoader());
    }

    public SubclassBytecodeGenerator(SubclassLoader loader) {
        this(loader, null, any());
    }

    public SubclassBytecodeGenerator(
            Implementation readReplace, ElementMatcher<? super MethodDescription> matcher) {
        this(new SubclassInjectionLoader(), readReplace, matcher);
    }

    protected SubclassBytecodeGenerator(
            SubclassLoader loader,
            Implementation readReplace,
            ElementMatcher<? super MethodDescription> matcher) {
        this.loader = loader;
        this.readReplace = readReplace;
        this.matcher = matcher;
        byteBuddy = new ByteBuddy().with(TypeValidation.DISABLED);
        handler = ModuleHandler.make(byteBuddy, loader);
    }

    private static boolean needsSamePackageClassLoader(MockFeatures<?> features) {
        if (!Modifier.isPublic(features.mockedType.getModifiers())
                || !features.mockedType.isInterface()) {
            // The mocked type is package private or is not an interface and thus may contain
            // package private methods.
            return true;
        }
        if (hasNonPublicTypeReference(features.mockedType)) {
            return true;
        }

        for (Class<?> iface : features.interfaces) {
            if (!Modifier.isPublic(iface.getModifiers())) {
                return true;
            }
            if (hasNonPublicTypeReference(iface)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNonPublicTypeReference(Class<?> iface) {
        for (Method method : iface.getMethods()) {
            if (!Modifier.isPublic(method.getReturnType().getModifiers())) {
                return true;
            }
            for (Class<?> param : method.getParameterTypes()) {
                if (!Modifier.isPublic(param.getModifiers())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public <T> Class<? extends T> mockClass(MockFeatures<T> features) {
        /**
         *
         * 这里出来了两个新事物，interceptor与handler都是什么鬼？通过interceptor 参数的注解关键字mockitoInterceptor，
         * 我们可以知道，这个也许就是前文所说实现MockAccess接口所传进来的，而这个handler，则是我们前文所说的重点:
         * MockHandlerImpl!(准确的说是InvocationNotifierHandler，只不过其内部将主要逻辑都转交给了MockHandlerImpl， 所以下文都已MockHandlerImpl为主)
         *
         *
         * 注意 MultipleParentClassLoader是 bytebutty的 组件。
         * appendMostSpecific的主要功能是：
         * 追加给定类型的类装入器，但过滤类装入器层次结构中的任何副本。引导类装入器隐式跳过，因为它是任何类装入器的隐式父类。类装入器放在类装入器列表的前面。
         * 参数:
         * type—收集类装入器的类型。
         * 返回:
         * 一个新的构建器实例，带有所提供类型的附加类加载器(如果它们还没有被收集的话)。
         *
         *
         * 这里是生成代理类的关键代码。Mockito使用的是ByteBuddy这个框架，它并不需要编译器的帮助，而是直接生成class，
         * 然后使用ClassLoader来进行加载，感兴趣的可以深入研究，其地址为:https://github.com/raphw/byte-buddy，简单入门介绍可见：https://zhuanlan.zhihu.com/p/151843984。
         *
         */

        MultipleParentClassLoader.Builder loaderBuilder =
                new MultipleParentClassLoader.Builder()
                        .appendMostSpecific(features.mockedType)
                        .appendMostSpecific(features.interfaces)
                        .appendMostSpecific(
                                MockAccess.class, DispatcherDefaultingToRealMethod.class)
                        .appendMostSpecific(
                            /**
                             *
                             */
                                MockMethodInterceptor.class,
                                MockMethodInterceptor.ForHashCode.class,
                                MockMethodInterceptor.ForEquals.class);


        ClassLoader contextLoader = currentThread().getContextClassLoader();
        boolean shouldIncludeContextLoader = true;
        if (needsSamePackageClassLoader(features)) {
            // For the generated class to access package-private methods, it must be defined by the
            // same classloader as its type. All the other added classloaders are required to load
            // the type; if the context classloader is a child of the mocked type's defining
            // classloader, it will break a mock that would have worked. Check if the context class
            // loader is a child of the classloader we'd otherwise use, and possibly skip it.
            ClassLoader candidateLoader = loaderBuilder.build();
            for (ClassLoader parent = contextLoader; parent != null; parent = parent.getParent()) {
                if (parent == candidateLoader) {
                    shouldIncludeContextLoader = false;
                    break;
                }
            }
        }
        if (shouldIncludeContextLoader) {
            loaderBuilder = loaderBuilder.appendMostSpecific(contextLoader);
        }
        ClassLoader classLoader = loaderBuilder.build();

        // If Mockito does not need to create a new class loader and if a mock is not based on a JDK
        // type, we attempt
        // to define the mock class in the user runtime package to allow for mocking package private
        // types and methods.
        // This also requires that we are able to access the package of the mocked class either by
        // override or explicit
        // privilege given by the target package being opened to Mockito.
        boolean localMock =
                classLoader == features.mockedType.getClassLoader()
                        && features.serializableMode != SerializableMode.ACROSS_CLASSLOADERS
                        && !isComingFromJDK(features.mockedType)
                        && (loader.isDisrespectingOpenness()
                                || handler.isOpened(features.mockedType, MockAccess.class))
                        && !GraalImageCode.getCurrent().isDefined();
        String typeName;
        if (localMock
                || (loader instanceof MultipleParentClassLoader
                        && !isComingFromJDK(features.mockedType))) {
            typeName = features.mockedType.getName();
        } else {
            typeName =
                    InjectionBase.class.getPackage().getName()
                            + "."
                            + features.mockedType.getSimpleName();
        }
        String name =
                String.format(
                        "%s$%s$%s",
                        typeName,
                        "MockitoMock",
                        GraalImageCode.getCurrent().isDefined()
                                ? suffix(features)
                                : RandomString.make());

        if (localMock) {
            handler.adjustModuleGraph(features.mockedType, MockAccess.class, false, true);
            for (Class<?> iFace : features.interfaces) {
                handler.adjustModuleGraph(iFace, features.mockedType, true, false);
                handler.adjustModuleGraph(features.mockedType, iFace, false, true);
            }
        } else {
            boolean exported = handler.isExported(features.mockedType);
            Iterator<Class<?>> it = features.interfaces.iterator();
            while (exported && it.hasNext()) {
                exported = handler.isExported(it.next());
            }
            // We check if all mocked types are exported without qualification to avoid generating a
            // hook type.
            // unless this is necessary. We expect this to be the case for most mocked types what
            // makes this a
            // worthy performance optimization.
            if (exported) {
                assertVisibility(features.mockedType);
                for (Class<?> iFace : features.interfaces) {
                    assertVisibility(iFace);
                }
            } else {
                Class<?> hook = handler.injectionBase(classLoader, typeName);
                assertVisibility(features.mockedType);
                handler.adjustModuleGraph(features.mockedType, hook, true, false);
                for (Class<?> iFace : features.interfaces) {
                    assertVisibility(iFace);
                    handler.adjustModuleGraph(iFace, hook, true, false);
                }
            }
        }
        // Graal requires that the byte code of classes is identical what requires that interfaces
        // are always defined in the exact same order. Therefore, we add an interface to the
        // interface set if not mocking a class when Graal is active.
        @SuppressWarnings("unchecked")
        Class<T> target =
                GraalImageCode.getCurrent().isDefined() && features.mockedType.isInterface()
                        ? (Class<T>) Object.class
                        : features.mockedType;
        // If we create a mock for an interface with additional interfaces implemented, we do not
        // want to preserve the annotations of either interface. The caching mechanism does not
        // consider the order of these interfaces and the same mock class might be reused for
        // either order. Also, it does not have clean semantics as annotations are not normally
        // preserved for interfaces in Java.
        Annotation[] annotationsOnType;
        if (features.stripAnnotations) {
            annotationsOnType = new Annotation[0];
        } else if (!features.mockedType.isInterface() || features.interfaces.isEmpty()) {
            annotationsOnType = features.mockedType.getAnnotations();
        } else {
            annotationsOnType = new Annotation[0];
        }
        DynamicType.Builder<T> builder =
                byteBuddy
                        .subclass(target)
                        .name(name)
                        .ignoreAlso(BytecodeGenerator.isGroovyMethod(false))
                        .annotateType(annotationsOnType)
                        .implement(
                                new ArrayList<>(
                                        GraalImageCode.getCurrent().isDefined()
                                                ? sortedSerializable(
                                                        features.interfaces,
                                                        GraalImageCode.getCurrent().isDefined()
                                                                        && features.mockedType
                                                                                .isInterface()
                                                                ? features.mockedType
                                                                : void.class)
                                                : features.interfaces))
                        .method(matcher)
                        .intercept(dispatcher)
                        .transform(withModifiers(SynchronizationState.PLAIN))
                        .attribute(
                                features.stripAnnotations
                                        ? MethodAttributeAppender.NoOp.INSTANCE
                                        : INCLUDING_RECEIVER)
                        .serialVersionUid(42L)
                        .defineField("mockitoInterceptor", MockMethodInterceptor.class, PRIVATE)
                        .implement(MockAccess.class)
                        .intercept(FieldAccessor.ofBeanProperty())
                        .method(isHashCode())
                        .intercept(hashCode)
                        .method(isEquals())
                        .intercept(equals);
        if (features.serializableMode == SerializableMode.ACROSS_CLASSLOADERS) {
            builder =
                    builder.implement(CrossClassLoaderSerializableMock.class)
                            .intercept(writeReplace);
        }
        if (readReplace != null) {
            builder =
                    builder.defineMethod("readObject", void.class, Visibility.PRIVATE)
                            .withParameters(ObjectInputStream.class)
                            .throwing(ClassNotFoundException.class, IOException.class)
                            .intercept(readReplace);
        }
        if (name.startsWith(CODEGEN_PACKAGE) || classLoader instanceof MultipleParentClassLoader) {
            builder =
                    builder.ignoreAlso(
                            isPackagePrivate()
                                    .or(returns(isPackagePrivate()))
                                    .or(hasParameters(whereAny(hasType(isPackagePrivate())))));
        }
        return builder.make()
                .load(
                        classLoader,
                        loader.resolveStrategy(features.mockedType, classLoader, localMock))
                .getLoaded();
    }

    private static CharSequence suffix(MockFeatures<?> features) {
        // Constructs a deterministic suffix for this mock to assure that mocks always carry the
        // same name.
        StringBuilder sb = new StringBuilder();
        Set<String> names = new TreeSet<>();
        names.add(features.mockedType.getName());
        for (Class<?> type : features.interfaces) {
            names.add(type.getName());
        }
        return sb.append(RandomString.hashOf(names.hashCode()))
                .append(RandomString.hashOf(features.serializableMode.name().hashCode()))
                .append(features.stripAnnotations ? "S" : "N");
    }

    private static Collection<? extends Type> sortedSerializable(
            Collection<Class<?>> interfaces, Class<?> mockedType) {
        SortedSet<Class<?>> types = new TreeSet<>(Comparator.comparing(Class::getName));
        types.addAll(interfaces);
        if (mockedType != void.class) {
            types.add(mockedType);
        }
        types.add(Serializable.class);
        return types;
    }

    @Override
    public void mockClassStatic(Class<?> type) {
        throw new MockitoException("The subclass byte code generator cannot create static mocks");
    }

    @Override
    public void mockClassConstruction(Class<?> type) {
        throw new MockitoException(
                "The subclass byte code generator cannot create construction mocks");
    }

    private boolean isComingFromJDK(Class<?> type) {
        // Comes from the manifest entry :
        // Implementation-Title: Java Runtime Environment
        // This entry is not necessarily present in every jar of the JDK
        return (type.getPackage() != null
                        && "Java Runtime Environment"
                                .equalsIgnoreCase(type.getPackage().getImplementationTitle()))
                || type.getName().startsWith("java.")
                || type.getName().startsWith("javax.");
    }

    private static void assertVisibility(Class<?> type) {
        if (!Modifier.isPublic(type.getModifiers())) {
            throw new MockitoException(
                    join(
                            "Cannot create mock for " + type,
                            "",
                            "The type is not public and its mock class is loaded by a different class loader.",
                            "This can have multiple reasons:",
                            " - You are mocking a class with additional interfaces of another class loader",
                            " - Mockito is loaded by a different class loader than the mocked type (e.g. with OSGi)",
                            " - The thread's context class loader is different than the mock's class loader"));
        }
    }
}
