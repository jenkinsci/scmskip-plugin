package net.plavcak.jenkins.plugins.scmskip;

import hudson.plugins.git.GitChangeSet;
import hudson.scm.ChangeLogSet;

public class GitMessageExtractor {
    public static String getFullMessage(ChangeLogSet.Entry entry) {
        if (entry instanceof GitChangeSet) {
            return ((GitChangeSet) entry).getComment();
        } else {
            return entry.getMsg();
        }
    }
}
