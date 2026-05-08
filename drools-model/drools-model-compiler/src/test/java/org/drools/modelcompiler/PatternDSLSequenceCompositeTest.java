package org.drools.modelcompiler;

import java.util.ArrayList;
import java.util.List;

import org.drools.model.Model;
import org.drools.model.Rule;
import org.drools.model.Variable;
import org.drools.model.impl.ModelImpl;
import org.drools.modelcompiler.domain.Adult;
import org.drools.modelcompiler.domain.Child;
import org.drools.modelcompiler.domain.Man;
import org.drools.modelcompiler.domain.Person;
import org.drools.modelcompiler.domain.Relationship;
import org.drools.modelcompiler.domain.Toy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.drools.model.DSL.declarationOf;
import static org.drools.model.DSL.execute;
import static org.drools.model.PatternDSL.and;
import static org.drools.model.PatternDSL.nor;
import static org.drools.model.PatternDSL.or;
import static org.drools.model.PatternDSL.pattern;
import static org.drools.model.PatternDSL.rule;
import static org.drools.model.PatternDSL.sequence;

public class PatternDSLSequenceCompositeTest {

    private final Variable<Person>       person       = declarationOf(Person.class);
    private final Variable<Adult>        adultVar     = declarationOf(Adult.class);
    private final Variable<Child>        childVar     = declarationOf(Child.class);
    private final Variable<Man>          manVar       = declarationOf(Man.class);
    private final Variable<Toy>          toy          = declarationOf(Toy.class);
    private final Variable<Relationship> relationship = declarationOf(Relationship.class);

    private KieSession ksession;

    @AfterEach
    public void tearDown() {
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

    @Test
    public void sequenceRejectsNorInsideSequence() {
        Rule r =
                rule("nor-rejected").build(
                        pattern(person),
                        sequence(
                                pattern(person).expr("anchor", p -> p.getName().equals("anchor")),
                                nor(pattern(toy).expr("ball", t -> t.getName().equals("ball"))),
                                pattern(relationship).expr("rel", x -> x.getStart().equals("go"))
                        ),
                        execute(() -> { })
                );

        Model model = new ModelImpl().addRule(r);

        assertThatThrownBy(() -> KieBaseBuilder.createKieBaseFromModel(model))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("ADR 0001");
    }

    @Test
    public void sequenceFiresWithOrOfTwo() {
        // Step 1 is or(toy/ball, man/Toni). Either child is enough to advance.
        final List<String> orResults = new ArrayList<>();

        Rule orRule =
                rule("or-rule").build(
                        pattern(person),
                        sequence(
                                pattern(adultVar).expr("anchor", a -> a.getName().equals("anchor")),
                                or(
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
        final List<String> andResults = new ArrayList<>();

        Rule andRule =
                rule("and-rule").build(
                        pattern(person),
                        sequence(
                                pattern(adultVar).expr("anchor", a -> a.getName().equals("anchor")),
                                and(
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

    @Test
    public void sequenceFiresWithNestedAndInsideOr() {
        // Step 1 is or(toy/ball, and(child/rope, man/Toni)).
        // Either branch suffices: a single ball, or rope + Toni together.
        // Note: Child substituted for a second Toy to keep all sequence patterns
        // of distinct object types (sequencer constraint).
        final List<String> nestedResults = new ArrayList<>();

        Rule nestedRule =
                rule("nested-rule").build(
                        pattern(person),
                        sequence(
                                pattern(adultVar).expr("anchor", a -> a.getName().equals("anchor")),
                                or(
                                        pattern(toy).expr("ball", t -> t.getName().equals("ball")),
                                        and(
                                                pattern(childVar).expr("rope", c -> c.getName().equals("rope")),
                                                pattern(manVar).expr("toni", m -> m.getName().equals("Toni"))
                                        )
                                ),
                                pattern(relationship).expr("rel", r -> r.getStart().equals("go"))
                        ),
                        execute(() -> nestedResults.add("fired"))
                );

        ksession = KieBaseBuilder.createKieBaseFromModel(new ModelImpl().addRule(nestedRule)).newKieSession();

        insertAndFire(new Person("trigger"));
        insertAndFire(new Adult("anchor", 30));
        // The AND branch matches: rope + Toni together, no ball needed.
        insertAndFire(new Child("rope", 8));
        insertAndFire(new Man("Toni", 44));
        insertAndFire(new Relationship("go", "done"));

        assertThat(nestedResults).containsExactly("fired");
    }

    @Test
    public void sequenceDoesNotFireWhenAndPartial() {
        // Same shape as sequenceFiresWithAndOfTwo, but only one of the AND children fires.
        // AND requires every child matched; one match is not enough; step never advances.
        final List<String> andResults = new ArrayList<>();

        Rule andRule =
                rule("and-partial-rule").build(
                        pattern(person),
                        sequence(
                                pattern(adultVar).expr("anchor", a -> a.getName().equals("anchor")),
                                and(
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
        insertAndFire(new Toy("ball"));            // AND child #1 fires; child #2 never does.
        insertAndFire(new Relationship("go", "done"));   // Step 1 still active; relationship is ignored.

        assertThat(andResults).isEmpty();
    }
}
