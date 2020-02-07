[![Apache-2.0 license](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# LightStep OpenTelemetry Exporter

The LightStep OpenTelemetry Exporter is a trace exporter that sends span data to LightStep via OkHttp

```xml
<dependency>
    <groupId>com.lightstep.opentelemetry</groupId>
    <artifactId>lightstep-opentelemetry-exporter</artifactId>
    <version>VERSION</version>
</dependency>
```

## Usage
```java

// Instantiate the exporter
LightStepSpanExporter exporter =
        LightStepSpanExporter.newBuilder()
            .withAccessToken("{your_access_token}")
            .withCollectorHost("{lightstep_host}")
            .withCollectorPort("{lightstep_port}")
            .withCollectorProtocol("{lightstep_protocol}")
            .build();
```

## License

[Apache 2.0 License](./LICENSE).
