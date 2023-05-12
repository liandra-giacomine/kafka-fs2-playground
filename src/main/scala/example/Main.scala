package example

import cats.effect._
import cats.effect.{IO, IOApp}
import cats.syntax.all._
import fs2.kafka.{
  commitBatchWithin,
  AutoOffsetReset,
  CommittableOffset,
  ConsumerRecord,
  ConsumerSettings,
  Deserializer,
  KafkaConsumer,
  KafkaProducer,
  ProducerRecord,
  ProducerRecords,
  ProducerSettings,
}
import io.circe.parser
import io.circe.syntax._
import scala.concurrent.duration._

object Main extends IOApp.Simple {
  val run: IO[Unit] = {

    def processRecord(record: ConsumerRecord[String, String]): IO[OutputMessage] =
      IO.pure(
        parser
          .decode[Animal](record.value)
          .fold(
            _ =>
              OutputMessage(
                record.key,
                "invalid json",
                "dead.queue",
              ),
            _ match {
              case m: Mammal =>
                OutputMessage(
                  record.key,
                  record.value,
                  "animal.mammal",
                )
              case f: Fish   =>
                OutputMessage(
                  record.key,
                  record.value,
                  "animal.fish",
                )
            },
          ),
      )

    val consumerSettings =
      ConsumerSettings[IO, String, String]
        .withAutoOffsetReset(AutoOffsetReset.Earliest)
        .withBootstrapServers("localhost:9092,localhost:9093,localhost:9094")
        .withGroupId("group")

    val consumer = KafkaConsumer.stream(consumerSettings).subscribeTo("animal")

    val producerSettings =
      ProducerSettings[IO, String, String]
        .withBootstrapServers("localhost:9092,localhost:9093,localhost:9094")

    val producer = (offsetsAndProducerRecords: fs2.Stream[IO, (CommittableOffset[IO], ProducerRecords[String, String])]) =>
      KafkaProducer.stream(producerSettings).flatMap { producer =>
        offsetsAndProducerRecords.evalMap { case (offset, producerRecord) =>
          producer
            .produce(producerRecord)
            .map(_.as(offset))
        }.parEvalMap(Int.MaxValue)(identity)
      }

    consumer.records
      .mapAsync(25) { committable =>
        processRecord(committable.record).map { outputMessage =>
          val record = ProducerRecord(outputMessage.outTopic, outputMessage.key, outputMessage.value)
          committable.offset -> ProducerRecords.one(record)
        }
      }
      .through(producer)
      .through(commitBatchWithin(500, 15.seconds))
      .compile
      .drain
  }
}
