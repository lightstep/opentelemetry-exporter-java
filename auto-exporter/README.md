# Lightstep OpenTelemetry Auto Exporter

Download the [latest release](https://github.com/lightstep/opentelemetry-exporter-java/releases).

The exporter is specified the `-ota.exporter.jar` flag. Configuration is provided
through environment variables and system properties, as defined by
`LightstepExporter.Builder.fromEnv()`. This is an example:

```sh
export LIGHTSTEP_COMPONENT_NAME="MyApp"
export LIGHTSTEP_ACCESS_TOKEN="my-access-token"
java -javaagent:path/to/opentelemetry-auto-<version>.jar \
     -Dota.exporter.jar=path/to/lightstep-opentelemetry-auto-exporter-<version>.jar \
     -jar myapp.jar
```
