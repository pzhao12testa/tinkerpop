/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.tinkergraph.structure;

import org.apache.tinkerpop.gremlin.process.computer.Computer;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.Pop;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization.EarlyLimitStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization.PathRetractionStrategy;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.structure.io.graphml.GraphMLIo;
import org.apache.tinkerpop.gremlin.util.TimeUtil;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static org.apache.tinkerpop.gremlin.process.traversal.Operator.sum;
import static org.apache.tinkerpop.gremlin.process.traversal.P.neq;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.*;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class TinkerGraphPlayTest {
    private static final Logger logger = LoggerFactory.getLogger(TinkerGraphPlayTest.class);

    @Test
    @Ignore
    public void testPlay8() throws Exception {
        Graph graph = TinkerFactory.createModern();
        GraphTraversalSource g = graph.traversal();
        System.out.println(g.withSack(1).inject(1).repeat(__.sack((BiFunction)sum).by(__.constant(1))).times(10).emit().math("sin _").by(sack()).toList());
    }

    @Test
    @Ignore
    public void benchmarkStandardTraversals() throws Exception {
        Graph graph = TinkerGraph.open();
        GraphTraversalSource g = graph.traversal();
        graph.io(GraphMLIo.build()).readGraph("data/grateful-dead.xml");
        final List<Supplier<Traversal>> traversals = Arrays.asList(
                () -> g.V().outE().inV().outE().inV().outE().inV(),
                () -> g.V().out().out().out(),
                () -> g.V().out().out().out().path(),
                () -> g.V().repeat(out()).times(2),
                () -> g.V().repeat(out()).times(3),
                () -> g.V().local(out().out().values("name").fold()),
                () -> g.V().out().local(out().out().values("name").fold()),
                () -> g.V().out().map(v -> g.V(v.get()).out().out().values("name").toList())
        );
        traversals.forEach(traversal -> {
            logger.info("\nTESTING: {}", traversal.get());
            for (int i = 0; i < 7; i++) {
                final long t = System.currentTimeMillis();
                traversal.get().iterate();
                System.out.print("   " + (System.currentTimeMillis() - t));
            }
        });
    }

    @Test
    @Ignore
    public void testPlay4() throws Exception {
        Graph graph = TinkerGraph.open();
        graph.io(GraphMLIo.build()).readGraph("/Users/marko/software/tinkerpop/tinkerpop3/data/grateful-dead.xml");
        GraphTraversalSource g = graph.traversal();
        final List<Supplier<Traversal>> traversals = Arrays.asList(
                () -> g.V().has(T.label, "song").out().groupCount().<Vertex>by(t ->
                        g.V(t).choose(r -> g.V(r).has(T.label, "artist").hasNext(),
                                in("writtenBy", "sungBy"),
                                both("followedBy")).values("name").next()).fold(),
                () -> g.V().has(T.label, "song").out().groupCount().<Vertex>by(t ->
                        g.V(t).choose(has(T.label, "artist"),
                                in("writtenBy", "sungBy"),
                                both("followedBy")).values("name").next()).fold(),
                () -> g.V().has(T.label, "song").out().groupCount().by(
                        choose(has(T.label, "artist"),
                                in("writtenBy", "sungBy"),
                                both("followedBy")).values("name")).fold(),
                () -> g.V().has(T.label, "song").both().groupCount().<Vertex>by(t -> g.V(t).both().values("name").next()),
                () -> g.V().has(T.label, "song").both().groupCount().by(both().values("name")));
        traversals.forEach(traversal -> {
            logger.info("\nTESTING: {}", traversal.get());
            for (int i = 0; i < 10; i++) {
                final long t = System.currentTimeMillis();
                traversal.get().iterate();
                //System.out.println(traversal.get().toList());
                System.out.print("   " + (System.currentTimeMillis() - t));
            }
        });
    }

    @Test
    @Ignore
    public void testPlayDK() throws Exception {

        Graph graph = TinkerGraph.open();
        graph.io(GraphMLIo.build()).readGraph("../data/grateful-dead.xml");

        GraphTraversalSource g = graph.traversal();//.withoutStrategies(EarlyLimitStrategy.class);
        g.V().has("name", "Bob_Dylan").in("sungBy").as("a").
                repeat(out().order().by(Order.shuffle).simplePath().from("a")).
                until(out("writtenBy").has("name", "Johnny_Cash")).limit(1).as("b").
                repeat(out().order().by(Order.shuffle).as("c").simplePath().from("b").to("c")).
                until(out("sungBy").has("name", "Grateful_Dead")).limit(1).
                path().from("a").unfold().
                <List<String>>project("song", "artists").
                by("name").
                by(__.coalesce(out("sungBy", "writtenBy").dedup().values("name"), constant("Unknown")).fold()).
                forEachRemaining(System.out::println);
    }

    @Test
    @Ignore
    public void testPlay7() throws Exception {
        /*TinkerGraph graph = TinkerGraph.open();
        graph.createIndex("name",Vertex.class);
        graph.io(GraphMLIo.build()).readGraph("/Users/marko/software/tinkerpop/tinkerpop3/data/grateful-dead.xml");*/
        //System.out.println(g.V().properties().key().groupCount().next());
        TinkerGraph graph = TinkerFactory.createModern();
        GraphTraversalSource g = graph.traversal();
        final List<Supplier<GraphTraversal<?, ?>>> traversals = Arrays.asList(
                () -> g.V().out().as("v").match(
                        __.as("v").outE().count().as("outDegree"),
                        __.as("v").inE().count().as("inDegree")).select("v", "outDegree", "inDegree").by(valueMap()).by().by().local(union(select("v"), select("inDegree", "outDegree")).unfold().fold())
        );

        traversals.forEach(traversal -> {
            logger.info("pre-strategy:  {}", traversal.get());
            logger.info("post-strategy: {}", traversal.get().iterate());
            logger.info(TimeUtil.clockWithResult(50, () -> traversal.get().toList()).toString());
        });
    }

    @Test
    @Ignore
    public void testPlay5() throws Exception {

        TinkerGraph graph = TinkerGraph.open();
        graph.createIndex("name", Vertex.class);
        graph.io(GraphMLIo.build()).readGraph("/Users/marko/software/tinkerpop/tinkerpop3/data/grateful-dead.xml");
        GraphTraversalSource g = graph.traversal();

        final Supplier<Traversal<?, ?>> traversal = () ->
                g.V().match(
                        as("a").has("name", "Garcia"),
                        as("a").in("writtenBy").as("b"),
                        as("b").out("followedBy").as("c"),
                        as("c").out("writtenBy").as("d"),
                        as("d").where(neq("a"))).select("a", "b", "c", "d").by("name");


        logger.info(traversal.get().toString());
        logger.info(traversal.get().iterate().toString());
        traversal.get().forEachRemaining(x -> logger.info(x.toString()));

    }

    @Test
    @Ignore
    public void testPaths() throws Exception {
        TinkerGraph graph = TinkerGraph.open();
        graph.io(GraphMLIo.build()).readGraph("/Users/twilmes/work/repos/scratch/tinkerpop/gremlin-test/src/main/resources/org/apache/tinkerpop/gremlin/structure/io/graphml/grateful-dead.xml");
//        graph = TinkerFactory.createModern();
        GraphTraversalSource g = graph.traversal().withComputer(Computer.compute().workers(1));

        System.out.println(g.V().match(
                __.as("a").in("sungBy").as("b"),
                __.as("a").in("sungBy").as("c"),
                __.as("b").out("writtenBy").as("d"),
                __.as("c").out("writtenBy").as("e"),
                __.as("d").has("name", "George_Harrison"),
                __.as("e").has("name", "Bob_Marley")).select("a").count().next());

//        System.out.println(g.V().out("created").
//                project("a","b").
//                by("name").
//                by(__.in("created").count()).
//                order().by(select("b")).
//                select("a").toList());

//        System.out.println(g.V().as("a").out().where(neq("a")).barrier().out().count().profile().next());
//        System.out.println(g.V().out().as("a").where(out().select("a").values("prop").count().is(gte(1))).out().where(neq("a")).toList());
//        System.out.println(g.V().match(
//                __.as("a").out().as("b"),
//                __.as("b").out().as("c")).select("c").count().profile().next());

    }

    @Test
    @Ignore
    public void testPlay9() throws Exception {
        Graph graph = TinkerGraph.open();
        graph.io(GraphMLIo.build()).readGraph("../data/grateful-dead.xml");

        GraphTraversalSource g = graph.traversal().withComputer(Computer.compute().workers(4)).withStrategies(PathRetractionStrategy.instance());
        GraphTraversalSource h = graph.traversal().withComputer(Computer.compute().workers(4)).withoutStrategies(PathRetractionStrategy.class);

        for (final GraphTraversalSource source : Arrays.asList(g, h)) {
            System.out.println(source.V().match(
                    __.as("a").in("sungBy").as("b"),
                    __.as("a").in("sungBy").as("c"),
                    __.as("b").out("writtenBy").as("d"),
                    __.as("c").out("writtenBy").as("e"),
                    __.as("d").has("name", "George_Harrison"),
                    __.as("e").has("name", "Bob_Marley")).select("a").count().profile().next());
        }
    }

    @Test
    @Ignore
    public void testPlay6() throws Exception {
        final Graph graph = TinkerGraph.open();
        final GraphTraversalSource g = graph.traversal();
        for (int i = 0; i < 1000; i++) {
            graph.addVertex(T.label, "person", T.id, i);
        }
        graph.vertices().forEachRemaining(a -> {
            graph.vertices().forEachRemaining(b -> {
                if (a != b) {
                    a.addEdge("knows", b);
                }
            });
        });
        graph.vertices(50).next().addEdge("uncle", graph.vertices(70).next());
        logger.info(TimeUtil.clockWithResult(500, () -> g.V().match(as("a").out("knows").as("b"), as("a").out("uncle").as("b")).toList()).toString());
    }

    @Test
    @Ignore
    public void testBugs() {
        GraphTraversalSource g = TinkerFactory.createModern().traversal();

        System.out.println(g.V().as("a").both().as("b").dedup("a", "b").by(T.label).select("a", "b").explain());
        System.out.println(g.V().as("a").both().as("b").dedup("a", "b").by(T.label).select("a", "b").toList());

    }
}
