/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.dmn.feel.lang.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.CompilationUnit;
import org.kie.dmn.api.feel.runtime.events.FEELEventListener;
import org.kie.dmn.feel.FEEL;
import org.kie.dmn.feel.codegen.feel11.CompiledFEELExpression;
import org.kie.dmn.feel.codegen.feel11.ProcessedExpression;
import org.kie.dmn.feel.codegen.feel11.ProcessedFEELUnit;
import org.kie.dmn.feel.codegen.feel11.ProcessedUnaryTest;
import org.kie.dmn.feel.lang.CompiledExpression;
import org.kie.dmn.feel.lang.CompilerContext;
import org.kie.dmn.feel.lang.EvaluationContext;
import org.kie.dmn.feel.lang.FEELProfile;
import org.kie.dmn.feel.lang.Type;
import org.kie.dmn.feel.parser.feel11.profiles.DoCompileFEELProfile;
import org.kie.dmn.feel.runtime.FEELFunction;
import org.kie.dmn.feel.runtime.UnaryTest;
import org.kie.dmn.feel.util.ClassLoaderUtil;
import org.kie.dmn.model.api.GwtIncompatible;

/**
 * Language runtime entry point
 */
public class FEELImpl
        implements FEEL {

    public FEELImpl() {


    }

    public FEELImpl(List<FEELProfile> profiles) {
// TODO

    }

    @Override
    public CompilerContext newCompilerContext() {
        return null;
    }

    @Override
    public CompiledExpression compile(String expression, CompilerContext ctx) {
        return null;
    }

    @Override
    public CompiledExpression compileUnaryTests(String expression, CompilerContext ctx) {
        return null;
    }

    @Override
    public Object evaluate(String expression) {
        return null;
    }

    @Override
    public Object evaluate(String expression, EvaluationContext ctx) {
        return null;
    }

    @Override
    public Object evaluate(String expression, Map<String, Object> inputVariables) {
        return null;
    }

    @Override
    public Object evaluate(CompiledExpression expression, Map<String, Object> inputVariables) {
        return null;
    }

    @Override
    public Object evaluate(CompiledExpression expr, EvaluationContext ctx) {
        return null;
    }

    @Override
    public List<UnaryTest> evaluateUnaryTests(String expression) {
        return null;
    }

    @Override
    public List<UnaryTest> evaluateUnaryTests(String expression, Map<String, Type> variableTypes) {
        return null;
    }

    @Override
    public void addListener(FEELEventListener listener) {

    }

    @Override
    public void removeListener(FEELEventListener listener) {

    }

    @Override
    public Set<FEELEventListener> getListeners() {
        return null;
    }
}
