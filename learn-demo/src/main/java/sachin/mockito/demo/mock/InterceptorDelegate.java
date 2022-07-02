package sachin.mockito.demo.mock;

import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;

public class InterceptorDelegate {

    @RuntimeType
    public static Object intercept(@This Object mock, @Origin Method invokedMethod,
                                   @FieldValue("interceptor") IMockInterceptor interceptor,
                                   @AllArguments Object[] arguments) {
        return interceptor.invoke(mock, invokedMethod, arguments);
    }
}
