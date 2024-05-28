package com.allevite.pekkoStream.part1_basics.chapt2_MaterializingStreams

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.{Sink, Source}

object MaterializingStream extends App:
  given system: ActorSystem = ActorSystem("MaterializingStreams")

  val simpleGraph = Source(1 to 10).to(Sink.foreach(println))
  val simpleMaterializedValue = simpleGraph.run()

  val source = Source(1 to 10)
  val sink = Sink.reduce[Int](_ + _)