/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iceberg.types;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.iceberg.Schema;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.expressions.Literal;
import org.apache.iceberg.relocated.com.google.common.base.Joiner;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableMap;
import org.apache.iceberg.types.Type.NestedType;
import org.apache.iceberg.types.Type.PrimitiveType;

public class Types {

  private Types() {}

  private static final ImmutableMap<String, Type> TYPES =
      ImmutableMap.<String, Type>builder()
          .put(BooleanType.get().toString(), BooleanType.get())
          .put(IntegerType.get().toString(), IntegerType.get())
          .put(LongType.get().toString(), LongType.get())
          .put(FloatType.get().toString(), FloatType.get())
          .put(DoubleType.get().toString(), DoubleType.get())
          .put(DateType.get().toString(), DateType.get())
          .put(TimeType.get().toString(), TimeType.get())
          .put(TimestampType.withZone().toString(), TimestampType.withZone())
          .put(TimestampType.withoutZone().toString(), TimestampType.withoutZone())
          .put(TimestampNanoType.withZone().toString(), TimestampNanoType.withZone())
          .put(TimestampNanoType.withoutZone().toString(), TimestampNanoType.withoutZone())
          .put(StringType.get().toString(), StringType.get())
          .put(UUIDType.get().toString(), UUIDType.get())
          .put(BinaryType.get().toString(), BinaryType.get())
          .put(UnknownType.get().toString(), UnknownType.get())
          .put(VariantType.get().toString(), VariantType.get())
          .put(GeometryType.crs84().toString(), GeometryType.crs84())
          .put(GeographyType.crs84().toString(), GeographyType.crs84())
          .buildOrThrow();

  private static final Pattern FIXED = Pattern.compile("fixed\\[\\s*(\\d+)\\s*\\]");
  private static final Pattern GEOMETRY_PARAMETERS =
      Pattern.compile("geometry\\s*(?:\\(\\s*([^)]*?)\\s*\\))?", Pattern.CASE_INSENSITIVE);
  private static final Pattern GEOGRAPHY_PARAMETERS =
      Pattern.compile(
          "geography\\s*(?:\\(\\s*([^,]*?)\\s*(?:,\\s*(\\w*)\\s*)?\\))?", Pattern.CASE_INSENSITIVE);
  private static final Pattern DECIMAL =
      Pattern.compile("decimal\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)");

  public static Type fromTypeName(String typeString) {
    String lowerTypeString = typeString.toLowerCase(Locale.ROOT);
    if (TYPES.containsKey(lowerTypeString)) {
      return TYPES.get(lowerTypeString);
    }

    Matcher geometry = GEOMETRY_PARAMETERS.matcher(typeString);
    if (geometry.matches()) {
      String crs = geometry.group(1);
      Preconditions.checkArgument(!crs.contains(","), "Invalid CRS: %s", crs);
      return GeometryType.of(crs);
    }

    Matcher geography = GEOGRAPHY_PARAMETERS.matcher(typeString);
    if (geography.matches()) {
      String crs = geography.group(1);
      String algorithmName = geography.group(2);
      EdgeAlgorithm algorithm =
          algorithmName == null ? null : EdgeAlgorithm.fromName(algorithmName);
      return GeographyType.of(crs, algorithm);
    }

    Matcher fixed = FIXED.matcher(lowerTypeString);
    if (fixed.matches()) {
      return FixedType.ofLength(Integer.parseInt(fixed.group(1)));
    }

    Matcher decimal = DECIMAL.matcher(lowerTypeString);
    if (decimal.matches()) {
      return DecimalType.of(Integer.parseInt(decimal.group(1)), Integer.parseInt(decimal.group(2)));
    }

    throw new IllegalArgumentException("Cannot parse type string to primitive: " + typeString);
  }

  public static PrimitiveType fromPrimitiveString(String typeString) {
    Type type = fromTypeName(typeString);
    if (type.isPrimitiveType()) {
      return type.asPrimitiveType();
    }

    throw new IllegalArgumentException("Cannot parse type string: variant is not a primitive type");
  }

  public static class BooleanType extends PrimitiveType {
    private static final BooleanType INSTANCE = new BooleanType();

    public static BooleanType get() {
      return INSTANCE;
    }

    @Override
    public TypeID typeId() {
      return TypeID.BOOLEAN;
    }

    @Override
    public String toString() {
      return "boolean";
    }
  }

  public static class IntegerType extends PrimitiveType {
    private static final IntegerType INSTANCE = new IntegerType();

    public static IntegerType get() {
      return INSTANCE;
    }

    @Override
    public TypeID typeId() {
      return TypeID.INTEGER;
    }

    @Override
    public String toString() {
      return "int";
    }
  }

  public static class LongType extends PrimitiveType {
    private static final LongType INSTANCE = new LongType();

    public static LongType get() {
      return INSTANCE;
    }

    @Override
    public TypeID typeId() {
      return TypeID.LONG;
    }

    @Override
    public String toString() {
      return "long";
    }
  }

  public static class FloatType extends PrimitiveType {
    private static final FloatType INSTANCE = new FloatType();

    public static FloatType get() {
      return INSTANCE;
    }

    @Override
    public TypeID typeId() {
      return TypeID.FLOAT;
    }

    @Override
    public String toString() {
      return "float";
    }
  }

  public static class DoubleType extends PrimitiveType {
    private static final DoubleType INSTANCE = new DoubleType();

    public static DoubleType get() {
      return INSTANCE;
    }

    @Override
    public TypeID typeId() {
      return TypeID.DOUBLE;
    }

    @Override
    public String toString() {
      return "double";
    }
  }

  public static class DateType extends PrimitiveType {
    private static final DateType INSTANCE = new DateType();

    public static DateType get() {
      return INSTANCE;
    }

    @Override
    public TypeID typeId() {
      return TypeID.DATE;
    }

    @Override
    public String toString() {
      return "date";
    }
  }

  public static class TimeType extends PrimitiveType {
    private static final TimeType INSTANCE = new TimeType();

    public static TimeType get() {
      return INSTANCE;
    }

    private TimeType() {}

    @Override
    public TypeID typeId() {
      return TypeID.TIME;
    }

    @Override
    public String toString() {
      return "time";
    }
  }

  public static class TimestampType extends PrimitiveType {
    private static final TimestampType INSTANCE_WITH_ZONE = new TimestampType(true);
    private static final TimestampType INSTANCE_WITHOUT_ZONE = new TimestampType(false);

    public static TimestampType withZone() {
      return INSTANCE_WITH_ZONE;
    }

    public static TimestampType withoutZone() {
      return INSTANCE_WITHOUT_ZONE;
    }

    private final boolean adjustToUTC;

    private TimestampType(boolean adjustToUTC) {
      this.adjustToUTC = adjustToUTC;
    }

    public boolean shouldAdjustToUTC() {
      return adjustToUTC;
    }

    @Override
    public TypeID typeId() {
      return TypeID.TIMESTAMP;
    }

    @Override
    public String toString() {
      if (shouldAdjustToUTC()) {
        return "timestamptz";
      } else {
        return "timestamp";
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      } else if (!(o instanceof TimestampType)) {
        return false;
      }

      TimestampType timestampType = (TimestampType) o;
      return adjustToUTC == timestampType.adjustToUTC;
    }

    @Override
    public int hashCode() {
      return Objects.hash(TimestampType.class, adjustToUTC);
    }
  }

  public static class TimestampNanoType extends PrimitiveType {
    private static final TimestampNanoType INSTANCE_WITH_ZONE = new TimestampNanoType(true);
    private static final TimestampNanoType INSTANCE_WITHOUT_ZONE = new TimestampNanoType(false);

    public static TimestampNanoType withZone() {
      return INSTANCE_WITH_ZONE;
    }

    public static TimestampNanoType withoutZone() {
      return INSTANCE_WITHOUT_ZONE;
    }

    private final boolean adjustToUTC;

    private TimestampNanoType(boolean adjustToUTC) {
      this.adjustToUTC = adjustToUTC;
    }

    public boolean shouldAdjustToUTC() {
      return adjustToUTC;
    }

    @Override
    public TypeID typeId() {
      return TypeID.TIMESTAMP_NANO;
    }

    @Override
    public String toString() {
      if (shouldAdjustToUTC()) {
        return "timestamptz_ns";
      } else {
        return "timestamp_ns";
      }
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      } else if (!(other instanceof TimestampNanoType)) {
        return false;
      }

      return adjustToUTC == ((TimestampNanoType) other).adjustToUTC;
    }

    @Override
    public int hashCode() {
      return Objects.hash(TimestampNanoType.class, adjustToUTC);
    }
  }

  public static class StringType extends PrimitiveType {
    private static final StringType INSTANCE = new StringType();

    public static StringType get() {
      return INSTANCE;
    }

    @Override
    public TypeID typeId() {
      return TypeID.STRING;
    }

    @Override
    public String toString() {
      return "string";
    }
  }

  public static class UUIDType extends PrimitiveType {
    private static final UUIDType INSTANCE = new UUIDType();

    public static UUIDType get() {
      return INSTANCE;
    }

    @Override
    public TypeID typeId() {
      return TypeID.UUID;
    }

    @Override
    public String toString() {
      return "uuid";
    }
  }

  public static class FixedType extends PrimitiveType {
    public static FixedType ofLength(int length) {
      return new FixedType(length);
    }

    private final int length;

    private FixedType(int length) {
      this.length = length;
    }

    public int length() {
      return length;
    }

    @Override
    public TypeID typeId() {
      return TypeID.FIXED;
    }

    @Override
    public String toString() {
      return String.format(Locale.ROOT, "fixed[%d]", length);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      } else if (!(o instanceof FixedType)) {
        return false;
      }

      FixedType fixedType = (FixedType) o;
      return length == fixedType.length;
    }

    @Override
    public int hashCode() {
      return Objects.hash(FixedType.class, length);
    }
  }

  public static class BinaryType extends PrimitiveType {
    private static final BinaryType INSTANCE = new BinaryType();

    public static BinaryType get() {
      return INSTANCE;
    }

    @Override
    public TypeID typeId() {
      return TypeID.BINARY;
    }

    @Override
    public String toString() {
      return "binary";
    }
  }

  public static class VariantType implements Type {
    private static final VariantType INSTANCE = new VariantType();

    public static VariantType get() {
      return INSTANCE;
    }

    @Override
    public TypeID typeId() {
      return TypeID.VARIANT;
    }

    @Override
    public String toString() {
      return "variant";
    }

    @Override
    public boolean isVariantType() {
      return true;
    }

    @Override
    public VariantType asVariantType() {
      return this;
    }

    Object writeReplace() throws ObjectStreamException {
      return new PrimitiveLikeHolder(toString());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      } else if (!(o instanceof VariantType)) {
        return false;
      }

      VariantType that = (VariantType) o;
      return typeId() == that.typeId();
    }

    @Override
    public int hashCode() {
      return Objects.hash(VariantType.class, typeId());
    }
  }

  public static class UnknownType extends PrimitiveType {
    private static final UnknownType INSTANCE = new UnknownType();

    public static UnknownType get() {
      return INSTANCE;
    }

    @Override
    public TypeID typeId() {
      return TypeID.UNKNOWN;
    }

    @Override
    public String toString() {
      return "unknown";
    }
  }

  public static class DecimalType extends PrimitiveType {
    public static DecimalType of(int precision, int scale) {
      return new DecimalType(precision, scale);
    }

    private final int scale;
    private final int precision;

    private DecimalType(int precision, int scale) {
      Preconditions.checkArgument(
          precision <= 38,
          "Decimals with precision larger than 38 are not supported: %s",
          precision);
      this.scale = scale;
      this.precision = precision;
    }

    public int scale() {
      return scale;
    }

    public int precision() {
      return precision;
    }

    @Override
    public TypeID typeId() {
      return TypeID.DECIMAL;
    }

    @Override
    public String toString() {
      return String.format(Locale.ROOT, "decimal(%d, %d)", precision, scale);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      } else if (!(o instanceof DecimalType)) {
        return false;
      }

      DecimalType that = (DecimalType) o;
      if (scale != that.scale) {
        return false;
      }
      return precision == that.precision;
    }

    @Override
    public int hashCode() {
      return Objects.hash(DecimalType.class, scale, precision);
    }
  }

  public static class GeometryType extends PrimitiveType {
    public static final String DEFAULT_CRS = "OGC:CRS84";

    public static GeometryType crs84() {
      return new GeometryType();
    }

    public static GeometryType of(String crs) {
      return new GeometryType(crs);
    }

    private final String crs;

    private GeometryType() {
      crs = null;
    }

    private GeometryType(String crs) {
      Preconditions.checkArgument(crs == null || !crs.isEmpty(), "Invalid CRS: (empty string)");
      this.crs = DEFAULT_CRS.equalsIgnoreCase(crs) ? null : crs;
    }

    @Override
    public TypeID typeId() {
      return TypeID.GEOMETRY;
    }

    public String crs() {
      return crs;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      } else if (!(o instanceof GeometryType)) {
        return false;
      }

      GeometryType that = (GeometryType) o;
      return Objects.equals(crs, that.crs);
    }

    @Override
    public int hashCode() {
      return Objects.hash(GeometryType.class, crs);
    }

    @Override
    public String toString() {
      if (crs == null) {
        return "geometry";
      }

      return String.format("geometry(%s)", crs);
    }
  }

  public static class GeographyType extends PrimitiveType {
    public static final String DEFAULT_CRS = "OGC:CRS84";

    public static GeographyType crs84() {
      return new GeographyType();
    }

    public static GeographyType of(String crs) {
      return new GeographyType(crs, null);
    }

    public static GeographyType of(String crs, EdgeAlgorithm algorithm) {
      return new GeographyType(crs, algorithm);
    }

    private final String crs;
    private final EdgeAlgorithm algorithm;

    private GeographyType() {
      this.crs = null;
      this.algorithm = null;
    }

    private GeographyType(String crs, EdgeAlgorithm algorithm) {
      Preconditions.checkArgument(crs == null || !crs.isEmpty(), "Invalid CRS: (empty string)");
      this.crs = DEFAULT_CRS.equalsIgnoreCase(crs) ? null : crs;
      this.algorithm = algorithm;
    }

    @Override
    public TypeID typeId() {
      return TypeID.GEOGRAPHY;
    }

    public String crs() {
      return crs;
    }

    public EdgeAlgorithm algorithm() {
      return algorithm;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      } else if (!(o instanceof GeographyType)) {
        return false;
      }

      GeographyType that = (GeographyType) o;
      return Objects.equals(crs, that.crs) && Objects.equals(algorithm, that.algorithm);
    }

    @Override
    public int hashCode() {
      return Objects.hash(GeographyType.class, crs, algorithm);
    }

    @Override
    public String toString() {
      if (algorithm != null) {
        return String.format("geography(%s, %s)", crs != null ? crs : DEFAULT_CRS, algorithm);
      } else if (crs != null) {
        return String.format("geography(%s)", crs);
      } else {
        return "geography";
      }
    }
  }

  public static class NestedField implements Serializable {
    public static NestedField optional(int id, String name, Type type) {
      return new NestedField(true, id, name, type, null, null, null);
    }

    public static NestedField optional(int id, String name, Type type, String doc) {
      return new NestedField(true, id, name, type, doc, null, null);
    }

    public static NestedField required(int id, String name, Type type) {
      return new NestedField(false, id, name, type, null, null, null);
    }

    public static NestedField required(int id, String name, Type type, String doc) {
      return new NestedField(false, id, name, type, doc, null, null);
    }

    /**
     * Create a nested field.
     *
     * @deprecated will be removed in 2.0.0; use {@link #builder()} instead.
     */
    @Deprecated
    public static NestedField of(int id, boolean isOptional, String name, Type type) {
      return new NestedField(isOptional, id, name, type, null, null, null);
    }

    /**
     * Create a nested field.
     *
     * @deprecated will be removed in 2.0.0; use {@link #builder()} instead.
     */
    @Deprecated
    public static NestedField of(int id, boolean isOptional, String name, Type type, String doc) {
      return new NestedField(isOptional, id, name, type, doc, null, null);
    }

    public static Builder from(NestedField field) {
      return new Builder(field);
    }

    public static Builder required(String name) {
      return new Builder(false, name);
    }

    public static Builder optional(String name) {
      return new Builder(true, name);
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private boolean isOptional = true;
      private String name = null;
      private Integer id = null;
      private Type type = null;
      private String doc = null;
      private Literal<?> initialDefault = null;
      private Literal<?> writeDefault = null;

      private Builder() {}

      private Builder(boolean isFieldOptional, String fieldName) {
        isOptional = isFieldOptional;
        name = fieldName;
      }

      private Builder(NestedField toCopy) {
        this.isOptional = toCopy.isOptional;
        this.name = toCopy.name;
        this.id = toCopy.id;
        this.type = toCopy.type;
        this.doc = toCopy.doc;
        this.initialDefault = toCopy.initialDefault;
        this.writeDefault = toCopy.writeDefault;
      }

      public Builder asRequired() {
        this.isOptional = false;
        return this;
      }

      public Builder asOptional() {
        this.isOptional = true;
        return this;
      }

      public Builder isOptional(boolean fieldIsOptional) {
        this.isOptional = fieldIsOptional;
        return this;
      }

      public Builder withName(String fieldName) {
        this.name = fieldName;
        return this;
      }

      public Builder withId(int fieldId) {
        id = fieldId;
        return this;
      }

      public Builder ofType(Type fieldType) {
        type = fieldType;
        return this;
      }

      public Builder withDoc(String fieldDoc) {
        doc = fieldDoc;
        return this;
      }

      /**
       * Set the initial default using an Object.
       *
       * @deprecated will be removed in 2.0.0; use {@link #withInitialDefault(Literal)} instead.
       */
      @Deprecated
      public Builder withInitialDefault(Object fieldInitialDefault) {
        return withInitialDefault(Expressions.lit(fieldInitialDefault));
      }

      public Builder withInitialDefault(Literal<?> fieldInitialDefault) {
        initialDefault = fieldInitialDefault;
        return this;
      }

      /**
       * Set the write default using an Object.
       *
       * @deprecated will be removed in 2.0.0; use {@link #withWriteDefault(Literal)} instead.
       */
      @Deprecated
      public Builder withWriteDefault(Object fieldWriteDefault) {
        return withWriteDefault(Expressions.lit(fieldWriteDefault));
      }

      public Builder withWriteDefault(Literal<?> fieldWriteDefault) {
        writeDefault = fieldWriteDefault;
        return this;
      }

      public NestedField build() {
        Preconditions.checkNotNull(id, "Id cannot be null");
        // the constructor validates the other fields
        return new NestedField(isOptional, id, name, type, doc, initialDefault, writeDefault);
      }
    }

    private final boolean isOptional;
    private final int id;
    private final String name;
    private final Type type;
    private final String doc;
    private final Literal<?> initialDefault;
    private final Literal<?> writeDefault;

    private NestedField(
        boolean isOptional,
        int id,
        String name,
        Type type,
        String doc,
        Literal<?> initialDefault,
        Literal<?> writeDefault) {
      Preconditions.checkNotNull(name, "Name cannot be null");
      Preconditions.checkNotNull(type, "Type cannot be null");
      Preconditions.checkArgument(
          isOptional || !type.equals(UnknownType.get()),
          "Cannot create required field with unknown type: %s",
          name);
      this.isOptional = isOptional;
      this.id = id;
      this.name = name;
      this.type = type;
      this.doc = doc;
      this.initialDefault = castDefault(initialDefault, type);
      this.writeDefault = castDefault(writeDefault, type);
    }

    private static Literal<?> castDefault(Literal<?> defaultValue, Type type) {
      if (type.isNestedType() && defaultValue != null) {
        throw new IllegalArgumentException(
            String.format("Invalid default value for %s: %s (must be null)", type, defaultValue));
      } else if (defaultValue != null) {
        Literal<?> typedDefault = defaultValue.to(type);
        Preconditions.checkArgument(
            typedDefault != null, "Cannot cast default value to %s: %s", type, defaultValue);
        return typedDefault;
      }

      return null;
    }

    public boolean isOptional() {
      return isOptional;
    }

    public NestedField asOptional() {
      if (isOptional) {
        return this;
      }
      return new NestedField(true, id, name, type, doc, initialDefault, writeDefault);
    }

    public boolean isRequired() {
      return !isOptional;
    }

    public NestedField asRequired() {
      if (!isOptional) {
        return this;
      }
      return new NestedField(false, id, name, type, doc, initialDefault, writeDefault);
    }

    /**
     * @deprecated will be removed in 2.0.0; use {@link Builder#withId(int)} instead
     */
    @Deprecated
    public NestedField withFieldId(int newId) {
      return new NestedField(isOptional, newId, name, type, doc, initialDefault, writeDefault);
    }

    public int fieldId() {
      return id;
    }

    public String name() {
      return name;
    }

    public Type type() {
      return type;
    }

    public String doc() {
      return doc;
    }

    public Literal<?> initialDefaultLiteral() {
      return initialDefault;
    }

    public Object initialDefault() {
      return initialDefault != null ? initialDefault.value() : null;
    }

    public Literal<?> writeDefaultLiteral() {
      return writeDefault;
    }

    public Object writeDefault() {
      return writeDefault != null ? writeDefault.value() : null;
    }

    @Override
    public String toString() {
      return String.format(
              Locale.ROOT, "%d: %s: %s %s", id, name, isOptional ? "optional" : "required", type)
          + (doc != null ? " (" + doc + ")" : "");
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      } else if (!(o instanceof NestedField)) {
        return false;
      }

      NestedField that = (NestedField) o;
      if (isOptional != that.isOptional) {
        return false;
      } else if (id != that.id) {
        return false;
      } else if (!name.equals(that.name)) {
        return false;
      } else if (!Objects.equals(doc, that.doc)) {
        return false;
      } else if (!type.equals(that.type)) {
        return false;
      } else if (!Objects.equals(initialDefault, that.initialDefault)) {
        return false;
      } else if (!Objects.equals(writeDefault, that.writeDefault)) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      return Objects.hash(NestedField.class, id, isOptional, name, type);
    }
  }

  public static class StructType extends NestedType {
    private static final Joiner FIELD_SEP = Joiner.on(", ");

    public static StructType of(NestedField... fields) {
      return of(Arrays.asList(fields));
    }

    public static StructType of(List<NestedField> fields) {
      return new StructType(fields);
    }

    private final NestedField[] fields;

    // lazy values
    private transient Schema schema = null;
    private transient List<NestedField> fieldList = null;
    private transient Map<String, NestedField> fieldsByName = null;
    private transient Map<String, NestedField> fieldsByLowerCaseName = null;
    private transient Map<Integer, NestedField> fieldsById = null;

    private StructType(List<NestedField> fields) {
      Preconditions.checkNotNull(fields, "Field list cannot be null");
      this.fields = new NestedField[fields.size()];
      for (int i = 0; i < this.fields.length; i += 1) {
        this.fields[i] = fields.get(i);
      }
    }

    @Override
    public List<NestedField> fields() {
      return lazyFieldList();
    }

    public NestedField field(String name) {
      return lazyFieldsByName().get(name);
    }

    @Override
    public NestedField field(int id) {
      return lazyFieldsById().get(id);
    }

    public NestedField caseInsensitiveField(String name) {
      return lazyFieldsByLowerCaseName().get(name.toLowerCase(Locale.ROOT));
    }

    @Override
    public Type fieldType(String name) {
      NestedField field = field(name);
      if (field != null) {
        return field.type();
      }
      return null;
    }

    @Override
    public TypeID typeId() {
      return TypeID.STRUCT;
    }

    @Override
    public boolean isStructType() {
      return true;
    }

    @Override
    public Types.StructType asStructType() {
      return this;
    }

    /**
     * Returns a schema which contains the columns inside struct type. This method can be used to
     * avoid expensive conversion of StructType containing large number of columns to Schema during
     * manifest evaluation.
     *
     * @return the schema containing columns of struct type.
     */
    public Schema asSchema() {
      if (this.schema == null) {
        this.schema = new Schema(Arrays.asList(this.fields));
      }
      return this.schema;
    }

    @Override
    public String toString() {
      return String.format("struct<%s>", FIELD_SEP.join(fields));
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      } else if (!(o instanceof StructType)) {
        return false;
      }

      StructType that = (StructType) o;
      return Arrays.equals(fields, that.fields);
    }

    @Override
    public int hashCode() {
      return Objects.hash(NestedField.class, Arrays.hashCode(fields));
    }

    private List<NestedField> lazyFieldList() {
      if (fieldList == null) {
        this.fieldList = ImmutableList.copyOf(fields);
      }
      return fieldList;
    }

    private Map<String, NestedField> lazyFieldsByName() {
      if (fieldsByName == null) {
        ImmutableMap.Builder<String, NestedField> byNameBuilder = ImmutableMap.builder();
        for (NestedField field : fields) {
          byNameBuilder.put(field.name(), field);
        }
        fieldsByName = byNameBuilder.build();
      }
      return fieldsByName;
    }

    private Map<String, NestedField> lazyFieldsByLowerCaseName() {
      if (fieldsByLowerCaseName == null) {
        ImmutableMap.Builder<String, NestedField> byLowerCaseNameBuilder = ImmutableMap.builder();
        for (NestedField field : fields) {
          byLowerCaseNameBuilder.put(field.name().toLowerCase(Locale.ROOT), field);
        }
        fieldsByLowerCaseName = byLowerCaseNameBuilder.build();
      }
      return fieldsByLowerCaseName;
    }

    private Map<Integer, NestedField> lazyFieldsById() {
      if (fieldsById == null) {
        ImmutableMap.Builder<Integer, NestedField> byIdBuilder = ImmutableMap.builder();
        for (NestedField field : fields) {
          byIdBuilder.put(field.fieldId(), field);
        }
        this.fieldsById = byIdBuilder.build();
      }
      return fieldsById;
    }
  }

  public static class ListType extends NestedType {
    public static ListType ofOptional(int elementId, Type elementType) {
      Preconditions.checkNotNull(elementType, "Element type cannot be null");
      return new ListType(NestedField.optional(elementId, "element", elementType));
    }

    public static ListType ofRequired(int elementId, Type elementType) {
      Preconditions.checkNotNull(elementType, "Element type cannot be null");
      return new ListType(NestedField.required(elementId, "element", elementType));
    }

    private final NestedField elementField;
    private transient List<NestedField> fields = null;

    private ListType(NestedField elementField) {
      this.elementField = elementField;
    }

    public Type elementType() {
      return elementField.type();
    }

    @Override
    public Type fieldType(String name) {
      if ("element".equals(name)) {
        return elementType();
      }
      return null;
    }

    @Override
    public NestedField field(int id) {
      if (elementField.fieldId() == id) {
        return elementField;
      }
      return null;
    }

    @Override
    public List<NestedField> fields() {
      return lazyFieldList();
    }

    public int elementId() {
      return elementField.fieldId();
    }

    public boolean isElementRequired() {
      return !elementField.isOptional;
    }

    public boolean isElementOptional() {
      return elementField.isOptional;
    }

    @Override
    public TypeID typeId() {
      return TypeID.LIST;
    }

    @Override
    public boolean isListType() {
      return true;
    }

    @Override
    public Types.ListType asListType() {
      return this;
    }

    @Override
    public String toString() {
      return String.format("list<%s>", elementField.type());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      } else if (!(o instanceof ListType)) {
        return false;
      }

      ListType listType = (ListType) o;
      return elementField.equals(listType.elementField);
    }

    @Override
    public int hashCode() {
      return Objects.hash(ListType.class, elementField);
    }

    private List<NestedField> lazyFieldList() {
      if (fields == null) {
        this.fields = ImmutableList.of(elementField);
      }
      return fields;
    }
  }

  public static class MapType extends NestedType {
    public static MapType ofOptional(int keyId, int valueId, Type keyType, Type valueType) {
      Preconditions.checkNotNull(valueType, "Value type cannot be null");
      return new MapType(
          NestedField.required(keyId, "key", keyType),
          NestedField.optional(valueId, "value", valueType));
    }

    public static MapType ofRequired(int keyId, int valueId, Type keyType, Type valueType) {
      Preconditions.checkNotNull(valueType, "Value type cannot be null");
      return new MapType(
          NestedField.required(keyId, "key", keyType),
          NestedField.required(valueId, "value", valueType));
    }

    private final NestedField keyField;
    private final NestedField valueField;
    private transient List<NestedField> fields = null;

    private MapType(NestedField keyField, NestedField valueField) {
      this.keyField = keyField;
      this.valueField = valueField;
    }

    public Type keyType() {
      return keyField.type();
    }

    public Type valueType() {
      return valueField.type();
    }

    @Override
    public Type fieldType(String name) {
      if ("key".equals(name)) {
        return keyField.type();
      } else if ("value".equals(name)) {
        return valueField.type();
      }
      return null;
    }

    @Override
    public NestedField field(int id) {
      if (keyField.fieldId() == id) {
        return keyField;
      } else if (valueField.fieldId() == id) {
        return valueField;
      }
      return null;
    }

    @Override
    public List<NestedField> fields() {
      return lazyFieldList();
    }

    public int keyId() {
      return keyField.fieldId();
    }

    public int valueId() {
      return valueField.fieldId();
    }

    public boolean isValueRequired() {
      return !valueField.isOptional;
    }

    public boolean isValueOptional() {
      return valueField.isOptional;
    }

    @Override
    public TypeID typeId() {
      return TypeID.MAP;
    }

    @Override
    public boolean isMapType() {
      return true;
    }

    @Override
    public Types.MapType asMapType() {
      return this;
    }

    @Override
    public String toString() {
      return String.format("map<%s, %s>", keyField.type(), valueField.type());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      } else if (!(o instanceof MapType)) {
        return false;
      }

      MapType mapType = (MapType) o;
      if (!keyField.equals(mapType.keyField)) {
        return false;
      }
      return valueField.equals(mapType.valueField);
    }

    @Override
    public int hashCode() {
      return Objects.hash(MapType.class, keyField, valueField);
    }

    private List<NestedField> lazyFieldList() {
      if (fields == null) {
        this.fields = ImmutableList.of(keyField, valueField);
      }
      return fields;
    }
  }
}
