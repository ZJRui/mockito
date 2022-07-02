package sachin.mockito.demo.mock;

import java.util.ArrayList;
import java.util.List;

public class IMockCore {


    private final List<InvocationDetail> invocationDetailList = new ArrayList<>(8);


    private final IMockCreator mockCreator = new ByteBuddyIMockCreator();

    public <T> T mock(Class<T> mockTargetClass) {
        T result = mockCreator.createMock(mockTargetClass, invocationDetailList);
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T> InvocationDetail<T> when(T methodCall){
        int size = invocationDetailList.size();
        return  invocationDetailList.get(size-1);
    }

}
