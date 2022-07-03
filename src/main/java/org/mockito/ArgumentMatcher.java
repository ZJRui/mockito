/*
 * Copyright (c) 2016 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito;

/**
 * Allows creating customized argument matchers.
 * This API was changed in Mockito 2.1.0 in an effort to decouple Mockito from Hamcrest
 * and reduce the risk of version incompatibility.
 * Migration guide is included close to the bottom of this javadoc.
 * <p>
 * For non-trivial method arguments used in stubbing or verification, you have following options
 * (in no particular order):
 * <ul>
 *     <li>refactor the code so that the interactions with collaborators are easier to test with mocks.
 *     Perhaps it is possible to pass a different argument to the method so that mocking is easier?
 *     If stuff is hard to test it usually indicates the design could be better, so do refactor for testability!
 *     </li>
 *     <li>don't match the argument strictly, just use one of the lenient argument matchers like
 *     {@link Mockito#notNull()}. Some times it is better to have a simple test that works than
 *     a complicated test that seem to work.
 *     </li>
 *     <li>implement equals() method in the objects that are used as arguments to mocks.
 *     Mockito naturally uses equals() for argument matching.
 *     Many times, this is option is clean and simple.
 *     </li>
 *     <li>use {@link ArgumentCaptor} to capture the arguments and perform assertions on their state.
 *     Useful when you need to verify the arguments. Captor is not useful if you need argument matching for stubbing.
 *     Many times, this option leads to clean and readable tests with fine-grained validation of arguments.
 *     </li>
 *     <li>use customized argument matchers by implementing {@link ArgumentMatcher} interface
 *     and passing the implementation to the {@link Mockito#argThat} method.
 *     This option is useful if custom matcher is needed for stubbing and can be reused a lot.
 *     Note that {@link Mockito#argThat} demonstrates <b>NullPointerException</b> auto-unboxing caveat.
 *     </li>
 *     <li>use an instance of hamcrest matcher and pass it to
 *     {@link org.mockito.hamcrest.MockitoHamcrest#argThat(org.hamcrest.Matcher)}
 *     Useful if you already have a hamcrest matcher. Reuse and win!
 *     Note that {@link org.mockito.hamcrest.MockitoHamcrest#argThat(org.hamcrest.Matcher)} demonstrates <b>NullPointerException</b> auto-unboxing caveat.
 *     </li>
 *     <li>Java 8 only - use a lambda in place of an {@link ArgumentMatcher} since {@link ArgumentMatcher}
 *     is effectively a functional interface. A lambda can be used with the {@link Mockito#argThat} method.</li>
 * </ul>
 *
 * <p>
 * Implementations of this interface can be used with {@link ArgumentMatchers#argThat} method.
 * Use <code>toString()</code> method for description of the matcher
 * - it is printed in verification errors.
 *
 * <pre class="code"><code class="java">
 * class ListOfTwoElements implements ArgumentMatcher&lt;List&gt; {
 *     public boolean matches(List list) {
 *         return list.size() == 2;
 *     }
 *     public String toString() {
 *         //printed in verification errors
 *         return "[list of 2 elements]";
 *     }
 * }
 *
 * List mock = mock(List.class);
 *
 * when(mock.addAll(argThat(new ListOfTwoElements()))).thenReturn(true);
 *
 * mock.addAll(Arrays.asList(&quot;one&quot;, &quot;two&quot;));
 *
 * verify(mock).addAll(argThat(new ListOfTwoElements()));
 * </code></pre>
 *
 * To keep it readable you can extract method, e.g:
 *
 * <pre class="code"><code class="java">
 *   verify(mock).addAll(<b>argThat(new ListOfTwoElements())</b>);
 *   //becomes
 *   verify(mock).addAll(<b>listOfTwoElements()</b>);
 * </code></pre>
 *
 * In Java 8 you can treat ArgumentMatcher as a functional interface
 * and use a lambda, e.g.:
 *
 * <pre class="code"><code class="java">
 *   verify(mock).addAll(<b>argThat(list -&gt; list.size() == 2)</b>);
 * </code></pre>
 *
 * <p>
 * Read more about other matchers in javadoc for {@link ArgumentMatchers} class.
 * <h2>2.1.0 migration guide</h2>
 *
 * All existing custom implementations of <code>ArgumentMatcher</code> will no longer compile.
 * All locations where hamcrest matchers are passed to <code>argThat()</code> will no longer compile.
 * There are 2 approaches to fix the problems:
 * <ul>
 * <li>a) Refactor the hamcrest matcher to Mockito matcher:
 * Use "implements ArgumentMatcher" instead of "extends ArgumentMatcher".
 * Then refactor <code>describeTo()</code> method into <code>toString()</code> method.
 * </li>
 * <li>
 * b) Use <code>org.mockito.hamcrest.MockitoHamcrest.argThat()</code> instead of <code>Mockito.argThat()</code>.
 * Ensure that there is <a href="http://hamcrest.org/JavaHamcrest/">hamcrest</a> dependency on classpath
 * (Mockito does not depend on hamcrest any more).
 *
 * </li>
 * </ul>
 * What option is right for you? If you don't mind compile dependency to hamcrest
 * then option b) is probably right for you.
 * Your choice should not have big impact and is fully reversible -
 * you can choose different option in future (and refactor the code)
 *
 * @param <T> type of argument
 * @since 2.1.0
 */
@SuppressWarnings("all")
public interface ArgumentMatcher<T> {


    /**
     *
     *
     * Mockito参数匹配器的实现使用了Hamcrest框架（一个书写匹配器对象时允许直接定义匹配规则的框架，
     * 网址：http://code.google.com/p/hamcrest/）。它已经提供了许多规则供我们使用，
     * Mockito在此基础上也内建了很规则。但有时我们还是需要更灵活的匹配，所以需要自定义参数匹配器。
     *
     * ArgumentMatcher抽象类
     * 自定义参数匹配器的时候需要继承ArgumentMatcher抽象类，它实现了Hamcrest框架的Matcher接口，
     * 定义了describeTo方法，所以我们只需要实现matches方法在其中定义规则即可。
     * 下面自定义的参数匹配器是匹配size大小为2的List：
     * Java代码   收藏代码
     * class IsListOfTwoElements extends ArgumentMatcher<List> {
     *     public boolean matches(Object list) {
     *         return ((List) list).size() == 2;
     *     }
     * }
     *
     * @Test
     * public void argumentMatchersTest(){
     *     List mock = mock(List.class);
     *     when(mock.addAll(argThat(new IsListOfTwoElements()))).thenReturn(true);
     *
     *     mock.addAll(Arrays.asList("one", "two", "three"));
     *     verify(mock).addAll(argThat(new IsListOfTwoElements()));
     * }
     *
     * argThat(Matcher<T> matcher)方法用来应用自定义的规则，可以传入任何实现Matcher接口的实现类。
     * 上例中在stubbing和verify addAll方法时通过argThat(Matcher<T> matcher)，
     * 传入了自定义的参数匹配器IsListOfTwoElements用来匹配size大小为2的List。因为例子中传入List的元素为三个，所以测试将失败。
     *
     * 较复杂的参数匹配将会降低测试代码的可读性。有时实现参数对象的equals()方法是个不错的选择
     * （Mockito默认使用equals()方法进行参数匹配），它可以使测试代码更为整洁。另外，有些场景使用参数
     * 捕获器（ArgumentCaptor）要比自定义参数匹配器更加合适
     */

    /**
     * Informs if this matcher accepts the given argument.
     * <p>
     * The method should <b>never</b> assert if the argument doesn't match. It
     * should only return false.
     * <p>
     * See the example in the top level javadoc for {@link ArgumentMatcher}
     *
     * @param argument
     *            the argument
     * @return true if this matcher accepts the given argument.
     */
    boolean matches(T argument);
}
