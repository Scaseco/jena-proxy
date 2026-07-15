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

#### Advanced Configuration Options

The assembler supports additional properties for fine-grained HTTP configuration:

```turtle
<#dataset> a dsg:DatasetHTTP ;
    dsg:destination "http://localhost:3030/dataset" ;

    # Accept headers for different query types
    dsg:acceptSelectQuery "application/sparql-results+json" ;
    dsg:acceptAskQuery "application/sparql-results+json" ;
    dsg:acceptGraph "text/turtle,application/n-triples" ;
    dsg:acceptDataset "application/trig,application/n-quads" ;
    dsg:acceptQuery "*/*" ;  # Fallback for unknown query types

    # Output formats
    dsg:quadsFormat "trig" ;
    dsg:triplesFormat "turtle" ;

    # Send modes (query and update)
    dsg:querySendMode "asPost" ;
    dsg:updateSendMode "asPostForm" ;

    # Parse checks
    dsg:parseCheckSPARQL true .
```

**Available properties:**

| Property | Description | Values |
|----------|-------------|--------|
| `dsg:acceptSelectQuery` | Accept header for SELECT queries | MIME type (e.g., `application/sparql-results+json`) |
| `dsg:acceptAskQuery` | Accept header for ASK queries | MIME type |
| `dsg:acceptGraph` | Accept header for CONSTRUCT/DESCRIBE (graphs) | MIME type |
| `dsg:acceptDataset` | Accept header for CONSTRUCT QUAD/datasets | MIME type |
| `dsg:acceptQuery` | Fallback Accept header for all queries | MIME type |
| `dsg:acceptQuery` | Fallback Accept header for all queries | MIME type |
| `dsg:quadsFormat` | Output format for quads | Language name (e.g., `trig`, `n-quads`) or MIME type |
| `dsg:triplesFormat` | Output format for triples | Language name (e.g., `turtle`, `n-triples`) or MIME type |
| `dsg:querySendMode` | How to send SPARQL queries | `asGetWithLimitForm`, `asGetWithLimitBody`, `asGetAlways`, `asPostForm`, `asPost` |
| `dsg:updateSendMode` | How to send SPARQL updates | `asPostForm`, `asPost` |
| `dsg:parseCheckSPARQL` | Validate SPARQL queries/updates | `true` or `false` |

#### HTTP User-Agent Configuration

**Technical limitation:** Due to Jena's HTTP architecture, the User-Agent header cannot be configured per-dataset via the assembler. The `HttpClient` in Java doesn't support default headers at the client level.

**Available options:**

1. **Global configuration** (applies to all HTTP clients in the JVM):
   ```java
   import org.apache.jena.http.HttpEnv;
   import java.net.http.HttpClient;
   
   HttpClient client = HttpClient.newBuilder()
       .header("User-Agent", "MyApp/1.0")
       .build();
   HttpEnv.setDftHttpClient(client);
   ```

2. **Per-dataset via assembler** (uses global registry):
   The assembler supports the `dsg:userAgent` property, which registers the User-Agent header globally for the configured endpoints:
   ```turtle
   <#dataset> a dsg:DatasetHTTP ;
       dsg:destination "http://localhost:3030/dataset" ;
       dsg:userAgent "MyApp/1.0" .
   ```
   **Warning:** This approach registers the header globally in a shared registry due to current technical limitations. If multiple datasets target the same URL with different User-Agents, they may interfere with each other. The assembler logs a warning the first time it encounters a `dsg:userAgent` property.

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
