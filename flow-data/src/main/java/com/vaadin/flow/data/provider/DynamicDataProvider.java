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

import com.vaadin.flow.data.provider.CallbackDataProvider.FetchCallback;
import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.shared.Registration;

import java.io.Serializable;
import java.util.Objects;
import java.util.stream.Stream;

public interface DynamicDataProvider<T, F> extends Serializable {

    /**
     * Gets whether the DataProvider content all available in memory or does it
     * use some external backend.
     *
     * @return {@code true} if all data is in memory; {@code false} if not
     */
    boolean isInMemory();

    /**
     * Gets the amount of data in this DataProvider.
     *
     * @param query
     *            query with sorting and filtering
     * @return the size of the data provider
     */
    int size(Query<T, F> query);

    /**
     * Fetches data from this DataProvider using given {@code query}.
     *
     * @param query
     *            given query to request data
     * @return the result of the query request: a stream of data objects, not
     *         {@code null}
     */
    Stream<T> fetch(Query<T, F> query);

    /**
     * Refreshes the given item. This method should be used to inform all
     * {@link DataProviderListener DataProviderListeners} that an item has been
     * updated or replaced with a new instance.
     * <p>
     * For this to work properly, the item must either implement
     * {@link Object#equals(Object)} and {@link Object#hashCode()} to consider
     * both the old and the new item instances to be equal, or alternatively
     * {@link #getId(Object)} should be implemented to return an appropriate
     * identifier.
     *
     * @see #getId(Object)
     *
     * @param item
     *            the item to refresh
     */
    void refreshItem(T item);

    /**
     * Refreshes the given item and all of the children of the item as well.
     *
     * @see #refreshItem(Object)
     *
     * By default it just does a standard refreshItem, in a hierarchical DataProvider
     * it is supposed to refresh all of the children as well in case 'refreshChildren'
     * is true.
     *
     * @param item
     *            the item to refresh
     * @param refreshChildren
     *            whether or not to refresh child items
     */
    default void refreshItem(T item, boolean refreshChildren) {
        refreshItem(item);
    }

    /**
     * Refreshes all data based on currently available data in the underlying
     * provider.
     */
    void refreshAll();

    /**
     * Gets an identifier for the given item. This identifier is used by the
     * framework to determine equality between two items.
     * <p>
     * Default is to use item itself as its own identifier. If the item has
     * {@link Object#equals(Object)} and {@link Object#hashCode()} implemented
     * in a way that it can be compared to other items, no changes are required.
     * <p>
     * <strong>Note:</strong> This method will be called often by the Framework.
     * It should not do any expensive operations.
     *
     * @param item
     *            the item to get identifier for; not {@code null}
     * @return the identifier for given item; not {@code null}
     */
    default Object getId(T item) {
        Objects.requireNonNull(item, "Cannot provide an id for a null item.");
        return item;
    }

    /**
     * Adds a data provider listener. The listener is called when some piece of
     * data is updated.
     * <p>
     * The {@link #refreshAll()} method fires {@link DataChangeEvent} each time
     * when it's called. It allows to update UI components when user changes
     * something in the underlying data.
     *
     * @see #refreshAll()
     * @param listener
     *            the data change listener, not null
     * @return a registration for the listener
     */
    Registration addDataProviderListener(DataProviderListener<T> listener);

    /**
     * Wraps this data provider to create a data provider that uses a different
     * filter type. This can be used for adapting this data provider to a filter
     * type provided by a Component such as ComboBox.
     * <p>
     * For example receiving a String from ComboBox and making a Predicate based
     * on it:
     *
     * <pre>
     * DataProvider&lt;Person, Predicate&lt;Person&gt;&gt; dataProvider;
     * // ComboBox uses String as the filter type
     * DataProvider&lt;Person, String&gt; wrappedProvider = dataProvider
     *         .withConvertedFilter(filterText -&gt; {
     *             Predicate&lt;Person&gt; predicate = person -&gt; person.getName()
     *                     .startsWith(filterText);
     *             return predicate;
     *         });
     * comboBox.setDataProvider(wrappedProvider);
     * </pre>
     *
     * @param filterConverter
     *            callback that converts the filter in the query of the wrapped
     *            data provider into a filter supported by this data provider.
     *            Will only be called if the query contains a filter. Not
     *            <code>null</code>
     *
     * @param <C>
     *            the filter type that the wrapped data provider accepts;
     *            typically provided by a Component
     *
     * @return wrapped data provider, not <code>null</code>
     */
    default <C> DynamicDataProvider<T, C> withConvertedFilter(
            SerializableFunction<C, F> filterConverter) {
        Objects.requireNonNull(filterConverter,
                "Filter converter can't be null");
        return new DataProviderWrapper<T, C, F>(this) {
            @Override
            protected F getFilter(Query<T, C> query) {
                return FilterUtils.convertFilter(filterConverter, query);
            }
        };
    }

    /**
     * Wraps this data provider to create a data provider that supports
     * programmatically setting a filter that will be combined with a filter
     * provided through the query.
     *
     * @see #withConfigurableFilter()
     * @see ConfigurableFilterDataProvider#setFilter(Object)
     *
     * @param filterCombiner
     *            a callback for combining and the configured filter with the
     *            filter from the query to get a filter to pass to the wrapped
     *            provider. Either parameter might be <code>null</code>, but the
     *            callback will not be invoked at all if both would be
     *            <code>null</code>. Not <code>null</code>.
     *
     * @param <Q>
     *            the query filter type
     * @param <C>
     *            the configurable filter type
     *
     * @return a data provider with a configurable filter, not <code>null</code>
     */
    default <Q, C> ConfigurableFilterDataProvider<T, Q, C> withConfigurableFilter(
            SerializableBiFunction<Q, C, F> filterCombiner) {
        return new ConfigurableFilterDataProviderWrapper<T, Q, C, F>(this) {
            @Override
            protected F combineFilters(Q queryFilter, C configuredFilter) {
                return FilterUtils.combineFilters(filterCombiner, queryFilter,
                        configuredFilter);
            }
        };
    }

    /**
     * Wraps this data provider to create a data provider that supports
     * programmatically setting a filter but no filtering through the query.
     *
     * @see #withConfigurableFilter(SerializableBiFunction)
     * @see ConfigurableFilterDataProvider#setFilter(Object)
     *
     * @return a data provider with a configurable filter, not <code>null</code>
     */
    default ConfigurableFilterDataProvider<T, Void, F> withConfigurableFilter() {
        return withConfigurableFilter((queryFilter, configuredFilter) -> {
            assert queryFilter == null : "Filter from Void query must be null";

            return configuredFilter;
        });
    }

    // TODO: find some better place for those methods !
    static <T, F> DynamicCallbackDataProvider<T, F> fromFilteringCallback(
            FetchCallback<T, F> fetchCallback,
            SizeProvider<T, F> sizeProvider) {
        return new DynamicCallbackDataProvider<T, F>(fetchCallback, sizeProvider);
    }

    static <T, F> DynamicCallbackDataProvider<T, F> fromFilteringCallback(
            FetchCallback<T, F> fetchCallback) {
        return new DynamicCallbackDataProvider<T, F>(fetchCallback);
    }

    static <T> DynamicCallbackDataProvider<T, Void> fromCallback(
            FetchCallback<T, Void> fetchCallback) {
        return fromFilteringCallback(fetchCallback);
    }

    static <T> DynamicCallbackDataProvider<T, Void> fromCallback(
            FetchCallback<T, Void> fetchCallback,
            AbstractDynamicDataProvider.SizeProvider<T, Void> sizeProvider) {
        return fromFilteringCallback(fetchCallback, sizeProvider);
    }

    // TODO: separate class?
    static class DynamicQuery<T, F> extends Query<T, F> {
        private int previousEstimated;
        private int fetchedSize;
        public DynamicQuery(Query<T, F> query, int previousEstimated, int fetchedSize) {
            super(query.getOffset(), query.getLimit(), query.getSortOrders(),
                    query.getInMemorySorting(), query.getFilter().orElse(null));
            this.previousEstimated = previousEstimated;
            this.fetchedSize = fetchedSize;
        }
        public int getFetchedSize() {
            return fetchedSize;
        }
        public int getPreviousEstimated() { return previousEstimated; }
    }

    // TODO: separate class?
    interface SizeProvider<T, F> extends SerializableFunction<DynamicQuery<T, F>, Integer> {}

}
