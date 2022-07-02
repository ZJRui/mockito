/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.progress;

/**
 * Provides access to the {@link MockingProgress} of a corresponding {@link Thread}. Every {@link Thread} in Mockito has it s own {@link MockingProgress} to avoid data races while stubbing.
 */
public class ThreadSafeMockingProgress {


    /**
     *  如果当前线程没有 MockingProgressImpl 对象，则为当前线程创建一个MockingProgressImpl对象
     *
     */
    private static final ThreadLocal<MockingProgress> MOCKING_PROGRESS_PROVIDER =
            new ThreadLocal<MockingProgress>() {
                @Override
                protected MockingProgress initialValue() {
                    return new MockingProgressImpl();
                }
            };

    private ThreadSafeMockingProgress() {}

    /**
     * Returns the {@link MockingProgress} for the current Thread.
     * <p>
     * <b>IMPORTANT</b>: Never assign and access the returned {@link MockingProgress} to an instance or static field. Thread safety can not be guaranteed in this case, cause the Thread that wrote the field might not be the same that read it. In other words multiple threads will access the same {@link MockingProgress}.
     *
     * @return never <code>null</code>
     *
     *
     * 返回当前线程的MockingProgress。
     * 注意:永远不要将返回的MockingProgress赋值和访问到实例或静态字段。
     * 在这种情况下，线程安全无法得到保证，因为写入字段的线程可能与读取字段的线程不同。换句话说，多个线程将访问相同的MockingProgress。
     *
     */
    public static final MockingProgress mockingProgress() {
        return MOCKING_PROGRESS_PROVIDER.get();
    }
}
