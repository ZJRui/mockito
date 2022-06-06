/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal;

import static org.mockito.internal.exceptions.Reporter.missingMethodInvocation;
import static org.mockito.internal.exceptions.Reporter.mocksHaveToBePassedToVerifyNoMoreInteractions;
import static org.mockito.internal.exceptions.Reporter.mocksHaveToBePassedWhenCreatingInOrder;
import static org.mockito.internal.exceptions.Reporter.notAMockPassedToVerify;
import static org.mockito.internal.exceptions.Reporter.notAMockPassedToVerifyNoMoreInteractions;
import static org.mockito.internal.exceptions.Reporter.notAMockPassedWhenCreatingInOrder;
import static org.mockito.internal.exceptions.Reporter.nullPassedToVerify;
import static org.mockito.internal.exceptions.Reporter.nullPassedToVerifyNoMoreInteractions;
import static org.mockito.internal.exceptions.Reporter.nullPassedWhenCreatingInOrder;
import static org.mockito.internal.exceptions.Reporter.stubPassedToVerify;
import static org.mockito.internal.progress.ThreadSafeMockingProgress.mockingProgress;
import static org.mockito.internal.util.MockUtil.createConstructionMock;
import static org.mockito.internal.util.MockUtil.createMock;
import static org.mockito.internal.util.MockUtil.createStaticMock;
import static org.mockito.internal.util.MockUtil.getInvocationContainer;
import static org.mockito.internal.util.MockUtil.getMockHandler;
import static org.mockito.internal.util.MockUtil.isMock;
import static org.mockito.internal.util.MockUtil.resetMock;
import static org.mockito.internal.util.MockUtil.typeMockabilityOf;
import static org.mockito.internal.verification.VerificationModeFactory.noInteractions;
import static org.mockito.internal.verification.VerificationModeFactory.noMoreInteractions;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mockito.InOrder;
import org.mockito.MockSettings;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.MockingDetails;
import org.mockito.exceptions.misusing.DoNotMockException;
import org.mockito.exceptions.misusing.NotAMockException;
import org.mockito.internal.configuration.plugins.Plugins;
import org.mockito.internal.creation.MockSettingsImpl;
import org.mockito.internal.invocation.finder.VerifiableInvocationsFinder;
import org.mockito.internal.listeners.VerificationStartedNotifier;
import org.mockito.internal.progress.MockingProgress;
import org.mockito.internal.stubbing.DefaultLenientStubber;
import org.mockito.internal.stubbing.InvocationContainerImpl;
import org.mockito.internal.stubbing.OngoingStubbingImpl;
import org.mockito.internal.stubbing.StubberImpl;
import org.mockito.internal.util.DefaultMockingDetails;
import org.mockito.internal.util.MockUtil;
import org.mockito.internal.verification.MockAwareVerificationMode;
import org.mockito.internal.verification.VerificationDataImpl;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.internal.verification.api.InOrderContext;
import org.mockito.internal.verification.api.VerificationDataInOrder;
import org.mockito.internal.verification.api.VerificationDataInOrderImpl;
import org.mockito.invocation.Invocation;
import org.mockito.invocation.MockHandler;
import org.mockito.mock.MockCreationSettings;
import org.mockito.plugins.DoNotMockEnforcer;
import org.mockito.plugins.MockMaker;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.LenientStubber;
import org.mockito.stubbing.OngoingStubbing;
import org.mockito.stubbing.Stubber;
import org.mockito.verification.VerificationMode;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public class MockitoCore {

    private static final DoNotMockEnforcer DO_NOT_MOCK_ENFORCER = Plugins.getDoNotMockEnforcer();
    private static final Set<Class<?>> MOCKABLE_CLASSES =
            Collections.synchronizedSet(new HashSet<>());

    public boolean isTypeMockable(Class<?> typeToMock) {
        return typeMockabilityOf(typeToMock).mockable();
    }

    public <T> T mock(Class<T> typeToMock, MockSettings settings) {
        if (!(settings instanceof MockSettingsImpl)) {
            throw new IllegalArgumentException(
                    "Unexpected implementation of '"
                            + settings.getClass().getCanonicalName()
                            + "'\n"
                            + "At the moment, you cannot provide your own implementations of that class.");
        }
        MockSettingsImpl impl = (MockSettingsImpl) settings;//Mockito.mock的时候会创建一个 MockSettingsImpl
        /**
         * 根据 被mock的class 构建MockCreationSettings
         */
        MockCreationSettings<T> creationSettings = impl.build(typeToMock);
        checkDoNotMockAnnotation(creationSettings.getTypeToMock(), creationSettings);
        T mock = createMock(creationSettings);
        /**
         * mockingProgress 将会通过ThreadLocal 返回当前线程的MockingProgressImpl对象，
         * 如果没有这个对象则创建一个
         */
        mockingProgress().mockingStarted(mock, creationSettings);
        return mock;
    }

    private void checkDoNotMockAnnotation(
            Class<?> typeToMock, MockCreationSettings<?> creationSettings) {
        checkDoNotMockAnnotationForType(typeToMock);
        for (Class<?> aClass : creationSettings.getExtraInterfaces()) {
            checkDoNotMockAnnotationForType(aClass);
        }
    }

    private static void checkDoNotMockAnnotationForType(Class<?> type) {
        // Object and interfaces do not have a super class
        if (type == null) {
            return;
        }

        if (MOCKABLE_CLASSES.contains(type)) {
            return;
        }

        String warning = DO_NOT_MOCK_ENFORCER.checkTypeForDoNotMockViolation(type);
        if (warning != null) {
            throw new DoNotMockException(warning);
        }

        checkDoNotMockAnnotationForType(type.getSuperclass());
        for (Class<?> aClass : type.getInterfaces()) {
            checkDoNotMockAnnotationForType(aClass);
        }

        MOCKABLE_CLASSES.add(type);
    }

    public <T> MockedStatic<T> mockStatic(Class<T> classToMock, MockSettings settings) {
        if (!MockSettingsImpl.class.isInstance(settings)) {
            throw new IllegalArgumentException(
                    "Unexpected implementation of '"
                            + settings.getClass().getCanonicalName()
                            + "'\n"
                            + "At the moment, you cannot provide your own implementations of that class.");
        }
        MockSettingsImpl impl = MockSettingsImpl.class.cast(settings);
        MockCreationSettings<T> creationSettings = impl.buildStatic(classToMock);
        MockMaker.StaticMockControl<T> control = createStaticMock(classToMock, creationSettings);
        control.enable();
        mockingProgress().mockingStarted(classToMock, creationSettings);
        return new MockedStaticImpl<>(control);
    }

    public <T> MockedConstruction<T> mockConstruction(
            Class<T> typeToMock,
            Function<MockedConstruction.Context, ? extends MockSettings> settingsFactory,
            MockedConstruction.MockInitializer<T> mockInitializer) {
        Function<MockedConstruction.Context, MockCreationSettings<T>> creationSettings =
                context -> {
                    MockSettings value = settingsFactory.apply(context);
                    if (!MockSettingsImpl.class.isInstance(value)) {
                        throw new IllegalArgumentException(
                                "Unexpected implementation of '"
                                        + value.getClass().getCanonicalName()
                                        + "'\n"
                                        + "At the moment, you cannot provide your own implementations of that class.");
                    }
                    MockSettingsImpl impl = MockSettingsImpl.class.cast(value);
                    return impl.build(typeToMock);
                };
        MockMaker.ConstructionMockControl<T> control =
                createConstructionMock(typeToMock, creationSettings, mockInitializer);
        control.enable();
        return new MockedConstructionImpl<>(control);
    }

    public <T> OngoingStubbing<T> when(T methodCall) {
        /**
         *
         * 这个mockingProgress是个什么东西？OngoingStubbing又是什么呢？想要弄清楚来龙去脉，
         * 还记得刚才一直强调的MockHandlerImpl吗？上文说过，mock对象所有的方法最终都会交由MockHandlerImpl的handle方法处理，
         * 所以这注定是一个不一样的女子，不，方法：
         *
         *  // 1.初始化MockProgressImpl对象
         */
        MockingProgress mockingProgress = mockingProgress();
        //        // 2.标记stub开始
        mockingProgress.stubbingStarted();
        /**
         * 再回过头来看MOCKITO_CORE 里的when方法就会清晰许多，它会取出刚刚存放在mockingProgress中的ongoingStubbing对象。
         *
         * 而我们熟知的thenReturn、thenThrow则是OngoingStubbing里的方法，这些方法最终都会调到如下方法: OngoingStubbing的thenAnswer
         *
         * answer即是对thenReturn、thenThrow之类的包装，当然我们也可以自己定义answer。我们有看到了刚刚说过的老朋友invocationContainerImpl，
         * 它会帮我们保管这个answer，待以后调用该方法时返回正确的值，与之对应的代码是handle方法中如下这句代码：
         *StubbedInvocationMatcher stubbedInvocation = invocationContainerImpl.findAnswerFor(invocation);
         *当stubbedInvocation不为空时，就会调用anwser方法来回去之前设定的值：
         *stubbedInvocation.answer(invocation)
         * 如此，对于when(mock.doSome()).thenReturn(obj)这样的用法的主要逻辑了。
         *
         *
         *3.获取最新的ongoingStubbing对象
         *
         * 其中OngoingStubbing对象会在每次MockHandlerImpl调用handle方法时创建一个，
         * 然后set到ThreadLocal的mockingProgress中，所以这里取出来的就是上一次的调用，这
         * 里也证明了其实when的参数是没用的，只要mock对象有方法调用就可以了。因此，when方法就是返回上次mock方法调用封装好的OngoingStubbing。
         */
        @SuppressWarnings("unchecked")
            OngoingStubbing<T> stubbing = (OngoingStubbing<T>) mockingProgress.pullOngoingStubbing();
        if (stubbing == null) {
            mockingProgress.reset();
            throw missingMethodInvocation();
        }
        // 4.返回ongoingStubbing对象
        return stubbing;
    }

    public <T> T verify(T mock, VerificationMode mode) {
        if (mock == null) {
            throw nullPassedToVerify();
        }
        MockingDetails mockingDetails = mockingDetails(mock);
        if (!mockingDetails.isMock()) {
            throw notAMockPassedToVerify(mock.getClass());
        }
        assertNotStubOnlyMock(mock);
        MockHandler handler = mockingDetails.getMockHandler();
        mock =
                (T)
                        VerificationStartedNotifier.notifyVerificationStarted(
                                handler.getMockSettings().getVerificationStartedListeners(),
                                mockingDetails);

        MockingProgress mockingProgress = mockingProgress();
        VerificationMode actualMode = mockingProgress.maybeVerifyLazily(mode);
        mockingProgress.verificationStarted(
                new MockAwareVerificationMode(
                        mock, actualMode, mockingProgress.verificationListeners()));
        return mock;
    }

    public <T> void reset(T... mocks) {
        MockingProgress mockingProgress = mockingProgress();
        mockingProgress.validateState();
        mockingProgress.reset();
        mockingProgress.resetOngoingStubbing();

        for (T m : mocks) {
            resetMock(m);
        }
    }

    public <T> void clearInvocations(T... mocks) {
        MockingProgress mockingProgress = mockingProgress();
        mockingProgress.validateState();
        mockingProgress.reset();
        mockingProgress.resetOngoingStubbing();

        for (T m : mocks) {
            getInvocationContainer(m).clearInvocations();
        }
    }

    public void verifyNoMoreInteractions(Object... mocks) {
        assertMocksNotEmpty(mocks);
        mockingProgress().validateState();
        for (Object mock : mocks) {
            try {
                if (mock == null) {
                    throw nullPassedToVerifyNoMoreInteractions();
                }
                InvocationContainerImpl invocations = getInvocationContainer(mock);
                assertNotStubOnlyMock(mock);
                VerificationDataImpl data = new VerificationDataImpl(invocations, null);
                noMoreInteractions().verify(data);
            } catch (NotAMockException e) {
                throw notAMockPassedToVerifyNoMoreInteractions();
            }
        }
    }

    public void verifyNoInteractions(Object... mocks) {
        assertMocksNotEmpty(mocks);
        mockingProgress().validateState();
        for (Object mock : mocks) {
            try {
                if (mock == null) {
                    throw nullPassedToVerifyNoMoreInteractions();
                }
                InvocationContainerImpl invocations = getInvocationContainer(mock);
                assertNotStubOnlyMock(mock);
                VerificationDataImpl data = new VerificationDataImpl(invocations, null);
                noInteractions().verify(data);
            } catch (NotAMockException e) {
                throw notAMockPassedToVerifyNoMoreInteractions();
            }
        }
    }

    public void verifyNoMoreInteractionsInOrder(List<Object> mocks, InOrderContext inOrderContext) {
        mockingProgress().validateState();
        VerificationDataInOrder data =
                new VerificationDataInOrderImpl(
                        inOrderContext, VerifiableInvocationsFinder.find(mocks), null);
        VerificationModeFactory.noMoreInteractions().verifyInOrder(data);
    }

    private void assertMocksNotEmpty(Object[] mocks) {
        if (mocks == null || mocks.length == 0) {
            throw mocksHaveToBePassedToVerifyNoMoreInteractions();
        }
    }

    private void assertNotStubOnlyMock(Object mock) {
        if (getMockHandler(mock).getMockSettings().isStubOnly()) {
            throw stubPassedToVerify(mock);
        }
    }

    public InOrder inOrder(Object... mocks) {
        if (mocks == null || mocks.length == 0) {
            throw mocksHaveToBePassedWhenCreatingInOrder();
        }
        for (Object mock : mocks) {
            if (mock == null) {
                throw nullPassedWhenCreatingInOrder();
            }
            if (!isMock(mock)) {
                throw notAMockPassedWhenCreatingInOrder();
            }
            assertNotStubOnlyMock(mock);
        }
        return new InOrderImpl(Arrays.asList(mocks));
    }

    public Stubber stubber() {
        return stubber(null);
    }

    public Stubber stubber(Strictness strictness) {
        MockingProgress mockingProgress = mockingProgress();
        mockingProgress.stubbingStarted();
        mockingProgress.resetOngoingStubbing();
        return new StubberImpl(strictness);
    }

    public void validateMockitoUsage() {
        mockingProgress().validateState();
    }

    /**
     * For testing purposes only. Is not the part of main API.
     *
     * @return last invocation
     */
    public Invocation getLastInvocation() {
        OngoingStubbingImpl ongoingStubbing =
                ((OngoingStubbingImpl) mockingProgress().pullOngoingStubbing());
        List<Invocation> allInvocations = ongoingStubbing.getRegisteredInvocations();
        return allInvocations.get(allInvocations.size() - 1);
    }

    public Object[] ignoreStubs(Object... mocks) {
        for (Object m : mocks) {
            InvocationContainerImpl container = getInvocationContainer(m);
            List<Invocation> ins = container.getInvocations();
            for (Invocation in : ins) {
                if (in.stubInfo() != null) {
                    in.ignoreForVerification();
                }
            }
        }
        return mocks;
    }

    public MockingDetails mockingDetails(Object toInspect) {
        return new DefaultMockingDetails(toInspect);
    }

    public LenientStubber lenient() {
        return new DefaultLenientStubber();
    }

    public void clearAllCaches() {
        MockUtil.clearAllCaches();
    }
}
