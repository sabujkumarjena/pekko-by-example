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
## Testing Streams ##

Unit testing Pekko Streams
- final results
- integrating with test actors
- Streams TestKit


# Design Principles behind Apache Pekko Streams
Pekko Streams does not send dropped stream elements to the dead letter office

One important consequence of offering only features that can be relied upon is the restriction that Pekko Streams cannot ensure that all objects sent through a processing topology will be processed. Elements can be dropped for a number of reasons:
- plain user code can consume one element in a map(…) operator and produce an entirely different one as its result
- common stream operators drop elements intentionally, e.g. take/drop/filter/conflate/buffer/…
- stream failure will tear down the stream without waiting for processing to finish, all elements that are in flight will be discarded
- stream cancellation will propagate upstream (e.g. from a take operator) leading to upstream processing steps being terminated without having processed all of their inputs

This means that sending JVM objects into a stream that need to be cleaned up will require the user to ensure that this happens outside of the Pekko Streams facilities (e.g. by cleaning them up after a timeout or when their results are observed on the stream output, or by using other means like finalizers etc.).

# What shall users of streaming libraries expect?
- libraries shall provide their users with reusable pieces, i.e. expose factories that return operators, allowing full compositionality
-  libraries may optionally and additionally provide facilities that consume and materialize operators

The reasoning behind the first rule is that compositionality would be destroyed if different libraries only accepted operators and expected to materialize them: using two of these together would be impossible because materialization can only happen once. As a consequence, the functionality of a library must be expressed such that materialization can be done by the user, outside of the library’s control.

# Resulting Implementation Constraints

Pekko Streams must enable a library to express any stream processing utility in terms of immutable blueprints. The most common building blocks are

- **Source**: something with exactly one output stream
- **Sink**: something with exactly one input stream
- **Flow**: something with exactly one input and output stream
- **BidiFlow**: something with exactly two input and two output streams
- **Graph**: a packaged stream processing topology that exposes a certain set of input and output ports, characterized by an object of type **Shape**

# The semantics of stream recovery¶
A recovery element (i.e. any transformation that absorbs an **onError** signal and turns that into possibly more data elements followed normal stream completion) acts as a bulkhead that confines a stream collapse to a given region of the stream topology. Within the collapsed region buffered elements may be lost, but the outside is not affected by the failure.

This works in the same fashion as a **try–catch** expression: it marks a region in which exceptions are caught, but the exact amount of code that was skipped within this region in case of a failure might not be known precisely—the placement of statements matters.

# Fast Publisher, slow Subscriber

This is the case when back-pressuring the **Publisher** is required, because the **Subscriber** is not able to cope with the rate at which its upstream would like to emit data elements.

Since the **Publisher** is not allowed to signal more elements than the pending demand signalled by the **Subscriber**, it will have to abide to this back-pressure by applying one of the below strategies:

- not generate elements, if it is able to control their production rate,
- try buffering the elements in a **bounded** manner until more demand is signalled,
- drop elements until more demand is signalled,
- tear down the stream if unable to apply any of the above strategies.

As we can see, this scenario effectively means that the **Subscriber** will pull the elements from the Publisher – this mode of operation is referred to as pull-based back-pressure.

# Actor Materializer Lifecycle 

# Constructing Graphs 

Pekko Streams currently provide these junctions:

##  Fan-out
- Broadcast[T] – (1 input, N outputs) given an input element emits to each output
- Balance[T] – (1 input, N outputs) given an input element emits to one of its output ports
- Partition[T]] – (1 input, N outputs) given an input element emits to specified output based on a partition function
- UnzipWith[In,A,B,...] – (1 input, N outputs) takes a function of 1 input that given a value for each input emits N output elements (where N <= 20)
- UnZip[A,B] – (1 input, 2 outputs) splits a stream of (A,B) tuples into two streams, one of type A and one of type B
## Fan-in
- Merge[In] – (N inputs , 1 output) picks randomly from inputs pushing them one by one to its output
- MergePreferred[In] – like Merge but if elements are available on preferred port, it picks from it, otherwise randomly from others
- MergePrioritized[In] – like Merge but if elements are available on all input ports, it picks from them randomly based on their priority
- MergeLatest[In] – (N inputs, 1 output) emits List[In], when i-th input stream emits element, then i-th element in emitted list is updated
- MergeSequence[In] – (N inputs, 1 output) emits List[In], where the input streams must represent a partitioned sequence that must be merged back together in order
- ZipWith[A,B,...,Out] – (N inputs, 1 output) which takes a function of N inputs that given a value for each input emits 1 output element
- Zip[A,B] – (2 inputs, 1 output) is a ZipWith specialised to zipping input streams of A and B into a (A,B) tuple stream
- Concat[A] – (2 inputs, 1 output) concatenates two streams (first consume one, then the second one)

# Context Propagation
It can be convenient to attach metadata to each element in the stream.

For example, when reading from an external data source it can be useful to keep track of the read offset, so it can be marked as processed when the element reaches the **Sink**.

For this use case we provide the **SourceWithContext** and **FlowWithContext** variations on **Source** and **Flow**.

Essentially, a **FlowWithContext** is just a **Flow** that contains tuples of element and context, but the advantage is in the operators: most operators on **FlowWithContext** will work on 
the element rather than on the tuple, allowing you to focus on your application logic rather without worrying about the context.

```scala
/**
 * A flow that provides operations which automatically propagate the context of an element. 
 * Only a subset of common operations from FlowOps is supported. 
 * As an escape hatch you can use FlowWithContextOps.via to manually provide the context propagation 
 * for otherwise unsupported operations.
 
 * An "empty" flow can be created by calling FlowWithContext[Ctx, T].
 */
final class FlowWithContext[-In, -CtxIn, +Out, +CtxOut, +Mat]
object FlowWithContext:
  // Creates an "empty" FlowWithContext that passes elements through with their context unchanged.
  def apply[In, Ctx]: FlowWithContext[In, Ctx, In, Ctx, NotUsed]
  // Creates a FlowWithContext from a regular flow that operates on a tuple of (data, context) elements.
  def fromTuples[In, CtxIn, Out, CtxOut, Mat](flow: Flow[(In, CtxIn), (Out, CtxOut), Mat]): FlowWithContext[In, CtxIn, Out, CtxOut, Mat]

object Source:
  //Transform this source whose element is e into a source producing tuple (e, f(e))
  def asSourceWithContext[Ctx](f: (Out) => Ctx): SourceWithContext[Out, Ctx, Mat]

final class SourceWithContext[+Out, +Ctx, +Mat]
object SourceWithContext:
  //Creates a SourceWithContext from a regular source that operates on a tuple of (data, context) elements.
  def fromTuples[Out, CtxOut, Mat](source: Source[(Out, CtxOut), Mat]): SourceWithContext[Out, CtxOut, Mat]

trait FlowWithContextOps[+Out, +Ctx, +Mat]:
/**
 * Transform this flow by the regular flow. The given flow must support manual context propagation by taking and producing tuples of (data, context).

 It is up to the implementer to ensure the inner flow does not exhibit any behavior that is not expected by the downstream elements, such as reordering. For more background on these requirements see https://pekko.apache.org/docs/pekko/current/stream/stream-context.html.

 This can be used as an escape hatch for operations that are not (yet) provided with automatic context propagation here.
 
 */
  abstract def via[Out2, Ctx2, Mat2](flow: Graph[FlowShape[(Out, Ctx), (Out2, Ctx2)], Mat2]): Repr[Out2, Ctx2]
  
  // Transform this flow by the regular flow.
  abstract def viaMat[Out2, Ctx2, Mat2, Mat3](flow: Graph[FlowShape[(Out, Ctx), (Out2, Ctx2)], Mat2])(combine: (Mat, Mat2) => Mat3): ReprMat[Out2, Ctx2, Mat3]
  
```