package org.aksw.jena.proxy;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.aksw.jena.exectracker.arq.system.TaskEventBroker;
import org.aksw.jena.exectracker.arq.system.TaskEventHistory;
import org.aksw.jena.rdflink.dataset.DatasetGraphOverRDFLink;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.main.sys.FusekiModules;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.rdflink.RDFLinkHTTP;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.util.Context;

public class TestDatasetGraphProxyWithExecTracker {
    @Test
    @Disabled
    public void test() {
        DatasetGraphOverRDFLink dsg = DatasetGraphOverRDFLink.newBuilder()
            .linkCreator(() -> RDFLinkHTTP.service("https://dbpedia.org/sparql").build())
            .supportsTransactions(false)
            .supportsTransactionAbort(false)
            .build();

        Context datasetCxt = dsg.getContext();
        TaskEventBroker taskTrackerRegistry = TaskEventBroker.getOrCreate(datasetCxt);
        TaskEventHistory historyTracker = TaskEventHistory.getOrCreate(datasetCxt);
        historyTracker.connect(taskTrackerRegistry);

        Table table = QueryExec
            .dataset(dsg)
            .query("PREFIX dbo: <http://dbpedia.org/ontology/> SELECT * { ?s a dbo:MusicalArtist } LIMIT 10")
            .table();

        int size = historyTracker.getHistory().size();
        System.out.println(size);
    }


    @Test
    @Disabled
    public void testEndpoint() {
        String assembler = """
            PREFIX fuseki:    <http://jena.apache.org/fuseki#>
            PREFIX rdf:       <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX rdfs:      <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX owl:       <http://www.w3.org/2002/07/owl#>
            PREFIX tdb1:      <http://jena.hpl.hp.com/2008/tdb#>
            PREFIX tdb2:      <http://jena.apache.org/2016/tdb#>
            PREFIX ja:        <http://jena.hpl.hp.com/2005/11/Assembler#>

            PREFIX jds:       <https://w3id.org/aksw/jena/dataset#>
            PREFIX jetf:      <https://w3id.org/aksw/jena/exectracker/fuseki#>

            <#service> rdf:type fuseki:Service ;
              fuseki:name "test" ;
              fuseki:endpoint [
                fuseki:operation fuseki:query ;
                ja:context [ ja:cxtName "arq:queryTimeout" ;  ja:cxtValue "600000" ] ;
              ] ;
              fuseki:endpoint [
                fuseki:name "update" ;
                fuseki:operation fuseki:update ;
              ] ;
              fuseki:endpoint [
                fuseki:name "tracker" ;
                fuseki:operation jetf:exectracker ;
                ja:context [ ] ;
              ] ;
              fuseki:endpoint [
                fuseki:name "admin-tracker" ;
                fuseki:operation jetf:exectracker ;
                ja:context [
                  ja:cxtName jetf:allowAbort ;
                  ja:cxtValue true ;
                ];
                fuseki:allowedUsers "test" ;
              ] ;
              fuseki:dataset <#proxyDS> ;
              .

            <#proxyDS> rdf:type jds:DatasetHTTP ;
              jds:destination <https://dbpedia.org/sparql> ;
              .
            """;

        Model config = ModelFactory.createDefaultModel();
        RDFParser.fromString(assembler, Lang.TTL).parse(config);

        // getSystemModules seems to include the custom auto-modules.
        FusekiModules modules = FusekiModules.getSystemModules();
//        FusekiModules modules = FusekiModules.add(
//            FusekiModules.getSystemModules(),
//            new FMod_ExecTracker()
//        );

        FusekiServer server = FusekiServer.create()
            .port(0)
            .fusekiModules(modules)
            .parseConfig(config)
            .build();

        server.start();

        long count;
        try {
            String serviceUrl = "http://localhost:" + server.getPort() + "/test";
            System.out.println("Server online at: " + serviceUrl);

            try (RDFConnection conn = RDFConnectionFuseki.connect(serviceUrl)) {
                try (QueryExecution qe =  conn.query("""
                    PREFIX dbo: <http://dbpedia.org/ontology/>
                    SELECT * { ?s a dbo:MusicalArtist } LIMIT 10
                """)) {
                    ResultSet rs = qe.execSelect();
                    count = ResultSetFormatter.consume(rs);
                }
            }

            try {
                Thread.sleep(1800000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } finally {
            server.stop();
        }
        assertTrue(count != 0);
    }}
