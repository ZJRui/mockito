package sachin.mockito.demo.mock;

import org.junit.jupiter.api.Assertions;
import org.springframework.util.Assert;

import javax.sound.midi.Soundbank;
import java.util.Objects;

public class MockTest {


    public static void main(String[] args) {

        //Objects.equals(arguments, invocationDetail.arguments)
        String[] data1 = {"a", "b"};
        String[] data2 = {"a", "b"};
        //false
        System.out.println(Objects.equals(data1, data2));
        //true
        System.out.println(Objects.deepEquals(data1, data2));


        String exceptedResult = "Mocked mghio";
        Target mockTarget = IMock.mock(Target.class);
        /**
         *
         * -----------
         * 《Mockito单元测试框架PDF》 https://github.com/mghio/imock/
         *
         * -----------
         * mockTarget.foo 方法的执行会被InterceptorDelegate 拦截，  然后交给
         * IMockInterceptor 方法处理， IMockInterceptro 的invoke方法中
         * 会根据当前方法的执行信息 创建一个InvocationDetail放置到 MockInterceptor对象的list中。
         * 然后 thenReturn 方法中就是从 List中取出 之前创建的InvocationDetail 然后将 thenReturn的参数
         * 设置到 InvocationDetail 中， 这样当你下次调用mockTarget.foo的时候就会发现List中
         * 已经有了针对 foo方法的InvocationDetail，然后就从这个InvocationDetai'l中取出result属性作为 foo方法的执行结果。
         *
         */
        IMock.when(mockTarget.foo("mghio")).thenReturn(exceptedResult);

        String actualResult = mockTarget.foo("mghio");

        Assertions.assertEquals(exceptedResult, actualResult);
    }
}
