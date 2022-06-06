/*
 * Copyright (c) 2016 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.creation.bytebuddy;

import org.mockito.mock.MockCreationSettings;
import org.mockito.plugins.MockMaker;

interface ClassCreatingMockMaker extends MockMaker {
    /**
     *
     从类图结构上来看， Mockito 通过 ByteBuddy 来创建 mock 类并进行实例化 proxy 对象。
     <b> 老版本中使用的是 cglib 的方式来进行创建子类 </b>
     其中主要通过接口类ClassCreatingMockMaker来创建proxy类。
     * @param settings
     * @return
     * @param <T>
     */
    <T> Class<? extends T> createMockType(MockCreationSettings<T> settings);
}
