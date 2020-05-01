package com.lightstep.opentelemetry.exporter.auto;

import com.google.common.annotations.VisibleForTesting;
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
    final Properties properties = LightstepConfig.loadConfig(getConfigFile(config));

    try {
      return LightstepSpanExporter.newBuilder()
          .setAccessToken(getAccessToken(config, properties))
          .setCollectorHost(getCollectorHost(config, properties))
          .setCollectorPort(getCollectorPort(config, properties))
          .setCollectorProtocol(getCollectorProtocol(config, properties))
          .setDeadlineMillis(getDeadlineMillis(config, properties))
          .setServiceName(getServiceName(config, properties))
          .setServiceVersion(getServiceVersion(config, properties))
          .build();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  @VisibleForTesting
  static String getConfigFile(Config config) {
    return config.getString(LightstepConfig.CONFIG_FILE_PROPERTY_KEY,
        getPropertyFromSystem(LightstepConfig.CONFIG_FILE, String.class, null));
  }

  @VisibleForTesting
  static String getServiceVersion(Config config, Properties properties) {
    return getProperty(config, properties,
        LightstepConfig.SERVICE_VERSION_PROPERTY_KEY,
        LightstepConfig.SERVICE_VERSION,
        String.class,
        null);
  }

  @VisibleForTesting
  static String getServiceName(Config config, Properties properties) {
    return getProperty(config, properties, LightstepConfig.SERVICE_NAME_PROPERTY_KEY,
        LightstepConfig.SERVICE_NAME,
        String.class,
        getProperty(config, properties, LightstepConfig.COMPONENT_NAME_PROPERTY_KEY,
            LightstepConfig.COMPONENT_NAME,
            String.class,
            LightstepConfig.defaultServiceName()));
  }

  @VisibleForTesting
  static String getAccessToken(Config config, Properties properties) {
    return getProperty(config, properties, LightstepConfig.ACCESS_TOKEN_PROPERTY_KEY,
        LightstepConfig.ACCESS_TOKEN, String.class, "");
  }

  @VisibleForTesting
  static String getCollectorHost(Config config, Properties properties) {
    return getProperty(config, properties, LightstepConfig.COLLECTOR_HOST_PROPERTY_KEY,
        LightstepConfig.COLLECTOR_HOST,
        String.class,
        LightstepConfig.DEFAULT_HOST);
  }

  @VisibleForTesting
  static int getCollectorPort(Config config, Properties properties) {
    return getProperty(config, properties, LightstepConfig.COLLECTOR_PORT_PROPERTY_KEY,
        LightstepConfig.COLLECTOR_PORT,
        Integer.class,
        LightstepConfig.DEFAULT_SECURE_PORT);
  }

  @VisibleForTesting
  static String getCollectorProtocol(Config config, Properties properties) {
    return getProperty(config, properties, LightstepConfig.COLLECTOR_PROTOCOL_PROPERTY_KEY,
        LightstepConfig.COLLECTOR_PROTOCOL,
        String.class,
        LightstepConfig.PROTOCOL_HTTPS);
  }

  @VisibleForTesting
  static long getDeadlineMillis(Config config, Properties properties) {
    return getProperty(config, properties, LightstepConfig.DEADLINE_MILLIS_PROPERTY_KEY,
        LightstepConfig.DEADLINE_MILLIS,
        Long.class,
        LightstepConfig.DEFAULT_DEADLINE_MILLIS);
  }

  private static <T> T getProperty(final Properties properties, final String key, Class<T> type) {
    if (properties == null) {
      return null;
    }
    String val = properties.getProperty(key);
    return convert(type, val);
  }

  @VisibleForTesting
  static <T> T convert(Class<T> type, String val) {
    if (val == null) {
      return null;
    }
    final String typeSimpleName = type.getSimpleName();
    switch (typeSimpleName) {
      case "String":
        return type.cast(val);
      case "Integer":
        return type.cast(Integer.parseInt(val));
      case "Long":
        return type.cast(Long.parseLong(val));
      default:
        throw new RuntimeException("Unsupported type " + typeSimpleName);
    }
  }

  private static <T> T getProperty(Config config, Properties properties, String propKey,
      String envKey, Class<T> type, T defaultValue) {

    T fromFile = getProperty(properties, propKey, type);
    if (fromFile != null) {
      return fromFile;
    }

    T fromSystemOrDefault = getPropertyFromSystem(propKey, type,
        getPropertyFromSystem(envKey, type, defaultValue));

    final String typeSimpleName = type.getSimpleName();
    switch (typeSimpleName) {
      case "String":
        return type.cast(config.getString(propKey, (String) fromSystemOrDefault));
      case "Integer":
        return type.cast(config.getInt(propKey, (Integer) fromSystemOrDefault));
      case "Long":
        return type.cast(config.getLong(propKey, (Long) fromSystemOrDefault));
      default:
        throw new RuntimeException("Unsupported type " + typeSimpleName);
    }
  }

  private static <T> T getPropertyFromSystem(String name, Class<T> type, T defaultValue) {
    String val = System.getProperty(name, System.getenv(name));
    if (val == null || val.isEmpty()) {
      return defaultValue;
    }
    return convert(type, val);
  }
}
