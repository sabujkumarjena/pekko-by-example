package com.allevite.pekkoStream.part1_basics.chapt4_BackpressureBasics.chapt_graphBasics

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.ClosedShape
import org.apache.pekko.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source}
//import GraphDSL.Implicits.*

object GraphBasics extends App:
  given system: ActorSystem = ActorSystem("GraphBasics")
  val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] => // builder = mutable data structure
      import GraphDSL.Implicits.* // brings nice operators into scope
      val in = Source(1 to 10)
//    val out = Sink.ignore
      val out = Sink.foreach(println)

      val bcast = builder.add(Broadcast[Int](2))
      val merge = builder.add(Merge[Int](2))

      val f1, f2, f3, f4 = Flow[Int].map(_ + 10)

      in ~> f1 ~> bcast ~> f2 ~> merge ~> f3 ~> out
      bcast ~> f4 ~> merge
      ClosedShape //shape //FREEZE the builder's shape
    } //static graph
  ) //runnable graph and materialize

  g.run()
