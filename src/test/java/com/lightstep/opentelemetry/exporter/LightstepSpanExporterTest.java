package com.lightstep.opentelemetry.exporter;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
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
import org.junit.Rule;
import org.junit.Test;

public class LightstepSpanExporterTest {
  private static final String TRACE_ID = "39431247078c75c1af46e0665b912ea9";
  private static final String SPAN_ID = "1213141516171819";

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(0);

  @Test
  public void testExport() throws Exception {
    final byte[] response = ReportResponse.newBuilder().build().toByteArray();

    WireMock.stubFor(
        WireMock.post(urlEqualTo(LightstepSpanExporter.PATH))
            .willReturn(aResponse().withStatus(200).withBody(response)));

    LightstepSpanExporter exporter =
        LightstepSpanExporter.newBuilder()
            .setAccessToken("token")
            .setCollectorHost("localhost")
            .setCollectorPort(wireMockRule.port())
            .setCollectorProtocol("http")
            .build();

    long duration = 900; // ms
    long startMs = System.currentTimeMillis();
    long endMs = startMs + duration;

    SpanData spanData =
        SpanData.newBuilder()
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
    assertEquals(4, reporter.getTagsCount());

    assertEquals(1, reportRequest.getSpansCount());
    final Span span = reportRequest.getSpans(0);
    assertEquals("GET /api/endpoint", span.getOperationName());
    assertEquals(900000L, span.getDurationMicros());
    assertEquals(2, span.getTagsCount());

    final SpanContext spanContext = span.getSpanContext();
    assertEquals(1302406798037686297L, spanContext.getSpanId());
    assertEquals(4126161779880129985L, spanContext.getTraceId());
  }
}
