/**
 * Copyright © 2013-2019 Commerce Technologies, LLC.
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
package io.ttyys.micrc.codegen.gradle.plugin.extension;

import io.ttyys.micrc.codegen.gradle.plugin.common.Constants;
import org.apache.avro.Conversion;
import org.apache.avro.LogicalTypes;
import org.apache.avro.compiler.specific.SpecificCompiler;
import org.apache.avro.generic.GenericData;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import javax.inject.Inject;
import java.nio.charset.Charset;
import java.util.Map;

@SuppressWarnings({"unused", "WeakerAccess"})
public class CodeGeneratorExtension {
    private final Property<String> resourceAvroDirPath;
    private final Property<String> outputCharacterEncoding;
    private final Property<String> stringType;
    private final Property<String> fieldVisibility;
    private final Property<String> templateDirectory;
    private final Property<Boolean> createSetters;
    private final Property<Boolean> createOptionalGetters;
    private final Property<Boolean> gettersReturnOptional;
    private final Property<Boolean> optionalGettersForNullableFieldsOnly;
    private final Property<Boolean> enableDecimalLogicalType;
    private final MapProperty<String, Class<? extends LogicalTypes.LogicalTypeFactory>> logicalTypeFactories;
    private final ListProperty<Class<? extends Conversion<?>>> customConversions;

    @Inject
    public CodeGeneratorExtension(ObjectFactory objects) {
        this.resourceAvroDirPath = objects.property(String.class).convention("");
        this.outputCharacterEncoding = objects.property(String.class);
        this.stringType = objects.property(String.class).convention(Constants.DEFAULT_STRING_TYPE);
        this.fieldVisibility = objects.property(String.class).convention(Constants.DEFAULT_FIELD_VISIBILITY);
        this.templateDirectory = objects.property(String.class);
        this.createSetters = objects.property(Boolean.class).convention(Constants.DEFAULT_CREATE_SETTERS);
        this.createOptionalGetters = objects.property(Boolean.class).convention(Constants.DEFAULT_CREATE_OPTIONAL_GETTERS);
        this.gettersReturnOptional = objects.property(Boolean.class).convention(Constants.DEFAULT_GETTERS_RETURN_OPTIONAL);
        this.optionalGettersForNullableFieldsOnly = objects.property(Boolean.class)
                .convention(Constants.DEFAULT_OPTIONAL_GETTERS_FOR_NULLABLE_FIELDS_ONLY);
        this.enableDecimalLogicalType = objects.property(Boolean.class).convention(Constants.DEFAULT_ENABLE_DECIMAL_LOGICAL_TYPE);
        this.logicalTypeFactories = objects.mapProperty(String.class, Constants.LOGICAL_TYPE_FACTORY_TYPE.getConcreteClass())
                .convention(Constants.DEFAULT_LOGICAL_TYPE_FACTORIES);
        this.customConversions =
                objects.listProperty(Constants.CONVERSION_TYPE.getConcreteClass()).convention(Constants.DEFAULT_CUSTOM_CONVERSIONS);
    }

    public Property<String> getResourceAvroDirPath() {
        return resourceAvroDirPath;
    }

    public void setResourceAvroDirPath(String resourceAvroDirPath) {
        this.resourceAvroDirPath.set(resourceAvroDirPath);
    }

    public void setResourceAvroDirPath(Charset resourceAvroDirPath) {
        setResourceAvroDirPath(resourceAvroDirPath.name());
    }

    public Property<String> getOutputCharacterEncoding() {
        return outputCharacterEncoding;
    }

    public void setOutputCharacterEncoding(String outputCharacterEncoding) {
        this.outputCharacterEncoding.set(outputCharacterEncoding);
    }

    public void setOutputCharacterEncoding(Charset outputCharacterEncoding) {
        setOutputCharacterEncoding(outputCharacterEncoding.name());
    }

    public Property<String> getStringType() {
        return stringType;
    }

    public void setStringType(String stringType) {
        this.stringType.set(stringType);
    }

    public void setStringType(GenericData.StringType stringType) {
        setStringType(stringType.name());
    }

    public Property<String> getFieldVisibility() {
        return fieldVisibility;
    }

    public void setFieldVisibility(String fieldVisibility) {
        this.fieldVisibility.set(fieldVisibility);
    }

    public void setFieldVisibility(SpecificCompiler.FieldVisibility fieldVisibility) {
        setFieldVisibility(fieldVisibility.name());
    }

    public Property<String> getTemplateDirectory() {
        return templateDirectory;
    }

    public void setTemplateDirectory(String templateDirectory) {
        this.templateDirectory.set(templateDirectory);
    }

    public Property<Boolean> isCreateSetters() {
        return createSetters;
    }

    public void setCreateSetters(String createSetters) {
        setCreateSetters(Boolean.parseBoolean(createSetters));
    }

    public void setCreateSetters(boolean createSetters) {
        this.createSetters.set(createSetters);
    }

    public Property<Boolean> isCreateOptionalGetters() {
        return createOptionalGetters;
    }

    public void setCreateOptionalGetters(String createOptionalGetters) {
        setCreateOptionalGetters(Boolean.parseBoolean(createOptionalGetters));
    }

    public void setCreateOptionalGetters(boolean createOptionalGetters) {
        this.createOptionalGetters.set(createOptionalGetters);
    }

    public Property<Boolean> isGettersReturnOptional() {
        return gettersReturnOptional;
    }

    public void setGettersReturnOptional(String gettersReturnOptional) {
        setGettersReturnOptional(Boolean.parseBoolean(gettersReturnOptional));
    }

    public void setGettersReturnOptional(boolean gettersReturnOptional) {
        this.gettersReturnOptional.set(gettersReturnOptional);
    }

    public Property<Boolean> isOptionalGettersForNullableFieldsOnly() {
        return optionalGettersForNullableFieldsOnly;
    }

    public void setOptionalGettersForNullableFieldsOnly(String optionalGettersForNullableFieldsOnly) {
        setOptionalGettersForNullableFieldsOnly(Boolean.parseBoolean(optionalGettersForNullableFieldsOnly));
    }

    public void setOptionalGettersForNullableFieldsOnly(boolean optionalGettersForNullableFieldsOnly) {
        this.optionalGettersForNullableFieldsOnly.set(optionalGettersForNullableFieldsOnly);
    }

    public Property<Boolean> isEnableDecimalLogicalType() {
        return enableDecimalLogicalType;
    }

    public void setEnableDecimalLogicalType(String enableDecimalLogicalType) {
        setEnableDecimalLogicalType(Boolean.parseBoolean(enableDecimalLogicalType));
    }

    public void setEnableDecimalLogicalType(boolean enableDecimalLogicalType) {
        this.enableDecimalLogicalType.set(enableDecimalLogicalType);
    }

    public MapProperty<String, Class<? extends LogicalTypes.LogicalTypeFactory>> getLogicalTypeFactories() {
        return logicalTypeFactories;
    }

    public void setLogicalTypeFactories(Provider<? extends Map<? extends String,
            ? extends Class<? extends LogicalTypes.LogicalTypeFactory>>> provider) {
        this.logicalTypeFactories.set(provider);
    }

    public void setLogicalTypeFactories(Map<? extends String,
            ? extends Class<? extends LogicalTypes.LogicalTypeFactory>> logicalTypeFactories) {
        this.logicalTypeFactories.set(logicalTypeFactories);
    }

    public ListProperty<Class<? extends Conversion<?>>> getCustomConversions() {
        return customConversions;
    }

    public void setCustomConversions(Provider<Iterable<Class<? extends Conversion<?>>>> provider) {
        this.customConversions.set(provider);
    }

    public void setCustomConversions(Iterable<Class<? extends Conversion<?>>> customConversions) {
        this.customConversions.set(customConversions);
    }

    public CodeGeneratorExtension logicalTypeFactory(String typeName, Class<? extends LogicalTypes.LogicalTypeFactory> typeFactoryClass) {
        logicalTypeFactories.put(typeName, typeFactoryClass);
        return this;
    }

    public CodeGeneratorExtension customConversion(Class<? extends Conversion<?>> conversionClass) {
        customConversions.add(conversionClass);
        return this;
    }
}
