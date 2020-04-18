/*
 * Copyright 2000-2020 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.data.binder;

import com.vaadin.flow.data.provider.DynamicDataProvider;
import com.vaadin.flow.function.SerializableFunction;

public interface HasFilterableDynamicDataProvider<T, F> extends HasItems<T> {

    default void setDataProvider(DynamicDataProvider<T, F> dataProvider) {
        setDataProvider(dataProvider, SerializableFunction.identity());
    }

    <C> void setDataProvider(DynamicDataProvider<T, C> dataProvider,
                             SerializableFunction<F, C> filterConverter);
}
