/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package morphia.org.bson.codecs.pojo;

import org.bson.codecs.pojo.TypeWithTypeParameters;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.bson.assertions.Assertions.notNull;

/**
 * Holds type information about a type element
 *
 * @param <T> the underlying type being represented
 * @morphia.internal
 */
public final class OldTypeData<T> implements TypeWithTypeParameters<T> {
    private static final Map<Class<?>, Class<?>> PRIMITIVE_CLASS_MAP;

    static {
        Map<Class<?>, Class<?>> map = new HashMap<Class<?>, Class<?>>();
        map.put(boolean.class, Boolean.class);
        map.put(byte.class, Byte.class);
        map.put(char.class, Character.class);
        map.put(double.class, Double.class);
        map.put(float.class, Float.class);
        map.put(int.class, Integer.class);
        map.put(long.class, Long.class);
        map.put(short.class, Short.class);
        PRIMITIVE_CLASS_MAP = map;
    }

    private final Class<T> type;
    private final List<OldTypeData<?>> typeParameters;

    private OldTypeData(Class<T> type, List<OldTypeData<?>> typeParameters) {
        this.type = boxType(type);
        this.typeParameters = typeParameters;
    }

    /**
     * Creates a new builder for ClassTypeData
     *
     * @param type the class for the type
     * @param <T>  the type
     * @return the builder
     */
    public static <T> Builder<T> builder(Class<T> type) {
        return new Builder<T>(notNull("type", type));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> void getNestedTypeData(OldTypeData.Builder<T> builder, Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            OldTypeData.Builder paramBuilder = OldTypeData.builder((Class) pType.getRawType());
            for (Type argType : pType.getActualTypeArguments()) {
                getNestedTypeData(paramBuilder, argType);
            }
            builder.addTypeParameter(paramBuilder.build());
        } else if (type instanceof WildcardType) {
            builder.addTypeParameter(OldTypeData.builder((Class) ((WildcardType) type).getUpperBounds()[0]).build());
        } else if (type instanceof TypeVariable) {
            builder.addTypeParameter(OldTypeData.builder(Object.class).build());
        } else if (type instanceof Class) {
            builder.addTypeParameter(OldTypeData.builder((Class) type).build());
        }
    }

    private static boolean isGetter(Method method) {
        if (method.getParameterTypes().length > 0) {
            return false;
        } else if (method.getName().startsWith("get") && method.getName().length() > "get".length()) {
            return Character.isUpperCase(method.getName().charAt("get".length()));
        } else {
            return method.getName().startsWith("is") && method.getName().length() > "is".length() && Character.isUpperCase(
                method.getName().charAt("is".length()));
        }
    }

    private static String nestedTypeParameters(List<OldTypeData<?>> typeParameters) {
        StringBuilder builder = new StringBuilder();
        int count = 0;
        int last = typeParameters.size();
        for (OldTypeData<?> typeParameter : typeParameters) {
            count++;
            builder.append(typeParameter.getType().getSimpleName());
            if (!typeParameter.getTypeParameters().isEmpty()) {
                builder.append(format("<%s>", nestedTypeParameters(typeParameter.getTypeParameters())));
            }
            if (count < last) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    /**
     * Creates a TypeData reflecting the type of the given method.
     *
     * @param method the method to analyze
     * @return the new TypeData information
     */
    public static OldTypeData<?> newInstance(Method method) {
        if (isGetter(method)) {
            return newInstance(method.getGenericReturnType(), method.getReturnType());
        } else {
            return newInstance(method.getGenericParameterTypes()[0], method.getParameterTypes()[0]);
        }
    }

    /**
     * Creates a TypeData reflecting the type of the given field.
     *
     * @param field the field to analyze
     * @return the new TypeData information
     */
    public static OldTypeData<?> newInstance(Field field) {
        return newInstance(field.getGenericType(), field.getType());
    }

    /**
     * Creates a TypeData reflecting the given generic type and class.
     *
     * @param genericType the type to analyze
     * @param clazz       the class to analyze
     * @param <T>         the type of the new TypeData instance
     * @return the new TypeData information
     */
    public static <T> OldTypeData<T> newInstance(Type genericType, Class<T> clazz) {
        OldTypeData.Builder<T> builder = OldTypeData.builder(clazz);
        if (genericType instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) genericType;
            for (Type argType : pType.getActualTypeArguments()) {
                getNestedTypeData(builder, argType);
            }
        }
        return builder.build();
    }

    /**
     * @return the class this {@code ClassTypeData} represents
     */
    @Override
    public Class<T> getType() {
        return type;
    }

    /**
     * @return the type parameters for the class
     */
    @Override
    public List<OldTypeData<?>> getTypeParameters() {
        return typeParameters;
    }

    @Override
    public int hashCode() {
        int result = getType().hashCode();
        result = 31 * result + getTypeParameters().hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OldTypeData)) {
            return false;
        }

        OldTypeData<?> that = (OldTypeData<?>) o;

        if (!getType().equals(that.getType())) {
            return false;
        }
        return getTypeParameters().equals(that.getTypeParameters());
    }

    @Override
    public String toString() {
        String typeParams = typeParameters.isEmpty() ? ""
                                                     : ", typeParameters=[" + nestedTypeParameters(typeParameters) + "]";
        return "TypeData{"
               + "type=" + type.getSimpleName()
               + typeParams
               + "}";
    }

    @SuppressWarnings("unchecked")
    private <S> Class<S> boxType(Class<S> clazz) {
        if (clazz.isPrimitive()) {
            return (Class<S>) PRIMITIVE_CLASS_MAP.get(clazz);
        } else {
            return clazz;
        }
    }

    boolean isAssignableFrom(Class<?> cls) {
        return type.isAssignableFrom(boxType(cls));
    }

    /**
     * A builder for TypeData
     *
     * @param <T> the main type
     */
    public static final class Builder<T> {
        private final Class<T> type;
        private final List<OldTypeData<?>> typeParameters = new ArrayList<OldTypeData<?>>();

        private Builder(Class<T> type) {
            this.type = type;
        }

        /**
         * Adds a type parameter
         *
         * @param typeParameter the type parameter
         * @param <S>           the type of the type parameter
         * @return this
         */
        public <S> Builder<T> addTypeParameter(OldTypeData<S> typeParameter) {
            typeParameters.add(notNull("typeParameter", typeParameter));
            return this;
        }

        /**
         * Adds multiple type parameters
         *
         * @param typeParameters the type parameters
         * @return this
         */
        public Builder<T> addTypeParameters(List<OldTypeData<?>> typeParameters) {
            notNull("typeParameters", typeParameters);
            for (OldTypeData<?> typeParameter : typeParameters) {
                addTypeParameter(typeParameter);
            }
            return this;
        }

        /**
         * @return the class type data
         */
        public OldTypeData<T> build() {
            return new OldTypeData<T>(type, Collections.unmodifiableList(typeParameters));
        }
    }
}
