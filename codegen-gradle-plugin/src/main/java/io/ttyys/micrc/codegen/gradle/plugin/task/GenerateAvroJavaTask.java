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
package io.ttyys.micrc.codegen.gradle.plugin.task;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.ttyys.micrc.codegen.gradle.plugin.common.*;
import io.ttyys.micrc.codegen.gradle.plugin.common.file.FileExtensionSpec;
import io.ttyys.micrc.codegen.gradle.plugin.common.file.FileUtils;
import org.apache.avro.Conversion;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.compiler.specific.SpecificCompiler;
import org.apache.avro.compiler.specific.SpecificCompiler.FieldVisibility;
import org.apache.avro.generic.GenericData.StringType;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.specs.NotSpec;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Task to generate Java source files based on Avro protocol files and Avro schema files using {@link Protocol} and
 * {@link SpecificCompiler}.
 */
@SuppressWarnings("WeakerAccess")
@CacheableTask
public class GenerateAvroJavaTask extends OutputDirTask {
    private static Set<String> SUPPORTED_EXTENSIONS =
            new SetBuilder<String>().add(Constants.PROTOCOL_EXTENSION).add(Constants.SCHEMA_EXTENSION).build();

    private final Property<String> resourceAvroDirPath;
    private final Property<String> outputCharacterEncoding;
    private final Property<String> stringType;
    private final Property<String> fieldVisibility;
    private final Property<String> templateDirectory;
    private final Property<Boolean> createOptionalGetters;
    private final Property<Boolean> gettersReturnOptional;
    private final Property<Boolean> optionalGettersForNullableFieldsOnly;
    private final Property<Boolean> createSetters;
    private final Property<Boolean> enableDecimalLogicalType;
    private final MapProperty<String, Class<? extends LogicalTypes.LogicalTypeFactory>> logicalTypeFactories;
    private final ListProperty<Class<? extends Conversion<?>>> customConversions;

    private final Provider<StringType> stringTypeProvider;
    private final Provider<FieldVisibility> fieldVisibilityProvider;

    private final ProjectLayout projectLayout;
    private final SchemaResolver resolver;

    @Inject
    public GenerateAvroJavaTask(ObjectFactory objects) {
        super();
        this.resourceAvroDirPath = objects.property(String.class).convention("");
        this.outputCharacterEncoding = objects.property(String.class);
        this.stringType = objects.property(String.class).convention(Constants.DEFAULT_STRING_TYPE);
        this.fieldVisibility = objects.property(String.class).convention(Constants.DEFAULT_FIELD_VISIBILITY);
        this.templateDirectory = objects.property(String.class);
        this.createOptionalGetters = objects.property(Boolean.class).convention(Constants.DEFAULT_CREATE_OPTIONAL_GETTERS);
        this.gettersReturnOptional = objects.property(Boolean.class).convention(Constants.DEFAULT_GETTERS_RETURN_OPTIONAL);
        this.optionalGettersForNullableFieldsOnly = objects.property(Boolean.class)
                .convention(Constants.DEFAULT_OPTIONAL_GETTERS_FOR_NULLABLE_FIELDS_ONLY);
        this.createSetters = objects.property(Boolean.class).convention(Constants.DEFAULT_CREATE_SETTERS);
        this.enableDecimalLogicalType = objects.property(Boolean.class).convention(Constants.DEFAULT_ENABLE_DECIMAL_LOGICAL_TYPE);
        this.stringTypeProvider = getStringType()
                .map(input -> Enums.parseCaseInsensitive(Constants.OPTION_STRING_TYPE, StringType.values(), input));
        this.fieldVisibilityProvider = getFieldVisibility()
                .map(input -> Enums.parseCaseInsensitive(Constants.OPTION_FIELD_VISIBILITY, FieldVisibility.values(), input));
        this.logicalTypeFactories = objects.mapProperty(String.class, Constants.LOGICAL_TYPE_FACTORY_TYPE.getConcreteClass())
                .convention(Constants.DEFAULT_LOGICAL_TYPE_FACTORIES);
        this.customConversions =
                objects.listProperty(Constants.CONVERSION_TYPE.getConcreteClass()).convention(Constants.DEFAULT_CUSTOM_CONVERSIONS);
        this.projectLayout = getProject().getLayout();
        this.resolver = new SchemaResolver(projectLayout, getLogger());
    }

    @Input
    public Property<String> getResourceAvroDirPath() {
        return resourceAvroDirPath;
    }

    public void setResourceAvroDirPath(String resourceAvroDirPath) {
        this.resourceAvroDirPath.set(resourceAvroDirPath);
    }

    public void setResourceAvroDirPath(Charset resourceAvroDirPath) {
        setResourceAvroDirPath(resourceAvroDirPath.name());
    }

    @Optional
    @Input
    public Property<String> getOutputCharacterEncoding() {
        return outputCharacterEncoding;
    }

    public void setOutputCharacterEncoding(String outputCharacterEncoding) {
        this.outputCharacterEncoding.set(outputCharacterEncoding);
    }

    public void setOutputCharacterEncoding(Charset outputCharacterEncoding) {
        setOutputCharacterEncoding(outputCharacterEncoding.name());
    }

    @Input
    public Property<String> getStringType() {
        return stringType;
    }

    public void setStringType(StringType stringType) {
        setStringType(stringType.name());
    }

    public void setStringType(String stringType) {
        this.stringType.set(stringType);
    }

    @Input
    public Property<String> getFieldVisibility() {
        return fieldVisibility;
    }

    public void setFieldVisibility(String fieldVisibility) {
        this.fieldVisibility.set(fieldVisibility);
    }

    public void setFieldVisibility(FieldVisibility fieldVisibility) {
        setFieldVisibility(fieldVisibility.name());
    }

    @Optional
    @Input
    public Property<String> getTemplateDirectory() {
        return templateDirectory;
    }

    public void setTemplateDirectory(String templateDirectory) {
        this.templateDirectory.set(templateDirectory);
    }

    public Property<Boolean> isCreateSetters() {
        return createSetters;
    }

    @Input
    public Property<Boolean> getCreateSetters() {
        return createSetters;
    }

    public void setCreateSetters(String createSetters) {
        this.createSetters.set(Boolean.parseBoolean(createSetters));
    }

    public Property<Boolean> isCreateOptionalGetters() {
        return createOptionalGetters;
    }

    @Input
    public Property<Boolean> getCreateOptionalGetters() {
        return createOptionalGetters;
    }

    public void setCreateOptionalGetters(String createOptionalGetters) {
        this.createOptionalGetters.set(Boolean.parseBoolean(createOptionalGetters));
    }

    public Property<Boolean> isGettersReturnOptional() {
        return gettersReturnOptional;
    }

    @Input
    public Property<Boolean> getGettersReturnOptional() {
        return gettersReturnOptional;
    }

    public void setGettersReturnOptional(String gettersReturnOptional) {
        this.gettersReturnOptional.set(Boolean.parseBoolean(gettersReturnOptional));
    }

    public Property<Boolean> isOptionalGettersForNullableFieldsOnly() {
        return optionalGettersForNullableFieldsOnly;
    }

    @Input
    public Property<Boolean> getOptionalGettersForNullableFieldsOnly() {
        return optionalGettersForNullableFieldsOnly;
    }

    public void setOptionalGettersForNullableFieldsOnly(String optionalGettersForNullableFieldsOnly) {
        this.optionalGettersForNullableFieldsOnly.set(Boolean.parseBoolean(optionalGettersForNullableFieldsOnly));
    }

    public Property<Boolean> isEnableDecimalLogicalType() {
        return enableDecimalLogicalType;
    }

    @Input
    public Property<Boolean> getEnableDecimalLogicalType() {
        return enableDecimalLogicalType;
    }

    public void setEnableDecimalLogicalType(String enableDecimalLogicalType) {
        this.enableDecimalLogicalType.set(Boolean.parseBoolean(enableDecimalLogicalType));
    }

    @Optional
    @Input
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

    @Optional
    @Input
    public ListProperty<Class<? extends Conversion<?>>> getCustomConversions() {
        return customConversions;
    }

    public void setCustomConversions(Provider<Iterable<Class<? extends Conversion<?>>>> provider) {
        this.customConversions.set(provider);
    }

    public void setCustomConversions(Iterable<Class<? extends Conversion<?>>> customConversions) {
        this.customConversions.set(customConversions);
    }

    @TaskAction
    protected void process() {
        getLogger().debug("Using outputCharacterEncoding {}", getOutputCharacterEncoding().getOrNull());
        getLogger().debug("Using stringType {}", stringTypeProvider.get().name());
        getLogger().debug("Using fieldVisibility {}", fieldVisibilityProvider.get().name());
        getLogger().debug("Using templateDirectory '{}'", getTemplateDirectory().getOrNull());
        getLogger().debug("Using createSetters {}", isCreateSetters().get());
        getLogger().debug("Using createOptionalGetters {}", isCreateOptionalGetters().get());
        getLogger().debug("Using gettersReturnOptional {}", isGettersReturnOptional().get());
        getLogger().debug("Using optionalGettersForNullableFieldsOnly {}", isOptionalGettersForNullableFieldsOnly().get());
        getLogger().debug("Using enableDecimalLogicalType {}", isEnableDecimalLogicalType().get());
        getLogger().debug("Using logicalTypeFactories {}",
                logicalTypeFactories.get().entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        (Map.Entry<String, Class<? extends LogicalTypes.LogicalTypeFactory>> e) -> e.getValue().getName()
                )));
        getLogger().debug("Using customConversions {}",
                customConversions.get().stream().map(v -> ((Class) v).getName()).collect(Collectors.toList()));
        getLogger().info("Found {} files", getInputs().getSourceFiles().getFiles().size());
        failOnUnsupportedFiles();
        processFiles();
    }

    private void failOnUnsupportedFiles() {
        FileCollection unsupportedFiles = filterSources(new NotSpec<>(new FileExtensionSpec(SUPPORTED_EXTENSIONS)));
        if (!unsupportedFiles.isEmpty()) {
            throw new GradleException(
                    String.format("Unsupported file extension for the following files: %s", unsupportedFiles));
        }
    }

    private void processFiles() {
        registerLogicalTypes();
        int processedFileCount = 0;
        processedFileCount += processProtoFiles();
        processedFileCount += processSchemaFiles();
        setDidWork(processedFileCount > 0);
    }

    private int processProtoFiles() {
        int processedFileCount = 0;
        for (File sourceFile : filterSources(new FileExtensionSpec(Constants.PROTOCOL_EXTENSION))) {
            processProtoFile(sourceFile);
            processedFileCount++;
        }
        return processedFileCount;
    }

    private void processProtoFile(File sourceFile) {
        getLogger().info("Processing {}", sourceFile);
        try {
            Protocol protocol = Protocol.parse(sourceFile);
            JSONObject jsonObject = JSONObject.parseObject(protocol.toString(true));
            dealStructure(jsonObject);
            protocol = Protocol.parse(jsonObject.toJSONString());
            compile(new SpecificCompiler(protocol), sourceFile);
        } catch (IOException ex) {
            throw new GradleException(String.format("Failed to compile protocol definition file %s", sourceFile), ex);
        }
    }

    private static final Pattern interfacePkgReg = Pattern.compile(".*interfacePkg=\"((\\w+.?)+)\".+");
    private static final Pattern objPkgReg = Pattern.compile(".+objPkg=\"((\\w+.?)+)\"\\).*");

    private void dealStructure(JSONObject jsonObject) {
        String javaAnnotationKey = "javaAnnotation";
        String namespaceKey = "namespace";
        String typesKey = "types";
        String typeKey = "type";
        String nameKey = "name";
        String fieldsKey = "fields";
        String messagesKey = "messages";
        String requestKey = "request";
        String responseKey = "response";
        // 获取协议文件注解
        String javaAnnotation = jsonObject.getString(javaAnnotationKey);
        if (javaAnnotation != null) {
            // 匹配/拼接 接口/对象 包配置
            String interfacePkg = getPkg(jsonObject, javaAnnotation, true);
            String objPkg = getPkg(jsonObject, javaAnnotation, false);
            // 设置生成接口的包
            jsonObject.put(namespaceKey, interfacePkg);
            // 设置 出入参对象类 的包
            Map<String, String> typeNameMap = new HashMap<>(0);
            JSONArray typeJsonArray = JSONObject.parseArray(jsonObject.getString(typesKey));
            for (int i = 0, len = typeJsonArray.size(); i < len; i++) {
                JSONObject schemaJson = typeJsonArray.getJSONObject(i);
                schemaJson.put(namespaceKey, objPkg);
                typeNameMap.put((String) schemaJson.get(nameKey), objPkg + Constants.NAMESPACE_SEPARATOR + schemaJson.get(nameKey));
            }
            // FIXME zhaowang 以下所有类型中涉及到List集合包装类型的,需特殊处理
            // 修复对象类中引用其他对象类
            for (int i = 0, len = typeJsonArray.size(); i < len; i++) {
                checkAndSetType(typeNameMap, typeJsonArray.getJSONObject(i).getJSONArray(fieldsKey), typeKey);
            }
            jsonObject.put(typesKey, typeJsonArray);

            // 设置 方法中的出入参数对应的应用类
            JSONObject messageJsonObject = jsonObject.getJSONObject(messagesKey);

            for (String messageName : messageJsonObject.keySet()) {
                JSONObject messageJson = messageJsonObject.getJSONObject(messageName);
                checkAndSetType(typeNameMap, messageJson.getJSONArray(requestKey), typeKey);
                checkAndReplaceValue(typeNameMap, messageJson, responseKey);
            }
            jsonObject.put(messagesKey, messageJsonObject);
        }
    }

    private String getPkg(JSONObject jsonObject, String javaAnnotation, boolean isInterface) {
        String namespaceKey = "namespace";
        Matcher matcher = isInterface ? interfacePkgReg.matcher(javaAnnotation) : objPkgReg.matcher(javaAnnotation);
        String pkg = jsonObject.getString(namespaceKey);
        if (matcher.find()) {
            pkg += AvroUtils.NAMESPACE_SEPARATOR + matcher.group(1);
        }
        return pkg;
    }

    private void checkAndSetType(Map<String, String> typeNameMap, JSONArray jsonArray, String key) {
        for (int j = 0, jLen = jsonArray.size(); j < jLen; j++) {
            JSONObject requestParam = jsonArray.getJSONObject(j);
            checkAndReplaceValue(typeNameMap, requestParam, key);
        }
    }

    private void checkAndReplaceValue(Map<String, String> typeNameMap, JSONObject json, String key) {
        Object type = json.get(key);
        if (type instanceof JSONObject) {
            checkAndReplaceValue(typeNameMap, json.getJSONObject(key), "items");
        } else {
            if (typeNameMap.containsKey(json.getString(key))) {
                json.put(key, typeNameMap.get(json.getString(key)));
            }
        }
    }

    private int processSchemaFiles() {
        Set<File> files = filterSources(new FileExtensionSpec(Constants.SCHEMA_EXTENSION)).getFiles();
        ProcessingState processingState = resolver.resolve(files);
        for (File file : files) {
            String path = FileUtils.projectRelativePath(projectLayout, file);
            for (Schema schema : processingState.getSchemasForLocation(path)) {
                try {
                    compile(new SpecificCompiler(schema), file);
                } catch (IOException ex) {
                    throw new GradleException(String.format("Failed to compile schema definition file %s", path), ex);
                }
            }
        }
        return processingState.getProcessedTotal();
    }

    private void compile(SpecificCompiler compiler, File sourceFile) throws IOException {
        compiler.setOutputCharacterEncoding(getOutputCharacterEncoding().getOrNull());
        compiler.setStringType(stringTypeProvider.get());
        compiler.setFieldVisibility(fieldVisibilityProvider.get());
        if (getTemplateDirectory().isPresent()) {
            compiler.setTemplateDir(getTemplateDirectory().get());
        }
        compiler.setCreateOptionalGetters(createOptionalGetters.get());
        compiler.setGettersReturnOptional(gettersReturnOptional.get());
        compiler.setOptionalGettersForNullableFieldsOnly(optionalGettersForNullableFieldsOnly.get());
        compiler.setCreateSetters(isCreateSetters().get());
        compiler.setEnableDecimalLogicalType(isEnableDecimalLogicalType().get());
        registerCustomConversions(compiler);

        compiler.compileToDestination(sourceFile, getOutputDir().get().getAsFile());
    }

    /**
     * Registers the logical types to be used in this run.
     * This must be called before the Schemas are parsed, or they will not be applied correctly.
     * Since {@link LogicalTypes} is a static registry, this may result in side-effects.
     */
    private void registerLogicalTypes() {
        Map<String, Class<? extends LogicalTypes.LogicalTypeFactory>> logicalTypeFactoryMap = logicalTypeFactories.get();
        Set<Map.Entry<String, Class<? extends LogicalTypes.LogicalTypeFactory>>> logicalTypeFactoryEntries =
                logicalTypeFactoryMap.entrySet();
        for (Map.Entry<String, Class<? extends LogicalTypes.LogicalTypeFactory>> entry : logicalTypeFactoryEntries) {
            String logicalTypeName = entry.getKey();
            Class<? extends LogicalTypes.LogicalTypeFactory> logicalTypeFactoryClass = entry.getValue();
            try {
                LogicalTypes.LogicalTypeFactory logicalTypeFactory = logicalTypeFactoryClass.getDeclaredConstructor().newInstance();
                LogicalTypes.register(logicalTypeName, logicalTypeFactory);
            } catch (ReflectiveOperationException ex) {
                getLogger().error("Could not instantiate logicalTypeFactory class \"" + logicalTypeFactoryClass.getName() + "\"");
            }
        }
    }

    private void registerCustomConversions(SpecificCompiler compiler) {
        customConversions.get().forEach(compiler::addCustomConversion);
    }
}
