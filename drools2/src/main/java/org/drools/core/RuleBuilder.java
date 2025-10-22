package org.drools.core;

import org.drools.api.data.DataSource;
import org.drools.core.function.Consumer2;
import org.drools.core.function.Consumer3;
import org.drools.core.function.Consumer4;
import org.drools.core.function.Predicate2;
import org.drools.core.function.Predicate3;
import org.drools.core.function.Predicate4;
import org.drools.core.function.Predicate5;
import org.kie.api.definition.rule.Rule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RuleBuilder<DS> {

    public RuleBuilder() {

    }

    private String pkgName;

    public ParametersBuilder<DS> rule(String name) {
        return new ParametersBuilder();
    }

    public <T> From1Builder<DS, T> from(DataSource<T> fromT) {
        return new From1Builder<>();
    }



    public static class BaseRuleBuilder {
        public Rule build() {
            return null;
        }
    }

    public record Parameter(String name, String type) {

    }

    public static class ArgList {
        private List<Object>       list = new ArrayList<>();

        public Object get(int index) {
            return list.get(index);
        }
    }

    public static class ArgMap {
        private Map<String, Object>       map = new HashMap<>();

        public Object get(String index) {
            return map.get(index);
        }
    }

    public static class ParametersBuilder<DS> extends BaseRuleBuilder {
        private List<Parameter> list = new ArrayList<Parameter>();

        public <T> ParametersBuilder2<DS, ArgList> param(String name, T... type) {
            return param(name, type.getClass().getComponentType().getName());
        }

        public ParametersBuilder2<DS, ArgList> list() {
            return new ParametersBuilder2<>(list);
        }

        public ParametersBuilder2<DS, ArgMap> map() {
            return new ParametersBuilder2<>(list);
        }

        public <A> From1Builder<DS, A> params(Class... cls) {
            return new From1Builder<>();
        }

        public <A> From1Builder<DS, A> from(DataSource<A> a) {
            return new From1Builder<>();
        }

        public ParametersBuilder ifn(Runnable fn0) {
            return this;
        }

        public void fn(Runnable fn0) {

        }
    }

    public static class ParametersBuilder2<DS, A> extends RuleBuilder  {
        private List<Parameter> parameters;

        public ParametersBuilder2(List<Parameter> list) {
            parameters = list;
        }

        public ParametersBuilder2<DS, A> param(String name, Class... cls) {
            return param(name, cls.getClass().getComponentType().getName());
        }

        public ParametersBuilder2<DS, A> param(String name, String type) {
            parameters.add(new Parameter(name, type));
            return this;
        }

        public <B> Join2<DS, A, B> join(From1Builder<DS, B> fromB) {
            return new Join2<>();
        }
    }

    public static class From1Builder<DS, A>  extends BaseRuleBuilder {
        public From1Builder<DS, A> filter(Predicate2<Context<DS>, A> predicate2) {
            return this;
        }

        public <B> Join2<DS, A, B> join(From1Builder<DS, B> fromB) {
            return null;
        }

        public <B, C> Join3<DS, A, B, C> join(Join2<DS, B, C> fromBC) {
            return null;
        }

        public <B, C, D> Join4<DS, A, B, C, D> join(Join3<DS, B, C, D> fromBCD) {
            return null;
        }

        public From1Builder<DS, A> ifn(Consumer2<Context<DS>, A> fn2) {
            return this;
        }

        public void fn(Consumer2<Context<DS>, A> fn2) {

        }
    }

    public static class Join2<DS, A, B>  extends BaseRuleBuilder  {

        public Join2<DS, A, B> filter(Predicate3<Context<DS>, A, B> predicate3) {
            return this;
        }

        public <C> Join3<DS, A, B, C> join(From1Builder<DS, C> fromC) {
            return null;
        }

        public <B, C, D> Join4<DS, A, B, C, D> join(Join3<DS, B, C, D> fromBCD) {
            return null;
        }

        public Join2<DS, A, B> ifn(Consumer3<Context<DS>, A, B> fn3) {
            return this;
        }

        public void fn(Consumer3<Context<DS>, A, B> fn3) {

        }

    }

    public static class Join3<DS, A, B, C> extends BaseRuleBuilder  {
        public Join3<DS, A, B, C> filter(Predicate4<Context<DS>, A, B, C> predicate4) {
            return this;
        }

        public Join3<DS, A, B, C> ifn(Consumer4<Context<DS>, A, B, C> fn4) {
            return this;
        }

        public void fn(Consumer4<Context<DS>, A, B, C> fn4) {

        }
    }

    public static class Join4<DS, A, B, C, D> {
        public Join4<DS, A, B, C, D> filter(Predicate5<Context<DS>, A, B, C, D> predicate5) {
            return this;
        }
    }

    public static class Terminal extends BaseRuleBuilder  {

    }

}
