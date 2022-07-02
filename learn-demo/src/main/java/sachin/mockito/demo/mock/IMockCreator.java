package sachin.mockito.demo.mock;

import java.util.List;

public interface IMockCreator {

    <T>  T createMock(Class<T> mockTargetClass, List<InvocationDetail> behaviorList);
}
