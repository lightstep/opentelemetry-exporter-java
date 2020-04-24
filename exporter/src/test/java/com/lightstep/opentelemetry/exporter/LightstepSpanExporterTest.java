package com.lightstep.opentelemetry.exporter;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.lightstep.tracer.grpc.KeyValue;
import com.lightstep.tracer.grpc.ReportRequest;
import com.lightstep.tracer.grpc.ReportResponse;
import com.lightstep.tracer.grpc.Reporter;
import com.lightstep.tracer.grpc.Span;
import com.lightstep.tracer.grpc.SpanContext;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.SpanData.Link;
import io.opentelemetry.sdk.trace.export.SpanExporter.ResultCode;
import io.opentelemetry.trace.Span.Kind;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.TraceId;
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
  private static final String TRACE_ID = "39431247078c75c1af46e0665b912ea9";
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

    assertEquals(1, reportRequest.getSpansCount());
    final Span span = reportRequest.getSpans(0);
    assertEquals("GET /api/endpoint", span.getOperationName());
    assertEquals(900000L, span.getDurationMicros());
    assertEquals(4, span.getTagsCount());

    // Verify that "service.version" tag is set to correct value
    assertTrue(tagEquals(span.getTagsList(), LightstepSpanExporter.SERVICE_VERSION_KEY, "1.0"));

    final SpanContext spanContext = span.getSpanContext();
    assertEquals(1302406798037686297L, spanContext.getSpanId());
    assertEquals(4126161779880129985L, spanContext.getTraceId());
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

    assertEquals(1, reportRequest.getSpansCount());
    final Span span = reportRequest.getSpans(0);
    if (serviceVersion != null && !serviceVersion.isEmpty()) {
      assertEquals(4, span.getTagsCount());
      // Verify that "service.version" tag is set to correct value
      assertTrue(
          tagEquals(span.getTagsList(), LightstepSpanExporter.SERVICE_VERSION_KEY, serviceVersion));
    } else {
      assertEquals(3, span.getTagsCount());
      // Verify that "service.version" tag is missing
      assertFalse(tagExist(span.getTagsList(), LightstepSpanExporter.SERVICE_VERSION_KEY));
    }

  }

  private SpanData spanData(long startMs, long endMs) {
    return SpanData.newBuilder()
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
}
