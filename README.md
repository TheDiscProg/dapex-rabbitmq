# dapex-rabbitmq library
A library to publish and consume DAPEX messages from RabbitMQ using the following libraries:
* FS2-Rabbit and Circe: Version 5.0.0
* Circe: Version 0.14.5
* Cats-Effect: Version 3.4.8

## How to Use
There is a sample project, [RabbitMQ-Tester](https://github.com/TheDiscProg/rabbitmq-tester), that will both 
publish and consume from RabbitMQ; see `PublishAndConsimeRMQ.scala` for more details.

##  Publishing to RabbitMQ
To publish to RabbitMQ, a `DapexMQPublisher` instance is required with a RabbitClient:
* `rmqPublisher = new DapexMQPublisher(rabbitClient)`

Once a publisher is created, it is simply a matter of publishing to a queue: `rmqPublisher.publistMessageToQueue(msg, queue)`
where:
* `msg` is an instance of `DapexMessage`
* `queue` is an instance of `RabbitQueue`

## Consuming from RabbitMQ
Consuming is a little more involved.

1. Define a RabbitMQ Consumer that handles `DapexMessage`, it should handle messages rec:

```
class RMQConsumer[F[_]]{ 
    def handleFirstQueue(msg: DapexMessage): F[Unit] = ...

    def handleSecondQueue(msg: DapexMessage): F[Unit] = ...

    ...

}
```
    
2. Create a list of `DapexMessageHandler` that handles each of the queues:

```
val handlers = Vector(
    DapexMessageHandler(FirstQueue, rmqConsumer.handleFirstQueue),
    DapexMessageHandler(SecondQueue, rmqConsumer.handleSecondQueue),
    ...
)
```

3. To start consuming from the RabbitMQ queues:
```
    val consumers = DapexMQConsumer.consumerRMQStream(rabbitClient, handlers.toList, amqpChannel)
    
```

And that's it!

