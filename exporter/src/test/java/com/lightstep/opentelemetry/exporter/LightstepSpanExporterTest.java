package com.lightstep.opentelemetry.exporter;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.lightstep.opentelemetry.exporter.LightstepSpanExporter.Builder;
import com.lightstep.tracer.grpc.KeyValue;
import com.lightstep.tracer.grpc.ReportRequest;
import com.lightstep.tracer.grpc.ReportResponse;
import com.lightstep.tracer.grpc.Reporter;
import com.lightstep.tracer.grpc.Span;
import com.lightstep.tracer.grpc.SpanContext;
import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.SpanData.Link;
import io.opentelemetry.sdk.trace.data.test.TestSpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter.ResultCode;
import io.opentelemetry.trace.Span.Kind;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.TraceId;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class LightstepSpanExporterTest {
  private static final String TRACE_ID = "463ac35c9f6413ad48485a3953bb6124";
  private static final String SPAN_ID = "1213141516171819";

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(0);

  @Before
  public void beforeClass() {
    final byte[] response = ReportResponse.newBuilder().build().toByteArray();

    WireMock.stubFor(
        WireMock.post(urlEqualTo(LightstepSpanExporter.PATH))
            .willReturn(aResponse().withStatus(200).withBody(response)));
  }

  @Test
  public void testExport() throws Exception {
    LightstepSpanExporter exporter =
        LightstepSpanExporter.newBuilder()
            .setAccessToken("token")
            .setCollectorHost("localhost")
            .setCollectorPort(wireMockRule.port())
            .setCollectorProtocol("http")
            .setServiceVersion("1.0")
            .build();

    long duration = 900; // ms
    long startMs = System.currentTimeMillis();
    long endMs = startMs + duration;

    SpanData spanData = spanData(startMs, endMs);

    final ResultCode resultCode = exporter.export(Collections.singletonList(spanData));
    assertEquals(ResultCode.SUCCESS, resultCode);

    assertEquals(ResultCode.SUCCESS, exporter.flush());
    exporter.shutdown();

    final List<ServeEvent> events = WireMock.getAllServeEvents();
    assertEquals(1, events.size());
    final LoggedRequest loggedRequest = events.get(0).getRequest();
    assertEquals("token", loggedRequest.getHeader(LightstepSpanExporter.LIGHTSTEP_ACCESS_TOKEN));
    assertEquals(LightstepSpanExporter.MEDIA_TYPE_STRING, loggedRequest.getHeader("Content-Type"));

    final ReportRequest reportRequest = ReportRequest.parseFrom(loggedRequest.getBody());

    assertEquals("token", reportRequest.getAuth().getAccessToken());

    final Reporter reporter = reportRequest.getReporter();
    assertEquals(6, reporter.getTagsCount());

    // Verify that "service.version" tag is set to correct value
    assertTrue(tagEquals(reporter.getTagsList(), LightstepSpanExporter.SERVICE_VERSION_KEY, "1.0"));

    // Verify "lightstep.hostname" is set
    assertNotNull(
        getTagValue(reporter.getTagsList(), LightstepSpanExporter.LIGHTSTEP_HOSTNAME_KEY));

    assertEquals(1, reportRequest.getSpansCount());
    final Span span = reportRequest.getSpans(0);
    assertEquals("GET /api/endpoint", span.getOperationName());
    assertEquals(900000L, span.getDurationMicros());
    assertEquals(5, span.getTagsCount());

    // Verify that Resources are added as tags to span
    assertTrue(tagEquals(span.getTagsList(), "resource_key", "resource_value"));

    // Verify that "service.version" tag is set to correct value
    assertTrue(tagEquals(span.getTagsList(), LightstepSpanExporter.SERVICE_VERSION_KEY, "1.0"));

    // Verify "lightstep.hostname" is set
    assertNotNull(
        getTagValue(span.getTagsList(), LightstepSpanExporter.LIGHTSTEP_HOSTNAME_KEY));

    final SpanContext spanContext = span.getSpanContext();
    assertEquals(1302406798037686297L, spanContext.getSpanId());
    assertEquals(5208512171318403364L, spanContext.getTraceId()); // trimmed id.
  }

  public String[] getServiceVersions() {
    return new String[]{null, "", "123"};
  }

  @Test
  @Parameters(method = "getServiceVersions")
  public void testServiceVersion(String serviceVersion) throws Exception {
    LightstepSpanExporter exporter =
        LightstepSpanExporter.newBuilder()
            .setAccessToken("token")
            .setCollectorHost("localhost")
            .setCollectorPort(wireMockRule.port())
            .setCollectorProtocol("http")
            .setServiceVersion(serviceVersion)
            .build();

    verifyServiceVersion(exporter, serviceVersion);

  }

  @Test
  public void testServiceVersionNotSet() throws Exception {
    LightstepSpanExporter exporter =
        LightstepSpanExporter.newBuilder()
            .setAccessToken("token")
            .setCollectorHost("localhost")
            .setCollectorPort(wireMockRule.port())
            .setCollectorProtocol("http")
            .build();

    verifyServiceVersion(exporter, null);
  }

  @Test
  public void testFromConfigFile() throws Exception {
    final LightstepSpanExporter exporter = Builder
        .fromConfigFile("src/test/resources/config.properties").build();

    assertThat(exporter.getCollectorUrl())
        .isEqualTo(new URL("http://localhost:8360/api/v2/reports"));

    assertThat(exporter.getServiceName()).isEqualTo("test");
    assertThat(exporter.getServiceVersion()).isEqualTo("1.0");
    assertThat(exporter.getAuth().getAccessToken()).isEqualTo("XXXXXXXXXX");
    assertThat(exporter.getClient().connectTimeoutMillis()).isEqualTo(1234);
    assertThat(exporter.getLsSpanAttributes()).hasSize(2);

    final KeyValue hostnameKeyValue = exporter.getLsSpanAttributes().get(0);
    assertThat(hostnameKeyValue.getKey()).isEqualTo(LightstepSpanExporter.LIGHTSTEP_HOSTNAME_KEY);
    assertThat(hostnameKeyValue.getStringValue()).isNotBlank();

    final KeyValue serviceVersionKeyValue = exporter.getLsSpanAttributes().get(1);
    assertThat(serviceVersionKeyValue.getKey())
        .isEqualTo(LightstepSpanExporter.SERVICE_VERSION_KEY);
    assertThat(serviceVersionKeyValue.getStringValue()).isEqualTo("1.0");

    final Reporter reporter = exporter.getReporter();
    final List<KeyValue> tags = reporter.getTagsList();
    tagEquals(tags, LightstepSpanExporter.COMPONENT_NAME_KEY, "test");
    tagEquals(tags, LightstepSpanExporter.LIGHTSTEP_HOSTNAME_KEY,
        hostnameKeyValue.getStringValue());
    tagEquals(tags, LightstepSpanExporter.LIGHTSTEP_TRACER_PLATFORM_KEY, "jre");
    tagEquals(tags, LightstepSpanExporter.SERVICE_VERSION_KEY, "1.0");
    tagEquals(tags, LightstepSpanExporter.LIGHTSTEP_TRACER_PLATFORM_VERSION_KEY,
        System.getProperty("java.version"));
  }

  private void verifyServiceVersion(LightstepSpanExporter exporter, String serviceVersion)
      throws Exception {
    long duration = 900; // ms
    long startMs = System.currentTimeMillis();
    long endMs = startMs + duration;

    SpanData spanData = spanData(startMs, endMs);

    final ResultCode resultCode = exporter.export(Collections.singletonList(spanData));
    assertEquals(ResultCode.SUCCESS, resultCode);

    exporter.shutdown();

    final List<ServeEvent> events = WireMock.getAllServeEvents();
    assertEquals(1, events.size());
    final LoggedRequest loggedRequest = events.get(0).getRequest();
    assertEquals("token", loggedRequest.getHeader(LightstepSpanExporter.LIGHTSTEP_ACCESS_TOKEN));
    assertEquals(LightstepSpanExporter.MEDIA_TYPE_STRING, loggedRequest.getHeader("Content-Type"));

    final ReportRequest reportRequest = ReportRequest.parseFrom(loggedRequest.getBody());

    final Reporter reporter = reportRequest.getReporter();
    if (serviceVersion != null && !serviceVersion.isEmpty()) {
      assertEquals(6, reporter.getTagsCount());
      // Verify that "service.version" tag is set to correct value
      assertTrue(tagEquals(reporter.getTagsList(), LightstepSpanExporter.SERVICE_VERSION_KEY,
          serviceVersion));
    } else {
      assertEquals(5, reporter.getTagsCount());
      // Verify that "service.version" tag is missing
      assertFalse(tagExist(reporter.getTagsList(), LightstepSpanExporter.SERVICE_VERSION_KEY));
    }

    // Verify "lightstep.hostname" is set
    assertNotNull(
        getTagValue(reporter.getTagsList(), LightstepSpanExporter.LIGHTSTEP_HOSTNAME_KEY));

    assertEquals(1, reportRequest.getSpansCount());
    final Span span = reportRequest.getSpans(0);
    if (serviceVersion != null && !serviceVersion.isEmpty()) {
      assertEquals(5, span.getTagsCount());
      // Verify that "service.version" tag is set to correct value
      assertTrue(
          tagEquals(span.getTagsList(), LightstepSpanExporter.SERVICE_VERSION_KEY, serviceVersion));
    } else {
      assertEquals(4, span.getTagsCount());
      // Verify that "service.version" tag is missing
      assertFalse(tagExist(span.getTagsList(), LightstepSpanExporter.SERVICE_VERSION_KEY));
    }

    // Verify "lightstep.hostname" is set
    assertNotNull(
        getTagValue(span.getTagsList(), LightstepSpanExporter.LIGHTSTEP_HOSTNAME_KEY));

  }

  private SpanData spanData(long startMs, long endMs) {
    return TestSpanData.newBuilder()
        .setHasEnded(true)
        .setTraceId(TraceId.fromLowerBase16(TRACE_ID, 0))
        .setSpanId(SpanId.fromLowerBase16(SPAN_ID, 0))
        .setName("GET /api/endpoint")
        .setStartEpochNanos(TimeUnit.MILLISECONDS.toNanos(startMs))
        .setEndEpochNanos(TimeUnit.MILLISECONDS.toNanos(endMs))
        .setStatus(Status.OK)
        .setKind(Kind.CONSUMER)
        .setLinks(Collections.<Link>emptyList())
        .setTotalRecordedLinks(0)
        .setTotalRecordedEvents(0)
        .setResource(Resource
            .create(Attributes.of("resource_key",
                AttributeValue.stringAttributeValue("resource_value"))))
        .build();
  }

  private boolean tagExist(List<KeyValue> tags, String key) {
    for (KeyValue tag : tags) {
      if (tag.getKey().equals(key)) {
        return true;
      }
    }
    return false;
  }

  private boolean tagEquals(List<KeyValue> tags, String key, String value) {
    for (KeyValue tag : tags) {
      if (tag.getKey().equals(key) && value.equals(tag.getStringValue())) {
        return true;
      }
    }
    return false;
  }

  private String getTagValue(List<KeyValue> tags, String key) {
    for (KeyValue tag : tags) {
      if (tag.getKey().equals(key)) {
        return tag.getStringValue();
      }
    }
    return null;
  }
}
