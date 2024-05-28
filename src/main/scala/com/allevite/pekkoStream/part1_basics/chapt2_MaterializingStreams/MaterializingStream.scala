package com.allevite.pekkoStream.part1_basics.chapt2_MaterializingStreams

import org.apache.pekko.{Done, NotUsed}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.concurrent.Future
import scala.util.{Failure, Success}

//import scala.concurrent.ExecutionContext.Implicits.global

object MaterializingStream extends App:
  given system: ActorSystem = ActorSystem("MaterializingStreams")

  val simpleGraph = Source(1 to 10).to(Sink.foreach(println))
  val simpleMaterializedValue = simpleGraph.run()

  val source = Source(1 to 10)
  val sink = Sink.reduce[Int](_ + _)
  import system.dispatcher

  val sumFuture: Future[Int] = source.runWith(sink)

  sumFuture.onComplete:
    case Success(value) => println(s"The sum of all elements is :$value")
    case Failure(ex) => println(s"The sum of the elements could not be computed: $ex")

  // By default left materialized value in the graph is kept.

  // choosing materialized values
  val simpleSource: Source[Int, NotUsed] = Source(1 to 10)
  val simpleFlow: Flow[Int, Int, NotUsed] = Flow[Int].map(x => x + 1)
  val simpleSink: Sink[Int, Future[Done]] = Sink.foreach[Int](println)
  val graph = simpleSource.viaMat(simpleFlow)((leftMat,rightMat) => rightMat).toMat(simpleSink)((leftMat,rightMat) => rightMat)
  val graph2 = simpleSource.viaMat(simpleFlow)(Keep.right).toMat(simpleSink)(Keep.right)
  //val graph3 = simpleSource.viaMat(simpleFlow)(Keep.both).toMat(simpleSink)(Keep.both)

  graph2.run().onComplete :
    case Success(m) => println(s"Stream processing finished with $m.")
    case Failure(exception) => println(s"Stream processing failed with: $exception")

  val a = Source(1 to 10).runWith(Sink.reduce[Int](_ + _)) // source.to(Sink.reduce)(Keep.right)
  val b = Source(1 to 10).runReduce[Int](_ + _) // same

  // backwards
  Sink.foreach[Int](println).runWith(Source.single(42)) // source(..).to(sink...).run()

  // both ways
  Flow[Int].map(_ * 2).runWith(simpleSource, simpleSink)

  //Exercise1 : return the last element out of a source (use Sink.last)
  val f1 = Source(1 to 10).toMat(Sink.last)(Keep.right).run()
  val f2 = Source(1 to 10).runWith(Sink.last)

  //Exercise2 : Compute total word count of stream of sentences
  val sentenceSource: Source[String, NotUsed] = Source(List(
    "Pekko is awesome",
    "I love streams",
    "Materialized values are different that emitted values"
  ))
  val wordCountSink: Sink[String, Future[Int]] =
    Sink.fold[Int, String](0)((currentWords, newSentence) => currentWords + newSentence.split(" ").length)
  val g1: Future[Int] = sentenceSource.toMat(wordCountSink)(Keep.right).run()
  val g2: Future[Int] = sentenceSource.runWith(wordCountSink)
  val g3 = sentenceSource.runFold(0)((currentWords, newSentence) => currentWords + newSentence.split(" ").length)

  val wordCountFlow: Flow[String, String, NotUsed]#Repr[Int] =
    Flow[String].fold[Int](0)((currentWords, newSentence) => currentWords + newSentence.split(" ").length)
  val g4 = sentenceSource.via(wordCountFlow).toMat(Sink.head)(Keep.right).run()
  val g5 = sentenceSource.viaMat(wordCountFlow)(Keep.left).toMat(Sink.head)(Keep.right).run()
  val g6 = sentenceSource.via(wordCountFlow).runWith(Sink.head)
  val g7 = wordCountFlow.runWith(sentenceSource, Sink.head)._2