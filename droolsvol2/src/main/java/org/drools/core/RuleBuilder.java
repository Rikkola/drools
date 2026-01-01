package org.drools.core;

import org.drools.api.data.DataSource;
import org.drools.core.RuleOOPathBuilder.Path2;
import org.drools.core.RuleOOPathBuilder.Path3;
import org.drools.core.RuleOOPathBuilder.Path4;
import org.drools.core.RuleOOPathBuilder.Path5;
import org.drools.core.RuleOOPathBuilder.Path6;
import org.drools.core.function.Consumer1;
import org.drools.core.function.Consumer2;
import org.drools.core.function.Consumer3;
import org.drools.core.function.Consumer4;
import org.drools.core.function.Function1;
import org.drools.core.function.Function2;
import org.drools.core.function.Predicate2;
import org.drools.core.function.Predicate3;
import org.drools.core.function.Predicate4;
import org.drools.core.function.Predicate5;
import org.drools.core.function.BaseTuple.Tuple1;
import org.drools.core.function.BaseTuple.Tuple6;
import org.drools.core.function.BaseTuple.Tuple5;
import org.drools.core.function.BaseTuple.Tuple4;
import org.drools.core.function.BaseTuple.Tuple3;
import org.drools.core.function.BaseTuple.Tuple2;
import org.kie.api.definition.rule.Rule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RuleBuilder<DS> {

    public RuleBuilder() {

    }

    private String pkgName;

    public ParametersFirst<Void, DS> rule(String name) {
        return new ParametersFirst();
    }

    public <T> From1First<Void, DS, T> from(Function1<DS, DataSource<T>> f) {
        return new From1First<>();
    }

    public <T> From1First<Void, DS, T> from(DataSource<T> fromT) {
        return new From1First<>();
    }

    //public From1First<Object, Object, Object> from(Function1<T, DS> persons) {}

    public static class BaseRuleBuilder<END> {
        private END end;

        public Rule build() {
            return null;
        }

        public END end() {
            return end;
        }

//        <A, B, C> Path3<END,Tuple3<A, B, C>, A, B, C>  path3(Tuple3<A, B, C>... tuple) {
//            return null;
//        }

//        <A, B, C, D> void path4(Tuple4<A, B, C, D>... capture) {
//            //return null;
//        }
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

    public static class ParametersFirst<END, DS> extends BaseRuleBuilder<END> {
        private List<Parameter> list = new ArrayList<Parameter>();

        public <T> ParametersSecond<END, DS, ArgList> param(String name, T... type) {
            return list().param(name, type.getClass().getComponentType().getName());
        }

        public ParametersSecond<END, DS, ArgList> list() {
            return new ParametersSecond<>(list);
        }

        public ParametersSecond<END, DS, ArgMap> map() {
            return new ParametersSecond<>(list);
        }

        public <B> From1First<END, DS, B> params(Class... cls) {
            return new From1First<>();
        }

        public <T> From1First<Void, DS, T> from(Function1<DS, DataSource<T>> f) {
            return new From1First<>();
        }

        public ParametersFirst ifn(Runnable fn0) {
            return this;
        }

        public void fn(Consumer1<Context<DS>> fn1) {

        }
    }

    public static class ParametersSecond<END, DS, B> extends BaseRuleBuilder<END>  {
        private List<Parameter> parameters;

        public ParametersSecond(List<Parameter> list) {
            parameters = list;
        }

        public <T> ParametersSecond<END, DS, B> param(String name, T... cls) {
            return param(name, cls.getClass().getComponentType().getName());
        }

        public ParametersSecond<END, DS, B> param(String name, String type) {
            parameters.add(new Parameter(name, type));
            return this;
        }

        public <C> Join2First<Void, DS, B, C> join(From1First<END, DS, C> fromC) {
            return new Join2First<>();
        }

//        public <A, B> OOPathBuilderA2<ParametersSecond<DS, B>, A, B> path(Function1<A,?> fn1,
//                                                                          Predicate1<B> flt1) {
////            OOPathBuilder1<> a = new OOPathBuilder1<>(AccessType.OBJECT, null, o -> true);
////            return new OOPathBuilderA2<>(new OOPathBuilder2<>(accessType, fn1, flt1, b1));
//            return null;
//        }
    }

    public static class From1First<END, DS, B> extends BaseRuleBuilder<END> {

        public <T extends B> From1First<END, DS, T> type(Class<T>... cls) {
            return (From1First<END, DS, T>)null;
        }

        public From1First<END, DS, B> filter(Predicate2<Context<DS>, B> prd2) {
            return this;
        }

        public <C> Join2First<END, DS, B, C> join(From1First<?, DS, C> fromC) {
            return null;
        }

        public <C> Join2First<END, DS, B, C> not(From1First<Void, DS, C> fromC) {
            return null;
        }

        public <C, D> Join3First<END, DS, B, C, D> join(Join2Second<Void, DS, C, D> fromCD) {
            return null;
        }

        public <C, D, E> Join4First<END, DS, B, C, D, E> join(Join3First<Void, DS, C, D, E> fromCDE) {
            return null;
        }

        public From1First<END, DS, B> ifn(Consumer2<Context<DS>, B> fn2) {
            return this;
        }

        public void fn(Consumer2<Context<DS>, B> fn2) {

        }

        <PB, PC, PD, PE, PF> Path6<Join2First<END, DS, B, Tuple6<B, PB, PC, PD, PE, PF>>, Tuple6<B, PB, PC, PD, PE, PF>, B, PB, PC,PD, PE, PF> path6() {
            return new Path6<>(null, null, null);
        }

        <PB, PC, PD, PE> Path4<Join2First<END, DS, B, Tuple5<B, PB, PC, PD, PE>>, Tuple5<B, PB, PC, PD, PE>, PB, PC,PD, PE> path5(Function2<PathContext<Tuple5<B, PB, PC, PD, PE>>, B,?> fn2,
                                                                                                                                  Predicate2<PathContext<Tuple5<B, PB, PC, PD, PE>>,PB> flt2) {
            Path5<Join2First<END, DS, B, Tuple5<B, PB, PC, PD, PE>>, Tuple5<B, PB, PC, PD, PE>, B, PB, PC,PD, PE> path5 = new Path5<>(null, null, null);
            return path5.path(fn2, flt2);
        }

        <PB, PC, PD> Path4<Join2First<END, DS, B, Tuple3<B, PB, PC>>, Tuple3<B, PB, PC>, B, PB, PC,PD> path4() {
            return new Path4<>(null, null, null);
        }

        <PB, PC> Path3<Join2First<END, DS, B, Tuple3<B, PB, PC>>,Tuple2<B, PB>, B, PB, PC>  path3() {
            return new Path3<>(null, null, null);
        }

        <PB> Path2<Join2First<END, DS, B, Tuple2<B, PB>>,Tuple1<B>, B, PB> path2() {
            return new Path2<>(null, null, null);
        }
    }


    public static class Join2First<END, DS, B, C> extends Join2Second<END, DS, B, C>  {

        public Join2First<END, DS, B, C> filter(Predicate3<Context<DS>, B, C> predicate3) {
            return this;
        }

        public Join2First<END, DS, B, C> index() {
            return this;
        }
    }

    public static class Join2Second<END, DS, B, C>  extends BaseRuleBuilder<END>  {
        public <D> Join2Second<END, DS, B, C> not(From1First<END, DS, D> fromD) {
            return this;
        }

        public Not2<Join2Second<END, DS, B, C>, DS, B, C> not() {
            Not2<Join2Second<END, DS, B, C>, DS, B, C> not = new Not2<>();
            return not;
        }

        public <D> Join3First<END, DS, B, C, D> join(From1First<Void, DS, D> fromD) {
            return null;
        }

        public <D, E> Join4First<END, DS, B, C, D, E> join(Join2First<?, DS, D, E> joinDE) {
            return null;
        }

        public Join2Second<END, DS, B, C> ifn(Consumer3<Context<DS>, B, C> fn3) {
            return this;
        }

        public BaseRuleBuilder<END>  fn(Consumer3<Context<DS>, B, C> fn3) {
            return this;
        }

        <PB, PC, PD, PE, PF> Path6<Join3First<END, DS, B, C, Tuple6<C, PB, PC, PD, PE, PF>>, Tuple6<C, PB, PC, PD, PE, PF>, C, PB, PC,PD, PE, PF> path6() {
            return new Path6<>(null, null, null);
        }

        <PB, PC, PD, PE> Path4<Join3First<END, DS, B, C, Tuple5<C, PB, PC, PD, PE>>, Tuple5<C, PB, PC, PD, PE>, PB, PC,PD, PE> path5(Function2<PathContext<Tuple5<C, PB, PC, PD, PE>>,C,?> fn2,
                                                                                                                           Predicate2<PathContext<Tuple5<C, PB, PC, PD, PE>>,PB> flt2) {
            Path5<Join3First<END, DS, B, C, Tuple5<C, PB, PC, PD, PE>>, Tuple5<C, PB, PC, PD, PE>, C, PB, PC,PD, PE> path5 = new Path5<>(null, null, null);
            return path5.path(fn2, flt2);
        }

        <PB, PC, PD> Path4<Join3First<END, DS, B, C, Tuple4<C, PB, PC, PD>>, Tuple4<C, PB, PC, PD>, C, PB, PC,PD> path4() {
            return new Path4<>(null, null, null);
        }

        <PB, PC> Path3<Join3First<END, DS, B, C, Tuple3<C, PB, PC>>,Tuple3<C, PB, PC>, C, PB, PC>  path3() {
            return new Path3<>(null, null, null);
        }

        <PB> Path2<Join3First<END, DS, B, C, Tuple2<B, PB>>,Tuple2<C, PB>, C, PB> path2() {
            return new Path2<>(null, null, null);
        }

        public END end() {
            return null;
        }
    }

//    public static class Group2First<END, DS, B, C>  extends BaseRuleBuilder   {
//        private Join2First<DS, B, C> join;
//
//        public <D> Join3<DS, B, C, D> join(From1First<DS, D> fromD) {
//            return null;
//        }
//    }

    public static class Not2<END, DS, B, C>  extends Group2<END, DS, B, C>   {

    }

    public static class Group2<END, DS, B, C> extends Join2Second<END, DS, B, C> {
        public END end() {
            return null;
        }

//        public Group2<END, DS, B, C> filter(Predicate3<Context<DS>, B, C> predicate3) {
//            super.filter(predicate3);
//            return this;
//        }

//        public <D> Join2First<DS, B, C> not(From1First<DS, D> fromD) {
//            return this;
//        }
//
//        public <D> Join3<DS, B, C, D> join(From1First<DS, D> fromD) {
//            return null;
//        }
//
//        public <D, E> Join4<DS, B, C, D, E> join(Join2First<DS, D, E> joinDE) {
//            return null;
//        }
//
//        public Join2First<DS, B, C> ifn(Consumer3<Context<DS>, B, C> fn3) {
//            return this;
//        }
//
//        public void fn(Consumer3<Context<DS>, B, C> fn3) {
//
//        }
//
//        <PB, PC, PD, PE, PF> Path6<Join3<DS, B, C, Tuple6<C, PB, PC, PD, PE, PF>>, Tuple6<C, PB, PC, PD, PE, PF>, C, PB, PC,PD, PE, PF> path6() {
//            return new Path6<>(null, null, null);
//        }
//
//        <PB, PC, PD, PE> Path4<Join3<DS, B, C, Tuple5<C, PB, PC, PD, PE>>, Tuple5<C, PB, PC, PD, PE>, PB, PC,PD, PE> path5(Function2<PathContext<Tuple5<C, PB, PC, PD, PE>>,C,?> fn2,
//                                                                                                                           Predicate2<PathContext<Tuple5<C, PB, PC, PD, PE>>,PB> flt2) {
//            Path5<Join3<DS, B, C, Tuple5<C, PB, PC, PD, PE>>, Tuple5<C, PB, PC, PD, PE>, C, PB, PC,PD, PE> path5 = new Path5<>(null, null, null);
//            return path5.path(fn2, flt2);
//        }
//
//        <PB, PC, PD> Path4<Join3<DS, B, C, Tuple4<C, PB, PC, PD>>, Tuple4<C, PB, PC, PD>, C, PB, PC,PD> path4() {
//            return new Path4<>(null, null, null);
//        }
//
//        <PB, PC> Path3<Join3<DS, B, C, Tuple3<C, PB, PC>>,Tuple3<C, PB, PC>, C, PB, PC>  path3() {
//            return new Path3<>(null, null, null);
//        }
//
//        <PB> Path2<Join3<DS, B, C, Tuple2<B, PB>>,Tuple2<C, PB>, C, PB> path2() {
//            return new Path2<>(null, null, null);
//        }
    }

    public static class Join3First <END, DS, B, C, D> extends Join3Second<END, DS, B, C, D> {
        public Join3First<END, DS, B, C, D> filter(Predicate4<Context<DS>, B, C, D> predicate4) {
            return this;
        }

        public Join3First<END, DS, B, C, D> ifn(Consumer4<Context<DS>, B, C, D> fn4) {
            return this;
        }
    }

    public static class Join3Second<END, DS, B, C, D> extends BaseRuleBuilder<END>  {

        public <E> Join4First<END, DS, B, C, D, E> join(From1First<Void, DS, E> fromE) {
            return null;
        }

//        public <E, F> Join5First<END, DS, B, C, D, E, F> join(Join2First<?, DS, D, E> joinDE) {
//            return null;
//        }

        public void fn(Consumer4<Context<DS>, B, C, D> fn4) {

        }

        <PB, PC, PD, PE, PF> Path6<Join2First<END, DS, B, Tuple6<B, PB, PC, PD, PE, PF>>, Tuple6<B, PB, PC, PD, PE, PF>, B, PB, PC,PD, PE, PF> path6() {
            return new Path6<>(null, null, null);
        }

        <PB, PC, PD, PE> Path4<Join2First<END, DS, B, Tuple5<B, PB, PC, PD, PE>>, Tuple4<B, PB, PC, PD>, PB, PC,PD, PE> path5(Function2<PathContext<Tuple4<B, PB, PC, PD>>,B,?> fn2,
                                                                                                                             Predicate2<PathContext<Tuple4<B, PB, PC, PD>>,PB> flt2) {
            Path5<Join2First<END, DS, B, Tuple5<B, PB, PC, PD, PE>>, Tuple4<B, PB, PC, PD>, B, PB, PC,PD, PE> path5 = new Path5<>(null, null, null);
            return path5.path(fn2, flt2);
        }

        <PB, PC, PD> Path4<Join2First<END, DS, B, Tuple4<B, PB, PC, PD>>, Tuple4<B, PB, PC, PD>, B, PB, PC,PD> path4() {
            return new Path4<>(null, null, null);
        }

        <PB, PC> Path3<Join2First<END, DS, B, Tuple3<B, PB, PC>>,Tuple3<B, PB, PC>, B, PB, PC>  path3() {
            return new Path3<>(null, null, null);
        }

        <PB> Path2<Join2First<END, DS, B, Tuple2<B, PB>>,Tuple2<B, PB>, B, PB> path2() {
            return new Path2<>(null, null, null);
        }
    }

    public static class Join4First<END, DS, B, C, D, E> extends Join4Second<END, DS, B, C, D, E> {
        public Join4First<END, DS, B, C, D, E> filter(Predicate5<Context<DS>, B, C, D, E> predicate5) {
            return this;
        }
    }

    public static class Join4Second<END, DS, B, C, D, E> extends BaseRuleBuilder<END> {
        <PB, PC, PD, PE, PF> Path6<Join2First<END, DS, B, Tuple6<B, PB, PC, PD, PE, PF>>, Tuple6<B, PB, PC, PD, PE, PF>, B, PB, PC,PD, PE, PF> path6() {
            return new Path6<>(null, null, null);
        }

        <PB, PC, PD, PE> Path4<Join2First<END, DS, B, Tuple5<B, PB, PC, PD, PE>>, Tuple5<B, PB, PC, PD, PE>, PB, PC,PD, PE> path5(Function2<PathContext<Tuple5<B, PB, PC, PD, PE>>,B,?> fn2,
                                                                                                                            Predicate2<PathContext<Tuple5<B, PB, PC, PD, PE>>,PB> flt2) {
            Path5<Join2First<END, DS, B, Tuple5<B, PB, PC, PD, PE>>, Tuple5<B, PB, PC, PD, PE>, B, PB, PC,PD, PE> path5 = new Path5<>(null, null, null);
            return path5.path(fn2, flt2);
        }

        <PB, PC, PD> Path4<Join2First<END, DS, B, Tuple4<B, PB, PC, PD>>, Tuple4<B, PB, PC, PD>, B, PB, PC,PD> path4() {
            return new Path4<>(null, null, null);
        }

        <PB, PC> Path3<Join2First<END, DS, B, Tuple3<B, PB, PC>>,Tuple3<B, PB, PC>, B, PB, PC>  path3() {
            return new Path3<>(null, null, null);
        }

        <PB> Path2<Join2First<END, DS, B, Tuple2<B, PB>>,Tuple2<B, PB>, B, PB> path2() {
            return new Path2<>(null, null, null);
        }
    }

    public static class Terminal extends BaseRuleBuilder  {

    }

}
