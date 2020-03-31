package com.lightstep.opentelemetry.exporter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import com.lightstep.tracer.grpc.KeyValue;
import com.lightstep.tracer.grpc.Log;
import com.lightstep.tracer.grpc.Reference;
import com.lightstep.tracer.grpc.Reference.Relationship;
import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.SpanData.Link;
import io.opentelemetry.sdk.trace.data.SpanData.TimedEvent;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.TraceFlags;
import io.opentelemetry.trace.TraceId;
import io.opentelemetry.trace.TraceState;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.junit.Test;

public class AdapterTest {
  private static final String LINK_TRACE_ID =
      "39431247078c75c1af46e0665b912ea9"; // 4126161779880129985
  private static final String LINK_SPAN_ID = "0000000000fed456"; // 16700502
  private static final String TRACE_ID = "39431247078c75c1af46e0665b912ea9"; // 4126161779880129985
  private static final String SPAN_ID = "0000000000def456"; // 14611542
  private static final String PARENT_SPAN_ID = "0000000000aef789"; // 11466633

  private static SpanData getSpanData(long startMs, long endMs) {
    AttributeValue valueB = AttributeValue.booleanAttributeValue(true);
    Map<String, AttributeValue> attributes = ImmutableMap.of("valueB", valueB);

    Link link = SpanData.Link.create(createSpanContext(LINK_TRACE_ID, LINK_SPAN_ID), attributes);

    return SpanData.newBuilder()
        .setHasEnded(true)
        .setTraceId(TraceId.fromLowerBase16(TRACE_ID, 0))
        .setSpanId(SpanId.fromLowerBase16(SPAN_ID, 0))
        .setParentSpanId(SpanId.fromLowerBase16(PARENT_SPAN_ID, 0))
        .setName("GET /api/endpoint")
        .setStartEpochNanos(TimeUnit.MILLISECONDS.toNanos(startMs))
        .setEndEpochNanos(TimeUnit.MILLISECONDS.toNanos(endMs))
        .setAttributes(attributes)
        .setTimedEvents(Collections.singletonList(getTimedEvent()))
        .setTotalRecordedEvents(1)
        .setLinks(Collections.singletonList(link))
        .setTotalRecordedLinks(1)
        .setKind(Span.Kind.SERVER)
        .setResource(Resource.create(Collections.<String, AttributeValue>emptyMap()))
        .setStatus(Status.OK)
        .build();
  }

  private static SpanContext createSpanContext(String traceId, String spanId) {
    return SpanContext.create(
        TraceId.fromLowerBase16(traceId, 0),
        SpanId.fromLowerBase16(spanId, 0),
        TraceFlags.builder().build(),
        TraceState.builder().build());
  }

  private static TimedEvent getTimedEvent() {
    long epochNanos = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
    AttributeValue valueS = AttributeValue.stringAttributeValue("bar");
    ImmutableMap<String, AttributeValue> attributes = ImmutableMap.of("foo", valueS);
    return TimedEvent.create(epochNanos, "the log message", attributes);
  }

  @Nullable
  private static KeyValue getValue(List<KeyValue> tagsList, String s) {
    for (KeyValue kv : tagsList) {
      if (kv.getKey().equals(s)) {
        return kv;
      }
    }
    return null;
  }

  private static void assertHasFollowsFrom(com.lightstep.tracer.grpc.Span lightStepSpan) {
    boolean found = false;
    for (Reference spanRef : lightStepSpan.getReferencesList()) {
      if (Relationship.FOLLOWS_FROM.equals(spanRef.getRelationship())) {
        assertEquals(4126161779880129985L, spanRef.getSpanContext().getTraceId());
        assertEquals(16700502, spanRef.getSpanContext().getSpanId());
        found = true;
      }
    }
    assertTrue("Should have found the follows-from reference", found);
  }

  private static void assertHasParent(com.lightstep.tracer.grpc.Span lightStepSpan) {
    boolean found = false;
    for (Reference spanRef : lightStepSpan.getReferencesList()) {
      if (Reference.Relationship.CHILD_OF.equals(spanRef.getRelationship())) {
        assertEquals(4126161779880129985L, spanRef.getSpanContext().getTraceId());
        assertEquals(11466633, spanRef.getSpanContext().getSpanId());
        found = true;
      }
    }
    assertTrue("Should have found the parent reference", found);
  }

  @Test
  public void testProtoSpans() {
    long duration = 900; // ms
    long startMs = System.currentTimeMillis();
    long endMs = startMs + duration;

    SpanData span = getSpanData(startMs, endMs);
    List<SpanData> spans = Collections.singletonList(span);

    final List<com.lightstep.tracer.grpc.Span> lightStepSpans = Adapter.toLightstepSpans(spans);

    // the span contents are checked somewhere else
    assertEquals(1, lightStepSpans.size());
  }

  @Test
  public void testProtoSpan() {
    long duration = 900; // ms
    long startMs = System.currentTimeMillis();
    long endMs = startMs + duration;

    final SpanData span = getSpanData(startMs, endMs);
    final com.lightstep.tracer.grpc.Span lightstepSpan = Adapter.toLightstepSpan(span);

    assertEquals(4126161779880129985L, lightstepSpan.getSpanContext().getTraceId());
    assertEquals(14611542, lightstepSpan.getSpanContext().getSpanId());

    assertEquals("GET /api/endpoint", lightstepSpan.getOperationName());

    assertEquals(Timestamps.fromMillis(startMs), lightstepSpan.getStartTimestamp());
    assertEquals(
        Durations.toMicros(Durations.fromMillis(duration)), lightstepSpan.getDurationMicros());

    assertEquals(3, lightstepSpan.getTagsCount());

    KeyValue keyValue = getValue(lightstepSpan.getTagsList(), Adapter.KEY_SPAN_KIND);
    assertNotNull(keyValue);
    assertEquals("SERVER", keyValue.getStringValue());

    keyValue = getValue(lightstepSpan.getTagsList(), Adapter.KEY_SPAN_STATUS_CODE);
    assertNotNull(keyValue);
    assertEquals(0, keyValue.getIntValue());

    assertEquals(1, lightstepSpan.getLogsCount());
    Log log = lightstepSpan.getLogs(0);
    keyValue = getValue(log.getFieldsList(), Adapter.KEY_LOG_MESSAGE);
    assertNotNull(keyValue);
    assertEquals("the log message", keyValue.getStringValue());

    keyValue = getValue(log.getFieldsList(), "foo");
    assertNotNull(keyValue);
    assertEquals("bar", keyValue.getStringValue());

    assertEquals(2, lightstepSpan.getReferencesCount());

    assertHasFollowsFrom(lightstepSpan);
    assertHasParent(lightstepSpan);
  }

  @Test
  public void testLightstepLogs() {
    // prepare
    SpanData.TimedEvent timedEvents = getTimedEvent();

    // test
    List<Log> logs = Adapter.toLightstepLogs(Collections.singletonList(timedEvents));

    // verify
    assertEquals(1, logs.size());
  }

  @Test
  public void testLightstepLog() {
    // prepare
    SpanData.TimedEvent timedEvent = getTimedEvent();

    // test
    Log log = Adapter.toLightstepLog(timedEvent);

    // verify
    assertEquals(2, log.getFieldsCount());

    KeyValue keyValue = getValue(log.getFieldsList(), Adapter.KEY_LOG_MESSAGE);
    assertNotNull(keyValue);
    assertEquals("the log message", keyValue.getStringValue());
    keyValue = getValue(log.getFieldsList(), "foo");
    assertNotNull(keyValue);
    assertEquals("bar", keyValue.getStringValue());
  }

  @Test
  public void testKeyValues() {
    // prepare
    AttributeValue valueB = AttributeValue.booleanAttributeValue(true);

    // test
    Collection<KeyValue> keyValues =
        Adapter.toKeyValues(Collections.singletonMap("valueB", valueB));

    // verify
    // the actual content is checked in some other test
    assertEquals(1, keyValues.size());
  }

  @Test
  public void testKeyValue() {
    // prepare
    AttributeValue valueB = AttributeValue.booleanAttributeValue(true);
    AttributeValue valueD = AttributeValue.doubleAttributeValue(1.);
    AttributeValue valueI = AttributeValue.longAttributeValue(2);
    AttributeValue valueS = AttributeValue.stringAttributeValue("foobar");

    // test
    KeyValue kvB = Adapter.toKeyValue("valueB", valueB);
    KeyValue kvD = Adapter.toKeyValue("valueD", valueD);
    KeyValue kvI = Adapter.toKeyValue("valueI", valueI);
    KeyValue kvS = Adapter.toKeyValue("valueS", valueS);

    // verify
    assertTrue(kvB.getBoolValue());
    assertEquals(1., kvD.getDoubleValue(), 0);
    assertEquals(2, kvI.getIntValue());
    assertEquals("foobar", kvS.getStringValue());
    assertEquals("foobar", kvS.getStringValueBytes().toStringUtf8());
  }

  @Test
  public void testSpanRefs() {
    // prepare
    Link link = SpanData.Link.create(createSpanContext(TRACE_ID, SPAN_ID));

    // test
    Collection<Reference> spanRefs = Adapter.toReferences(Collections.singletonList(link));

    // verify
    assertEquals(1, spanRefs.size()); // the actual span ref is tested in another test
  }

  @Test
  public void testSpanRef() {
    // prepare
    Link link = SpanData.Link.create(createSpanContext(TRACE_ID, SPAN_ID));

    // test
    Reference spanRef = Adapter.toReference(link);

    // verify
    assertEquals(14611542, spanRef.getSpanContext().getSpanId());
    assertEquals(4126161779880129985L, spanRef.getSpanContext().getTraceId());
    assertEquals(Reference.Relationship.FOLLOWS_FROM, spanRef.getRelationship());
  }

  @Test
  public void testStatusNotOk() {
    long startMs = System.currentTimeMillis();
    long endMs = startMs + 900;
    SpanData span =
        SpanData.newBuilder()
            .setHasEnded(true)
            .setTraceId(TraceId.fromLowerBase16(TRACE_ID, 0))
            .setSpanId(SpanId.fromLowerBase16(SPAN_ID, 0))
            .setName("GET /api/endpoint")
            .setStartEpochNanos(TimeUnit.MILLISECONDS.toNanos(startMs))
            .setEndEpochNanos(TimeUnit.MILLISECONDS.toNanos(endMs))
            .setKind(Span.Kind.SERVER)
            .setStatus(Status.CANCELLED)
            .setTotalRecordedEvents(0)
            .setTotalRecordedLinks(0)
            .build();

    assertNotNull(Adapter.toLightstepSpan(span));
  }

  @Test
  public void testTraceIdToLong() {
    final TraceId traceId = TraceId.fromLowerBase16(TRACE_ID, 0);
    final long traceIdLong = Adapter.traceIdToLong(traceId);
    assertEquals(4126161779880129985L, traceIdLong);

    final TraceId traceId2 = new TraceId(123456789L, 0L);
    final long traceIdLong2 = Adapter.traceIdToLong(traceId2);
    assertEquals(123456789L, traceIdLong2);
  }

  @Test
  public void testSpanIdToLong() {
    final SpanId spanId = SpanId.fromLowerBase16(SPAN_ID, 0);
    final long spanIdLong = Adapter.spanIdToLong(spanId);
    assertEquals(14611542L, spanIdLong);

    final SpanId spanId2 = new SpanId(123456789L);
    final long spanIdLong2 = Adapter.spanIdToLong(spanId2);
    assertEquals(123456789L, spanIdLong2);
  }
}
