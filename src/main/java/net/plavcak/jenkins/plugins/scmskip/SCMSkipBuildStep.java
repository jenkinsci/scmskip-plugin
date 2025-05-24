package net.plavcak.jenkins.plugins.scmskip;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest2;

public class SCMSkipBuildStep extends Builder implements SimpleBuildStep {

    private static final Logger LOGGER = Logger.getLogger(SCMSkipBuildStep.class.getName());

    private SCMSkipMatcher skipMatcher;
    private boolean deleteBuild;
    private String skipPattern;

    public SCMSkipBuildStep(boolean deleteBuild, String skipPattern) {
        this.deleteBuild = deleteBuild;
        this.skipPattern = skipPattern;
        if (this.skipPattern == null) {
            this.skipPattern = SCMSkipConstants.DEFAULT_PATTERN;
        }
        this.skipMatcher = new SCMSkipMatcher(getSkipPattern());
    }

    @DataBoundConstructor
    public SCMSkipBuildStep() {
        this(false, null);
    }

    public boolean isDeleteBuild() {
        return deleteBuild;
    }

    @DataBoundSetter
    public void setDeleteBuild(boolean deleteBuild) {
        this.deleteBuild = deleteBuild;
    }

    public String getSkipPattern() {
        if (this.skipPattern == null) {
            return getDescriptor().getSkipPattern();
        }
        return skipPattern;
    }

    @DataBoundSetter
    public void setSkipPattern(String skipPattern) {
        this.skipPattern = skipPattern;
        this.skipMatcher.setPattern(this.skipPattern);
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull EnvVars env, @NonNull TaskListener listener)
            throws IOException, FlowInterruptedException {
        if (SCMSkipTools.inspectChangeSetAndCause(run, skipMatcher, listener)) {
            SCMSkipTools.tagRunForDeletion(run, deleteBuild);

            try {
                SCMSkipTools.stopBuild(run);
            } catch (AbortException | FlowInterruptedException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "SCM Skip Build Step", e);
            }
        } else {
            SCMSkipTools.tagRunForDeletion(run, false);
        }
    }

    @Override
    public boolean requiresWorkspace() {
        return false;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Symbol("scmSkip")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        private String skipPattern = SCMSkipConstants.DEFAULT_PATTERN;

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return false;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "SCM Skip Step";
        }

        @Override
        public boolean configure(StaplerRequest2 req, JSONObject json) {
            req.bindJSON(this, json.getJSONObject("scmSkip"));
            save();
            return true;
        }

        public String getSkipPattern() {
            return skipPattern;
        }

        @DataBoundSetter
        public void setSkipPattern(String skipPattern) {
            this.skipPattern = skipPattern;
        }
    }
}
