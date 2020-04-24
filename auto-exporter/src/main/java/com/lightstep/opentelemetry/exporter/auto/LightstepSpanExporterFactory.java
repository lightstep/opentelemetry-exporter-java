package com.lightstep.opentelemetry.exporter.auto;

import com.lightstep.opentelemetry.exporter.LightstepConfig;
import com.lightstep.opentelemetry.exporter.LightstepSpanExporter;
import io.opentelemetry.sdk.contrib.auto.config.Config;
import io.opentelemetry.sdk.contrib.auto.config.SpanExporterFactory;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.net.MalformedURLException;
import java.util.Properties;

public class LightstepSpanExporterFactory implements SpanExporterFactory {

  @Override
  public SpanExporter fromConfig(Config config) {
    final Properties properties = LightstepConfig
        .loadConfig(config.getString(LightstepConfig.CONFIG_FILE, null));

    try {
      return LightstepSpanExporter.newBuilder()
          .setAccessToken(getProperty(properties, LightstepConfig.ACCESS_TOKEN_PROPERTY_KEY,
              config.getString(LightstepConfig.ACCESS_TOKEN_PROPERTY_KEY, "")))
          .setCollectorHost(getProperty(properties,
              LightstepConfig.COLLECTOR_HOST_PROPERTY_KEY,
              config.getString(LightstepConfig.COLLECTOR_HOST_PROPERTY_KEY,
                  LightstepConfig.DEFAULT_HOST)))
          .setCollectorPort(getProperty(properties, LightstepConfig.COLLECTOR_PORT_PROPERTY_KEY,
              config.getInt(LightstepConfig.COLLECTOR_PORT_PROPERTY_KEY,
                  LightstepConfig.DEFAULT_SECURE_PORT)))
          .setCollectorProtocol(
              getProperty(properties, LightstepConfig.COLLECTOR_PROTOCOL_PROPERTY_KEY,
                  config.getString(LightstepConfig.COLLECTOR_PROTOCOL_PROPERTY_KEY,
                      LightstepConfig.PROTOCOL_HTTPS)))
          .setDeadlineMillis(getProperty(properties, LightstepConfig.DEADLINE_MILLIS_PROPERTY_KEY,
              config.getLong(LightstepConfig.DEADLINE_MILLIS_PROPERTY_KEY,
                  LightstepConfig.DEFAULT_DEADLINE_MILLIS)))
          .setServiceName(getProperty(properties, LightstepConfig.SERVICE_NAME_PROPERTY_KEY,
              config.getString(LightstepConfig.SERVICE_NAME_PROPERTY_KEY,
                  config.getString(LightstepConfig.COMPONENT_NAME_PROPERTY_KEY,
                      LightstepConfig.defaultServiceName()))))
          .setServiceVersion(getProperty(properties,
              LightstepConfig.SERVICE_VERSION_PROPERTY_KEY,
              config.getString(LightstepConfig.SERVICE_VERSION_PROPERTY_KEY, null)))
          .build();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  private String getProperty(final Properties properties, final String key,
      final String defaultValue) {
    if (properties == null) {
      return defaultValue;
    }
    return properties.getProperty(key, defaultValue);
  }

  private int getProperty(final Properties properties, final String key, final int defaultValue) {
    if (properties == null) {
      return defaultValue;
    }
    return Integer.parseInt(properties.getProperty(key, String.valueOf(defaultValue)));
  }

  private long getProperty(final Properties properties, final String key, final long defaultValue) {
    if (properties == null) {
      return defaultValue;
    }
    return Long.parseLong(properties.getProperty(key, String.valueOf(defaultValue)));
  }
}
