package net.plavcak.jenkins.plugins.scmskip;

import hudson.Extension;
import hudson.model.listeners.RunListener;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class SCMSkipWorkflowRunListener extends RunListener<WorkflowRun> {

    private static final Logger LOGGER = Logger.getLogger(SCMSkipWorkflowRunListener.class.getName());

    @Override
    public void onFinalized(WorkflowRun workflowRun) {

        try {
            if (SCMSkipTools.isBuildToDelete(workflowRun)) {
                LOGGER.info("Deleting build...");
                SCMSkipTools.deleteRun(workflowRun);
            }
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "SCM Skip Run Listener", e);
            }
        }
    }
}
