import org.scalatest._


class ExampleSpec extends FlatSpec with Matchers {

  "Get time" should "parse lines with time records" in {
    import xjacka.Main
    import xjacka.Comment
    val comment1 = Comment("ahoj\n:clock2: 1h\n:clock4: 30min ahoj\ncau", "2011-04-14T16:00:49Z", null)
    val comment2 = Comment(":clock2: 1.5h\n:clock1: 1 h 30 min", "2011-04-14T16:00:49Z", null)

    val timeRecords = List(comment1, comment2).flatMap(Main.getTime)

    timeRecords should be (List("14.04: :clock2: 1h","14.04: :clock4: 30min ahoj", "14.04: :clock2: 1.5h", "14.04: :clock1: 1 h 30 min"))

    timeRecords.map(Main.getMinutes(_)).sum should be (270f)
  }

}