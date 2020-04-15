package com.lightstep.opentelemetry.exporter;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LightstepConfig {
  private static final Logger logger = Logger.getLogger(LightstepConfig.class.getName());

  public static final String DEFAULT_HOST = "collector-grpc.lightstep.com";

  /**
   * Default collector port for HTTP.
   */
  static final int DEFAULT_PLAINTEXT_PORT = 80;

  /**
   * Default collector port for HTTPS.
   */
  public static final int DEFAULT_SECURE_PORT = 443;
  public static final String PROTOCOL_HTTPS = "https";
  static final String PROTOCOL_HTTP = "http";

  /**
   * Default duration the tracer should wait for a response from the collector when sending a
   * report.
   */
  public static final long DEFAULT_DEADLINE_MILLIS = 30000;

  /**
   * Use {@link #SERVICE_NAME} instead
   */
  @Deprecated
  public static final String COMPONENT_NAME = "LIGHTSTEP_COMPONENT_NAME";

  public static final String SERVICE_NAME = "LIGHTSTEP_SERVICE_NAME";
  public static final String DEADLINE_MILLIS = "LIGHTSTEP_DEADLINE_MILLIS";
  public static final String COLLECTOR_PROTOCOL = "LIGHTSTEP_COLLECTOR_PROTOCOL";
  public static final String COLLECTOR_HOST = "LIGHTSTEP_COLLECTOR_HOST";
  public static final String COLLECTOR_PORT = "LIGHTSTEP_COLLECTOR_PORT";
  public static final String ACCESS_TOKEN = "LIGHTSTEP_ACCESS_TOKEN";
  public static final String CONFIG_FILE = "LIGHTSTEP_CONFIG_FILE";

  /**
   * Use {@link #SERVICE_NAME_PROPERTY_KEY} instead
   */
  @Deprecated
  public static final String COMPONENT_NAME_PROPERTY_KEY = "lightstep.component.name";

  public static final String SERVICE_NAME_PROPERTY_KEY = "lightstep.service.name";
  public static final String DEADLINE_MILLIS_PROPERTY_KEY = "lightstep.deadline.millis";
  public static final String COLLECTOR_PROTOCOL_PROPERTY_KEY = "lightstep.collector.protocol";
  public static final String COLLECTOR_HOST_PROPERTY_KEY = "lightstep.collector.host";
  public static final String COLLECTOR_PORT_PROPERTY_KEY = "lightstep.collector.port";
  public static final String ACCESS_TOKEN_PROPERTY_KEY = "lightstep.access.token";

  /**
   * Java System property that will be used as the service name when no other value is provided.
   */
  private static final String SERVICE_NAME_SYSTEM_PROPERTY_KEY = "sun.java.command";


  public static String defaultServiceName() {
    String serviceNameSystemProperty = System.getProperty(SERVICE_NAME_SYSTEM_PROPERTY_KEY);
    if (serviceNameSystemProperty != null) {
      StringTokenizer st = new StringTokenizer(serviceNameSystemProperty);
      if (st.hasMoreTokens()) {
        return st.nextToken();
      }
    }
    return null;
  }

  public static Properties loadConfig(final String file) {
    if (file == null || file.isEmpty()) {
      return null;
    }
    try (FileInputStream fs = new FileInputStream(file);) {
      final Properties config = new Properties();
      config.load(fs);
      return config;
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Failed to load properties from file " + file, e);
      return null;
    }
  }
}
