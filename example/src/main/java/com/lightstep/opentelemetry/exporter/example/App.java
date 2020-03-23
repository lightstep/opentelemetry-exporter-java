package com.lightstep.opentelemetry.exporter.example;

import com.lightstep.opentelemetry.exporter.LightStepSpanExporter;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.export.SimpleSpansProcessor;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Span.Kind;
import io.opentelemetry.trace.Tracer;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class App {
  public static void main(String[] args) throws Exception {
    final Properties properties = loadConfig(args);

    Tracer tracer =
        OpenTelemetry.getTracerProvider().get("LightStepExample");

    LightStepSpanExporter lightStepSpanExporter = LightStepSpanExporter.newBuilder()
        .setAccessToken(properties.getProperty("access_token"))
        .setCollectorHost(properties.getProperty("collector_host"))
        .setCollectorPort(Integer.parseInt(properties.getProperty("collector_port")))
        .setCollectorProtocol(properties.getProperty("collector_protocol"))
        .setComponentName(properties.getProperty("component_name"))
        .build();

    OpenTelemetrySdk.getTracerProvider()
        .addSpanProcessor(SimpleSpansProcessor.newBuilder(lightStepSpanExporter).build());

    Span span = tracer.spanBuilder("start example").setSpanKind(Kind.CLIENT).startSpan();
    span.setAttribute("Attribute 1", "Value 1");
    span.addEvent("Event 0");
    // execute my use case - here we simulate a wait
    doWork();
    span.addEvent("Event 1");
    span.end();

    // wait some seconds
    try {
      Thread.sleep(5000);
    } catch (InterruptedException ignore) {
    }
    OpenTelemetrySdk.getTracerProvider().shutdown();
    System.out.println("Bye");
  }

  private static void doWork() {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException ignore) {
    }
  }

  private static Properties loadConfig(String[] args)
      throws IOException {
    String file = "config.properties";
    if (args.length > 0) {
      file = args[0];
    }

    FileInputStream fs = new FileInputStream(file);
    Properties config = new Properties();
    config.load(fs);
    return config;
  }
}
