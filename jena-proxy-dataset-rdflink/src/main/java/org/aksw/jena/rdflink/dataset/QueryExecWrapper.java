/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 */

package org.aksw.jena.rdflink.dataset;

import java.util.Iterator;

import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.exec.RowSet;
import org.apache.jena.sparql.util.Context;

/**
 * Wrapper interface for {@link QueryExec} that provides hooks for before/after execution
 * and exception handling.
 *
 * <p>This interface delegates all methods to a delegate {@link QueryExec} instance
 * and provides extension points for custom behavior.</p>
 */
public interface QueryExecWrapper
    extends QueryExec
{
    /**
     * Get the wrapped delegate.
     *
     * @return the delegate query execution
     */
    QueryExec getDelegate();

    @Override
    default Context getContext() {
        return getDelegate().getContext();
    }

    @Override
    default Query getQuery() {
        return getDelegate().getQuery();
    }

    @Override
    default String getQueryString() {
        return getDelegate().getQueryString();
    }

    @Override
    default void close() {
        getDelegate().close();
    }

    @Override
    default boolean isClosed() {
        return getDelegate().isClosed();
    }

    @Override
    default void abort() {
        getDelegate().abort();
    }

    /**
     * Hook called before query execution.
     */
    default void beforeExec() {
    }

    /**
     * Hook called after successful query execution.
     */
    default void afterExec() {
    }

    /**
     * Hook called when an exception occurs during query execution.
     *
     * @param e the exception that occurred
     */
    default void onException(Exception e) {
    }

    @Override
    default RowSet select() {
        beforeExec();
        try {
            return getDelegate().select();
        } catch(Exception e) {
            onException(e);
            throw e;
        } finally {
            afterExec();
        }
    }

    @Override
    default Graph construct() {
        beforeExec();
        try {
            return getDelegate().construct();
        } catch(Exception e) {
            onException(e);
            throw e;
        } finally {
            afterExec();
        }
    }

    @Override
    default Graph construct(Graph graph) {
        beforeExec();
        try {
            return getDelegate().construct(graph);
        } catch(Exception e) {
            onException(e);
            throw e;
        } finally {
            afterExec();
        }
    }

    @Override
    default Graph describe() {
        beforeExec();
        try {
            return getDelegate().describe();
        } catch(Exception e) {
            onException(e);
            throw e;
        } finally {
            afterExec();
        }
    }

    @Override
    default Graph describe(Graph graph) {
        beforeExec();
        try {
            return getDelegate().describe(graph);
        } catch(Exception e) {
            onException(e);
            throw e;
        } finally {
            afterExec();
        }
    }

    @Override
    default boolean ask() {
        beforeExec();
        try {
            return getDelegate().ask();
        } catch(Exception e) {
            onException(e);
            throw e;
        } finally {
            afterExec();
        }
    }

    @Override
    default Iterator<Triple> constructTriples() {
        beforeExec();
        try {
            return getDelegate().constructTriples();
        } catch(Exception e) {
            onException(e);
            throw e;
        } finally {
            afterExec();
        }
    }

    @Override
    default Iterator<Triple> describeTriples() {
        beforeExec();
        try {
            return getDelegate().describeTriples();
        } catch(Exception e) {
            onException(e);
            throw e;
        } finally {
            afterExec();
        }
    }

    @Override
    default Iterator<Quad> constructQuads() {
        beforeExec();
        try {
            return getDelegate().constructQuads();
        } catch(Exception e) {
            onException(e);
            throw e;
        } finally {
            afterExec();
        }
    }

    @Override
    default DatasetGraph constructDataset() {
        beforeExec();
        try {
            return getDelegate().constructDataset();
        } catch(Exception e) {
            onException(e);
            throw e;
        } finally {
            afterExec();
        }
    }

    @Override
    default DatasetGraph constructDataset(DatasetGraph dataset) {
        beforeExec();
        try {
            return getDelegate().constructDataset(dataset);
        } catch(Exception e) {
            onException(e);
            throw e;
        } finally {
            afterExec();
        }
    }

    @Override
    default JsonArray execJson() {
        beforeExec();
        try {
            return getDelegate().execJson();
        } catch(Exception e) {
            onException(e);
            throw e;
        } finally {
            afterExec();
        }
    }

    @Override
    default Iterator<JsonObject> execJsonItems() {
        beforeExec();
        try {
            return getDelegate().execJsonItems();
        } catch(Exception e) {
            onException(e);
            throw e;
        } finally {
            afterExec();
        }
    }


    @Override
    default DatasetGraph getDataset() {
        return getDelegate().getDataset();
    }
}
