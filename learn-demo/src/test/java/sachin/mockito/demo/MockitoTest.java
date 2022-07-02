package sachin.mockito.demo;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.ArrayList;

public class MockitoTest {
    //Mockito mock 泛型类、泛型接口

    @Test
    public void testA(){

        ArrayList<String> mock = Mockito.mock(ArrayList.class);
        Object abc = Mockito.when(mock.get(0)).thenReturn("abc").getMock();
        ExampleService mock1 = Mockito.mock(ExampleService.class);
        // Mockito.when(mock1.run()).thenThrow(new Exception());

        // Car boringStubbedCar = Mockito.when(Mockito.mock(Car.class).shiftGear()).thenThrow(EngineNotStarted.class).getMock();

        //验证方法调用的顺序
        mock.size();
        mock.add("abc");
        mock.clear();
        InOrder inOrder = Mockito.inOrder(mock);
        inOrder.verify(mock).size();
        inOrder.verify(mock).add("abcd");
        inOrder.verify(mock).clear();

    }
    // 测试 spy
    @Test
    public void test_spy() {

        ExampleService spyExampleService =Mockito.spy(new ExampleService());

        // 默认会走真实方法
        Assert.assertEquals(3, spyExampleService.add(1, 2));

        // 打桩后，不会走了
        Mockito.when(spyExampleService.add(1, 2)).thenReturn(10);
        Assert.assertEquals(10, spyExampleService.add(1, 2));

        // 但是参数比匹配的调用，依然走真实方法
        Assert.assertEquals(3, spyExampleService.add(2, 1));

    }

    class ExampleService {

        int add(int a, int b) {
            return a+b;
        }
        void run(){

        }

    }

}
