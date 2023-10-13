# dapex-rabbitmq library
A library to publish and consume DAPEX messages from RabbitMQ using the following libraries:
* FS2-Rabbit and Circe: Version 5.0.0
* Circe: Version 0.14.5
* Cats-Effect: Version 3.4.8

## How to Use
See the integration test `RabbitPublisherConsumerTest.scala` and `RMQService.scala` on how to wire up both a publisher
and a consumer for an actual implementation.

##  Publishing to RabbitMQ
To publish to RabbitMQ, a `DapexMQPublisher` instance is required with a RabbitClient:
* `rmqPublisher = new DapexMQPublisher(rabbitClient)`

Once a publisher is created, it is simply a matter of publishing to a queue: `rmqPublisher.publistMessageToQueue(msg, queue)`
where:
* `msg` is an instance of `DapexMessage`
* `queue` is an instance of `RabbitQueue`

## Consuming from RabbitMQ
Consuming is a little more involved.

1. Define a RabbitMQ Consumer that handles `DapexMessage`, it should handle messages received on each of the queues:

```
class RMQConsumer[F[_]]{ 
    def handleFirstQueue(msg: DapexMessage): F[Unit] = ...

    def handleSecondQueue(msg: DapexMessage): F[Unit] = ...

    ...

}
```
    
2. Create a list of `DapexMessageHandler` that routes messages recieved on different queues to the handler:

```
val handlers = Vector(
    DapexMessageHandler(FirstQueue, rmqConsumer.handleFirstQueue),
    DapexMessageHandler(SecondQueue, rmqConsumer.handleSecondQueue),
    ...
)
```

An example of this is in `DapexMessgeHandlerConfigurator.scala` creates the above list.

3. Create a consuming stream from the handlers:
```
    val consumers: fs2.Stream[F, Unit] = DapexMQConsumer.consumerRMQStream(rabbitClient, handlers.toList, amqpChannel)
    
```

4. Run the stream:
You can run the consumer stream directly as in:

```
    ResilientStream.run(consumerStream).as ....
```
or using `DapexMQConsumer.consumeRMQ` method. It's method signature is:
```scala
  def consumeRMQ[F[_]: Log: Temporal: Logger](
      rmqClient: RabbitClient[F],
      handlers: List[DapexMessageHandler[F]],
      channel: AMQPChannel
  ): F[ExitCode] 
```
In which case, provided that an AppService class is returned:
```scala
case class AppService[F[_]](
    server: Server,
    rmqHandler: Vector[DapexMessageHandler[F]],
    rmqClient: RabbitClient[F],
    channel: AMQPChannel
)
```
Then the `run` method in the `MainApp` will look like:
```scala
  override def run(args: List[String]): IO[ExitCode] =
    Resource
      .eval(Slf4jLogger.create[IO])
      .use { implicit logger: Logger[IO] =>
        AppServer
          .createServer[IO]()
          .use(service =>
            DapexMQConsumer
              .consumeRMQ(service.rmqClient, service.rmqHandler.toList, service.channel)
          )
          .as(ExitCode.Success)
      }
```