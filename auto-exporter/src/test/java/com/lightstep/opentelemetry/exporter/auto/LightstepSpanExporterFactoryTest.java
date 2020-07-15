package com.lightstep.opentelemetry.exporter.auto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;

import com.lightstep.opentelemetry.exporter.LightstepConfig;
import io.opentelemetry.sdk.extensions.auto.config.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LightstepSpanExporterFactory.class})
public class LightstepSpanExporterFactoryTest {

  private Config config;

  @Before
  public void before() {
    config = Mockito.mock(Config.class);

    Mockito.when(config.getString(anyString(), anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocationOnMock) {
        return invocationOnMock.getArgument(1);
      }
    });

    Mockito.when(config.getInt(anyString(), anyInt())).thenAnswer(new Answer<Integer>() {
      @Override
      public Integer answer(InvocationOnMock invocationOnMock) {
        return invocationOnMock.getArgument(1);
      }
    });

    Mockito.when(config.getLong(anyString(), anyLong())).thenAnswer(new Answer<Long>() {
      @Override
      public Long answer(InvocationOnMock invocationOnMock) {
        return invocationOnMock.getArgument(1);
      }
    });
  }

  @After
  public void after() {
    System.clearProperty(LightstepConfig.ACCESS_TOKEN_PROPERTY_KEY);
    System.clearProperty(LightstepConfig.COLLECTOR_HOST_PROPERTY_KEY);
    System.clearProperty(LightstepConfig.COLLECTOR_PORT_PROPERTY_KEY);
    System.clearProperty(LightstepConfig.COLLECTOR_PROTOCOL_PROPERTY_KEY);
    System.clearProperty(LightstepConfig.DEADLINE_MILLIS_PROPERTY_KEY);
    System.clearProperty(LightstepConfig.SERVICE_NAME_PROPERTY_KEY);
    System.clearProperty(LightstepConfig.COMPONENT_NAME_PROPERTY_KEY);
    System.clearProperty(LightstepConfig.SERVICE_VERSION_PROPERTY_KEY);
    System.clearProperty(LightstepConfig.CONFIG_FILE_PROPERTY_KEY);
  }

  private void mockSystem() {
    PowerMockito.mockStatic(System.class);
    Mockito.when(System.getProperty(anyString(), anyString())).thenAnswer(
        new Answer<String>() {
          @Override
          public String answer(InvocationOnMock invocationOnMock) {
            return invocationOnMock.getArgument(1);
          }
        });
  }

  @Test
  public void testConvert() {
    assertEquals("test", LightstepSpanExporterFactory.convert(String.class, "test"));
    assertEquals(123, (int) LightstepSpanExporterFactory.convert(Integer.class, "123"));
    assertEquals(Long.MAX_VALUE,
        (long) LightstepSpanExporterFactory.convert(Long.class, String.valueOf(Long.MAX_VALUE)));
  }

  @Test
  public void testGetAccessToken_fromSystemProperty() {
    System.setProperty(LightstepConfig.ACCESS_TOKEN_PROPERTY_KEY, "token-from-system-property");
    final String accessToken = LightstepSpanExporterFactory.getAccessToken(config, null);
    assertEquals("token-from-system-property", accessToken);
  }

  @Test
  public void testGetAccessToken_fromConfig() {
    Mockito.when(config.getString(LightstepConfig.ACCESS_TOKEN_PROPERTY_KEY, ""))
        .thenReturn("token-from-config");
    final String accessToken = LightstepSpanExporterFactory.getAccessToken(config, null);
    assertEquals("token-from-config", accessToken);
  }

  @Test
  public void testGetAccessToken_fromEnvVariable() {
    mockSystem();
    Mockito.when(System.getenv(LightstepConfig.ACCESS_TOKEN)).thenReturn("token-from-env-variable");
    final String accessToken = LightstepSpanExporterFactory.getAccessToken(config, null);
    assertEquals("token-from-env-variable", accessToken);
  }

  @Test
  public void testGetCollectorPort_default() {
    mockSystem();
    final int port = LightstepSpanExporterFactory.getCollectorPort(config, null);
    assertEquals(LightstepConfig.DEFAULT_SECURE_PORT, port);
  }

  @Test
  public void testGetCollectorPort_fromEnvVariable() {
    mockSystem();
    Mockito.when(System.getenv(LightstepConfig.COLLECTOR_PORT)).thenReturn("123");
    final int port = LightstepSpanExporterFactory.getCollectorPort(config, null);
    assertEquals(123, port);
  }

  @Test
  public void testGetCollectorPort_fromSystemProperty() {
    System.setProperty(LightstepConfig.COLLECTOR_PORT_PROPERTY_KEY, "111");
    final int port = LightstepSpanExporterFactory.getCollectorPort(config, null);
    assertEquals(111, port);
  }

  @Test
  public void testGetCollectorPort_fromConfig() {
    Mockito.when(config
        .getInt(LightstepConfig.COLLECTOR_PORT_PROPERTY_KEY, LightstepConfig.DEFAULT_SECURE_PORT))
        .thenReturn(222);
    final int port = LightstepSpanExporterFactory.getCollectorPort(config, null);
    assertEquals(222, port);
  }

  @Test
  public void testGetCollectorProtocol_default() {
    mockSystem();
    final String protocol = LightstepSpanExporterFactory.getCollectorProtocol(config, null);
    assertEquals(LightstepConfig.PROTOCOL_HTTPS, protocol);
  }

  @Test
  public void testGetCollectorProtocol_fromEnvVariable() {
    mockSystem();
    Mockito.when(System.getenv(LightstepConfig.COLLECTOR_PROTOCOL))
        .thenReturn("protocol-from-env-variable");
    final String protocol = LightstepSpanExporterFactory.getCollectorProtocol(config, null);
    assertEquals("protocol-from-env-variable", protocol);
  }

  @Test
  public void testGetCollectorProtocol_fromSystemProperty() {
    System.setProperty(LightstepConfig.COLLECTOR_PROTOCOL_PROPERTY_KEY,
        "protocol-from-system-property");
    final String protocol = LightstepSpanExporterFactory.getCollectorProtocol(config, null);
    assertEquals("protocol-from-system-property", protocol);
  }

  @Test
  public void testGetCollectorProtocol_fromConfig() {
    Mockito.when(config
        .getString(LightstepConfig.COLLECTOR_PROTOCOL_PROPERTY_KEY, LightstepConfig.PROTOCOL_HTTPS))
        .thenReturn("protocol-from-config");
    final String protocol = LightstepSpanExporterFactory.getCollectorProtocol(config, null);
    assertEquals("protocol-from-config", protocol);
  }

  @Test
  public void testGetDeadlineMillis_default() {
    mockSystem();
    final long deadlineMillis = LightstepSpanExporterFactory.getDeadlineMillis(config, null);
    assertEquals(LightstepConfig.DEFAULT_DEADLINE_MILLIS, deadlineMillis);
  }

  @Test
  public void testGetDeadlineMillis_fromEnvVariable() {
    mockSystem();
    Mockito.when(System.getenv(LightstepConfig.DEADLINE_MILLIS)).thenReturn("123456789");
    final long deadlineMillis = LightstepSpanExporterFactory.getDeadlineMillis(config, null);
    assertEquals(123456789, deadlineMillis);
  }

  @Test
  public void testGetDeadlineMillis_fromSystemProperty() {
    System.setProperty(LightstepConfig.DEADLINE_MILLIS_PROPERTY_KEY, "1235");
    final long deadlineMillis = LightstepSpanExporterFactory.getDeadlineMillis(config, null);
    assertEquals(1235, deadlineMillis);
  }

  @Test
  public void testGetDeadlineMillis_fromConfig() {
    Mockito.when(config
        .getLong(LightstepConfig.DEADLINE_MILLIS_PROPERTY_KEY,
            LightstepConfig.DEFAULT_DEADLINE_MILLIS))
        .thenReturn(333L);
    final long deadlineMillis = LightstepSpanExporterFactory.getDeadlineMillis(config, null);
    assertEquals(333, deadlineMillis);
  }

  @Test
  public void testGetCollectorHost_default() {
    mockSystem();
    final String host = LightstepSpanExporterFactory.getCollectorHost(config, null);
    assertEquals(LightstepConfig.DEFAULT_HOST, host);
  }

  @Test
  public void testGetCollectorHost_fromEnvVariable() {
    mockSystem();
    Mockito.when(System.getenv(LightstepConfig.COLLECTOR_HOST))
        .thenReturn("host-from-env-variable");
    final String host = LightstepSpanExporterFactory.getCollectorHost(config, null);
    assertEquals("host-from-env-variable", host);
  }

  @Test
  public void testGetCollectorHost_fromSystemProperty() {
    System.setProperty(LightstepConfig.COLLECTOR_HOST_PROPERTY_KEY,
        "host-from-system-property");
    final String host = LightstepSpanExporterFactory.getCollectorHost(config, null);
    assertEquals("host-from-system-property", host);
  }

  @Test
  public void testGetCollectorHost_fromConfig() {
    Mockito.when(config
        .getString(LightstepConfig.COLLECTOR_HOST_PROPERTY_KEY, LightstepConfig.DEFAULT_HOST))
        .thenReturn("host-from-config");
    final String host = LightstepSpanExporterFactory.getCollectorHost(config, null);
    assertEquals("host-from-config", host);
  }

  @Test
  public void testGetServiceName_default() {
    mockSystem();
    final String serviceName = LightstepSpanExporterFactory.getServiceName(config, null);
    assertThat(serviceName).isNotBlank();
  }

  @Test
  public void testGetServiceName_fromEnvVariable() {
    mockSystem();
    Mockito.when(System.getenv(LightstepConfig.SERVICE_NAME))
        .thenReturn("service-from-env-variable");
    final String serviceName = LightstepSpanExporterFactory.getServiceName(config, null);
    assertEquals("service-from-env-variable", serviceName);
  }

  @Test
  public void testGetServiceName_fromEnvVariable_componentName() {
    mockSystem();
    Mockito.when(System.getenv(LightstepConfig.COMPONENT_NAME))
        .thenReturn("component-from-env-variable");
    final String serviceName = LightstepSpanExporterFactory.getServiceName(config, null);
    assertEquals("component-from-env-variable", serviceName);
  }

  @Test
  public void testGetServiceName_fromSystemProperty() {
    System.setProperty(LightstepConfig.SERVICE_NAME_PROPERTY_KEY,
        "service-from-system-property");
    final String serviceName = LightstepSpanExporterFactory.getServiceName(config, null);
    assertEquals("service-from-system-property", serviceName);
  }

  @Test
  public void testGetServiceName_fromSystemProperty_componentName() {
    System.setProperty(LightstepConfig.COMPONENT_NAME_PROPERTY_KEY,
        "component-from-system-property");
    final String serviceName = LightstepSpanExporterFactory.getServiceName(config, null);
    assertEquals("component-from-system-property", serviceName);
  }

  @Test
  public void testGetServiceName_fromConfig() {
    Mockito.when(config
        .getString(eq(LightstepConfig.SERVICE_NAME_PROPERTY_KEY), anyString()))
        .thenReturn("service-from-config");
    final String serviceName = LightstepSpanExporterFactory.getServiceName(config, null);
    assertEquals("service-from-config", serviceName);
  }

  @Test
  public void testGetServiceName_fromConfig_componentName() {
    Mockito.when(config
        .getString(eq(LightstepConfig.COMPONENT_NAME_PROPERTY_KEY), anyString()))
        .thenReturn("component-from-config");
    final String serviceName2 = LightstepSpanExporterFactory.getServiceName(config, null);
    assertEquals("component-from-config", serviceName2);
  }

  @Test
  public void testGetServiceVersion_default() {
    mockSystem();
    final String serviceVersion = LightstepSpanExporterFactory.getServiceVersion(config, null);
    assertThat(serviceVersion).isNull();
  }

  @Test
  public void testGetServiceVersion_fromEnvVariable() {
    mockSystem();
    Mockito.when(System.getenv(LightstepConfig.SERVICE_VERSION))
        .thenReturn("version-from-env-variable");
    final String serviceVersion = LightstepSpanExporterFactory.getServiceVersion(config, null);
    assertEquals("version-from-env-variable", serviceVersion);
  }

  @Test
  public void testGetServiceVersion_fromSystemProperty() {
    System.setProperty(LightstepConfig.SERVICE_VERSION_PROPERTY_KEY,
        "version-from-system-property");
    final String serviceVersion = LightstepSpanExporterFactory.getServiceVersion(config, null);
    assertEquals("version-from-system-property", serviceVersion);
  }

  @Test
  public void testGetServiceVersion_fromConfig() {
    Mockito.when(config
        .getString(eq(LightstepConfig.SERVICE_VERSION_PROPERTY_KEY), nullable(String.class)))
        .thenReturn("version-from-config");
    final String serviceVersion = LightstepSpanExporterFactory.getServiceVersion(config, null);
    assertEquals("version-from-config", serviceVersion);
  }

  @Test
  public void testConfigFile_default() {
    mockSystem();
    final String configFile = LightstepSpanExporterFactory.getConfigFile(config);
    assertThat(configFile).isNull();
  }

  @Test
  public void testConfigFile_fromEnvVariable() {
    mockSystem();
    Mockito.when(System.getenv(LightstepConfig.CONFIG_FILE))
        .thenReturn("file-from-env-variable");
    final String configFile = LightstepSpanExporterFactory.getConfigFile(config);
    assertEquals("file-from-env-variable", configFile);
  }

  @Test
  public void testConfigFile_fromSystemProperty() {
    System.setProperty(LightstepConfig.CONFIG_FILE,
        "file-from-system-property");
    final String configFile = LightstepSpanExporterFactory.getConfigFile(config);
    assertEquals("file-from-system-property", configFile);
  }

  @Test
  public void testConfigFile_fromConfig() {
    Mockito.when(config
        .getString(eq(LightstepConfig.CONFIG_FILE_PROPERTY_KEY), nullable(String.class)))
        .thenReturn("file-from-config");
    final String configFile = LightstepSpanExporterFactory.getConfigFile(config);
    assertEquals("file-from-config", configFile);
  }
}
