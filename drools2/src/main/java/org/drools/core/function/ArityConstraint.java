package org.drools.core.function;

import org.drools.api.data.DataHandle;
import org.drools.base.rule.Declaration;
import org.drools.base.rule.constraint.Constraint;
import org.drools.core.Tuple;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ArityConstraint implements Constraint {
    private Declaration[] requiredDeclarations;
    private boolean temporal;

    Predicate predicate;
    Predicate1 predicate1;
    Predicate2 predicate2;
    Predicate3 predicate3;

    public boolean test(Tuple tuple) {
        switch (predicate.getArity()) {
            case 3:
                Tuple ary2 = tuple.getParent();
                return predicate3.test(ary2.getHandle(), ary2.getParent().getHandle(), tuple.getHandle());
            case 2:
                return predicate2.test(tuple.getParent().getHandle(), tuple.getHandle());
            case 1:
                return predicate1.test(tuple.getHandle());
        }
        throw new IllegalStateException("Arity of " + predicate.getArity() + " is not supported");
    }

    public void setPredicate(Predicate predicate ) {
        this.predicate = predicate;
        switch (predicate.getArity()) {
            case 3:
                predicate3 = (Predicate3) predicate;
            case 2:
                predicate2 = (Predicate2) predicate;
            case 1:
                predicate1 = (Predicate1) predicate;
        }
    }

    @Override
    public Declaration[] getRequiredDeclarations() {
        return requiredDeclarations;
    }

    @Override
    public void replaceDeclaration(Declaration oldDecl, Declaration newDecl) {
        for (int i = 0; i < requiredDeclarations.length; i++) {
            if (requiredDeclarations[i] == oldDecl) {
                requiredDeclarations[i] = newDecl;
            }
        }
    }

    @Override
    public Constraint clone() {
        return null;
    }

    @Override
    public ConstraintType getType() {
        return null;
    }

    @Override
    public boolean isTemporal() {
        return temporal;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {

    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

    }
}
