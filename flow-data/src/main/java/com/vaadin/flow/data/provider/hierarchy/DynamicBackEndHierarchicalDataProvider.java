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
package com.vaadin.flow.data.provider.hierarchy;

import com.vaadin.flow.data.provider.AbstractDynamicDataProvider;
import com.vaadin.flow.data.provider.DataProviderListener;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.shared.Registration;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class DynamicBackEndHierarchicalDataProvider<T, F>
        extends AbstractDynamicDataProvider<T, F> {

    private AbstractBackEndHierarchicalDataProvider<T, F> backEndHierarchicalDataProvider;

    // TODO: add support for filtering: combine parentKey and Filter into one key
    private Map<String, Integer> estimatedChildCount = new HashMap<>();

    public DynamicBackEndHierarchicalDataProvider() {
        super();
    }

    public DynamicBackEndHierarchicalDataProvider(SizeProvider<T, F> sizeProvider) {
        super(sizeProvider);
        this.backEndHierarchicalDataProvider = new AbstractBackEndHierarchicalDataProvider<T, F>() {
            @Override
            protected Stream<T> fetchChildrenFromBackEnd(HierarchicalQuery<T, F> query) {
                if (query instanceof DynamicHierarchicalQuery) {
                    DynamicHierarchicalQuery<T, F> dynamicHierarchicalQuery =
                            (DynamicHierarchicalQuery<T, F>) query;

                    // TODO: add an EXTRA items
                    Stream<T> fetchedChildren = doFetchChildrenFromBackEnd(query);
                    List<T> fetchedChildrenItems = fetchedChildren.collect(Collectors.toList());

                    final int fetchedChildrenSize = fetchedChildrenItems.size();

                    estimatedChildCount.compute(dynamicHierarchicalQuery.getParentKey(),
                            (parentKey, oldCount) -> {
                                DynamicQuery<T, F> newQuery = new DynamicQuery<>(query,
                                                Optional.ofNullable(oldCount).orElse(-1),
                                                fetchedChildrenSize);
                                return sizeProvider.apply(newQuery);
                            });
                    return fetchedChildrenItems.stream();
                }
                throw new IllegalArgumentException(
                        "Query is not an instance of DynamicHierarchicalQuery");
            }

            @Override
            public int getChildCount(HierarchicalQuery<T, F> query) {
                if (query instanceof DynamicHierarchicalQuery) {
                    DynamicHierarchicalQuery<T, F> dynamicHierarchicalQuery =
                            (DynamicHierarchicalQuery<T, F>) query;
                    return estimatedChildCount.getOrDefault(dynamicHierarchicalQuery.getParentKey(),
                            DEFAULT_INITIAL_SIZE_ASSUMPTION);
                }
                throw new IllegalArgumentException(
                        "Query is not an instance of DynamicHierarchicalQuery");
            }

            @Override
            public boolean hasChildren(T item) {
                return hasChildrenInternal(item);
            }
        };
    }

    @Override
    public Registration addDataProviderListener(DataProviderListener<T> listener) {
        return backEndHierarchicalDataProvider.addDataProviderListener(listener);
    }

    @Override
    public void refreshAll() {
        backEndHierarchicalDataProvider.refreshAll();
    }

    @Override
    public void refreshItem(T item, boolean refreshChildren) {
        backEndHierarchicalDataProvider.refreshItem(item, refreshChildren);
    }

    @Override
    public void refreshItem(T item) {
        backEndHierarchicalDataProvider.refreshItem(item);
    }

    @Override
    public int size(Query<T, F> query) {
        return backEndHierarchicalDataProvider.size(query);
    }

    @Override
    public Stream<T> fetch(Query<T, F> query) {
        return backEndHierarchicalDataProvider.fetch(query);
    }

    @Override
    public Object getId(T item) {
        return backEndHierarchicalDataProvider.getId(item);
    }

    protected abstract Stream<T> doFetchChildrenFromBackEnd(HierarchicalQuery<T, F> query);

    protected abstract boolean hasChildrenInternal(T item);

    // TODO: separate class?
    public static class DynamicHierarchicalQuery<T, F> extends HierarchicalQuery<T, F> {
        private DynamicQuery<T, F> dynamicQuery;
        private String parentKey;

        //TODO: establish Parent Item<-> Parent Key mapping on DataCommunicator side

        public DynamicHierarchicalQuery(F filter, T parent) {
            super(filter, parent);
        }
        public DynamicHierarchicalQuery(int offset, int limit, List<QuerySortOrder> sortOrders,
                                        Comparator<T> inMemorySorting, F filter, T parent) {
            super(offset, limit, sortOrders, inMemorySorting, filter, parent);
        }
        public DynamicQuery<T, F> getDynamicQuery() { return dynamicQuery; }
        public String getParentKey() { return parentKey; }
    }
}
