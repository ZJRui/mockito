package sachin.mockito.demo.mock;

public class IMock {


    private static final IMockCore I_MOCK_CORE = new IMockCore();


    public static <T> T mock(Class<T> clazz) {
        return I_MOCK_CORE.mock(clazz);
    }

    public static <T>  InvocationDetail when(T methodCalll){
        return I_MOCK_CORE.when(methodCalll);
    }
}
