package org.drools.modelcompiler;

import java.util.ArrayList;
import java.util.List;

import org.drools.model.Model;
import org.drools.model.Rule;
import org.drools.model.Variable;
import org.drools.model.impl.ModelImpl;
import org.drools.modelcompiler.domain.Person;
import org.drools.modelcompiler.domain.Relationship;
import org.drools.modelcompiler.domain.Toy;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.drools.model.DSL.declarationOf;
import static org.drools.model.DSL.execute;
import static org.drools.model.PatternDSL.pattern;
import static org.drools.model.PatternDSL.rule;
import static org.drools.model.PatternDSL.sequence;

public class PatternDSLSequenceTest {

    @Test
    public void sequenceFiresWhenToyThenRelationship() {
        List<String> results = new ArrayList<>();

        Variable<Person>       anchorVar = declarationOf(Person.class);
        Variable<Toy>          toyVar    = declarationOf(Toy.class);
        Variable<Relationship> relVar    = declarationOf(Relationship.class);

        Rule rule = rule("seq-rule").build(
                pattern(anchorVar),
                sequence(
                        pattern(toyVar).expr("toy-filter",
                                t -> t.getName().equals("ball")),
                        pattern(relVar).expr("rel-filter",
                                r -> r.getStart().equals("go"))
                ),
                execute(() -> results.add("fired"))
        );

        Model model = new ModelImpl().addRule(rule);
        KieBase kieBase = KieBaseBuilder.createKieBaseFromModel(model);
        KieSession ksession = kieBase.newKieSession();

        // LHS anchor — activates the sequencer
        ksession.insert(new Person("anchor"));
        ksession.fireAllRules();

        // Step 1, then step 2 — rule should fire
        ksession.insert(new Toy("ball"));
        ksession.insert(new Relationship("go", "done"));
        ksession.fireAllRules();

        assertThat(results).containsExactly("fired");
        ksession.dispose();
    }

    @Test
    public void sequenceDoesNotFireWithoutToy() {
        List<String> results = new ArrayList<>();

        Variable<Person>       anchorVar = declarationOf(Person.class);
        Variable<Toy>          toyVar    = declarationOf(Toy.class);
        Variable<Relationship> relVar    = declarationOf(Relationship.class);

        Rule rule = rule("seq-rule-neg").build(
                pattern(anchorVar),
                sequence(
                        pattern(toyVar).expr("toy-filter",
                                t -> t.getName().equals("ball")),
                        pattern(relVar).expr("rel-filter",
                                r -> r.getStart().equals("go"))
                ),
                execute(() -> results.add("fired"))
        );

        Model model = new ModelImpl().addRule(rule);
        KieBase kieBase = KieBaseBuilder.createKieBaseFromModel(model);
        KieSession ksession = kieBase.newKieSession();

        ksession.insert(new Person("anchor"));
        ksession.fireAllRules();

        // Step 2 arrives without step 1 — rule must NOT fire
        ksession.insert(new Relationship("go", "done"));
        ksession.fireAllRules();

        assertThat(results).isEmpty();
        ksession.dispose();
    }
}
