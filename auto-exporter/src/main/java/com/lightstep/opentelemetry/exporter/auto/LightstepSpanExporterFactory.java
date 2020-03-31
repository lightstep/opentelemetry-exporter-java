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
      return LightstepSpanExporter.Builder.fromEnv().build();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
}
