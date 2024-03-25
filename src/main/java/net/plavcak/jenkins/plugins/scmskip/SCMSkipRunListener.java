package net.plavcak.jenkins.plugins.scmskip;

import hudson.Extension;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class SCMSkipRunListener extends RunListener<AbstractBuild> {

    private static final Logger LOGGER = Logger.getLogger(SCMSkipRunListener.class.getName());

    @Override
    public void onFinalized(AbstractBuild build) {
        try {
            if (SCMSkipTools.isBuildToDelete((Run) build)) {
                SCMSkipTools.deleteBuild(build);
            }
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "SCM Skip", e);
            }
        }
    }
}
