package net.plavcak.jenkins.plugins.scmskip;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.FakeChangeLogSCM;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;

public class SCMSkipBuildStepTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject project = createFreestyleProject(null);

        project = jenkins.configRoundtrip(project);

        jenkins.assertEqualDataBoundBeans(
                new SCMSkipBuildStep(),
                project.getBuilders().get(0));
    }

    @Test
    public void testBuildWithoutChangelog() throws Exception {
        FreeStyleProject project = createFreestyleProject(null);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        jenkins.assertLogContains("SCM Skip: Changelog is empty!", build);
    }

    @Test
    public void testBuildWithChangelog() throws Exception {
        FreeStyleProject project = createFreestyleProject(null);

        FakeChangeLogSCM fakeScm = new FakeChangeLogSCM();
        fakeScm.addChange().withMsg("Some change [ci skip] in code.");
        project.setScm(fakeScm);

        FreeStyleBuild build = jenkins.assertBuildStatus(Result.ABORTED, project.scheduleBuild2(0));

        Assert.assertEquals("SCM Skip - build skipped", build.getDescription());
        jenkins.assertLogContains("SCM Skip: Pattern .*\\[ci skip\\].* matched on message: Some change [ci skip] in code.", build);
    }

    @Test
    public void testBuildWithCustomPattern() throws Exception {
        FreeStyleProject project = createFreestyleProject(".*\\[(ci skip|skip ci)\\].*");

        FakeChangeLogSCM fakeScm = new FakeChangeLogSCM();
        fakeScm.addChange().withMsg("Some change [skip ci] in code.");
        project.setScm(fakeScm);

        FreeStyleBuild build = jenkins.assertBuildStatus(Result.ABORTED, project.scheduleBuild2(0));

        Assert.assertEquals("SCM Skip - build skipped", build.getDescription());
        jenkins.assertLogContains("SCM Skip: Pattern .*\\[(ci skip|skip ci)\\].* matched on message: Some change [skip ci] in code.", build);
    }

    @Test
    public void testBuildWithMultiChangelog() throws Exception {
        FreeStyleProject project = createFreestyleProject(null);

        FakeChangeLogSCM fakeScm = new FakeChangeLogSCM();
        fakeScm.addChange().withMsg("Some change [ci skip] in code.");
        fakeScm.addChange().withMsg("Another change.");
        project.setScm(fakeScm);

        FreeStyleBuild build = jenkins.assertBuildStatus(Result.ABORTED, project.scheduleBuild2(0));
        jenkins.assertLogContains("SCM Skip: Pattern .*\\[ci skip\\].* matched on message: "
            + "Some change [ci skip] in code. Another change.", build);
    }

    @Test
    public void testBuildWithGlobalConfiguration() {

        SCMSkipBuildStep.DescriptorImpl descriptor = jenkins.getInstance().getDescriptorByType(SCMSkipBuildStep.DescriptorImpl.class);

        Assert.assertEquals(SCMSkipConstants.DEFAULT_PATTERN, descriptor.getSkipPattern());

        descriptor.setSkipPattern(".*\\[skip-ci\\].*");

        descriptor = jenkins.getInstance().getDescriptorByType(SCMSkipBuildStep.DescriptorImpl.class);

        Assert.assertEquals(".*\\[skip-ci\\].*", descriptor.getSkipPattern());
    }

    private FreeStyleProject createFreestyleProject(String skipPattern) throws IOException {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SCMSkipBuildStep builder = new SCMSkipBuildStep();

        if (skipPattern != null) {
            builder.setSkipPattern(skipPattern);
        }

        project.getBuildersList().add(builder);

        return project;
    }
}
