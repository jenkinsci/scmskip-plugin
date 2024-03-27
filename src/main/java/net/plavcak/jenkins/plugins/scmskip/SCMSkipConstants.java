package net.plavcak.jenkins.plugins.scmskip;

public final class SCMSkipConstants {
    private SCMSkipConstants() {
        // utility class
    }

    public static final String DELETE_BUILD = "BUILD_SCM_SKIP_DELETE_BUILD";
    public static final String DEFAULT_PATTERN = ".*\\[ci skip\\].*";
}
