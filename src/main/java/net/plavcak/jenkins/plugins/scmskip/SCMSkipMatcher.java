package net.plavcak.jenkins.plugins.scmskip;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SCMSkipMatcher {

    private Pattern pattern;

    public SCMSkipMatcher(String pattern) {
        setPattern(pattern);
    }

    public SCMSkipMatcher() {
        setPattern(null);
    }

    public boolean match(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        final Matcher matcher = pattern.matcher(message);
        return matcher.matches();
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(String regex) {
        if (regex == null || regex.isEmpty()) {
            regex = SCMSkipConstants.DEFAULT_PATTERN;
        }
        this.pattern = Pattern.compile(regex, Pattern.MULTILINE);
    }
}
