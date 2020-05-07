package com.lightstep.opentelemetry.exporter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Properties;
import org.junit.Test;

public class LightstepConfigTest {

  @Test
  public void loadConfig() {
    final Properties properties = LightstepConfig
        .loadConfig("src/test/resources/config.properties");
    assertNotNull(properties);
    assertEquals("XXXXXXXXXX", properties.get(LightstepConfig.ACCESS_TOKEN_PROPERTY_KEY));
    assertEquals("localhost",
        properties.get(LightstepConfig.COLLECTOR_HOST_PROPERTY_KEY));
    assertEquals("8360", properties.get(LightstepConfig.COLLECTOR_PORT_PROPERTY_KEY));
    assertEquals("http", properties.get(LightstepConfig.COLLECTOR_PROTOCOL_PROPERTY_KEY));
    assertEquals("test", properties.get(LightstepConfig.SERVICE_NAME_PROPERTY_KEY));
    assertEquals("1.0", properties.get(LightstepConfig.SERVICE_VERSION_PROPERTY_KEY));
    assertEquals("1234", properties.get(LightstepConfig.DEADLINE_MILLIS_PROPERTY_KEY));
  }

  @Test
  public void getHostName() {
    assertThat(LightstepConfig.getHostName()).isNotBlank();
  }

  @Test
  public void defaultServiceName() {
    assertThat(LightstepConfig.defaultServiceName()).isNotBlank();
  }
}