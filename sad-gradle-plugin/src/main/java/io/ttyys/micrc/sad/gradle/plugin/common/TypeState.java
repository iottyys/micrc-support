/**
 * Copyright © 2015 Commerce Technologies, LLC.
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

import org.apache.avro.Schema;
import org.gradle.api.GradleException;

import java.util.Set;
import java.util.TreeSet;

public class TypeState {
    private final String name;
    private final Set<String> locations = new TreeSet<>();
    private Schema schema;

    public TypeState(String name) {
        this.name = name;
    }

    public void processTypeDefinition(String path, Schema schemaToProcess) {
        locations.add(path);
        if (this.schema == null) {
            this.schema = schemaToProcess;
        } else if (!this.schema.equals(schemaToProcess)) {
            throw new GradleException(String.format("Found conflicting definition of type %s in %s", name, locations));
        } // Otherwise duplicate declaration of identical schema; nothing to do
    }

    public String getName() {
        return name;
    }

    public Schema getSchema() {
        return schema;
    }

    public boolean hasLocation(String location) {
        return locations.contains(location);
    }
}
