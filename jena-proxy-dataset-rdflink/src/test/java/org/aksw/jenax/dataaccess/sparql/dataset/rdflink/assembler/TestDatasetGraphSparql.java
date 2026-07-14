package org.aksw.jenax.dataaccess.sparql.dataset.rdflink.assembler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.aksw.jena.sparql.core.remote.DatasetGraphSparql;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.exec.UpdateExec;
import org.apache.jena.update.UpdateRequest;

public class TestDatasetGraphSparql
    /* extends AbstractDatasetGraphTests // Could integrate with Jena's test suite in ARQ. */
{

    protected DatasetGraph newDataset() {
        DatasetGraph backend = DatasetGraphFactory.create();

        DatasetGraph frontend = new DatasetGraphSparql() {
            @Override
            protected UpdateExec update(UpdateRequest update) {
                return UpdateExec.dataset(backend).update(update).build();
            }

            @Override
            protected QueryExec query(Query query) {
                return QueryExec.dataset(backend).query(query).build();
            }
        };

        return frontend;
    }

    @Test
    public void deleteDefaultGraph() {
        DatasetGraph dsg = testDataset();
        dsg.deleteAny(Quad.defaultGraphIRI, Node.ANY, Node.ANY, Node.ANY);
        assertFalse(dsg.isEmpty());
        assertTrue(dsg.getDefaultGraph().isEmpty());
    }

    @Test
    public void deleteNamedGraph() {
        DatasetGraph dsg = testDataset();
        dsg.deleteAny(NodeFactory.createURI("http://www.example.org/g"), Node.ANY, Node.ANY, Node.ANY);
        assertEquals(0, dsg.size());
        assertFalse(dsg.isEmpty());
    }

    @Test
    public void deleteAllNamedGraphs() {
        DatasetGraph dsg = testDataset();
        dsg.deleteAny(Quad.unionGraph, Node.ANY, Node.ANY, Node.ANY);
        assertEquals(0, dsg.size());
        assertFalse(dsg.isEmpty());
    }

    @Test
    public void deleteAllGraphs() {
        DatasetGraph dsg = testDataset();
        dsg.deleteAny(Node.ANY, Node.ANY, Node.ANY, Node.ANY);
        assertTrue(dsg.isEmpty());
    }

    private DatasetGraph testDataset() {
        DatasetGraph dsg = newDataset();
        RDFParser.fromString("""
            PREFIX eg: <http://www.example.org/>
            eg:s1 eg:p eg:o .
            eg:g {
                eg:s2 eg:p eg:o
            }
            """, Lang.TRIG).parse(dsg);
        assertFalse(dsg.getDefaultGraph().isEmpty());
        assertEquals(1, dsg.size());
        return dsg;
    }
}
