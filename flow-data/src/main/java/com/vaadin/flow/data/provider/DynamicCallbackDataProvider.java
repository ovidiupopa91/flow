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

import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.shared.Registration;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DynamicCallbackDataProvider<T, F> extends AbstractDynamicDataProvider<T, F> {

    private int estimatedSize = -1;

    private F lastFilter;

    protected BackEndDataProvider<T, F> backEndDataProvider;

    public DynamicCallbackDataProvider(CallbackDataProvider.FetchCallback<T, F> fetchCallBack,
                                       ValueProvider<T, Object> identifierGetter,
                                       SizeProvider<T, F> sizeProvider) {
        super(sizeProvider);
        this.backEndDataProvider =
                new CallbackDataProvider<>(fetchCallBack, q -> 0, identifierGetter);
    }

    public DynamicCallbackDataProvider(CallbackDataProvider.FetchCallback<T, F> fetchCallBack,
                                       SizeProvider<T, F> sizeProvider) {
        this(fetchCallBack, t -> t, sizeProvider);
    }

    public DynamicCallbackDataProvider(CallbackDataProvider.FetchCallback<T, F> fetchCallback) {
        this(fetchCallback, q -> 0);
        this.sizeProvider = this::getDefaultSizeProvider;
    }

    @Override
    public boolean isInMemory() {
        return false;
    }

    @Override
    public Object getId(T item) {
        return backEndDataProvider.getId(item);
    }

    @Override
    public Registration addDataProviderListener(DataProviderListener<T> listener) {
        return backEndDataProvider.addDataProviderListener(listener);
    }

    @Override
    public void refreshAll() {
        backEndDataProvider.refreshAll();
    }

    @Override
    public void refreshItem(T item, boolean refreshChildren) {
        backEndDataProvider.refreshItem(item, refreshChildren);
    }

    @Override
    public void refreshItem(T item) {
        backEndDataProvider.refreshItem(item);
    }

    @Override
    public Stream<T> fetch(Query<T, F> query) {
        final Query<T, F> extendedRangeQuery = addExtraItemsToQuery(query);

        Stream<T> fetchedItemsStream = backEndDataProvider.fetch(extendedRangeQuery);
        List<T> fetchedItems = fetchedItemsStream.collect(Collectors.toList());
        final int fetchedSize = fetchedItems.size();

        DynamicQuery<T, F> dynamicQuery = new DynamicQuery<>(query, estimatedSize, fetchedSize);

        estimatedSize = sizeProvider.apply(dynamicQuery);
        lastFilter = query.getFilter().orElse(null);

        return limitFetchedItems(query.getLimit(), fetchedItems);
    }

    @Override
    public int size(Query<T, F> query) {
        if (estimatedSize == -1) {

            // TODO: throw an exception?
            LoggerFactory.getLogger(DynamicCallbackDataProvider.class)
                    .warn("Fetch method should be invoked first, default initial size is used");
            return DEFAULT_INITIAL_SIZE_ASSUMPTION;
        }

        final String errorMessage =
                "DynamicDataProvider size should be requested for the same filter as a fetch query";

        // Check if query filter is in-sync with last fetch query filter
        if (query != null) {
            Optional<F> sizeFilter = query.getFilter();

            if (sizeFilter.isPresent() ^ lastFilter != null) {
                throw new IllegalArgumentException(errorMessage);
            } else if (sizeFilter.isPresent() && !sizeFilter.get().equals(lastFilter)) {
                throw new IllegalArgumentException(errorMessage);
            }
        } else if (lastFilter != null) {
            throw new IllegalArgumentException(errorMessage);
        }

        return estimatedSize;
    }

    private Stream<T> limitFetchedItems(int limit, List<T> fetchedData) {
        return fetchedData.size() <= limit ?
                fetchedData.stream() :
                fetchedData.subList(0, limit).stream();
    }

    private QueryTrace<T, F> addExtraItemsToQuery(Query<T, F> query) {
        return new QueryTrace<T, F>(
                query.getOffset(),
                query.getLimit() + EXTRA_ITEMS_FETCHED,
                query.getSortOrders(),
                query.getInMemorySorting(),
                query.getFilter().orElse(null));
    }
}
