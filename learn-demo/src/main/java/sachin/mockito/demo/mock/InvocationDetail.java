package sachin.mockito.demo.mock;

import lombok.Data;
import sun.java2d.pipe.SpanIterator;

import java.util.Arrays;
import java.util.Objects;

@Data
public class InvocationDetail<T> {

    private String attachedClassName;

    private String methodName;
    private Object[] arguments;

    private T result;

    public InvocationDetail(String attachedClassName, String methodName, Object[] arguments) {
        this.attachedClassName = attachedClassName;
        this.methodName = methodName;
        this.arguments = arguments;
    }

    public void thenReturn(T t ){
        this.result=t;
    }

    public T getResult() {
        return result;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        InvocationDetail<?> invocationDetail = (InvocationDetail<?>) obj;


        // return Objects.equals(arguments, invocationDetail.arguments) &&
        //     Objects.equals(methodName, invocationDetail.methodName)
        //     && Objects.equals(attachedClassName, invocationDetail.attachedClassName);

        //上面的写法 是不正确的， arguemnts是一个数组对象， Objects.equals方法 仅仅是 使用了数组对象的equals方法，因此
        //两个不同的数组对象 即便含有相同的元素 equals也是false
        return Objects.deepEquals(arguments, invocationDetail.arguments)
            && Objects.equals(methodName, invocationDetail.methodName)
            && Objects.equals(attachedClassName, invocationDetail.attachedClassName);
    }

    @Override
    public int hashCode() {
        int result=Objects.hash(attachedClassName,methodName);
        result = 31 * result + Arrays.hashCode(arguments);
        return result;

    }
}
