package org.kie.dmn.validation.dtanalysis.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DDTARule {

    private List<DDTAInputEntry> inputEntry = new ArrayList<>();

    public List<DDTAInputEntry> getInputEntry() {
        return inputEntry;
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DDTARule inputEntries > ");
        builder.append(inputEntry.stream().map(DDTAInputEntry::toString).collect(Collectors.joining(" | ")));
        return builder.toString();
    }

}