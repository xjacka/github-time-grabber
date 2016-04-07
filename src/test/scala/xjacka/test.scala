import org.scalatest._


class ExampleSpec extends FlatSpec with Matchers {

  "Get time" should "parse lines with time records" in {
    import xjacka.Main
    import xjacka.Comment
    val comment1 = Comment("ahoj\n:clock2: 1h\n:clock4: 30min ahoj\ncau", "dnes", null)
    val comment2 = Comment(":clock2: 1.5h\n:clock1: 1 h 30 min", "dnes", null)

    val timeRecords = List(comment1, comment2).flatMap(Main.getTime)

    timeRecords should be (List(":clock2: 1h",":clock4: 30min ahoj", ":clock2: 1.5h", ":clock1: 1 h 30 min"))

    timeRecords.map(Main.getMinutes(_)).sum should be (270f)
  }

}