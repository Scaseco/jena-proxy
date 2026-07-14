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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.atlas.iterator.IteratorCloseable;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Substitute;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.aggregate.AggCount;
import org.apache.jena.sparql.modify.request.QuadAcc;
import org.apache.jena.sparql.modify.request.QuadDataAcc;
import org.apache.jena.sparql.modify.request.Target;
import org.apache.jena.sparql.modify.request.UpdateClear;
import org.apache.jena.sparql.modify.request.UpdateDataDelete;
import org.apache.jena.sparql.modify.request.UpdateDeleteWhere;
import org.apache.jena.sparql.modify.request.UpdateDrop;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.sparql.syntax.ElementUnion;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateRequest;

/**
 * Static helper methods that build SPARQL queries and turn the results into the
 * iterator types required by {@link DatasetGraphSparql}.
 */
final class DsgSparqlExecUtils {
    /**
     * Variable for graph IRI.
     */
    static final Var vg = Var.alloc("g");

    /**
     * Variable for subject.
     */
    static final Var vs = Var.alloc("s");

    /**
     * Variable for predicate.
     */
    static final Var vp = Var.alloc("p");

    /**
     * Variable for object.
     */
    static final Var vo = Var.alloc("o");

    /**
     * Variable for count results.
     */
    static final Var vc = Var.alloc("c");

    /**
     * Query to list all graph IRIs.
     */
    static final Query graphsQuery           = QueryFactory.create("SELECT ?g { GRAPH ?g { } }");

    /**
     * Query to count all graphs.
     */
    static final Query graphsCountQuery      = QueryFactory.create("SELECT (COUNT(*) AS ?c) { GRAPH ?g { } }");

    /**
     * Query to count triples in the default graph.
     */
    static final Query defaultGraphSizeQuery = QueryFactory.create("SELECT (COUNT(*) AS ?c) { ?s ?p ?o }");

    /**
     * List all graph nodes in the dataset.
     *
     * @param executor Function to create query execution
     * @return Iterator of graph node names
     */
    static IteratorCloseable<Node> listGraphNodes(
            Function<Query, ? extends QueryExec> executor) {
        QueryExec qExec = executor.apply(graphsQuery);
        return Iter.onClose(
                Iter.map(qExec.select(), b -> b.get(vg)),
                qExec::close);
    }

    /**
     * Find triples matching the given pattern.
     *
     * @param executor Function to create query execution
     * @param s Subject node
     * @param p Predicate node
     * @param o Object node
     * @return Iterator of matching triples
     */
    static IteratorCloseable<Triple> findTriples(
            Function<Query, ? extends QueryExec> executor,
            Node s, Node p, Node o) {
        Triple pattern = matchTriple(s, p, o);
        Query query = createQueryTriple(pattern);
        QueryExec qExec = executor.apply(query);
        return Iter.onClose(
                Iter.map(qExec.select(), b -> Substitute.substitute(pattern, b)),
                qExec::close);
    }

    /**
     * Find quads in a specific graph matching the given pattern.
     *
     * @param executor Function to create query execution
     * @param g Graph node
     * @param s Subject node
     * @param p Predicate node
     * @param o Object node
     * @return Iterator of matching quads
     */
    static IteratorCloseable<Quad> findQuads(
            Function<Query, ? extends QueryExec> executor,
            Node g, Node s, Node p, Node o) {
        Quad pattern = matchQuad(g, s, p, o);
        Query query = createQueryQuad(pattern);
        QueryExec qExec = executor.apply(query);
        return Iter.onClose(
                Iter.map(qExec.select(), b -> Substitute.substitute(pattern, b)),
                qExec::close);
    }

    /**
     * Find triples or quads depending on dataset configuration.
     *
     * @param executor Function to create query execution
     * @param s Subject node
     * @param p Predicate node
     * @param o Object node
     * @return Iterator of matching quads
     */
    static IteratorCloseable<Quad> findTriplesOrQuads(
            Function<Query, ? extends QueryExec> executor,
            Node s, Node p, Node o) {
        Quad pattern = matchQuad(vg, s, p, o);
        Query query = createQueryTriplesAndQuads(vg, s, p, o);
        QueryExec qExec = executor.apply(query);
        return Iter.onClose(Iter.map(qExec.select(),
            b -> {
                if (!b.contains(vg)) {
                    b = BindingFactory.binding(b, vg, Quad.defaultGraphIRI);
                }
                return Substitute.substitute(pattern, b);
            }), qExec::close);
    }

    /**
     * Check if a quad exists in the dataset.
     *
     * @param executor Function to create query execution
     * @param g Graph node
     * @param s Subject node
     * @param p Predicate node
     * @param o Object node
     * @return true if the quad exists
     */
    static boolean contains(
            Function<Query, ? extends QueryExec> executor,
            Node g, Node s, Node p, Node o) {
        Query baseQuery = DsgSparqlExecUtils.createQueryTriplesAndQuads(g, s, p, o);
        baseQuery.setQueryAskType();
        QueryExec qExec = executor.apply(baseQuery);
        return qExec.ask();
    }

    /**
     * Check if a graph exists in the dataset.
     *
     * @param executor Function to create query execution
     * @param g Graph node
     * @return true if the graph exists
     */
    static boolean containsGraph(
            Function<Query, ? extends QueryExec> executor,
            Node g) {
        Query query = QueryFactory.create();
        query.setQueryAskType();
        Element element = new ElementNamedGraph(g, new ElementGroup());
        query.setQueryPattern(element);
        QueryExec qExec = executor.apply(query);
        return qExec.ask();
    }

    /**
     * Fetch a long value from query results.
     *
     * @param executor Function to create query execution
     * @param query The query to execute
     * @param numberVar Variable containing the number
     * @return The long value
     */
    static long fetchLong(
            Function<Query, ? extends QueryExec> executor,
            Query query,
            Var numberVar) {
        try (QueryExec qExec = executor.apply(query)) {
            Binding b = qExec.select().next();
            Number n = (Number) b.get(numberVar).getLiteralValue();
            return n.longValue();
        }
    }

    /**
     * Get the total number of graphs in the dataset.
     *
     * @param executor Function to create query execution
     * @return Number of graphs
     */
    public static long fetchGraphCount(Function<Query, ? extends QueryExec> executor) {
        long count =  fetchLong(executor, graphsCountQuery, vc);
        return count;
    }

    /**
     * Get the size of the default graph.
     *
     * @param executor Function to create query execution
     * @return Size of the default graph
     */
    public static long fetchDefaultGraphSize(Function<Query, ? extends QueryExec> executor) {
        long size = fetchLong(executor, defaultGraphSizeQuery, vc);
        return size;
    }

    /**
     * Get the size of a specific graph.
     *
     * @param executor Function to create query execution
     * @param g Graph node
     * @return Size of the graph
     */
    public static long fetchGraphSize(Function<Query, ? extends QueryExec> executor, Node g) {
        Query q = createQueryNamedGraphSize(g, vc);
        long size = fetchLong(executor, q, vc);
        return size;
    }

    /**
     * Match a triple pattern with variables for missing nodes.
     *
     * @param s Subject node
     * @param p Predicate node
     * @param o Object node
     * @return Triple with variables for missing nodes
     */
    private static Triple matchTriple(Node s, Node p, Node o) {
        return Triple.create(matchNode(s, vs), matchNode(p, vp), matchNode(o, vo));
    }

    /**
     * Match a quad pattern with variables for missing nodes.
     *
     * @param g Graph node
     * @param s Subject node
     * @param p Predicate node
     * @param o Object node
     * @return Quad with variables for missing nodes
     */
    static Quad matchQuad(Node g, Node s, Node p, Node o) {
        return Quad.create(matchNode(g, vg), matchNode(s, vs),
                           matchNode(p, vp), matchNode(o, vo));
    }

    /**
     * Match a single node, returning a variable if the supplied node is null or ANY.
     *
     * @param supplied Supplied node
     * @param defaultVar Default variable to use if supplied is null or ANY
     * @return The supplied node or the default variable
     */
    private static Node matchNode(Node supplied, Node defaultVar) {
        return supplied == null || supplied.equals(Node.ANY) ? defaultVar : supplied;
    }

    /**
     * Create a query for matching triples.
     *
     * @param m Triple pattern
     * @return Query for matching the triple pattern
     */
    private static Query createQueryTriple(Triple m) {
        BasicPattern bgp = new BasicPattern();
        bgp.add(m);
        Element element = new ElementTriplesBlock(bgp);

        Query q = QueryFactory.create();
        q.setQuerySelectType();
        q.setQueryResultStar(true);
        q.setQueryPattern(element);
        return q;
    }

    /**
     * Create a query for matching quads in a named graph.
     *
     * @param quad Quad pattern
     * @return Query for matching the quad pattern
     */
    private static Query createQueryQuad(Quad quad) {
        BasicPattern bgp = new BasicPattern();
        bgp.add(quad.asTriple());

        Element element = new ElementNamedGraph(quad.getGraph(),
                                                new ElementTriplesBlock(bgp));

        Query q = QueryFactory.create();
        q.setQuerySelectType();
        q.setQueryResultStar(true);
        q.setQueryPattern(element);
        return q;
    }

    /**
     * Create a SELECT query that matches across the default graph and the named graphs.
     *
     * If g is a variable then match triples and quads with a UNION graph pattern.
     * If g is concrete then match either triples or quads as appropriate.
     *
     * @param g Graph node
     * @param s Subject node
     * @param p Predicate node
     * @param o Object node
     * @return Query for matching triples and/or quads
     */
    private static Query createQueryTriplesAndQuads(Node g, Node s, Node p, Node o) {
        List<Element> elts = new ArrayList<>(2);
        Node matchG = matchNode(g, vg);
        if (matchG.isVariable() || Quad.isDefaultGraph(matchG)) {
            BasicPattern bgpDefault = new BasicPattern();
            bgpDefault.add(matchTriple(s, p, o));
            elts.add(new ElementTriplesBlock(bgpDefault));
        }

        if (matchG.isVariable() || !Quad.isDefaultGraph(matchG)) {
            Quad quad = matchQuad(matchG, s, p, o);
            BasicPattern bgpNamed = new BasicPattern();
            bgpNamed.add(quad.asTriple());
            elts.add(new ElementNamedGraph(matchG,
                    new ElementTriplesBlock(bgpNamed)));
        }

        Element finalElt;
        if (elts.size() == 1) {
            finalElt = elts.get(0);
        } else {
            ElementUnion union = new ElementUnion();
            elts.forEach(union::addElement);
            finalElt = union;
        }
        Query q = QueryFactory.create();
        q.setQuerySelectType();
        q.setQueryResultStar(true);
        q.setQueryPattern(finalElt);
        return q;
    }

    /**
     * Create a query to count triples in a named graph.
     *
     * @param graphName Graph node
     * @param outputVar Variable to hold the count
     * @return Query for counting graph size
     */
    static Query createQueryNamedGraphSize(Node graphName, Var outputVar) {
        BasicPattern bgp = new BasicPattern();
        bgp.add(Triple.create(vs, vp, vo));

        Element element = new ElementNamedGraph(graphName,
                new ElementTriplesBlock(bgp));

        Query q = QueryFactory.create();
        q.setQuerySelectType();
        q.setQueryPattern(element);
        Expr agg = q.allocAggregate(new AggCount());
        q.getProject().add(outputVar, agg);
        return q;
    }

    /**
     * Build an update request to delete quads by pattern.
     *
     * @param g Graph node
     * @param s Subject node
     * @param p Predicate node
     * @param o Object node
     * @return Update request with delete operations
     */
    static UpdateRequest buildDeleteByPattern(Node g, Node s, Node p, Node o) {
        UpdateRequest updateRequest = new UpdateRequest();
        if (isWildcard(g)) {
            updateRequest.add(buildDelete(Quad.defaultGraphIRI, s, p, o));
            updateRequest.add(buildDelete(g, s, p, o));
        } else {
            updateRequest.add(buildDelete(g, s, p, o));
        }
        return updateRequest;
    }

    /**
     * Check if a node is a wildcard (null, ANY, or a variable).
     *
     * @param g Node to check
     * @return true if the node is a wildcard
     */
    static boolean isWildcard(Node g) {
        return g == null || Node.ANY.equals(g) || g.isVariable();
    }

    /**
     * Build a delete update for a quad pattern.
     *
     * @param g Graph node
     * @param s Subject node
     * @param p Predicate node
     * @param o Object node
     * @return Update for deleting the quad pattern
     */
    private static Update buildDelete(Node g, Node s, Node p, Node o) {
        Quad quad = DsgSparqlExecUtils.matchQuad(g, s, p, o);
        Update update = quad.isConcrete()
            ? new UpdateDataDelete(new QuadDataAcc(List.of(quad)))
            : new UpdateDeleteWhere(new QuadAcc(List.of(quad)));
        return update;
    }

    /**
     * Build a graph removal update.
     *
     * @param g Graph node
     * @param useDrop true to use DROP, false to use CLEAR
     * @param silent true to use SILENT variant
     * @return Update for removing the graph
     */
    static Update buildGraphRemoval(Node g, boolean useDrop, boolean silent) {
        Target target = chooseTarget(g);
        Update update = useDrop
            ? new UpdateDrop(target, silent)
            : new UpdateClear(target, silent);
        return update;
    }

    /**
     * Choose a target for graph operations.
     *
     * @param g Graph node
     * @return Target for the operation
     */
    static Target chooseTarget(Node g) {
        Target target = Quad.isDefaultGraph(g)
            ? Target.DEFAULT
            : Quad.isUnionGraph(g)
                ? Target.NAMED
                : (g == null || Node.ANY.equals(g))
                    ? Target.ALL
                    : Target.create(g);
        return target;
    }
}
