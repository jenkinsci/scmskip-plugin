package net.plavcak.jenkins.plugins.scmskip;

import hudson.Extension;
import hudson.model.listeners.RunListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

@Extension
public class SCMSkipWorkflowRunListener extends RunListener<WorkflowRun> {

    private static final Logger LOGGER = Logger.getLogger(SCMSkipWorkflowRunListener.class.getName());

    @Override
    public void onFinalized(WorkflowRun workflowRun) {

        try {
            if (SCMSkipTools.isBuildToDelete(workflowRun)) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Deleting build: " + workflowRun.getId());
                }
                SCMSkipTools.deleteRun(workflowRun);
            }
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "SCM Skip Run Listener", e);
            }
        }
    }
}
