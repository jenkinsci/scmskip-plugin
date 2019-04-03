package net.plavcak.jenkins.plugins.scmskip;

import hudson.*;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {

        if(SCMSkipTools.inspectChangeSet(run, skipMatcher, listener)) {
            SCMSkipTools.setRunToDelete(run, deleteBuild);

            try {
                SCMSkipTools.stopBuild(run);
            } catch (AbortException e) {
                throw e;
            } catch (Exception e) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "SCM Skip Build Step", e);
                }
            }
        } else {
            SCMSkipTools.setRunToDelete(run, false);
        }
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

        @Override
        public String getDisplayName() {
            return "SCM Skip Step";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
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
