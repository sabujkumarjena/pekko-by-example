package com.allevite.pekkoStream.part1_basics.chapt1_Introduction

import org.apache.pekko
import org.apache.pekko.{Done, NotUsed}
import org.apache.pekko.actor.ActorSystem
import pekko.stream.*
import pekko.stream.scaladsl.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Introduction extends App :
  given system: ActorSystem = ActorSystem("Introduction")

//  Source.single("Allevite").to(Sink.foreach(println))
//  Source.single("Allevite").runForeach(i => println(i))

  //sources //Source[+Out, +Mat]
  val source: Source[Int, NotUsed] = Source(1 to 10)

  //sinks // Sink[-In, +Mat]
  val sink: Sink[Int, Future[Done]] = Sink.foreach[Int](println)

  val graph: RunnableGraph[NotUsed] = source.to(sink)
//  graph.run()

  //flows - transform elements // Flow[-In, +Out, +Mat]
  val flow: Flow[Int, Int, NotUsed] = Flow[Int].map(_ + 1)

  val sourceWithFlow = source.via(flow)
  val flowWithSink: Sink[Int, NotUsed] = flow.to(sink)

//  sourceWithFlow.to(sink).run()
//  source.to(flowWithSink).run()
//  source.via(flow).to(sink).run()

  //nulls are NOT allowed
//  val ilegalSource = Source.single[String](null)
//  ilegalSource.to(Sink.foreach(println)) // throws exception
//Use Option instead of null

  // various kind of sources
  val finiteSource: Source[Int, NotUsed] = Source.single(1)
  val anotherFiniteSource: Source[Int, NotUsed] = Source(List(1, 2, 3))
  val emptySource: Source[Int, NotUsed] = Source.empty[Int]
  val infiniteSource: Source[Int, NotUsed] = Source(LazyList.from(1))
  val futureSource = Source.future(Future(42)) // This source emit value when the future completes

  // sinks
  val ignoringSink = Sink.ignore
  val foreachSink = Sink.foreach[String](println)
  val headSink = Sink.head[Int] // retrieves head and then closes the stream
  val foldSink = Sink.fold[Int, Int](0)(_+_)

  // flows - usually mapped to collection operators
  val mapFlow = Flow[Int].map(2 * _)
  val takeFlow = Flow[Int].take(5)
  val aggFlow = Flow[Int].fold(0)(_ + _)
// drop, filter
// NOT have flatMap

  //source.via(aggFlow).to(sink).run()

  // source -> flow -> flow -> ... -> sink
  val doubleFlowGraph = source.via(mapFlow).via(takeFlow).to(sink)
  //  doubleFlowGraph.run()

  // syntactic sugars
  val mapSource = Source(1 to 10).map(_* 2) // Source(1 to 10).via(Flow[Int].map(_ * 2))

  // run streams directly
  //  mapSource.runForeach(println) // mapSource.to(Sink.foreach[Int](println)).run()

  // OPERATORS = components

  /**
   * Exercise: create a stream that takes the names of persons, then you will keep the first 2 names with length > 5 characters.
   *
   */
  val names = List("Alice", "Bob", "Charlie", "David", "Martin", "AkkaStreams")
  val nameSource: Source[String, NotUsed] = Source(names)
  val longNameFlow = Flow[String].filter(_.length > 5)
  val limitFlow = Flow[String].take(2)
  val nameSink = Sink.foreach[String](println)

  nameSource.via(longNameFlow).via(limitFlow).to(nameSink).run()
  
//nameSource.filter(_.length > 5).take(2).runForeach(println)


