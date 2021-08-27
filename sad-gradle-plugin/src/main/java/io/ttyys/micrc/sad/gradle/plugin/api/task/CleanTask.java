package io.ttyys.micrc.sad.gradle.plugin.api.task;

import io.ttyys.micrc.sad.gradle.plugin.api.Constants;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskContainer;

import javax.inject.Inject;

public class CleanTask extends SourceTask {
    @Inject
    public CleanTask() {
    }

    @TaskAction
    public void clean() {
        TaskContainer taskContainer = getProject().getTasks();
        getLogger().debug("====================");
    }

    @Override
    public String getGroup() {
        return Constants.GROUP;
    }
}
