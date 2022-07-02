/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

/**
 * Mark a field as a mock.
 *
 * <ul>
 * <li>Allows shorthand mock creation.</li>
 * <li>Minimizes repetitive mock creation code.</li>
 * <li>Makes the test class more readable.</li>
 * <li>Makes the verification error easier to read because the <b>field name</b> is used to identify the mock.</li>
 * <li>Automatically detects static mocks of type {@link MockedStatic} and infers the static mock type of the type parameter.</li>
 * </ul>
 *
 * <pre class="code"><code class="java">
 *   public class ArticleManagerTest extends SampleBaseTestCase {
 *
 *       &#064;Mock private ArticleCalculator calculator;
 *       &#064;Mock(name = "database") private ArticleDatabase dbMock;
 *       &#064;Mock(answer = RETURNS_MOCKS) private UserProvider userProvider;
 *       &#064;Mock(extraInterfaces = {Queue.class, Observer.class}) private ArticleMonitor articleMonitor;
 *       &#064;Mock(strictness = Mock.Strictness.LENIENT) private ArticleConsumer articleConsumer;
 *       &#064;Mock(stubOnly = true) private Logger logger;
 *
 *       private ArticleManager manager;
 *
 *       &#064;Before public void setup() {
 *           manager = new ArticleManager(userProvider, database, calculator, articleMonitor, articleConsumer, logger);
 *       }
 *   }
 *
 *   public class SampleBaseTestCase {
 *
 *       private AutoCloseable closeable;
 *
 *       &#064;Before public void openMocks() {
 *           closeable = MockitoAnnotations.openMocks(this);
 *       }
 *
 *       &#064;After public void releaseMocks() throws Exception {
 *           closeable.close();
 *       }
 *   }
 * </code></pre>
 *
 * <p>
 * <strong><code>MockitoAnnotations.openMocks(this)</code></strong> method has to be called to initialize annotated objects.
 * In above example, <code>openMocks()</code> is called in &#064;Before (JUnit4) method of test's base class.
 * <strong>Instead</strong> you can also put openMocks() in your JUnit runner (&#064;RunWith) or use the built-in
 * {@link MockitoJUnitRunner}. Also, make sure to release any mocks after disposing your test class with a corresponding hook.
 * </p>
 *
 * @see Mockito#mock(Class)
 * @see Spy
 * @see InjectMocks
 * @see MockitoAnnotations#openMocks(Object)
 * @see MockitoJUnitRunner
 */
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Documented
@SuppressWarnings("all")
public @interface Mock {
    /***
     *
     * 第一中方式： Mockito.mock(ArrayList.class)
     * 第二种方式就是使用 @Mock 注解方式来创建 Mock 对象，使用该方式创需要注意的是要在运行测试方法前使用
     * MockitoAnnotations.initMocks(this) 或者单元测试类上加上 @ExtendWith(MockitoExtension.class) 注解，
     * 如下所示代码创建了一个 List 类型的 Mock 对象(PS: @BeforeEach 是 Junit 5 的注解，功能类似于 Junit 4 的 @Before 注解。)：
     *
     * //@ExtendWith(MockitoExtension.class)
     * public class MockitoTest {
     *
     *   @Mock
     *   private List<String> mockList;
     *
     *   @BeforeEach
     *   public void beforeEach() {
     *     MockitoAnnotations.initMocks(this);
     *   }
     * }
     *
     *
     */

    /**
     * Mock will have custom answer, see {@link MockSettings#defaultAnswer(Answer)}.
     * For examples how to use 'Mock' annotation and parameters see {@link Mock}.
     */
    Answers answer() default Answers.RETURNS_DEFAULTS;

    /**
     * Mock will be 'stubOnly', see {@link MockSettings#stubOnly()}.
     * For examples how to use 'Mock' annotation and parameters see {@link Mock}.
     */
    boolean stubOnly() default false;

    /**
     * Mock will have custom name (shown in verification errors), see {@link MockSettings#name(String)}.
     * For examples how to use 'Mock' annotation and parameters see {@link Mock}.
     */
    String name() default "";

    /**
     * Mock will have extra interfaces, see {@link MockSettings#extraInterfaces(Class[])}.
     * For examples how to use 'Mock' annotation and parameters see {@link Mock}.
     */
    Class<?>[] extraInterfaces() default {};

    /**
     * Mock will be serializable, see {@link MockSettings#serializable()}.
     * For examples how to use 'Mock' annotation and parameters see {@link Mock}.
     */
    boolean serializable() default false;

    /**
     * @deprecated Use {@link Mock#strictness()} instead.
     *
     * Mock will be lenient, see {@link MockSettings#lenient()}.
     * For examples how to use 'Mock' annotation and parameters see {@link Mock}.
     *
     * @since 2.23.3
     */
    @Deprecated
    boolean lenient() default false;

    /**
     * Mock will have custom strictness, see {@link MockSettings#strictness(org.mockito.quality.Strictness)}.
     * For examples how to use 'Mock' annotation and parameters see {@link Mock}.
     *
     * @since 4.6.1
     */
    Strictness strictness() default Strictness.TEST_LEVEL_DEFAULT;

    enum Strictness {

        /**
         * Default value used to indicate the mock does not override the test level strictness.
         *
         * @since 4.6.1
         */
        TEST_LEVEL_DEFAULT,

        /**
         * See {@link org.mockito.quality.Strictness#LENIENT}
         */
        LENIENT,

        /**
         * See {@link org.mockito.quality.Strictness#WARN}
         */
        WARN,

        /**
         * See {@link org.mockito.quality.Strictness#STRICT_STUBS}
         */
        STRICT_STUBS
    }
}
