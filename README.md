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
