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


    /**
     *
     *
     *
     *
     *
     * @param methodCall
     * @return
     * @param <T>
     */

    public <T> OngoingStubbing<T> when(T methodCall) {
        /**
         *
         * ----------------参考内容《Mockito单元测试框架-ongoingstubbing》
         * when 方法接收的参数类型时T，这个T时方法预定义的类型T，T的作用时根据方法的入参 限定方法返回值的反省类型。
         *
         * Mockito.when(userService.getNameByUid(anyInt())).thenReturn(xx);
         * 问题： getNameByUid的返回值时一个String类型，那么 上面的代码可以这样写吗？
         * Mockito.when("abc").thenReturn(xx)
         * 答案时不可以的， 因为 when方法虽然接受收一个String类型的参数，但是  第二中写法 没有执行mock对象的 getNameByUid 这个方法。
         * 实际上在执行mock对象的getNameByUid 方法的时候 会创建一个方法调用对象信息保存到Mock对象的上下文中，这个是通过mock的拦截器实现的。
         * 当调用when方法的时候，实际上是从该上下文中获取最后一个注册的方法调用，然后把thenReturn的参数作为其返回值保存。
         *
         * 然后当我们再次调用 mock对象的getNameByUid 方法时 之前已经记录的方法行为将被再次回访， 该方法触发拦截器重新调用并且返回我们在thenReturn
         * 方法指定的返回值。
         *
         * 也就是说 以下两种方式都能实现mock
         * （1）
         *  Mockito.when(userService.getNameByUid(anyInt())).thenReturn(xx);
         *  userService.getNameByUid(1)==xx
         *  上面userService 是mock对象， 默认情况下 getNameByUid返回null，因此when方法接收的值其实是null， 也就是methodCall 为null，但是
         *  我们知道getNameByUid返回的类型是String。
         *   所以 when方法的methodCall 参数 在方法内实际没有使用到。
         *  (2)
         *  userSerivce.getNameByUid(12)//因为没有mock，所以方法返回值为null
         *  when("abc").thenReturn(xx)
         *  userService.getNameByUid(12)==xx
         *  因为首先执行了 userSerivce.getNameByUid(12) 这就会 在mock上下文中记录一次方法调用
         *  然后又执行了when.thenReturn 因此 thenReturn 的值就会记录到 getNameBYUid的方法调用中。
         *  最后我们再次执行 userService.getNameByUid(12)就会返回xx
         *
         *
         *---------------------------------------------------------------------------------------
         *
         *实现原理解析：
         *每一个线程都会有一个 MockinigProgressImpl 对象， 这个对象 是在调用
         * org.mockito.internal.progress.ThreadSafeMockingProgress#mockingProgress() 方法的时候触发创建。
         *
         * 当执行mock对象的 方法的时候，  下面的mockingProgress 方法会返回MockingProgresssImpl对象，然后 将OngoingStubbingImpl
         * 对象记录到 MockingProgressimpl对象中.
         *
         * 而OngoingStubbingImpl对象中记录了 调用信息invocationContainer。
         *
         * 那么也就是说  mock对象的方法执行 -->创建OngoingStubbingImpl 记录方法调用信息， 这个OngoingStubbingImpl 被保存到了
         * 当前线程的 MockingProgressImpl 中。
         *
         * when方法就是从 MockingProgreessImpl 中 获取 当前的OngoingStubbingImpl 【注意是当前的 OngoingStubbingImpl  ，因为
         * mock对象的每次方法调用都会被MockHandlerImpl拦截，创建新的OngoingStubbingImpl 记录到MockingProgressImpl中 】，
         * 然后 这个被获取到的OngoingStubbingImpl 就是when方法的返回值。  when方法的返回值会被调用.thenReturn,
         * 因此也就是执行OngoingStuggingImpl的 thenReturn. 在thenReturn 方法中会创建一个answer， 然后这个answer被加入到了invocationContainer 中。
         *  invocationContainer.addAnswer(answer, strictness);
         *
         *  因此这就导致 当我们下次调用mock对象的这个方法的时候 就会从这个invocationContainer 对象中findAnswer
         *
         *----------------------------------------
         *
         * 这个mockingProgress是个什么东西？OngoingStubbing又是什么呢？想要弄清楚来龙去脉，
         * 还记得刚才一直强调的MockHandlerImpl吗？上文说过，mock对象所有的方法最终都会交由MockHandlerImpl的handle方法处理，
         * 所以这注定是一个不一样的女子，不，方法：
         *
         *  // 1.初始化/或者获取 MockProgressImpl对象
         *
         *
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
