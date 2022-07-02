package sachin.mockito.demo.mock;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Modifier;
import java.util.List;

public class ByteBuddyIMockCreator implements  IMockCreator{

    private final ObjenesisStd objenessisStd = new ObjenesisStd();
    @Override
    public <T> T createMock(Class<T> mockTargetClass, List<InvocationDetail> behaviorList) {

        /**
         * 使用Byte Buddy库在运行时生成Mock类对象代码，然后使用Objenesis去实例化该对象
         *
         */
        ByteBuddy byteBuddy = new ByteBuddy();
        Class<? extends T> classWithInterceptor = byteBuddy.subclass(mockTargetClass)
            .method(ElementMatchers.any())
            .intercept(MethodDelegation.to(InterceptorDelegate.class))
            .defineField("interceptor", IMockInterceptor.class, Modifier.PRIVATE)
            .implement(IMockIntercepable.class)
            .intercept(FieldAccessor.ofBeanProperty())
            .make()
            .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
            .getLoaded();


        T mockTargetInstance = objenessisStd.newInstance(classWithInterceptor);
        ((IMockIntercepable) mockTargetInstance).setInterceptor(new IMockInterceptor(behaviorList));


        return mockTargetInstance;
    }
}
