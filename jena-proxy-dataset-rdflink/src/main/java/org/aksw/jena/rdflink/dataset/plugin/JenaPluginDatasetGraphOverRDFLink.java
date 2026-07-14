package org.aksw.jena.rdflink.dataset.plugin;

import org.aksw.jena.rdflink.dataset.DatasetGraphOverRDFLink;
import org.aksw.jena.rdflink.dataset.QueryEngineFactoryDatasetGraphOverRDFLink;
import org.aksw.jena.rdflink.dataset.assembler.DatasetAssemblerHTTP;
import org.apache.jena.sparql.engine.QueryEngineRegistry;
import org.apache.jena.sparql.modify.UpdateEngineRegistry;
import org.apache.jena.sys.JenaSubsystemLifecycle;

/**
 * Plugin that registers a query and update engine for {@link DatasetGraphOverRDFLink}.
 */
public class JenaPluginDatasetGraphOverRDFLink
    implements JenaSubsystemLifecycle
{
    @Override
    public void start() {
        DatasetAssemblerHTTP.init();

        QueryEngineRegistry queryReg = QueryEngineRegistry.get();
        init(queryReg);

        UpdateEngineRegistry updateReg = UpdateEngineRegistry.get();
        init(updateReg);
    }

    @Override
    public void stop() {
    }

    public static void init(QueryEngineRegistry reg) {
        reg.add(new QueryEngineFactoryDatasetGraphOverRDFLink());
    }

    public static void init(UpdateEngineRegistry reg) {
         // reg.add(new UpdateEngineFactoryover());
    }

    @Override
    public int level() {
        return 1000000;
    }
}
