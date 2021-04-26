/**
 * Copyright © 2013-2015 Commerce Technologies, LLC.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.ttyys.micrc.codegen.gradle.plugin.common;

import org.apache.avro.Conversion;
import org.apache.avro.LogicalTypes;
import org.apache.avro.compiler.specific.SpecificCompiler.FieldVisibility;
import org.apache.avro.generic.GenericData.StringType;
import org.gradle.api.reflect.TypeOf;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Various constants needed by the plugin.
 *
 * <p>The default values from {@code avro-compiler} aren't exposed in a way that's easily accessible, so even default
 * values that we want to match are still reproduced here.</p>
 */
public class Constants {
    /**
     * The namespace separator.
     */
    public static final String NAMESPACE_SEPARATOR = ".";
    public static final String UTF8_ENCODING = "UTF-8";

    public static final String DEFAULT_STRING_TYPE = StringType.String.name();
    public static final String DEFAULT_FIELD_VISIBILITY = FieldVisibility.PUBLIC_DEPRECATED.name();
    public static final boolean DEFAULT_CREATE_SETTERS = true;
    public static final boolean DEFAULT_CREATE_OPTIONAL_GETTERS = false;
    public static final boolean DEFAULT_GETTERS_RETURN_OPTIONAL = false;
    public static final boolean DEFAULT_OPTIONAL_GETTERS_FOR_NULLABLE_FIELDS_ONLY = false;
    public static final boolean DEFAULT_ENABLE_DECIMAL_LOGICAL_TYPE = true;
    public static final Map<String, Class<? extends LogicalTypes.LogicalTypeFactory>> DEFAULT_LOGICAL_TYPE_FACTORIES = Collections.emptyMap();
    public static final List<Class<? extends Conversion<?>>> DEFAULT_CUSTOM_CONVERSIONS = Collections.emptyList();

    public static final String SCHEMA_EXTENSION = "avsc";
    public static final String PROTOCOL_EXTENSION = "avpr";
    public static final String IDL_EXTENSION = "avdl";
    public static final String JAVA_EXTENSION = "java";

    public static final String GROUP_SOURCE_GENERATION = "Source Generation";

    public static final String AVRO_EXTENSION_NAME = "avro";
    public static final String JAVA_ANNOTATION_KEY = "javaAnnotation";
    public static final String SCHEMA_DESIGN_EXTENSION_NAME = "codegen";

    public static final String OPTION_FIELD_VISIBILITY = "fieldVisibility";
    public static final String OPTION_STRING_TYPE = "stringType";

    public static final TypeOf<Class<? extends LogicalTypes.LogicalTypeFactory>> LOGICAL_TYPE_FACTORY_TYPE =
            new TypeOf<Class<? extends LogicalTypes.LogicalTypeFactory>>() {};

    public static final TypeOf<Class<? extends Conversion<?>>> CONVERSION_TYPE =
            new TypeOf<Class<? extends Conversion<?>>>() {};
    public static final String SCHEMA_DESIGN_PATH_KEY = "program";
    public static final String SERVICE_INTEGRATION_PATH_KEY = "feature";
}
