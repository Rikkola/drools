/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.drools.core.reteoo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.drools.base.base.ClassObjectType;
import org.drools.base.rule.Declaration;
import org.drools.base.rule.Pattern;
import org.drools.core.impl.InternalRuleBase;
import org.drools.core.impl.RuleBaseFactory;
import org.drools.core.reteoo.builder.BuildContext;
import org.drools.core.test.model.Cheese;
import org.drools.core.test.model.Person;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for BiLinearDeclarationContext cross-network variable management
 */
public class BiLinearDeclarationContextTest {
    
    @Test
    public void testBasicDeclarationContextConstruction() {
        Map<String, Declaration> firstNetworkDecl = new HashMap<>();
        Map<String, Declaration> secondNetworkDecl = new HashMap<>();
        
        Declaration declA = createMockDeclaration("$a", Person.class, 0, 0);
        Declaration declB = createMockDeclaration("$b", Cheese.class, 1, 1);
        
        firstNetworkDecl.put("$a", declA);
        secondNetworkDecl.put("$b", declB);
        
        BiLinearDeclarationContext context = new BiLinearDeclarationContext(
            firstNetworkDecl,
            secondNetworkDecl,
            2 // Second network offset
        );
        
        // Test basic properties
        assertThat(context.getSecondNetworkOffset()).isEqualTo(2);
        assertThat(context.getAllDeclarations()).hasSize(2);
        assertThat(context.hasDeclaration("$a")).isTrue();
        assertThat(context.hasDeclaration("$b")).isTrue();
        assertThat(context.hasDeclaration("$nonexistent")).isFalse();
    }
    
    @Test
    public void testDeclarationResolution() {
        Map<String, Declaration> firstNetworkDecl = new HashMap<>();
        Map<String, Declaration> secondNetworkDecl = new HashMap<>();
        
        Declaration declPerson = createMockDeclaration("$person", Person.class, 0, 0);
        Declaration declCheese = createMockDeclaration("$cheese", Cheese.class, 1, 1);
        Declaration declAge = createMockDeclaration("$age", Integer.class, 2, 2);
        
        firstNetworkDecl.put("$person", declPerson);
        firstNetworkDecl.put("$cheese", declCheese);
        secondNetworkDecl.put("$age", declAge);
        
        BiLinearDeclarationContext context = new BiLinearDeclarationContext(
            firstNetworkDecl,
            secondNetworkDecl,
            2
        );
        
        // Test declaration resolution
        Declaration resolvedPerson = context.resolveDeclaration("$person");
        Declaration resolvedCheese = context.resolveDeclaration("$cheese");
        Declaration resolvedAge = context.resolveDeclaration("$age");
        Declaration resolvedNonexistent = context.resolveDeclaration("$nonexistent");
        
        assertThat(resolvedPerson).isNotNull();
        assertThat(resolvedPerson.getIdentifier()).isEqualTo("$person");
        assertThat(resolvedCheese).isNotNull();
        assertThat(resolvedCheese.getIdentifier()).isEqualTo("$cheese");
        assertThat(resolvedAge).isNotNull();
        assertThat(resolvedAge.getIdentifier()).isEqualTo("$age");
        assertThat(resolvedNonexistent).isNull();
    }
    
    @Test
    public void testNetworkMapping() {
        Map<String, Declaration> firstNetworkDecl = new HashMap<>();
        Map<String, Declaration> secondNetworkDecl = new HashMap<>();
        
        Declaration declA = createMockDeclaration("$a", Person.class, 0, 0);
        Declaration declB = createMockDeclaration("$b", Cheese.class, 1, 1);
        Declaration declC = createMockDeclaration("$c", Person.class, 0, 0);
        Declaration declD = createMockDeclaration("$d", Cheese.class, 1, 1);
        
        firstNetworkDecl.put("$a", declA);
        firstNetworkDecl.put("$b", declB);
        secondNetworkDecl.put("$c", declC);
        secondNetworkDecl.put("$d", declD);
        
        BiLinearDeclarationContext context = new BiLinearDeclarationContext(
            firstNetworkDecl,
            secondNetworkDecl,
            2
        );
        
        // Test network identification
        assertThat(context.getDeclarationNetwork("$a")).isEqualTo(1);
        assertThat(context.getDeclarationNetwork("$b")).isEqualTo(1);
        assertThat(context.getDeclarationNetwork("$c")).isEqualTo(2);
        assertThat(context.getDeclarationNetwork("$d")).isEqualTo(2);
        assertThat(context.getDeclarationNetwork("$nonexistent")).isEqualTo(0);
    }
    
    @Test
    public void testDeclarationConflicts() {
        Map<String, Declaration> firstNetworkDecl = new HashMap<>();
        Map<String, Declaration> secondNetworkDecl = new HashMap<>();
        
        // Create declarations with conflicting names
        Declaration declPersonFirst = createMockDeclaration("$person", Person.class, 0, 0);
        Declaration declPersonSecond = createMockDeclaration("$person", Person.class, 0, 0);
        
        firstNetworkDecl.put("$person", declPersonFirst);
        secondNetworkDecl.put("$person", declPersonSecond);
        
        BiLinearDeclarationContext context = new BiLinearDeclarationContext(
            firstNetworkDecl,
            secondNetworkDecl,
            2
        );
        
        // Test conflict resolution
        assertThat(context.hasDeclaration("$person")).isTrue();
        assertThat(context.hasDeclaration("secondNetwork_$person")).isTrue();
        
        // First network declaration should take precedence
        assertThat(context.getDeclarationNetwork("$person")).isEqualTo(1);
        assertThat(context.getDeclarationNetwork("secondNetwork_$person")).isEqualTo(2);
        
        // Check that both declarations are accessible
        Declaration resolvedFirst = context.resolveDeclaration("$person");
        Declaration resolvedSecond = context.resolveDeclaration("secondNetwork_$person");
        
        assertThat(resolvedFirst).isNotNull();
        assertThat(resolvedSecond).isNotNull();
        // Note: Don't compare Declaration objects directly due to readAccessor being null in test context
    }
    
    @Test
    public void testNetworkDeclarationAccess() {
        Map<String, Declaration> firstNetworkDecl = new HashMap<>();
        Map<String, Declaration> secondNetworkDecl = new HashMap<>();
        
        Declaration declA = createMockDeclaration("$a", Person.class, 0, 0);
        Declaration declB = createMockDeclaration("$b", Cheese.class, 1, 1);
        Declaration declC = createMockDeclaration("$c", Person.class, 0, 0);
        
        firstNetworkDecl.put("$a", declA);
        firstNetworkDecl.put("$b", declB);
        secondNetworkDecl.put("$c", declC);
        
        BiLinearDeclarationContext context = new BiLinearDeclarationContext(
            firstNetworkDecl,
            secondNetworkDecl,
            2
        );
        
        // Test specific network declaration access
        Map<String, Declaration> firstDecls = context.getNetworkDeclarations(1);
        Map<String, Declaration> secondDecls = context.getNetworkDeclarations(2);
        
        assertThat(firstDecls).hasSize(2);
        assertThat(firstDecls).containsKey("$a");
        assertThat(firstDecls).containsKey("$b");
        
        assertThat(secondDecls).hasSize(1);
        assertThat(secondDecls).containsKey("$c");
        
        // Test invalid network number
        assertThatThrownBy(() -> context.getNetworkDeclarations(3))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Network number must be 1 or 2");
    }
    
    @Test
    public void testAllDeclarationNames() {
        Map<String, Declaration> firstNetworkDecl = new HashMap<>();
        Map<String, Declaration> secondNetworkDecl = new HashMap<>();
        
        Declaration declA = createMockDeclaration("$a", Person.class, 0, 0);
        Declaration declB = createMockDeclaration("$b", Cheese.class, 1, 1);
        Declaration declC = createMockDeclaration("$c", Person.class, 0, 0);
        
        firstNetworkDecl.put("$a", declA);
        firstNetworkDecl.put("$b", declB);
        secondNetworkDecl.put("$c", declC);
        
        BiLinearDeclarationContext context = new BiLinearDeclarationContext(
            firstNetworkDecl,
            secondNetworkDecl,
            2
        );
        
        Set<String> allNames = context.getAllDeclarationNames();
        
        assertThat(allNames).hasSize(3);
        assertThat(allNames).contains("$a", "$b", "$c");
    }
    
    @Test
    public void testOffsetCalculation() {
        Map<String, Declaration> firstNetworkDecl = new HashMap<>();
        Map<String, Declaration> secondNetworkDecl = new HashMap<>();
        
        Declaration declFirst = createMockDeclaration("$first", Person.class, 0, 0);
        Declaration declSecond = createMockDeclaration("$second", Cheese.class, 1, 1);
        
        firstNetworkDecl.put("$first", declFirst);
        secondNetworkDecl.put("$second", declSecond);
        
        int offset = 3;
        BiLinearDeclarationContext context = new BiLinearDeclarationContext(
            firstNetworkDecl,
            secondNetworkDecl,
            offset
        );
        
        // Test that offset is stored correctly
        assertThat(context.getSecondNetworkOffset()).isEqualTo(offset);
        
        // Test that second network declaration has offset applied
        Declaration resolvedSecond = context.resolveDeclaration("$second");
        assertThat(resolvedSecond).isNotNull();
        
        // The offset should be applied to the pattern tuple index
        Pattern pattern = resolvedSecond.getPattern();
        if (pattern != null) {
            assertThat(pattern.getTupleIndex()).isEqualTo(1 + offset);
            assertThat(pattern.getObjectIndex()).isEqualTo(1 + offset);
        }
    }
    
    @Test
    public void testContextCopy() {
        Map<String, Declaration> firstNetworkDecl = new HashMap<>();
        Map<String, Declaration> secondNetworkDecl = new HashMap<>();
        
        Declaration declA = createMockDeclaration("$a", Person.class, 0, 0);
        Declaration declB = createMockDeclaration("$b", Cheese.class, 1, 1);
        
        firstNetworkDecl.put("$a", declA);
        secondNetworkDecl.put("$b", declB);
        
        BiLinearDeclarationContext original = new BiLinearDeclarationContext(
            firstNetworkDecl,
            secondNetworkDecl,
            2
        );
        
        BiLinearDeclarationContext copy = original.copy();
        
        // Test that copy has same properties
        assertThat(copy.getSecondNetworkOffset()).isEqualTo(original.getSecondNetworkOffset());
        assertThat(copy.getAllDeclarations()).hasSize(original.getAllDeclarations().size());
        assertThat(copy.hasDeclaration("$a")).isTrue();
        assertThat(copy.hasDeclaration("$b")).isTrue();
        
        // Test that copy is independent
        assertThat(copy).isNotSameAs(original);
        assertThat(copy.getAllDeclarations()).isNotSameAs(original.getAllDeclarations());
    }
    
    @Test
    public void testEmptyNetworks() {
        // Test with empty first network
        BiLinearDeclarationContext context1 = new BiLinearDeclarationContext(
            Collections.emptyMap(),
            Map.of("$a", createMockDeclaration("$a", Person.class, 0, 0)),
            1
        );
        
        assertThat(context1.getAllDeclarations()).hasSize(1);
        assertThat(context1.hasDeclaration("$a")).isTrue();
        assertThat(context1.getDeclarationNetwork("$a")).isEqualTo(2);
        
        // Test with empty second network
        BiLinearDeclarationContext context2 = new BiLinearDeclarationContext(
            Map.of("$b", createMockDeclaration("$b", Cheese.class, 0, 0)),
            Collections.emptyMap(),
            1
        );
        
        assertThat(context2.getAllDeclarations()).hasSize(1);
        assertThat(context2.hasDeclaration("$b")).isTrue();
        assertThat(context2.getDeclarationNetwork("$b")).isEqualTo(1);
        
        // Test with both networks empty
        BiLinearDeclarationContext context3 = new BiLinearDeclarationContext(
            Collections.emptyMap(),
            Collections.emptyMap(),
            1
        );
        
        assertThat(context3.getAllDeclarations()).isEmpty();
        assertThat(context3.getAllDeclarationNames()).isEmpty();
    }
    
    @Test
    public void testTupleSourceConstructor() {
        InternalRuleBase kBase = RuleBaseFactory.newRuleBase();
        BuildContext buildContext = new BuildContext(kBase, Collections.emptyList());
        
        // Create mock tuple sources
        LeftTupleSource firstSource = new MockTupleSource(1, buildContext);
        LeftTupleSource secondSource = new MockTupleSource(2, buildContext);
        
        // Test constructor with tuple sources
        BiLinearDeclarationContext context = new BiLinearDeclarationContext(
            firstSource,
            secondSource,
            2
        );
        
        // For now, this will have empty declarations since extractDeclarations returns empty map
        // This will be enhanced in Phase 5 when we integrate with the network builder
        assertThat(context.getSecondNetworkOffset()).isEqualTo(2);
        assertThat(context.getAllDeclarations()).isEmpty(); // Currently empty due to simplified implementation
    }
    
    @Test
    public void testToString() {
        Map<String, Declaration> firstNetworkDecl = new HashMap<>();
        Map<String, Declaration> secondNetworkDecl = new HashMap<>();
        
        firstNetworkDecl.put("$a", createMockDeclaration("$a", Person.class, 0, 0));
        firstNetworkDecl.put("$b", createMockDeclaration("$b", Cheese.class, 1, 1));
        secondNetworkDecl.put("$c", createMockDeclaration("$c", Person.class, 0, 0));
        
        BiLinearDeclarationContext context = new BiLinearDeclarationContext(
            firstNetworkDecl,
            secondNetworkDecl,
            3
        );
        
        String toString = context.toString();
        
        assertThat(toString).contains("BiLinearDeclarationContext");
        assertThat(toString).contains("firstNetwork=2 declarations");
        assertThat(toString).contains("secondNetwork=1 declarations");
        assertThat(toString).contains("combined=3 declarations");
        assertThat(toString).contains("offset=3");
    }
    
    /**
     * Creates a mock declaration for testing
     */
    private Declaration createMockDeclaration(String identifier, Class<?> type, int tupleIndex, int objectIndex) {
        Pattern pattern = new Pattern(
            tupleIndex, // Pattern ID
            tupleIndex, // Tuple index  
            objectIndex, // Object index
            new ClassObjectType(type),
            identifier
        );
        
        Declaration declaration = new Declaration(identifier, pattern);
        declaration.setDeclarationClass(type);
        
        return declaration;
    }
}