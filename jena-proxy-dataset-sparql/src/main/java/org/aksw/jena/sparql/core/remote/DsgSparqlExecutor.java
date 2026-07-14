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

package org.aksw.jena.sparql.core.remote;

import java.util.function.Function;

import org.apache.jena.atlas.iterator.IteratorCloseable;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.exec.UpdateExec;
import org.apache.jena.update.UpdateRequest;

/**
 * Helper that knows how to turn a {@link Query}/{@link UpdateRequest} into a
 * {@link QueryExec}/{@link UpdateExec} and then transform the result as
 * needed by {@link DatasetGraphSparql}.
 *
 * The {@link PrefixMapping} parameters are intended to receive the DatasetGraph's prefix mapping.
 */
public interface DsgSparqlExecutor {

    /**
     * Default executor instance.
     */
    public static final DsgSparqlExecutor DEFAULT = new DsgSparqlExecutorImpl();

    /* ------------  Query execution  ------------ */

    /**
     * List all graph nodes in the dataset.
     *
     * @param executor Function to create query execution
     * @param prefixes Prefix mapping for the query
     * @return Iterator of graph node names
     */
    public IteratorCloseable<Node> listGraphNodes(
            Function<Query, ? extends QueryExec> executor,
            PrefixMapping prefixes);

    /**
     * Find quads matching the given pattern.
     *
     * @param executor Function to create query execution
     * @param prefixes Prefix mapping for the query
     * @param g Graph node (null or ANY for default graph)
     * @param s Subject node
     * @param p Predicate node
     * @param o Object node
     * @return Iterator of matching quads
     */
    public IteratorCloseable<Quad> find(
            Function<Query, ? extends QueryExec> executor,
            PrefixMapping prefixes,
            Node g, Node s, Node p, Node o);

    /**
     * Find quads in named graphs matching the given pattern.
     *
     * @param executor Function to create query execution
     * @param prefixes Prefix mapping for the query
     * @param g Graph node (must be a named graph, not null or ANY)
     * @param s Subject node
     * @param p Predicate node
     * @param o Object node
     * @return Iterator of matching quads
     */
    public IteratorCloseable<Quad> findNG(
            Function<Query, ? extends QueryExec> executor,
            PrefixMapping prefixes,
            Node g, Node s, Node p, Node o);

    /**
     * Check if a quad exists in the dataset.
     *
     * @param executor Function to create query execution
     * @param prefixes Prefix mapping for the query
     * @param g Graph node (null or ANY for default graph)
     * @param s Subject node
     * @param p Predicate node
     * @param o Object node
     * @return true if the quad exists
     */
    public boolean contains(
            Function<Query, ? extends QueryExec> executor,
            PrefixMapping prefixes,
            Node g, Node s, Node p, Node o);

    /**
     * Check if a graph exists in the dataset.
     *
     * @param executor Function to create query execution
     * @param prefixes Prefix mapping for the query
     * @param g Graph node to check
     * @return true if the graph exists
     */
    public boolean containsGraph(
            Function<Query, ? extends QueryExec> executor,
            PrefixMapping prefixes,
            Node g);

    /**
     * Get the total number of graphs in the dataset.
     *
     * @param executor Function to create query execution
     * @param prefixes Prefix mapping for the query
     * @return Number of graphs
     */
    public long fetchGraphCount(
            Function<Query, ? extends QueryExec> executor,
            PrefixMapping prefixes);

    /**
     * Get the size of the default graph.
     *
     * @param executor Function to create query execution
     * @param prefixes Prefix mapping for the query
     * @return Size of the default graph
     */
    public long fetchDefaultGraphSize(
            Function<Query, ? extends QueryExec> executor,
            PrefixMapping prefixes);

    /**
     * Get the size of a specific graph.
     *
     * @param executor Function to create query execution
     * @param prefixes Prefix mapping for the query
     * @param g Graph node
     * @return Size of the graph
     */
    public long fetchGraphSize(
            Function<Query, ? extends QueryExec> executor,
            PrefixMapping prefixes,
            Node g);

    /* ------------  Update execution  ------------ */

    /**
     * Add a quad to the dataset.
     *
     * @param executor Function to create update execution
     * @param prefixes Prefix mapping for the update
     * @param quad Quad to add
     */
    public void add(
            Function<UpdateRequest, ? extends UpdateExec> executor,
            PrefixMapping prefixes,
            Quad quad);

    /**
     * Delete a quad from the dataset.
     *
     * @param executor Function to create update execution
     * @param prefixes Prefix mapping for the update
     * @param quad Quad to delete
     */
    public void delete(
            Function<UpdateRequest, ? extends UpdateExec> executor,
            PrefixMapping prefixes,
            Quad quad);

    /**
     * Delete quads matching a pattern, potentially removing the entire graph if
     * all parameters are wildcards.
     *
     * @param executor Function to create update execution
     * @param prefixes Prefix mapping for the update
     * @param g Graph node
     * @param s Subject node
     * @param p Predicate node
     * @param o Object node
     */
    public void deleteAny(
            Function<UpdateRequest, ? extends UpdateExec> executor,
            PrefixMapping prefixes,
            Node g, Node s, Node p, Node o);

    /**
     * Remove an entire graph from the dataset.
     *
     * @param executor Function to create update execution
     * @param prefixes Prefix mapping for the update
     * @param g Graph node to remove
     */
    public void removeGraph(
            Function<UpdateRequest, ? extends UpdateExec> executor,
            PrefixMapping prefixes,
            Node g);

    /**
     * Copy data from one graph to another.
     *
     * @param executor Function to create update execution
     * @param prefixes Prefix mapping for the update
     * @param source Source graph node
     * @param destination Destination graph node
     */
    public void copy(
            Function<UpdateRequest, ? extends UpdateExec> executor,
            PrefixMapping prefixes,
            Node source, Node destination);
}
