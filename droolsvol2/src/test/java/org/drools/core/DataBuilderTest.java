package org.drools.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class DataBuilderTest {

    @Test
    public void test1() {
        DataBuilder.store("persons").<Person>type().filter(p -> p.age > 100);

        DataBuilder.store("persons").<Person>type()
                   .filter(p -> p.age > 100)
                   .index(Person::age)
                   .filter( p -> p.age > 100);

        DataBuilder.store("persons").<Person>type()
                   .filter(p -> p.age > 100)
                   .index("personIndex", Person::age)
                   .filter( p -> p.age > 100);

    }

    public record Person(String name, int age) {

    }

}
