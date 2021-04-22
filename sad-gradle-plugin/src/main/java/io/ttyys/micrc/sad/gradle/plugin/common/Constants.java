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
package io.ttyys.micrc.sad.gradle.plugin.common;

/**
 * Various constants needed by the plugin.
 *
 * <p>The default values from {@code avro-compiler} aren't exposed in a way that's easily accessible, so even default
 * values that we want to match are still reproduced here.</p>
 */
public class Constants {
    public static final String UTF8_ENCODING = "UTF-8";

    public static final String PROTOCOL_EXTENSION = "avpr";
    public static final String IDL_EXTENSION = "avdl";

    public static final String GROUP_SOURCE_GENERATION = "Protocol Generation";

    public static final String SCHEMA_DESIGN_EXTENSION_NAME = "codegen-config";

    public static final String PROTOCOL_SOURCE_PATH_KEY = "program";
    public static final String PROTOCOL_DEST_PATH_KEY = "feature";
}
