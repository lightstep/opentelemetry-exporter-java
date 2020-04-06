package com.lightstep.opentelemetry.exporter.auto;

import com.lightstep.opentelemetry.exporter.LightstepSpanExporter;
import io.opentelemetry.sdk.contrib.auto.config.Config;
import io.opentelemetry.sdk.contrib.auto.config.SpanExporterFactory;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.net.MalformedURLException;

public class LightstepSpanExporterFactory implements SpanExporterFactory {

  @Override
  public SpanExporter fromConfig(Config config) {
    try {
      return LightstepSpanExporter.newBuilder()
          .setAccessToken(config.getString("lightstep.access.token", ""))
          .setCollectorHost(
              config.getString("lightstep.collector.host", LightstepSpanExporter.DEFAULT_HOST))
          .setCollectorPort(
              config.getInt("lightstep.collector.port", LightstepSpanExporter.DEFAULT_SECURE_PORT))
          .setCollectorProtocol(config
              .getString("lightstep.collector.protocol", LightstepSpanExporter.PROTOCOL_HTTPS))
          .setDeadlineMillis(config
              .getLong("lightstep.deadline.millis", LightstepSpanExporter.DEFAULT_DEADLINE_MILLIS))
          .setComponentName(config.getString("lightstep.component.name",
              LightstepSpanExporter.Builder.defaultComponentName()))
          .build();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
}
