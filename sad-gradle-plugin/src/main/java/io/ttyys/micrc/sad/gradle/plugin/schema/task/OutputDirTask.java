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
package io.ttyys.micrc.sad.gradle.plugin.schema.task;

import io.ttyys.micrc.sad.gradle.plugin.schema.Constants;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.SourceTask;

import javax.annotation.Nonnull;
import java.io.File;

import static org.gradle.api.tasks.PathSensitivity.RELATIVE;

public class OutputDirTask extends SourceTask {
    private final DirectoryProperty outputDir;

    public OutputDirTask() {
        this.outputDir = getProject().getObjects().directoryProperty();
    }

    public void setOutputDir(File outputDir) {
        this.outputDir.set(outputDir);
        getOutputs().dir(outputDir);
    }

    @Nonnull
    @PathSensitive(value = RELATIVE)
    public FileTree getSource() {
        return super.getSource();
    }

    @OutputDirectory
    public DirectoryProperty getOutputDir() {
        return outputDir;
    }

    public FileCollection filterSources(Spec<? super File> spec) {
        return getSource().filter(spec);
    }

    @Override
    public String getGroup() {
        return Constants.GROUP;
    }
}
