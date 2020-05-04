package org.kie.dmn.feel.lang.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kie.dmn.model.api.GwtIncompatible;

@GwtIncompatible
public class FunctionParserImpl
        implements FunctionParser {

    private static final Pattern METHOD_PARSER = Pattern.compile("(.+)\\((.*)\\)");
    private static final Pattern PARAMETER_PARSER = Pattern.compile("([^, ]+)");

    // TODO used to be static
    @Override
    public String[] parseMethod(String signature) {
        Matcher m = METHOD_PARSER.matcher(signature);
        if (m.matches()) {
            String[] result = new String[2];
            result[0] = m.group(1);
            result[1] = m.group(2);
            return result;
        }
        return null;
    }

    // TODO used to be static
    @Override
    public String[] parseParams(String params) {
        List<String> ps = new ArrayList<>();
        if (params.trim().length() > 0) {
            Matcher m = PARAMETER_PARSER.matcher(params.trim());
            while (m.find()) {
                ps.add(m.group().trim());
            }
        }
        return ps.toArray(new String[ps.size()]);
    }
}
