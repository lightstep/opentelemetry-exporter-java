[![Circle CI](https://circleci.com/gh/lightstep/opentelemetry-exporter-java.svg?style=shield)](https://circleci.com/gh/lightstep/opentelemetry-exporter-java) [![Apache-2.0 license](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# LightStep OpenTelemetry Exporter

The LightStep OpenTelemetry Exporter is a trace exporter that sends span data to LightStep via OkHttp

```xml
<dependency>
    <groupId>com.lightstep.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-java</artifactId>
    <version>VERSION</version>
</dependency>
```

## Usage
```java

// Instantiate the exporter
LightStepSpanExporter exporter =
        LightStepSpanExporter.newBuilder()
            .setAccessToken("{your_access_token}")
            .setCollectorHost("{lightstep_host}")
            .setCollectorPort("{lightstep_port}")
            .setCollectorProtocol("{lightstep_protocol}")
            .build();

// Add Span Processor with LightStep exporter
OpenTelemetrySdk.getTracerProvider()
       .addSpanProcessor(SimpleSpansProcessor.newBuilder(exporter).build());
```

## License

[Apache 2.0 License](./LICENSE).
