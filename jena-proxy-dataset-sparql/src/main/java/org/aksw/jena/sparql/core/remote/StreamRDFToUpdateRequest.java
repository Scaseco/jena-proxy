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

import java.util.Objects;
import java.util.function.Consumer;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWrapper;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.modify.request.QuadDataAcc;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.update.UpdateRequest;

/**
 * {@link StreamRDF} implementation that chunks RDF triples/quads and delivers
 * each chunk as an {@link UpdateRequest} to the configured consumer.
 */
/* package */ class StreamRDFToUpdateRequest implements StreamRDF {
    /**
     * Default buffer size.
     */
    public static final int DFT_BUFFER_SIZE = Integer.MAX_VALUE;

    private Consumer<UpdateRequest> sink;
    private int bufferSize;
    private PrefixMapping prefixes;
    private QuadDataAcc quadAcc = new QuadDataAcc();

    /**
     * Constructs the StreamRDFToUpdateRequest using default {@value #DFT_BUFFER_SIZE} quad buffer size.
     *
     * @param sink the consumer to send update requests to
     */
    public StreamRDFToUpdateRequest(Consumer<UpdateRequest> sink) {
        this(sink, null);
    }

    /**
     * Constructs the StreamRDFToUpdateRequest with a prefix mapping and default buffer size.
     *
     * @param sink the consumer to send update requests to
     * @param prefixes prefix mapping for the updates
     */
    public StreamRDFToUpdateRequest(Consumer<UpdateRequest> sink, PrefixMapping prefixes) {
        this(sink, prefixes, DFT_BUFFER_SIZE);
    }

    /**
     * Constructs the StreamRDFToUpdateRequest.
     *
     * @param sink the consumer to send update requests to
     * @param prefixes prefix mapping for the updates
     * @param bufferSize maximum number of quads to buffer before flushing
     */
    public StreamRDFToUpdateRequest(Consumer<UpdateRequest> sink, PrefixMapping prefixes, int bufferSize) {
        super();
        if (bufferSize < 1) {
            throw new IllegalArgumentException("Buffer size must be at least 1");
        }

        this.sink = Objects.requireNonNull(sink);
        this.prefixes = prefixes;
        this.bufferSize = bufferSize;
    }

    /**
     * Check if the buffer should be flushed.
     */
    private void isBufferFull() {
        if ( quadAcc.getQuads().size() >= bufferSize ) {
            flush();
        }
    }

    /**
     * Flush the buffer to the sink.
     */
    @Override
    public void flush() {
        if (!quadAcc.getQuads().isEmpty()) {
            UpdateRequest updateRequest = new UpdateRequest(new UpdateDataInsert(quadAcc));
            if (prefixes != null) {
                updateRequest.setPrefixMapping(prefixes);
            }
            try {
                sink.accept(updateRequest);
            } finally {
                quadAcc.close();
            }
            quadAcc = new QuadDataAcc();
        }
    }

    @Override
    public void start() {
    }

    @Override
    public void triple(Triple triple) {
        quadAcc.addTriple(triple);
        isBufferFull();
    }

    @Override
    public void quad(Quad quad) {
        quadAcc.addQuad(quad);
        isBufferFull();
    }

    @Override
    public void base(String base) {
    }

    @Override
    public void version(String version) {
    }

    @Override
    public void prefix(String prefix, String iri) {
        if (prefixes != null) {
            prefixes.setNsPrefix(prefix, iri);
        }
    }

    @Override
    public void finish() {
        flush();
        quadAcc.close();
    }

    /**
     * StreamRDF wrapper that converts triples to quads in a specified graph.
     */
    static class StreamRDFTriplesToQuads
        extends StreamRDFWrapper {

        protected final Node graphName;

        /**
         * Create a new wrapper.
         *
         * @param other the underlying stream
         * @param graphName the graph name to use for converted quads
         */
        public StreamRDFTriplesToQuads(StreamRDF other, Node graphName) {
            super(other);
            this.graphName = Objects.requireNonNull(graphName);
        }

        @Override
        public void triple(Triple triple) {
            Quad quad = Quad.create(graphName, triple);
            get().quad(quad);
        }
    }

    /**
     * Send triples of a source graph as quads in a target graph to a stream.
     *
     * @param sourceGraph the source graph
     * @param targetGraphName the target graph node (null for default graph)
     * @param sink the target stream
     */
    static void sendGraphTriplesToStream(Graph sourceGraph, Node targetGraphName, StreamRDF sink) {
        boolean isSinkDefaultGraph = targetGraphName == null || Quad.isDefaultGraph(targetGraphName);
        StreamRDF effectiveSink = isSinkDefaultGraph ? sink : new StreamRDFTriplesToQuads(sink, targetGraphName);
        StreamRDFOpsExtra.sendGraphTriplesToStream(sourceGraph, effectiveSink);
    }
}
