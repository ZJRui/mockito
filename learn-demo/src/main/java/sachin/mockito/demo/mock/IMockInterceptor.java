package sachin.mockito.demo.mock;

import java.lang.reflect.Method;
import java.util.List;

public class IMockInterceptor {
    List<InvocationDetail> recordedInvocationDetails;

    public IMockInterceptor(List<InvocationDetail> recordedInvocationDetails) {
        this.recordedInvocationDetails = recordedInvocationDetails;
    }

    public Object invoke(Object mock, Method invokedMethod, Object[] arguemnts) {
        String methodName=invokedMethod.getName();
        String attachedClassName=mock.getClass().getName();

        InvocationDetail invocationDetail = new InvocationDetail(attachedClassName, methodName, arguemnts);

        //第一种方式
        //recordedInvocationDetails.add(invocationDetail);

        //第二中方式
        if (!recordedInvocationDetails.contains(invocationDetail)) {
            System.out.println("not contain:"+invocationDetail.toString());
            recordedInvocationDetails.add(invocationDetail);
            /**
             *
             *
             * Annotations have their "attributes" as methods. For instance:
             *
             * public @interface Example {
             *     public String stringValue() default "string default value";
             *     public int intValue() default 10;
             * }
             * The getDefaultValue() of a Method from an annotation returns the default value of an
             * annotation "attribute" defined this way. In the example, the default value
             * of the Method stringValue() is "string default value".
             */
            return invokedMethod.getDefaultValue();
        }else{
            int index = recordedInvocationDetails.indexOf(invocationDetail);
            InvocationDetail invocationDetail1 = recordedInvocationDetails.get(index);
            return invocationDetail1.getResult();
        }
    }
}
