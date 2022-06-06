package sachin.com.learn;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import sachin.com.learn.service.Student;
import sachin.com.learn.service.UserService;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class MockitoDemoTest {


    @Test
    public void testParamMatch(){

        UserService mock = Mockito.mock(UserService.class);
        Student student = new Student();
        Mockito.when(mock.getStudent(student)).thenReturn(student);
        Student student2 = new Student();
        Student student2Res = mock.getStudent(student2);
        Assert.assertEquals(student, student2Res);
    }



    @Test
    public void testGetMockListByteCode() throws Exception{
        /**
         *
         * https://blog.csdn.net/yitian_z/article/details/114223395
         * 如何反编译查看 生成的代理类
         * （1）首先写一个main mock 一下list， 输出 生成的mockedList的classname，然后read阻塞，
         * （2）启动arthas ，通过sc > 1.txt 命令打出所有的类
         * （3） jad org.mockito.codegen.List$MockitoMock$jukTZ93m > 2.txt
         *
         *  sc查询到有几个值得注意的 mockedList
         *
         * org.mockito.codegen.List$MockitoMock$jukTZ93m
         * org.mockito.codegen.List$MockitoMock$jukTZ93m$auxiliary$0VR89SN5
         * org.mockito.codegen.List$MockitoMock$jukTZ93m$auxiliary$5Bj4ieM4
         * org.mockito.codegen.List$MockitoMock$jukTZ93m$auxiliary$DJ3RUnqb
         * org.mockito.codegen.List$MockitoMock$jukTZ93m$auxiliary$FdpyymLH
         *
         * 对应的源文件 分别在  docsSachin目录下的 List$MockitoMock$jukTZ93m.txt
         * List$MockitoMock$jukTZ93m$auxiliary$5Bj4ieM4.txt
         *
         * 在 List$MockitoMock$jukTZ93m 中 可以看到几点关键的地方：
         *（1）持有一个MockMethodInterceptor对象
         * （2）继承了MockAccess
         * （3）List原来的方法都被DispatcherDefaultingToRealMethod拦截了，其中，DispatcherDefaultingToRealMethod是MockMethodInterceptor的内部类，
         * 最终它还是调用MockMethodInterceptor的doIntercept，而该方法中调用的是MockHanderImpl.handle方法：
         *
         */
        List<String> mockedList = Mockito.mock(List.class);
        // 查看mock对象的类名: $java.util.List$$EnhancerByMockitoWithCGLIB$$6c214c3e
        System.out.println(mockedList.getClass().getName());

    }
}
