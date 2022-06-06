/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.runners;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.notification.RunNotifier;

/**
 * I'm using this surrogate interface to hide internal Runner implementations.
 * Surrogate cannot be used with &#064;RunWith therefore it is less likely clients will use interal runners.
 *
 * 我使用这个代理接口来隐藏内部的Runner实现。代理不能与@RunWith一起使用，因此客户端不太可能使用内部运行器
 */
public interface InternalRunner extends Filterable {

    void run(RunNotifier notifier);

    Description getDescription();
}
