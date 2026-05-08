package org.drools.modelcompiler;

import java.util.ArrayList;
import java.util.List;

import org.drools.model.Model;
import org.drools.model.Rule;
import org.drools.model.Variable;
import org.drools.model.impl.ModelImpl;
import org.drools.modelcompiler.domain.Adult;
import org.drools.modelcompiler.domain.Man;
import org.drools.modelcompiler.domain.Person;
import org.drools.modelcompiler.domain.Relationship;
import org.drools.modelcompiler.domain.Toy;
import org.junit.jupiter.api.AfterEach;
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

    private final Variable<Person>       person = declarationOf(Person.class);
    private final Variable<Toy>          toy = declarationOf(Toy.class);
    private final Variable<Relationship> relationship = declarationOf(Relationship.class);
    private final List<String>           results = new ArrayList<>();
    private KieSession                   ksession;

    private final Rule rule =
            rule("seq-rule").build(
                pattern(person),
                sequence(
                    pattern(toy).expr("toy-filter",
                            t -> t.getName().equals("ball")),
                    pattern(relationship).expr("rel-filter",
                            r -> r.getStart().equals("go"))
                ),
                execute(() -> results.add("fired"))
    );

    @Test
    public void sequenceFiresWhenToyThenRelationship() {
        ksession = makeKSession();

        // LHS anchor — activates the sequencer
        insertAndFire(
           new Person("anchor")
        );

        // Step 1, then step 2 — rule should fire
        insertAndFire(
           new Toy("ball"),
           new Relationship("go", "done")
        );

        assertThat(results).containsExactly("fired");
    }

    @Test
    public void sequenceFiresWithSeparateInserts() {
        ksession = makeKSession();

        // LHS anchor — activates the sequencer
        insertAndFire(
                new Person("anchor")
        );

        // Step 1, then fireAll() — rule should NOT fire
        insertAndFire(
                new Toy("ball")
        );

        assertThat(results).isEmpty();

        // Then step 2 — rule should fire
        insertAndFire(
                new Relationship("go", "done")
        );

        assertThat(results).containsExactly("fired");
    }

    @Test
    public void sequenceDoesNotFireWithoutCorrectOrder() {
        ksession = makeKSession();

        // LHS anchor — activates the sequencer
        insertAndFire(
           new Person("anchor")
        );

        // Step 1, then step 2 — sequence expects Toy then Relationship. We provide Relationship then Toy
        insertAndFire(
            new Relationship("go", "done"),
            new Toy("ball"));

        assertThat(results).isEmpty();
    }

    @Test
    public void sequenceDoesNotFireWithoutToy() {
        ksession = makeKSession();

        insertAndFire(
            new Person("anchor")
        );

        // Step 2 arrives without step 1 — rule must NOT fire
        insertAndFire(
            new Relationship("go", "done")
        );

        assertThat(results).isEmpty();
    }

    @AfterEach
    public void tearDown() {
        results.clear();
        if (ksession != null) {
            ksession.dispose();
        }
    }

    private void insertAndFire(Object... facts) {
        for (Object fact : facts) {
            ksession.insert(fact);
        }
        ksession.fireAllRules();
    }

    private KieSession makeKSession() {
        final Model model = new ModelImpl().addRule(rule);
        final KieBase kieBase = KieBaseBuilder.createKieBaseFromModel(model);
        return kieBase.newKieSession();
    }

    @Test
    public void norFactoryProducesNorType() {
        org.drools.model.view.CombinedExprViewItem item =
                org.drools.model.PatternDSL.nor(pattern(person));
        assertThat(item.getType()).isEqualTo(org.drools.model.Condition.Type.NOR);
        assertThat(item.getExpressions()).hasSize(1);
    }

    @Test
    public void norFactoryAcceptsMultipleArgs() {
        org.drools.model.view.CombinedExprViewItem item =
                org.drools.model.PatternDSL.nor(pattern(person), pattern(toy));
        assertThat(item.getType()).isEqualTo(org.drools.model.Condition.Type.NOR);
        assertThat(item.getExpressions()).hasSize(2);
    }

    @Test
    public void nandFactoryRequiresTwoArgs() {
        org.drools.model.view.CombinedExprViewItem item =
                org.drools.model.PatternDSL.nand(pattern(person), pattern(toy));
        assertThat(item.getType()).isEqualTo(org.drools.model.Condition.Type.NAND);
        assertThat(item.getExpressions()).hasSize(2);
    }

    @Test
    public void xorFactoryRequiresTwoArgs() {
        org.drools.model.view.CombinedExprViewItem item =
                org.drools.model.PatternDSL.xor(pattern(person), pattern(toy));
        assertThat(item.getType()).isEqualTo(org.drools.model.Condition.Type.XOR);
        assertThat(item.getExpressions()).hasSize(2);
    }

    @Test
    public void xnorFactoryRequiresTwoArgs() {
        org.drools.model.view.CombinedExprViewItem item =
                org.drools.model.PatternDSL.xnor(pattern(person), pattern(toy));
        assertThat(item.getType()).isEqualTo(org.drools.model.Condition.Type.XNOR);
        assertThat(item.getExpressions()).hasSize(2);
    }

    @Test
    public void sequenceRejectsNorInsideSequence() {
        org.drools.model.Rule r =
                rule("nor-rejected").build(
                        pattern(person),
                        sequence(
                                pattern(person).expr("anchor", p -> p.getName().equals("anchor")),
                                org.drools.model.PatternDSL.nor(
                                        pattern(toy).expr("ball", t -> t.getName().equals("ball"))
                                ),
                                pattern(relationship).expr("rel", x -> x.getStart().equals("go"))
                        ),
                        execute(() -> { })
                );

        org.drools.model.Model model = new ModelImpl().addRule(r);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> KieBaseBuilder.createKieBaseFromModel(model)
        ).isInstanceOf(UnsupportedOperationException.class)
         .hasMessageContaining("ADR 0001");
    }

    @Test
    public void sequenceFiresWithOrOfTwo() {
        // Step 1 is or(toy/ball, man/Toni). Either child is enough to advance.
        final java.util.List<String> orResults = new java.util.ArrayList<>();
        final org.drools.model.Variable<Adult> adultVar = declarationOf(Adult.class);
        final org.drools.model.Variable<Man> manVar = declarationOf(Man.class);

        org.drools.model.Rule orRule =
                rule("or-rule").build(
                        pattern(person),
                        sequence(
                                pattern(adultVar).expr("anchor", a -> a.getName().equals("anchor")),
                                org.drools.model.PatternDSL.or(
                                        pattern(toy).expr("ball", t -> t.getName().equals("ball")),
                                        pattern(manVar).expr("toni", m -> m.getName().equals("Toni"))
                                ),
                                pattern(relationship).expr("rel", r -> r.getStart().equals("go"))
                        ),
                        execute(() -> orResults.add("fired"))
                );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(orRule)).newKieSession();

        insertAndFire(new Person("trigger"));
        insertAndFire(new Adult("anchor", 30));     // step 0 anchor
        insertAndFire(new Toy("ball"));             // OR child #1 fires → step advances
        insertAndFire(new Relationship("go", "done"));

        assertThat(orResults).containsExactly("fired");
    }

    @Test
    public void sequenceFiresWithAndOfTwo() {
        // Step 1 is and(toy/ball, man/Toni). Both children must match before step advances.
        final java.util.List<String> andResults = new java.util.ArrayList<>();
        final org.drools.model.Variable<Adult> adultVar = declarationOf(Adult.class);
        final org.drools.model.Variable<Man> manVar = declarationOf(Man.class);

        org.drools.model.Rule andRule =
                rule("and-rule").build(
                        pattern(person),
                        sequence(
                                pattern(adultVar).expr("anchor", a -> a.getName().equals("anchor")),
                                org.drools.model.PatternDSL.and(
                                        pattern(toy).expr("ball", t -> t.getName().equals("ball")),
                                        pattern(manVar).expr("toni", m -> m.getName().equals("Toni"))
                                ),
                                pattern(relationship).expr("rel", r -> r.getStart().equals("go"))
                        ),
                        execute(() -> andResults.add("fired"))
                );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(andRule)).newKieSession();

        insertAndFire(new Person("trigger"));
        insertAndFire(new Adult("anchor", 30));
        insertAndFire(new Toy("ball"));            // AND child #1 fires → step still waiting
        insertAndFire(new Man("Toni", 44));        // AND child #2 fires → step advances
        insertAndFire(new Relationship("go", "done"));

        assertThat(andResults).containsExactly("fired");
    }
}
