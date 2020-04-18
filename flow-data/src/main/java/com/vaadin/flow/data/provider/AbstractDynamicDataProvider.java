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
package com.vaadin.flow.data.provider;

import java.util.Objects;

public abstract class AbstractDynamicDataProvider<T, F> implements DynamicDataProvider<T, F> {

    protected static final int DEFAULT_INITIAL_SIZE_ASSUMPTION = 150;

    protected static final int EXTRA_ITEMS_FETCHED = 5;

    protected SizeProvider<T, F> sizeProvider;

    public AbstractDynamicDataProvider() {
        this.sizeProvider = this::getDefaultSizeProvider;
    }

    public AbstractDynamicDataProvider(SizeProvider<T, F> sizeProvider) {
        Objects.requireNonNull(sizeProvider, "Size Provider callback cannot be null");
        this.sizeProvider = sizeProvider;
    }

    // TODO: final?
    protected Integer getDefaultSizeProvider(DynamicQuery<T, F> query) {

        // TODO: what is the optimal extra items count?
        final int extraItemsAssumption = DEFAULT_INITIAL_SIZE_ASSUMPTION;

        final int offset = query.getOffset();
        final int limit = query.getLimit();
        final int previousEstimated = query.getPreviousEstimated();
        final int fetchedSize = query.getFetchedSize();
        final int lastIndexExpected = offset + limit;

        int estimated;

        if (previousEstimated == -1) {
            return DEFAULT_INITIAL_SIZE_ASSUMPTION;
        }

        if (fetchedSize == 0) {
            // Size is somewhere between 0 and offset, [0..offset]
            estimated = offset;
        } else if (fetchedSize <= limit) {
            // Size is <offset + fetchedSize> strictly
            estimated = offset + fetchedSize;
        } else {
            // Size is somewhere more than <offset + limit>, [offset+limit..infinite]
            estimated = lastIndexExpected + extraItemsAssumption;
        }

        return estimated;
    }
}
