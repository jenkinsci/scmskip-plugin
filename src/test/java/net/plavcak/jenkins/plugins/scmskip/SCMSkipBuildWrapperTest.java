package net.plavcak.jenkins.plugins.scmskip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.BuildWrapperDescriptor;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.FakeChangeLogSCM;
import org.jvnet.hudson.test.JenkinsRule;

public class SCMSkipBuildWrapperTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();

        BuildWrapperDescriptor descriptor = new SCMSkipBuildWrapper.DescriptorImpl();

        project.getBuildWrappersList().add(new SCMSkipBuildWrapper(false, null));

        project = jenkins.configRoundtrip(project);

        jenkins.assertEqualDataBoundBeans(
                new SCMSkipBuildWrapper(false, null),
                project.getBuildWrappersList().get(0));
    }

    private FreeStyleProject createFreestyleProject(boolean delete) throws IOException {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SCMSkipBuildWrapper builder = new SCMSkipBuildWrapper(delete, null);
        project.getBuildWrappersList().add(builder);
        FakeChangeLogSCM fakeScm = new FakeChangeLogSCM();
        fakeScm.addChange().withMsg("Some change [ci skip] in code.");
        project.setScm(fakeScm);
        return project;
    }

    @Test
    public void testFreestyleProject() throws Exception {
        FreeStyleProject project = createFreestyleProject(false);

        FreeStyleBuild build = jenkins.assertBuildStatus(Result.ABORTED, project.scheduleBuild2(0));

        jenkins.assertLogContains("matched on message", build);
    }

    @Test
    public void testFreestyleProjectWithDelete() throws Exception {
        FreeStyleProject project = createFreestyleProject(true);
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.ABORTED, project.scheduleBuild2(0));
        assertEquals("", JenkinsRule.getLog(build), "Should delete log");
    }
}
