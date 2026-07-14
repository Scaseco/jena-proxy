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

import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.iterator.QueryIter;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.exec.RowSet;

/**
 * Query iterator over {@link QueryExec}.
 */
public class QueryIterOverQueryExec
    extends QueryIter
{
    /** The query execution. */
    protected QueryExec queryExec;
    
    /** The row set. */
    protected RowSet rowSet;

    /**
     * Create a new query iterator.
     *
     * @param execCxt the execution context
     * @param queryExec the query execution to iterate over
     */
    public QueryIterOverQueryExec(ExecutionContext execCxt, QueryExec queryExec) {
        super(execCxt);
        this.queryExec = queryExec;
    }

    @Override
    protected boolean hasNextBinding() {
        if (rowSet == null) {
            rowSet = queryExec.select();
        }

        return rowSet.hasNext();
    }

    @Override
    protected Binding moveToNextBinding() {
        return rowSet.next();
    }

    @Override
    protected void closeIterator() {
        queryExec.close();
    }

    @Override
    protected void requestCancel() {
        queryExec.abort();
    }
}
