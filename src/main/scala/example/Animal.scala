package example

import cats.syntax.functor._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.deriveDecoder
import io.circe.generic.semiauto.deriveEncoder
sealed trait Animal
object Animal {
  implicit val fishDecoder: Decoder[Fish] = deriveDecoder[Fish]
  implicit val fishEncoder: Encoder[Fish] = deriveEncoder[Fish]

  implicit val mammalDecoder: Decoder[Mammal] = deriveDecoder[Mammal]
  implicit val mammalEncoder: Encoder[Mammal] = deriveEncoder[Mammal]

  implicit val decoder: Decoder[Animal] =
    List[Decoder[Animal]](
      Decoder[Fish].widen,
      Decoder[Mammal].widen,
    ).reduceLeft(_ or _)
}

case class Fish(
  swimmingSpeed: String,
  waterType: String,
) extends Animal

case class Mammal(
  landSpeed: Int,
  lifeExpectancy: Int,
) extends Animal
