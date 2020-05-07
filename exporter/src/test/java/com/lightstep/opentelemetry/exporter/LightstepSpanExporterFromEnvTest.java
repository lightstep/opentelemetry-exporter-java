package com.lightstep.opentelemetry.exporter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

import com.lightstep.opentelemetry.exporter.LightstepSpanExporter.Builder;
import java.net.URL;
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
@PrepareForTest({LightstepSpanExporter.class})
public class LightstepSpanExporterFromEnvTest {

  @Before
  public void before() {
    System.clearProperty(LightstepConfig.ACCESS_TOKEN);
    System.clearProperty(LightstepConfig.COLLECTOR_PROTOCOL);
    System.clearProperty(LightstepConfig.COLLECTOR_HOST);
    System.clearProperty(LightstepConfig.COLLECTOR_PORT);
    System.clearProperty(LightstepConfig.SERVICE_NAME);
    System.clearProperty(LightstepConfig.SERVICE_VERSION);
    System.clearProperty(LightstepConfig.DEADLINE_MILLIS);
  }

  @Test
  public void testFromSystemProperties() throws Exception {
    System.setProperty(LightstepConfig.ACCESS_TOKEN, "token-from-system-property");
    System.setProperty(LightstepConfig.COLLECTOR_PROTOCOL, "http");
    System.setProperty(LightstepConfig.COLLECTOR_HOST, "localhost");
    System.setProperty(LightstepConfig.COLLECTOR_PORT, "1234");
    System.setProperty(LightstepConfig.SERVICE_NAME, "name-from-system-property");
    System.setProperty(LightstepConfig.SERVICE_VERSION, "1.0");
    System.setProperty(LightstepConfig.DEADLINE_MILLIS, "4321");

    final Builder builder = Builder.fromEnv();
    assertThat(builder.getCollectorUrl())
        .isEqualTo(new URL("http://localhost:1234/api/v2/reports"));
    assertThat(builder.getAccessToken()).isEqualTo("token-from-system-property");
    assertThat(builder.getServiceName()).isEqualTo("name-from-system-property");
    assertThat(builder.getServiceVersion()).isEqualTo("1.0");
    assertThat(builder.getDeadlineMillis()).isEqualTo(4321);
  }

  @Test
  public void testFromEnv() throws Exception {
    PowerMockito.mockStatic(System.class);
    Mockito.when(System.getProperty(anyString(), anyString())).thenAnswer(
        new Answer<String>() {
          @Override
          public String answer(InvocationOnMock invocationOnMock) {
            return invocationOnMock.getArgument(1);
          }
        });

    Mockito.when(System.getenv(LightstepConfig.ACCESS_TOKEN)).thenReturn("token-from-env-var");
    Mockito.when(System.getenv(LightstepConfig.COLLECTOR_PROTOCOL)).thenReturn("http");
    Mockito.when(System.getenv(LightstepConfig.COLLECTOR_HOST)).thenReturn("localhost");
    Mockito.when(System.getenv(LightstepConfig.COLLECTOR_PORT)).thenReturn("1234");
    Mockito.when(System.getenv(LightstepConfig.SERVICE_NAME)).thenReturn("name-from-env-var");
    Mockito.when(System.getenv(LightstepConfig.SERVICE_VERSION)).thenReturn("1.0");
    Mockito.when(System.getenv(LightstepConfig.DEADLINE_MILLIS)).thenReturn("4321");

    final Builder builder = Builder.fromEnv();
    assertThat(builder.getCollectorUrl())
        .isEqualTo(new URL("http://localhost:1234/api/v2/reports"));
    assertThat(builder.getAccessToken()).isEqualTo("token-from-env-var");
    assertThat(builder.getServiceName()).isEqualTo("name-from-env-var");
    assertThat(builder.getServiceVersion()).isEqualTo("1.0");
    assertThat(builder.getDeadlineMillis()).isEqualTo(4321);
  }
}
