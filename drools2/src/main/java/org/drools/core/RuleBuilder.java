package org.drools.core;

import org.drools.api.data.DataSource;
import org.drools.core.PathNode.RootPathNode;
import org.drools.core.RuleOOPathBuilder.OOPathBuilder1;
import org.drools.core.RuleOOPathBuilder.OOPathBuilder2;
import org.drools.core.RuleOOPathBuilder.OOPathBuilder3;
import org.drools.core.RuleOOPathBuilder.OOPathBuilderA1;
import org.drools.core.RuleOOPathBuilder.OOPathBuilderA2;
import org.drools.core.function.Consumer1;
import org.drools.core.function.Consumer2;
import org.drools.core.function.Consumer3;
import org.drools.core.function.Consumer4;
import org.drools.core.function.Function1;
import org.drools.core.function.Function2;
import org.drools.core.function.Predicate1;
import org.drools.core.function.Predicate2;
import org.drools.core.function.Predicate3;
import org.drools.core.function.Predicate4;
import org.drools.core.function.Predicate5;
import org.drools.core.function.Tuple.Tuple1;
import org.drools.core.function.Tuple.Tuple2;
import org.drools.core.function.Tuple.Tuple3;
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
            return list().param(name, type.getClass().getComponentType().getName());
        }

        public ParametersBuilder2<DS, ArgList> list() {
            return new ParametersBuilder2<>(list);
        }

        public ParametersBuilder2<DS, ArgMap> map() {
            return new ParametersBuilder2<>(list);
        }

        public <B> From1Builder<DS, B> params(Class... cls) {
            return new From1Builder<>();
        }

        public <B> From1Builder<DS, B> from(DataSource<B> b) {
            return new From1Builder<>();
        }

        public ParametersBuilder ifn(Runnable fn0) {
            return this;
        }

        public void fn(Consumer1<Context<DS>> fn1) {

        }
    }

    public static class ParametersBuilder2<DS, B> extends RuleBuilder  {
        private List<Parameter> parameters;

        public ParametersBuilder2(List<Parameter> list) {
            parameters = list;
        }

        public <T> ParametersBuilder2<DS, B> param(String name, T... cls) {
            return param(name, cls.getClass().getComponentType().getName());
        }

        public ParametersBuilder2<DS, B> param(String name, String type) {
            parameters.add(new Parameter(name, type));
            return this;
        }

        public <C> Join2<DS, B, C> join(From1Builder<DS, C> fromC) {
            return new Join2<>();
        }

        public <A, B> OOPathBuilderA2<A, B> path(AccessType accessType,
                                              Function1<A,?> fn1,
                                              Predicate1<B> flt1) {
//            OOPathBuilder1<> a = new OOPathBuilder1<>(AccessType.OBJECT, null, o -> true);
//            return new OOPathBuilderA2<>(new OOPathBuilder2<>(accessType, fn1, flt1, b1));
            return null;
        }
    }

    public static class From1Builder<DS, B>  extends BaseRuleBuilder {
        public From1Builder<DS, B> filter(Predicate2<Context<DS>, B> prd2) {
            return this;
        }

        public <C> Join2<DS, B, C> join(From1Builder<DS, C> fromC) {
            return null;
        }

        public <C, D> Join3<DS, B, C, D> join(Join2<DS, C, D> fromCD) {
            return null;
        }

        public <C, D, E> Join4<DS, B, C, D, E> join(Join3<DS, C, D, E> fromCDE) {
            return null;
        }

        public From1Builder<DS, B> ifn(Consumer2<Context<DS>, B> fn2) {
            return this;
        }

        public void fn(Consumer2<Context<DS>, B> fn2) {

        }

        public <C> OOPathBuilderA2<PathContext, B> path(Function2<PathContext, B, ?> fn2,
                                                        Predicate2<PathContext, C> flt2) {
            OOPathBuilderA1 root =  new OOPathBuilderA1<>(new OOPathBuilder1<>(AccessType.OBJECT, null, r -> true));

            //RootPathNode<PathContext, Tuple1<PathContext>> root = new RootPathNode<>(r -> true);

            //OOPathBuilder2<Context<DS>, B, Tuple2<Context<DS>, B>> a = new OOPathBuilder2<>(AccessType.OBJECT, null, o -> true);

//            OOPathBuilder3<PathContext, B, C> a = new OOPathBuilder3<>(AccessType.LIST, fn2, flt2, null);
            return null;
        }

    }

    public static class Join2<DS, B, C>  extends BaseRuleBuilder  {

        public Join2<DS, B, C> filter(Predicate3<Context<DS>, B, C> predicate3) {
            return this;
        }

        public <D> Join3<DS, B, C, D> join(From1Builder<DS, D> fromD) {
            return null;
        }

        public <D, E> Join4<DS, B, C, D, E> join(Join2<DS, D, E> joinDE) {
            return null;
        }

        public Join2<DS, B, C> ifn(Consumer3<Context<DS>, B, C> fn3) {
            return this;
        }

        public void fn(Consumer3<Context<DS>, B, C> fn3) {

        }

    }

    public static class Join3<DS, B, C, D> extends BaseRuleBuilder  {
        public Join3<DS, B, C, D> filter(Predicate4<Context<DS>, B, C, D> predicate4) {
            return this;
        }

        public Join3<DS, B, C, D> ifn(Consumer4<Context<DS>, B, C, D> fn4) {
            return this;
        }

        public void fn(Consumer4<Context<DS>, B, C, D> fn4) {

        }
    }

    public static class Join4<DS, B, C, D, E> {
        public Join4<DS, B, C, D, E> filter(Predicate5<Context<DS>, B, C, D, E> predicate5) {
            return this;
        }
    }

    public static class Terminal extends BaseRuleBuilder  {

    }

}
