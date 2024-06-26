package com.allevite.pekkoStream.part3_techniques.chapt_FaultTolerance

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.ActorAttributes
import org.apache.pekko.stream.Supervision.{Resume, Stop}
import org.apache.pekko.stream.scaladsl.{RestartSource, Sink, Source}

import scala.concurrent.duration.*
import scala.util.Random

object FaultTolerance extends App:
  given system: ActorSystem = ActorSystem("FaultTolerance")

  // 1 - logging
  val faultySource: Source[Int, NotUsed] = Source(1 to 10).map(e => if (e == 6) throw new RuntimeException else e)
  faultySource.log("trackingElements")
//    .runForeach(println)
//    .to(Sink.ignore).run()

  // 2 - gracefully terminating a stream
  faultySource.recover {
    case _ : RuntimeException => Int.MinValue
  }.log("gracefulSource")
//    .to(Sink.ignore).run()
//    .runForeach(println)

  // 3 - recover with another stream
  faultySource.recoverWithRetries(3, {
    case _ : RuntimeException => Source(90 to 99)
  }) .log("recoverWithRetries")
//    .runForeach(println)

    // 4 - backoff supervision
  val restartSource = RestartSource.onFailuresWithBackoff(
    minBackoff = 1.second,
    maxBackoff = 10.seconds,
    randomFactor = 0.2,
    maxRestarts = 3
  )(() => {
    val randomNumber = new Random().nextInt(20)
    Source(1 to 10).map(elem => if elem == randomNumber then throw new RuntimeException else elem)
  })

  restartSource
    .log("restartBackoff")
//    .runForeach(println)

  // 5 - supervision strategy
  val numbers: Source[Int, NotUsed] = Source(1 to 20).map(n => if (n == 13) throw new RuntimeException("bad luck") else n).log("supervision")
  val supervisedNumbers = numbers.withAttributes(ActorAttributes.supervisionStrategy {
    /*
     Resume = skips the faulty element
     Stop = stop the stream
     Restart = resume + clears internal state
    */
    case _: RuntimeException => Resume
    case _ => Stop
  })

  supervisedNumbers.runForeach(println)