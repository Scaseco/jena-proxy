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

package org.aksw.jena.rdflink.dataset.assembler;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.system.Vocab;

/**
 * Vocabulary definitions for the HTTP dataset assembler.
 *
 * @since 0.7.0
 */
public class VocabAssemblerHTTP
{
    private static final String NS = "https://w3id.org/aksw/jena/dataset#";

    /**
     * Utility classes should not be instantiated.
     */
    private VocabAssemblerHTTP() {
    }

    /**
     * Get the namespace URI.
     *
     * @return the namespace URI
     */
    public static String getURI() { return NS; }

    // Types

    // Preferred
    /** Dataset HTTP resource type. */
    public static final Resource tDatasetHTTP        = Vocab.type(NS, "DatasetHTTP");

    // Property to specify the auth type
    /** Property for authentication type. */
    public static final Property pAuth               = Vocab.property(NS, "auth");

    // public static final Resource tAuthBasic        = Vocab.type(NS, "AuthBasic");
    // public static final Resource tAuthBearer        = Vocab.type(NS, "AuthBaerer");

    // Basic auth
    /** Property for username in basic authentication. */
    public static final Property pUser               = Vocab.property(NS, "user");
    /** Property for password in basic authentication. */
    public static final Property pPass               = Vocab.property(NS, "pass");

    /** Property for token authentication. */
    public static final Property pToken               = Vocab.property(NS, "token");

    // Destination sets query, update and gsp endpoint to the same default value.
    // The specific properties can override.
    /** Property for destination endpoint (used as default for query, update and gsp endpoints). */
    public static final Property pDestination        = Vocab.property(NS, "destination");
    /** Property for SPARQL query endpoint. */
    public static final Property pQueryEndpoint      = Vocab.property(NS, "queryEndpoint");
    /** Property for SPARQL update endpoint. */
    public static final Property pUpdateEndpoint     = Vocab.property(NS, "updateEndpoint");
    /** Property for SPARQL graph store protocol endpoint. */
    public static final Property pGspEndpoint        = Vocab.property(NS, "gspEndpoint");

    // Accept headers for different query types
    /** Property for Accept header on SELECT queries. */
    public static final Property pAcceptSelectQuery  = Vocab.property(NS, "acceptSelectQuery");
    /** Property for Accept header on ASK queries. */
    public static final Property pAcceptAskQuery     = Vocab.property(NS, "acceptAskQuery");
    /** Property for Accept header on CONSTRUCT/DESCRIBE (graphs). */
    public static final Property pAcceptGraph        = Vocab.property(NS, "acceptGraph");
    /** Property for Accept header on CONSTRUCT QUAD/datasets. */
    public static final Property pAcceptDataset      = Vocab.property(NS, "acceptDataset");
    /** Property for Accept header fallback for all queries. */
    public static final Property pAcceptQuery        = Vocab.property(NS, "acceptQuery");

    // Output formats (language names as strings)
    /** Property for quads output format. */
    public static final Property pQuadsFormat        = Vocab.property(NS, "quadsFormat");
    /** Property for triples output format. */
    public static final Property pTriplesFormat      = Vocab.property(NS, "triplesFormat");

    // Send modes
    /** Property for query send mode. */
    public static final Property pQuerySendMode      = Vocab.property(NS, "querySendMode");
    /** Property for update send mode. */
    public static final Property pUpdateSendMode     = Vocab.property(NS, "updateSendMode");

    // Parse checks
    /** Property for parse check on SPARQL queries/updates. */
    public static final Property pParseCheckSPARQL   = Vocab.property(NS, "parseCheckSPARQL");

    /** Property for the HTTP User Agent. */
    public static final Property pUserAgent          = Vocab.property(NS, "userAgent");
}
