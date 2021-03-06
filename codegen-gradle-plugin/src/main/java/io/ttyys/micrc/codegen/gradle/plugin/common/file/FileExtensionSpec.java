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
package io.ttyys.micrc.codegen.gradle.plugin.common.file;

import org.gradle.api.specs.Spec;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class FileExtensionSpec implements Spec<File> {
    private final Set<String> extensions;

    public FileExtensionSpec(String... extensions) {
        this.extensions = new HashSet<>(Arrays.asList(extensions));
    }

    public FileExtensionSpec(Collection<String> extensions) {
        this.extensions = new HashSet<>(extensions);
    }

    @Override
    public boolean isSatisfiedBy(File file) {
        return extensions.contains(FilenameUtils.getExtension(file.getName()));
    }
}
