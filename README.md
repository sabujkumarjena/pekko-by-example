 ## [Reactive Stream](http://reactive-streams.org) ##
 Specification of async, back pressured stream
 
Concepts:
- publisher = emits elements (asynchronously)
- subscriber = receive elements
- processor = transform the elements along the way
- async
- back pressure

Reactive Stream is a SPI ( Service Provider Interface), not an API

## Pekko Stream ##

Source = "publisher"
- emits elements asynchronously
-  may or may not terminate

Sink = "subscriber"
- receives elements
- terminates only when the publisher terminates

Flow = "processor"
- transform elements

We build streams by connecting components

Directions
- upstream = to the source
- downstream = to the sink

Materialising
- Components are static until they run
```scala
val graph = source.via(flow).to(sink)

val result = graph.run()
```
- result is the materialized value
- A graph is a "blueprint" for a stream
Running a graph allocates the right resources
- actors, thread pools
- sockets, connections
- etc - everything is transparent
 Running a graph = materializing

Matrializing a graph = materializing all components

- each component produces a materialised value when run
- each graph produces a single materialized value
- we have to choose one

A materialized value can be ANYTHING.

A component can materialize multiple times

## Operator Fusion ##

- stream components running on the same actor
- async boundaries between stream components

An sync boundary contains
- everything from the previous boundary (if any)
- everything between the previous boundary and this boundary

Communication based on actor messages

### Summary ###

Akka Streams components are fused = run on the same actor

#### Async boundaries ####

```scala 
val graph = source.async
        .via(Flow[Int].map(_+1)).async
        .via(...).async
        .to(Sink.foreach(println))
        
```
- components run on different actors
- Best when : individual operations are expensive
- Avoid when: operations are comparable with a message pass
- Ordering guarantee  of individual element through the graph across async boundaries

## Backpressure ##
- Elements flow as response to demand from consumers
- Fast consumers: all is well
- Slow consumer: problem
- if the consumer (sink) slows down it signals immediate previous component to slow down and the process may repeat till source.
- default buffer in pekko stream is 16 elements.

Reactions to backpressure (in order)
- try to slow down if possible
- buffer elements util there is more demand
- drop down elements from the buffer if it overflows
- tear down/kill the whole stream (failure)

### Summary ###
- Data flows through streams in response to demand
- Pekko Streams can slow down fast producers
- Back pressure protocol is transparent

```scala
aFlow.buffer(size = 10, overflowStrategy = OverflowStrategy.dropHead)
```
## Graphs ##
- The Graph DSL
- Non-linear components
- - fan-out
- - fan-in
- Fan-out components
- - Broadcast
- - Balance
- Fan-in components
- - zip/ZipWith
- - Merge
- - Concat
- uniform/non-uniform

## Fault Tolerance ##

Agenda
- React to failures in streams
- Logging
```scala
aStream.log("tag")
```
- Recovering
```scala
aStream.recover {
 case ex: RuntimeException => 42
}

aStream.recoverWithRetries(3,  {
 case ex: RuntimeException => 42
})

```
- Backoff supervision
```scala
RestartSource.onFailureWithBackoff(
 minBackoff = 1.second,
 maxBackoff = 10.seconds,
 randomFactor = 0.2,
 maxRestarts = 3
)(createAnotherSourceInCaseOfFailure)

```
- Supervision strategies
```scala
aStream.withAttributes(ActorAttributes.supervisionStrategy(decider))
```