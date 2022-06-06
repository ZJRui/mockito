/*
 * Copyright (c) 2016 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.creation.instance;

import org.mockito.creation.instance.Instantiator;
import org.mockito.internal.configuration.GlobalConfiguration;
import org.objenesis.ObjenesisStd;

class ObjenesisInstantiator implements Instantiator {

    // TODO: in order to provide decent exception message when objenesis is not found,
    // have a constructor in this class that tries to instantiate ObjenesisStd and if it fails then
    // show decent exception that dependency is missing
    // TODO: for the same reason catch and give better feedback when hamcrest core is not found.
    private final ObjenesisStd objenesis =
            new ObjenesisStd(new GlobalConfiguration().enableClassCache());

    @Override
    public <T> T newInstance(Class<T> cls) {
        /**
         * 这里调用了objenesis.newInstance，那这个objenesis是何许人也呢？
         * 这又是一个相当牛逼的框架，可以根据不同的平台选择不同的方法来new对象。总之一句话，你只要输入一个class进去，它就会输出其一个实例对象。感兴趣的可以查看其Github:https://github.com/easymock/objenesis。
         */
        return objenesis.newInstance(cls);
    }
}
