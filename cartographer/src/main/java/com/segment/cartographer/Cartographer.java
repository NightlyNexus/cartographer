/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment.io, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.segment.cartographer;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cartographer creates {@link Map} from JSON encoded streams. Use {@link Builder} to construct
 * instances.
 */
public class Cartographer {
  private final boolean isLenient;

  Cartographer(boolean isLenient) {
    this.isLenient = isLenient;
  }

  /**
   * Deserializes the specified json into a {@link Map}. If you have the Json in a {@link Reader}
   * form instead of a {@link String}, use {@link #fromJson(Reader)} instead.
   */
  public Map<String, Object> fromJson(String json) throws IOException {
    return fromJson(new StringReader(json));
  }

  /**
   * Deserializes the json read from the specified {@link Reader} into a {@link Map}.
   * If you have the Json in a String form instead of a {@link Reader}, use {@link
   * #fromJson(String)} instead.
   */
  public Map<String, Object> fromJson(Reader reader) throws IOException {
    JsonReader jsonReader = new JsonReader(reader);
    jsonReader.setLenient(isLenient);
    try {
      return readerToMap(jsonReader);
    } finally {
      reader.close();
    }
  }

  /**
   * Serializes the map into it's json representation and returns it as a String. If you want to
   * write the json to {@link Writer} instead of retrieving it as a String, use {@link #toJson(Map,
   * Writer)} instead.
   */
  public String toJson(Map<?, ?> map) throws IOException {
    StringWriter stringWriter = new StringWriter();
    toJson(map, stringWriter);
    return stringWriter.toString();
  }

  /**
   * Serializes the map into it's json representation into the provided {@link Writer}. If you want
   * to retrieve the json as a string,  use {@link #toJson(Map)} instead.
   */
  public void toJson(Map<?, ?> map, Writer writer) throws IOException {
    if (map == null) {
      throw new IllegalArgumentException("map == null");
    }

    JsonWriter jsonWriter = new JsonWriter(writer);
    jsonWriter.setLenient(isLenient);
    try {
      mapToWriter(map, jsonWriter);
    } finally {
      jsonWriter.close();
    }
  }

  // Decoding

  /** Reads the {@link JsonReader} into a {@link Map}. */
  private Map<String, Object> readerToMap(JsonReader reader) throws IOException {
    Map<String, Object> map = new LinkedHashMap<String, Object>();
    reader.beginObject();
    while (reader.hasNext()) {
      map.put(reader.nextName(), readValue(reader));
    }
    reader.endObject();
    return map;
  }

  /** Reads the {@link JsonReader} into a {@link List}. */
  private List<Object> readerToList(JsonReader reader) throws IOException {
    // todo: try to infer the type of the List?
    List<Object> list = new ArrayList<Object>();
    reader.beginArray();
    while (reader.hasNext()) {
      list.add(readValue(reader));
    }
    reader.endArray();
    return list;
  }

  /** Reads the next value in the {@link JsonReader}. */
  private Object readValue(JsonReader reader) throws IOException {
    JsonToken token = reader.peek();
    switch (token) {
      case BEGIN_OBJECT:
        return readerToMap(reader);
      case BEGIN_ARRAY:
        return readerToList(reader);
      case BOOLEAN:
        return reader.nextBoolean();
      case NULL:
        reader.nextNull(); // consume the null token
        return null;
      case NUMBER:
        return reader.nextDouble();
      case STRING:
        return reader.nextString();
      default:
        throw new IllegalStateException("Invalid token " + token);
    }
  }

  // Encoding

  /** Encode the given {@link Map} into the {@link JsonWriter}. */
  private void mapToWriter(Map<?, ?> map, JsonWriter writer) throws IOException {
    writer.beginObject();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      writer.name(String.valueOf(entry.getKey()));
      writeValue(entry.getValue(), writer);
    }
    writer.endObject();
  }

  /** Print the json representation of a List to the given writer. */
  private void listToWriter(List<?> list, JsonWriter writer) throws IOException {
    writer.beginArray();
    for (Object value : list) {
      writeValue(value, writer);
    }
    writer.endArray();
  }

  /**
   * Writes the given {@link Object} to the {@link JsonWriter}.
   *
   * @throws IOException
   */
  private void writeValue(Object value, JsonWriter writer) throws IOException {
    if (value == null) {
      writer.nullValue();
    } else if (value instanceof Number) {
      writer.value((Number) value);
    } else if (value instanceof Boolean) {
      writer.value((Boolean) value);
    } else if (value instanceof List) {
      listToWriter((List) value, writer);
    } else if (value instanceof Map) {
      mapToWriter((Map) value, writer);
    } else {
      writer.value(String.valueOf(value));
    }
  }

  /** Fluent API to construct instances of {@link Cartographer}. */
  public static class Builder {
    private boolean isLenient;

    public Builder setLenient(boolean isLenient) {
      this.isLenient = isLenient;
      return this;
    }

    public Cartographer build() {
      return new Cartographer(isLenient);
    }
  }
}