# Lightstep OpenTelemetry Auto Exporter

## Download and run

Download the [latest release](https://github.com/lightstep/opentelemetry-exporter-java/releases).

The exporter is specified the `-ota.exporter.jar` flag.  
Configuration parameters are passed as Java system properties (-D flags) or as environment variables.  
Parameters also can be loaded from configuration file specified by system property `ota.exporter.lightstep.config.file` or 
environment variable `LIGHTSTEP_CONFIG_FILE`.

This is an example:

```sh
export LIGHTSTEP_ACCESS_TOKEN="my-access-token"
java -javaagent:path/to/opentelemetry-auto-<version>.jar \
     -Dota.exporter.jar=path/to/lightstep-opentelemetry-auto-exporter-<version>.jar \
     -Dota.exporter.lightstep.component.name=MyApp \
     -jar myapp.jar
```

## Configuration parameters
| System property                           | Environment variable         | Config file property         | Purpose                                            |
|-------------------------------------------|------------------------------|------------------------------|----------------------------------------------------|
| ota.exporter.jar                          | OTA_EXPORTER_JAR             |                              | Path to the exporter fat-jar that you want to use |
| ota.exporter.lightstep.config.file        | LIGHTSTEP_CONFIG_FILE        |                              | Path to property file to load configuration parameters. Properties defined in the config file override same-named properties defined in system properties and environment variables. |
| ota.exporter.lightstep.component.name     | LIGHTSTEP_COMPONENT_NAME     | lightstep.component.name     | Name of the component being traced, default is Java runtime command |
| ota.exporter.lightstep.collector.protocol | LIGHTSTEP_COLLECTOR_PROTOCOL | lightstep.collector.protocol | Protocol which will be used when sending data to the tracer, `http` or `https`, default is `https` |
| ota.exporter.lightstep.collector.host     | LIGHTSTEP_COLLECTOR_HOST     | lightstep.collector.host     | Host to which the tracer will send data, default is `collector-grpc.lightstep.com` |
| ota.exporter.lightstep.collector.port     | LIGHTSTEP_COLLECTOR_PORT     | lightstep.collector.port     | Port to which the tracer will send data, default is `443` |
| ota.exporter.lightstep.deadline.millis    | LIGHTSTEP_DEADLINE_MILLIS    | lightstep.deadline.millis    | Maximum amount of time the tracer should wait for a response from the collector when sending a report, default is 30000 |
| ota.exporter.lightstep.access.token       | LIGHTSTEP_ACCESS_TOKEN       | lightstep.access.token       | Token for Lightstep access |
