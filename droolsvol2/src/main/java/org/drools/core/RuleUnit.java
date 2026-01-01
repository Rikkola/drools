package org.drools.core;

import org.drools.api.data.DataSource;
import org.kie.api.definition.rule.Rule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RuleUnit {
    private List<DataSource> dataSources;

    private Map<String, Rule> rulesMap = new HashMap<>();
    private List<Rule>        rulesList = new ArrayList<>();



}
