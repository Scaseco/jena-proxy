# jena-proxy

Apache Jena Dataset Proxy implementations for remote SPARQL and RDFLink endpoints.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.aksw.jena.proxy/jena-proxy-parent.svg)](https://maven-badges.herokuapp.com/maven-central/org.aksw.jena.proxy)

## Overview

jena-proxy provides **Proxy pattern** implementations for Apache Jena `DatasetGraph` that transparently forward operations to remote endpoints:

- **jena-proxy-dataset-sparql**: DatasetGraph over SPARQL Query/Update protocol
- **jena-proxy-dataset-rdflink**: DatasetGraph over Apache Jena RDFLink

These modules allow you to work with remote datasets as if they were local, while all SPARQL operations are executed against remote endpoints.

## Features

- **Transparent remote access**: Use standard Jena `DatasetGraph` API
- **SPARQL protocol support**: Query and update via HTTP SPARQL endpoints
- **RDFLink integration**: Use Apache Jena's modern RDFLink interface
- **Configurable transactions**: Enable/disable transactional support
- **HTTP authentication**: Basic authentication support
- **Jena assembler support**: Configure via Turtle configuration files

## Modules

### jena-proxy-dataset-sparql

A `DatasetGraph` implementation that forwards all operations to a remote endpoint via the SPARQL Query/Update protocol.

**Key classes:**
- `DatasetGraphSparql` - Base implementation
- `DsgSparqlExecutor` - SPARQL execution strategy
- `DsgSparqlExecutorImpl` - Default executor

### jena-proxy-dataset-rdflink

A `DatasetGraph` implementation that uses Apache Jena's `RDFLink` interface to connect to remote endpoints.

**Key classes:**
- `DatasetGraphOverRDFLink` - Main implementation
- `DatasetAssemblerHTTP` - Jena assembler for HTTP configuration
- `JenaPluginDatasetGraphOverRDFLink` - Auto-registration plugin

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>org.aksw.jena.proxy</groupId>
    <artifactId>jena-proxy-dataset-rdflink</artifactId>
    <version>0.7.0</version>
</dependency>
```

### Programmatic API

```java
import org.aksw.jena.rdflink.dataset.DatasetGraphOverRDFLink;
import org.apache.jena.rdflink.RDFLink;
import org.apache.jena.rdflink.RDFLinkHTTP;
import org.apache.jena.sparql.core.DatasetGraph;

// Create an RDFLink to a remote endpoint
RDFLink link = RDFLinkHTTP.newBuilder()
    .queryEndpoint("http://localhost:3030/dataset/query")
    .updateEndpoint("http://localhost:3030/dataset/update")
    .gspEndpoint("http://localhost:3030/dataset/data")
    .build();

// Create a DatasetGraph proxy
DatasetGraph dsg = DatasetGraphOverRDFLink.create(() -> link);

// Use it like a local dataset
dsg.getDefaultGraph().contains(...);
```

### Jena Assembler Configuration

```turtle
@prefix dsg: <https://w3id.org/aksw/jena/dataset#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .

<#dataset> a dsg:DatasetHTTP ;
    dsg:destination "http://localhost:3030/dataset" ;
    dsg:auth [
        dsg:user "username" ;
        dsg:pass "password" ;
    ] .
```

## Build

```bash
# Clone the repository
git clone https://github.com/Scaseco/jena-proxy.git
cd jena-proxy-parent

# Build with Maven
mvn clean install

# Run tests
mvn test
```

## Requirements

- Java 21 or later
- Maven 3.8+
- Apache Jena 6.2.0+

## License

This project is licensed under the [Apache License 2.0](LICENSE).

## Project Information

- **GitHub**: https://github.com/Scaseco/jena-proxy
- **Issues**: https://github.com/Scaseco/jena-proxy/issues
- **Maven Central**: https://search.maven.org/search?q=g:org.aksw.jena.proxy
