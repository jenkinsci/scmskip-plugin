package net.plavcak.jenkins.plugins.scmskip;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.SCMRevisionState;
import org.apache.commons.io.IOUtils;
import org.jvnet.hudson.test.FakeChangeLogSCM;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SCMSkipFakeSCM extends FakeChangeLogSCM {
    private final String path;
    private final byte[] contents;

    /**
     * Changes to be reported in the next build.
     */
    private List<EntryImpl> entries = new ArrayList<>();

    public SCMSkipFakeSCM(String path, URL resource) throws IOException {
        this.path = path;
        this.contents = IOUtils.toByteArray(resource.openStream());
    }

    public EntryImpl addChange() {
        EntryImpl e = new EntryImpl();
        entries.add(e);
        return e;
    }

    @Override
    public void checkout(Run<?, ?> build, Launcher launcher, FilePath remoteDir, TaskListener listener, File changeLogFile, SCMRevisionState baseline) throws IOException, InterruptedException {
        new FilePath(changeLogFile).touch(0);
        build.addAction(new ChangelogAction(entries, changeLogFile.getName()));
        entries = new ArrayList<>();

        listener.getLogger().println("Staging "+path);
        OutputStream os = remoteDir.child(path).write();
        IOUtils.write(contents, os);
        os.close();
    }
}
