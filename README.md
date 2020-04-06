[ ![Download](https://api.bintray.com/packages/lightstep/maven/lightstep-opentelemetry-exporter/images/download.svg) ](https://bintray.com/lightstep/maven/lightstep-opentelemetry-exporter) [![Circle CI](https://circleci.com/gh/lightstep/opentelemetry-exporter-java.svg?style=shield)](https://circleci.com/gh/lightstep/opentelemetry-exporter-java) [![Apache-2.0 license](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# Lightstep OpenTelemetry Exporter

The Lightstep OpenTelemetry Exporter is a trace exporter that sends span data to Lightstep via OkHttp

```xml
<dependency>
    <groupId>com.lightstep.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-java</artifactId>
    <version>VERSION</version>
</dependency>
```

## Usage

### Manual configuration

```java
// Create builder
Builder builder = LightstepSpanExporter.newBuilder()
                      .setAccessToken("{your_access_token}")
                      .setCollectorHost("{lightstep_host}")
                      .setCollectorPort("{lightstep_port}")
                      .setCollectorProtocol("{lightstep_protocol}")
                      .setComponentName(""{lightstep_component}");

// Instantiate the exporter
LightstepSpanExporter exporter = builder.build();

// Add Span Processor with Lightstep exporter
OpenTelemetrySdk.getTracerProvider()
       .addSpanProcessor(SimpleSpansProcessor.newBuilder(exporter).build());
```

### Configuration from system properties and environmental variables

Lightstep exporter can be configured by system properties and environmental variables:

```java
Builder builder = LightstepSpanExporter.Builder.fromEnv();
```

Supported system properties and environmental variables:

* `LIGHTSTEP_COLLECTOR_PROTOCOL` - protocol which will be used when sending data to the tracer, `http` or `https`, default is `https`
* `LIGHTSTEP_COLLECTOR_HOST` -  host to which the tracer will send data, default is `collector-grpc.lightstep.com`
* `LIGHTSTEP_COLLECTOR_PORT` -  port to which the tracer will send data, default is `443`
* `LIGHTSTEP_DEADLINE_MILLIS` - maximum amount of time the tracer should wait for a response from the collector when sending a report, default is 30000
* `LIGHTSTEP_COMPONENT_NAME` - name of the component being traced, default is Java runtime command
* `LIGHTSTEP_ACCESS_TOKEN` - token for Lightstep access

### Easy initialization

```java
// Installs exporter into tracer SDK default provider with batching span processor.
builder.install();
```

### Usage with OpenTelemetry Auto-Instrumentation

A dedicated [artifact](auto-exporter/) is provided to be used with the `OpenTelemetry Auto-Instrumentation` agent.
Observe configuration is specified through system properties and environment variables, as defined by
`LightstepSpanExporter.Builder.fromEnv()`.

## License

[Apache 2.0 License](./LICENSE).
