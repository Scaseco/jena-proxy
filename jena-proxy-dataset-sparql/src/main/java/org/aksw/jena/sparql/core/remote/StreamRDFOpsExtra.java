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

import org.apache.jena.graph.Graph ;
import org.apache.jena.graph.Triple ;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapFactory;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFOps;
import org.apache.jena.util.iterator.ExtendedIterator;

/**
 * Utilities for sending RDF data to a {@link StreamRDF}.
 * Unless otherwise stated, send* operations do not call stream.start()/stream.finish()
 * whereas other operations do.
 */
public class StreamRDFOpsExtra {

    private StreamRDFOpsExtra() {
    }

    /**
     * Send the triples of a graph to a StreamRDF.
     * This operation does not include start/finish nesting.
     *
     * @param graph The graph to send
     * @param stream The target stream
     */
    public static void sendGraphToStream(Graph graph, StreamRDF stream) {
        PrefixMap prefixMap = PrefixMapFactory.create(graph.getPrefixMapping()) ;
        sendGraphToStream(graph, stream, null, prefixMap) ;
    }

    /**
     * Send the triples of a graph to a StreamRDF with optional base URI and prefix mapping.
     *
     * @param graph The graph to send
     * @param stream The target stream
     * @param baseURI Base URI for the stream, or null
     * @param prefixMap Prefix mapping for the stream, or null
     */
    public static void sendGraphToStream(Graph graph, StreamRDF stream, String baseURI, PrefixMap prefixMap) {
        if ( baseURI != null )
            stream.base(baseURI);
        if ( prefixMap != null )
            StreamRDFOps.sendPrefixesToStream(prefixMap, stream) ;
        sendGraphTriplesToStream(graph, stream);
    }

    /**
     * Send only the triples of a graph to a StreamRDF.
     *
     * @param graph The graph to send
     * @param stream The target stream
     */
    public static void sendGraphTriplesToStream(Graph graph, StreamRDF stream) {
        ExtendedIterator<Triple> iter = graph.find(null, null, null) ;
        try {
            StreamRDFOps.sendTriplesToStream(iter, stream) ;
        } finally { iter.close(); }
    }
}
