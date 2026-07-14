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

import java.util.Objects;
import java.util.Optional;

import org.aksw.jena.sparql.core.remote.DatasetGraphSparql;
import org.aksw.jena.sparql.core.remote.DsgSparqlExecutor;
import org.apache.jena.atlas.lib.Creator;
import org.apache.jena.query.Query;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.TxnType;
import org.apache.jena.rdflink.RDFLink;
import org.apache.jena.sparql.JenaTransactionException;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.exec.UpdateExec;
import org.apache.jena.update.UpdateRequest;

/**
 * DatasetGraph implementation that implements all methods
 * against an RDFLink.
 * All returned iterators are backed by a fresh RDFLink instance.
 * The iterators must be closed to free the resources.
 */
public abstract class DatasetGraphOverRDFLink
    extends DatasetGraphSparql
{
    private boolean supportsTransactions;
    private boolean supportsTransactionAbort;

    private final TransactionalOverRDFLink transactional;

    /**
     * Create a new dataset graph over RDF link.
     *
     * @param execution the executor for SPARQL operations
     * @param supportsTransactions whether transactions are supported
     * @param supportsTransactionAbort whether transaction abort is supported
     */
    protected DatasetGraphOverRDFLink(DsgSparqlExecutor execution, boolean supportsTransactions, boolean supportsTransactionAbort) {
        super(execution);
        this.supportsTransactions = supportsTransactions;
        this.supportsTransactionAbort = supportsTransactionAbort;
        this.transactional = new TransactionalOverRDFLink(this::newLink);
    }

    @Override
    protected Transactional getTransactional() {
        return transactional;
    }

    @Override
    public boolean supportsTransactions() {
        return supportsTransactions;
    }

    @Override
    public boolean supportsTransactionAbort() {
        return supportsTransactionAbort;
    }

    /**
     * Create a new RDF link for operations.
     *
     * @return a new RDF link instance
     */
    public abstract RDFLink newLink();

    /**
     * Get the currently active RDF link for the current thread.
     *
     * @return the active RDF link wrapped in an Optional
     */
    protected Optional<RDFLink> activeLink() {
        return transactional.activeLink();
    }

    @Override
    protected QueryExec query(Query query) {
        QueryExec result;
        RDFLink activeLink = activeLink().orElse(null);
        if (activeLink != null) {
            result = activeLink.query(query);
        } else {
            RDFLink link = newLink();
            result = link.newQuery()
                .query(query)
                .build();

            result = new QueryExecWrapperCloseRDFLink(result, link);
        }
        return result;
    }

    @Override
    protected UpdateExec update(UpdateRequest update) {
        RDFLink activeLink = activeLink().orElse(null);
        if (activeLink != null) {
            return new UpdateExecOverRDFLink(() -> activeLink, false, null, null, false, update, null);
        }
        return new UpdateExecOverRDFLink(this::newLink, true, null, null, false, update, null);
    }

    /**
     * Builder for {@link DatasetGraphOverRDFLink} instances.
     */
    public static class Builder {
        private Creator<RDFLink> rdfLinkCreator;

        private DsgSparqlExecutor executor = DsgSparqlExecutor.DEFAULT;

        private boolean supportsTransactions;
        private boolean supportsTransactionAbort;

        private Builder() { }

        /**
         * Set the RDF link creator.
         *
         * @param rdfLinkCreator the creator function for RDF links
         * @return this builder
         */
        public Builder linkCreator(Creator<RDFLink> rdfLinkCreator) {
            this.rdfLinkCreator = Objects.requireNonNull(rdfLinkCreator);
            return this;
        }

        /**
         * Set the executor for SPARQL operations.
         *
         * @param executor the executor
         * @return this builder
         */
        public Builder executor(DsgSparqlExecutor executor) {
            this.executor = Objects.requireNonNull(executor);
            return this;
        }

        /**
         * Set whether transactions are supported.
         *
         * @param supportsTransactions true to enable transactions
         * @return this builder
         */
        public Builder supportsTransactions(boolean supportsTransactions) {
            this.supportsTransactions = supportsTransactions;
            return this;
        }

        /**
         * Set whether transaction abort is supported.
         *
         * @param supportsTransactionAbort true to enable transaction abort
         * @return this builder
         */
        public Builder supportsTransactionAbort(boolean supportsTransactionAbort) {
            this.supportsTransactionAbort = supportsTransactionAbort;
            return this;
        }

        /**
         * Build the dataset graph.
         *
         * @return a new dataset graph instance
         */
        public DatasetGraphOverRDFLink build() {
            Objects.requireNonNull(rdfLinkCreator);
            return new DatasetGraphOverRDFLink(executor, supportsTransactions, supportsTransactionAbort) {
                @Override
                public RDFLink newLink() {
                    return rdfLinkCreator.create();
                }
            };
        }
    }

    /**
     * Create a new builder instance.
     *
     * @return a new builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Create a new dataset graph with the given RDF link creator.
     *
     * @param rdfLinkCreator the creator function for RDF links
     * @return a new dataset graph instance
     */
    public static DatasetGraphOverRDFLink create(Creator<RDFLink> rdfLinkCreator) {
        return newBuilder().linkCreator(rdfLinkCreator).build();
    }
}

/**
 * Transactional implementation that creates a fresh thread-local
 * RDFLink whenever a transaction is started.
 */
class TransactionalOverRDFLink
    implements Transactional
{
    private Creator<RDFLink> rdfLinkCreator;
    private ThreadLocal<RDFLink> activeTxn = new ThreadLocal<>();

    /**
     * Create a new transactional handler.
     *
     * @param rdfLinkCreator the creator function for RDF links
     */
    public TransactionalOverRDFLink(Creator<RDFLink> rdfLinkCreator) {
        super();
        this.rdfLinkCreator = rdfLinkCreator;
    }

    /**
     * Get the currently active RDF link for the current thread.
     *
     * @return the active RDF link wrapped in an Optional
     */
    public Optional<RDFLink> activeLink() {
        return Optional.ofNullable(activeTxn.get());
    }

    /**
     * Get the currently active RDF link or throw an exception if not in a transaction.
     *
     * @return the active RDF link
     * @throws JenaTransactionException if not in a transaction
     */
    public RDFLink requireLink() {
        RDFLink result = activeTxn.get();
        if (result == null) {
            throw new JenaTransactionException("Not in a transaction");
        }
        return result;
    }

    @Override
    public void begin(TxnType type) {
        RDFLink tmp = activeTxn.get();
        if (tmp != null) {
            throw new JenaTransactionException("Transactions cannot be nested");
        }
        RDFLink result = rdfLinkCreator.create();
        activeTxn.set(result);
        result.begin(type);
    }

    @Override
    public boolean promote(Promote mode) {
        return requireLink().promote(mode);
    }

    @Override
    public void commit() {
        requireLink().commit();
    }

    @Override
    public void abort() {
        activeLink().ifPresent(RDFLink::abort);
    }

    @Override
    public void end() {
        try {
            activeLink().ifPresent(RDFLink::end);
        } finally {
            activeTxn.set(null);
        }
    }

    @Override
    public ReadWrite transactionMode() {
        return activeLink().map(RDFLink::transactionMode).orElse(null);
    }

    @Override
    public TxnType transactionType() {
        return activeLink().map(RDFLink::transactionType).orElse(null);
    }

    @Override
    public boolean isInTransaction() {
        RDFLink link = activeLink().orElse(null);
        if (link != null) {
             if (!link.isInTransaction()) {
                  return false;
             }
             return true;
        }
        return false;
    }
}
