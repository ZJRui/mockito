/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito;

import org.mockito.internal.stubbing.answers.CallsRealMethods;
import org.mockito.internal.stubbing.defaultanswers.GloballyConfiguredAnswer;
import org.mockito.internal.stubbing.defaultanswers.ReturnsDeepStubs;
import org.mockito.internal.stubbing.defaultanswers.ReturnsMocks;
import org.mockito.internal.stubbing.defaultanswers.ReturnsSmartNulls;
import org.mockito.internal.stubbing.defaultanswers.TriesToReturnSelf;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Enumeration of pre-configured mock answers
 * <p>
 * You can use it to pass extra parameters to &#064;Mock annotation, see more info here: {@link Mock}
 * <p>
 * Example:
 * <pre class="code"><code class="java">
 *   &#064;Mock(answer = RETURNS_DEEP_STUBS) UserProvider userProvider;
 * </code></pre>
 * <b>This is not the full list</b> of Answers available in Mockito. Some interesting answers can be found in org.mockito.stubbing.answers package.
 */
public enum Answers implements Answer<Object> {
    /**
     * The default configured answer of every mock.
     *
     * <p>Please see the {@link org.mockito.Mockito#RETURNS_DEFAULTS} documentation for more details.</p>
     *
     * @see org.mockito.Mockito#RETURNS_DEFAULTS
     */
    RETURNS_DEFAULTS(new GloballyConfiguredAnswer()),

    /**
     * An answer that returns smart-nulls.
     *
     * <p>Please see the {@link org.mockito.Mockito#RETURNS_SMART_NULLS} documentation for more details.</p>
     *
     * @see org.mockito.Mockito#RETURNS_SMART_NULLS
     */
    RETURNS_SMART_NULLS(new ReturnsSmartNulls()),

    /**
     * An answer that returns <strong>mocks</strong> (not stubs).
     *
     * <p>Please see the {@link org.mockito.Mockito#RETURNS_MOCKS} documentation for more details.</p>
     *
     * @see org.mockito.Mockito#RETURNS_MOCKS
     */
    RETURNS_MOCKS(new ReturnsMocks()),

    /**
     * An answer that returns <strong>deep stubs</strong> (not mocks).
     *
     * <p>Please see the {@link org.mockito.Mockito#RETURNS_DEEP_STUBS} documentation for more details.</p>
     *
     * 场景：
     * HelloService的getByName 返回一个Student。 我们针对HelloService 进行mock，并when getByName thenReturn(  StudentA)
     *
     * 其中StudentA 是我们手动new 的一个对象。然后 我们 就可以这样执行
     * when(helooService.getByName()).thenReturn(StudentA)
     *  helloService.getByName().getName();
     *
     *
     * 但是 如果我们 想 对 getByName的返回值进行mock， 也就是 Student studnetB=mock(Student.class)
     * 然后这个StudnetB 作为 getByNmae 方法的 thenReturn的参数。
     *  when(helooService.getByName()).thenReturn(StudentB)
     *  helloService.getByName().getName();
     *
     * 那么我们 调用 helloService.getByName 就会返回一个mock的StudentB。 缺点是 需要手动 mock Student 或者需要手动 new Student 并设置 thenReturn。
     *
     *
     * RETURNS_DEEP_STUBS 配置就是 对被mock的Service的返回值也会自动创建Mock 并返回
     *
     *
     *
     * @see org.mockito.Mockito#RETURNS_DEEP_STUBS
     */
    RETURNS_DEEP_STUBS(new ReturnsDeepStubs()),

    /**
     * An answer that calls the real methods (used for partial mocks).
     *
     * <p>Please see the {@link org.mockito.Mockito#CALLS_REAL_METHODS} documentation for more details.</p>
     *
     * @see org.mockito.Mockito#CALLS_REAL_METHODS
     */
    CALLS_REAL_METHODS(new CallsRealMethods()),

    /**
     * An answer that tries to return itself. This is useful for mocking {@code Builders}.
     *
     * <p>Please see the {@link org.mockito.Mockito#RETURNS_SELF} documentation for more details.</p>
     *
     * @see org.mockito.Mockito#RETURNS_SELF
     */
    RETURNS_SELF(new TriesToReturnSelf());

    private final Answer<Object> implementation;

    Answers(Answer<Object> implementation) {
        this.implementation = implementation;
    }

    @Override
    public Object answer(InvocationOnMock invocation) throws Throwable {
        return implementation.answer(invocation);
    }
}
