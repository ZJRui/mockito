package sachin.com.learn;

import org.mockito.Mockito;

import java.util.List;

public class MainDemo {
    public static void main(String[] args) throws Exception {


        List<String> mockedList = Mockito.mock(List.class);
        // 查看mock对象的类名: $java.util.List$$EnhancerByMockitoWithCGLIB$$6c214c3e
        System.out.println(mockedList.getClass().getName());
        System.in.read();
    }
}
