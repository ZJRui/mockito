/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.handler;

import static org.mockito.internal.listeners.StubbingLookupNotifier.notifyStubbedAnswerLookup;
import static org.mockito.internal.progress.ThreadSafeMockingProgress.mockingProgress;

import org.mockito.internal.creation.settings.CreationSettings;
import org.mockito.internal.invocation.InvocationMatcher;
import org.mockito.internal.invocation.MatchersBinder;
import org.mockito.internal.stubbing.InvocationContainerImpl;
import org.mockito.internal.stubbing.OngoingStubbingImpl;
import org.mockito.internal.stubbing.StubbedInvocationMatcher;
import org.mockito.internal.stubbing.answers.DefaultAnswerValidator;
import org.mockito.internal.util.MockUtil;
import org.mockito.internal.verification.MockAwareVerificationMode;
import org.mockito.internal.verification.VerificationDataImpl;
import org.mockito.invocation.Invocation;
import org.mockito.invocation.InvocationContainer;
import org.mockito.invocation.MockHandler;
import org.mockito.mock.MockCreationSettings;
import org.mockito.verification.VerificationMode;

/**
 * Invocation handler set on mock objects.
 *
 * @param <T> type of mock object to handle
 */
public class MockHandlerImpl<T> implements MockHandler<T> {

    private static final long serialVersionUID = -2917871070982574165L;

    InvocationContainerImpl invocationContainer;

    MatchersBinder matchersBinder = new MatchersBinder();

    private final MockCreationSettings<T> mockSettings;

    public MockHandlerImpl(MockCreationSettings<T> mockSettings) {
        this.mockSettings = mockSettings;

        this.matchersBinder = new MatchersBinder();
        /**
         * 创建 InvocationContainerImpl
         */
        this.invocationContainer = new InvocationContainerImpl(mockSettings);
    }

    @Override
    public Object handle(Invocation invocation) throws Throwable {
        /**
         参数Invocation即InterceptedInvocation，把代理类拦截时的方法调用即参数封装在一起
         它包含一下几个对象：真正的方法realMethod，Mockito的方法MockitoMethod，参数arguments，以及mock对象mockRef
         private final MockReference<Object> mockRef;
         private final MockitoMethod mockitoMethod;
         private final Object[] arguments, rawArguments;
         private final RealMethod realMethod;
         */

        //-----------
        /**
         *
         *
         * when调用的基本形式是when(mock.doSome()), 此时，当mock.doSome()时即会触发这里的handler的执行，其中：
         * OngoingStubbingImpl表示正在对一个方法打桩的包装。
         * invocationContainerImpl 相当于一个mock对象的管家，记录着mock对象方法的调用。
         * mockingProgress则可以理解为一个和线程相关的记录器，用于存放每个线程正要准备做的一些事情，它的内部包含了几个report 和 pull 这样的函数，
         * 如下面看到，mockingProgress记录着ongoingStubbing对象。
         *
         *
         *
         * thenReturn方法： thenReturn是BaseStubbing的方法，其实它也是一个OngoingStubbing：
         *
         *
         * OngoingStubbing.thenReturn方法：再回过头来看MOCKITO_CORE里的when方法就会清晰许多，它会取出刚刚存放在mockingProgress
         * 中的ongoingStubbing对象。OngoingStubbing<T> stubbing = mockingProgress.pullOngoingStubbing();，而thenReturn、
         * thenThrow则是OngoingStubbing里的方法，这些方法最终都会调到如下方法:thenReturn调用thenAnswer
         */




        // 判断stub类型，对于doThrow和doAnswer先行处理
        if (invocationContainer.hasAnswersForStubbing()) {
            // stubbing voids with doThrow() or doAnswer() style
            InvocationMatcher invocationMatcher =
                    matchersBinder.bindMatchers(
                            mockingProgress().getArgumentMatcherStorage(), invocation);
            invocationContainer.setMethodForStubbing(invocationMatcher);
            return null;
        }
        // 获取验证模式
        VerificationMode verificationMode = mockingProgress().pullVerificationMode();
        // 获取调用匹配器对象
        InvocationMatcher invocationMatcher =
                matchersBinder.bindMatchers(
                        mockingProgress().getArgumentMatcherStorage(), invocation);

        mockingProgress().validateState();

        // 如果调用过verify验证方法，则这里不为null
        // if verificationMode is not null then someone is doing verify()
        if (verificationMode != null) {
            // We need to check if verification was started on the correct mock
            // - see VerifyingWithAnExtraCallToADifferentMockTest (bug 138)
            if (MockUtil.areSameMocks(
                    ((MockAwareVerificationMode) verificationMode).getMock(),
                    invocation.getMock())) {
                VerificationDataImpl data =
                        new VerificationDataImpl(invocationContainer, invocationMatcher);
                verificationMode.verify(data);
                return null;
            } else {
                // this means there is an invocation on a different mock. Re-adding verification
                // mode
                // - see VerifyingWithAnExtraCallToADifferentMockTest (bug 138)
                mockingProgress().verificationStarted(verificationMode);
            }
        }

        // prepare invocation for stubbing
        invocationContainer.setInvocationForPotentialStubbing(invocationMatcher);
        /**
         * when调用的基本形式是when(mock.doSome()), 此时，当mock.doSome()时即会触发上面的语句， OngoingStubbingImpl表示正在对一个
         * 方法打桩的包装，invocationContainerImpl 相当于一个mock对象的管家，记录着mock对象方法的调用。
         * 后面我们还会再见到它。mockingProgress则可以理解为一个和线程相关的记录器，用于存放每个线程正要准备做的一些事情，
         * 它的内部包含了几个report* 和 pull* 这样的函数，如上所看到，mockingProgress记录着ongoingStubbing对象。
         *
         * 其中OngoingStubbing对象会在每次MockHandlerImpl调用handle方法时创建一个，然后set到ThreadLocal的mockingProgress中
         *
         *
         *
         * --------------
         * 这里的调用栈就是 ： HttpClientHelper$MockitoMock$1962819602 是Mock生成的代理对象， 他的registerModule方法被代理了
         *   以List接口为例子， List接口被mock后的代理对象的add方法的源码如下：
         *     @Override
         *     public boolean add(Object object) { //------------------------------------> List接口的所有方法都被代理
         *         return (Boolean)MockMethodInterceptor.DispatcherDefaultingToRealMethod.interceptAbstract((Object)this, (MockMethodInterceptor)this.mockitoInterceptor, (Object)false, (Method)cachedValue$xiVxa7lf$sgg2351, (Object[])new Object[]{object});
         *     }
         *     从生成的代理方法中我们看到所有mock对象的方法调用都会被MockMethodInterceptor拦截。而MockMethodInterceptor最终会调到MockHandlerImpl的handle方法
         * 栈轨迹：
         * <init>:22, OngoingStubbingImpl (org.mockito.internal.stubbing)
         * handle:86, MockHandlerImpl (org.mockito.internal.handler)
         * handle:29, NullResultGuardian (org.mockito.internal.handler)
         * handle:33, InvocationNotifierHandler (org.mockito.internal.handler)
         * doIntercept:82, MockMethodInterceptor (org.mockito.internal.creation.bytebuddy)
         * doIntercept:56, MockMethodInterceptor (org.mockito.internal.creation.bytebuddy)
         * interceptAbstract:157, MockMethodInterceptor$DispatcherDefaultingToRealMethod (org.mockito.internal.creation.bytebuddy)
         * registerModule:-1, HttpClientHelper$MockitoMock$1962819602 (com.ebay.marketing.feeds.common.restclient)--------》代理对象 执行接口方法
         *
         * 那么也就是说每次执行 mock代理对象的 代理方法都会 执行MockHandlerImpl的handle 方法 从而创建一个OngoingStubbingImpl对象
         *
         *
         */
        OngoingStubbingImpl<T> ongoingStubbing = new OngoingStubbingImpl<T>(invocationContainer);
        mockingProgress().reportOngoingStubbing(ongoingStubbing);

        // look for existing answer for this invocation          // 为当前拦截的调用对象，查找存在的返回值对象answer
        StubbedInvocationMatcher stubbing = invocationContainer.findAnswerFor(invocation);
        // TODO #793 - when completed, we should be able to get rid of the casting below
        notifyStubbedAnswerLookup(
                invocation,
                stubbing,
                invocationContainer.getStubbingsAscending(),
                (CreationSettings) mockSettings);

        if (stubbing != null) {
            stubbing.captureArgumentsFrom(invocation);

            try {
                // 这里执行的answer方法，即是thenReturn的返回内容
                return stubbing.answer(invocation);
            } finally {
                // Needed so that we correctly isolate stubbings in some scenarios
                // see MockitoStubbedCallInAnswerTest or issue #1279
                mockingProgress().reportOngoingStubbing(ongoingStubbing);
            }
        } else {
            // 如果打桩对象为空，也就是mock对象的默认行为，由于mock对象的每一个操作都是“无效”的，因此都会返回一个相应类型的默认值（stub除外）
            Object ret = mockSettings.getDefaultAnswer().answer(invocation);
            DefaultAnswerValidator.validateReturnValueFor(invocation, ret);

            // Mockito uses it to redo setting invocation for potential stubbing in case of partial
            // mocks / spies.
            // Without it, the real method inside 'when' might have delegated to other self method
            // and overwrite the intended stubbed method with a different one.
            // This means we would be stubbing a wrong method.
            // Typically this would led to runtime exception that validates return type with stubbed
            // method signature.
            invocationContainer.resetInvocationForPotentialStubbing(invocationMatcher);
            return ret;
        }
    }

    @Override
    public MockCreationSettings<T> getMockSettings() {
        return mockSettings;
    }

    @Override
    public InvocationContainer getInvocationContainer() {
        return invocationContainer;
    }
}
