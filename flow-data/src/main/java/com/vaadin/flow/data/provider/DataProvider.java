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

import com.vaadin.flow.data.binder.HasDataProvider;
import com.vaadin.flow.data.binder.HasFilterableDataProvider;
import com.vaadin.flow.data.provider.CallbackDataProvider.CountCallback;
import com.vaadin.flow.data.provider.CallbackDataProvider.FetchCallback;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A common interface for fetching data from a backend. The {@link DataProvider}
 * interface is used by listing components implementing {@link HasDataProvider}
 * or {@link HasFilterableDataProvider}. The listing component will provide a
 * {@link Query} object with request information, and the data provider uses
 * this information to return a stream containing requested beans.
 * <p>
 * Vaadin comes with a ready-made solution for in-memory data, known as
 * {@link ListDataProvider} which can be created using static {@code create}
 * methods in this interface. For custom backends such as SQL, EntityManager,
 * REST APIs or SpringData, use a {@link BackEndDataProvider} or its subclass.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <T>
 *            data type
 * @param <F>
 *            filter type
 *
 * @see #ofCollection(Collection)
 * @see #ofItems(Object...)
 * @see #fromStream(Stream)
 * @see #fromCallbacks(CallbackDataProvider.FetchCallback,
 *      CallbackDataProvider.CountCallback)
 * @see #fromFilteringCallbacks(CallbackDataProvider.FetchCallback,
 *      CallbackDataProvider.CountCallback)
 * @see ListDataProvider
 * @see BackEndDataProvider
 */
public interface DataProvider<T, F> extends DynamicDataProvider<T, F> {

    /**
     * Creates a new data provider backed by a collection.
     * <p>
     * The collection is used as-is. Changes in the collection will be visible
     * via the created data provider. The caller should copy the collection if
     * necessary.
     *
     * @param <T>
     *            the data item type
     * @param items
     *            the collection of data, not <code>null</code>
     * @return a new list data provider
     */
    static <T> ListDataProvider<T> ofCollection(Collection<T> items) {
        return new ListDataProvider<>(items);
    }

    /**
     * Creates a new data provider from the given items.
     * <p>
     * The items are copied into a new backing list, so structural changes to
     * the provided array will not be visible via the created data provider.
     *
     * @param <T>
     *            the data item type
     * @param items
     *            the data items
     * @return a new list data provider
     */
    @SafeVarargs
    static <T> ListDataProvider<T> ofItems(T... items) {
        return new ListDataProvider<>(Arrays.asList(items));
    }

    /**
     * Creates a new data provider from the given stream. <b>All items in the
     * stream are eagerly collected to a list.</b>
     * <p>
     * This is a shorthand for using {@link #ofCollection(Collection)} after
     * collecting the items in the stream to a list with e.g.
     * {@code stream.collect(Collectors.toList));}.
     * <p>
     * <strong>Using big streams is not recommended, you should instead use a
     * lazy data provider.</strong> See
     * {@link #fromCallbacks(CallbackDataProvider.FetchCallback, CallbackDataProvider.CountCallback)}
     * or {@link BackEndDataProvider} for more info.
     *
     * @param <T>
     *            the data item type
     * @param items
     *            a stream of data items, not {@code null}
     * @return a new list data provider
     */
    static <T> ListDataProvider<T> fromStream(Stream<T> items) {
        return new ListDataProvider<>(items.collect(Collectors.toList()));
    }

    /**
     * Creates a new data provider that uses filtering callbacks for fetching
     * and counting items from any backing store.
     * <p>
     * The query that is passed to each callback may contain a filter value that
     * is provided by the component querying for data.
     *
     * @param fetchCallback
     *            function that returns a stream of items from the back end for
     *            a query
     * @param countCallback
     *            function that returns the number of items in the back end for
     *            a query
     * @param <T>
     *            data provider data type
     * @param <F>
     *            data provider filter type
     * @return a new callback data provider
     */
    static <T, F> CallbackDataProvider<T, F> fromFilteringCallbacks(
            FetchCallback<T, F> fetchCallback,
            CountCallback<T, F> countCallback) {
        return new CallbackDataProvider<>(fetchCallback, countCallback);
    }

    /**
     * Creates a new data provider that uses callbacks for fetching and counting
     * items from any backing store.
     * <p>
     * The query that is passed to each callback will not contain any filter
     * values.
     *
     * @param fetchCallback
     *            function that returns a stream of items from the back end for
     *            a query
     * @param countCallback
     *            function that returns the number of items in the back end for
     *            a query
     * @param <T>
     *            data provider data type
     * @return a new callback data provider
     */
    static <T> CallbackDataProvider<T, Void> fromCallbacks(
            FetchCallback<T, Void> fetchCallback,
            CountCallback<T, Void> countCallback) {
        return fromFilteringCallbacks(fetchCallback, countCallback);
    }
}
