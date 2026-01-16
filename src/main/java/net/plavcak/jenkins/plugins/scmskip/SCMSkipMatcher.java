package net.plavcak.jenkins.plugins.scmskip;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public class SCMSkipMatcher {

    private Pattern pattern;

    public SCMSkipMatcher(String pattern) {
        setPattern(pattern);
    }

    public SCMSkipMatcher() {
        setPattern(null);
    }

    public boolean match(String message) {
        if (StringUtils.isEmpty(message)) {
            return false;
        }
        final Matcher matcher = pattern.matcher(message);
        return matcher.matches();
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(String regex) {
        if (StringUtils.isEmpty(regex)) {
            regex = SCMSkipConstants.DEFAULT_PATTERN;
        }
        this.pattern = Pattern.compile(regex, Pattern.DOTALL);
    }
}
