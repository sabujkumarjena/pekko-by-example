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
