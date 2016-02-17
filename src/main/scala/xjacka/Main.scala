package xjacka

import java.io.{FileNotFoundException, InputStream}
import java.net.{URL, URLConnection}
import java.text.SimpleDateFormat
import java.util.{Calendar, Date, TimeZone}

import spray.json._

import scala.util.Try

case class Assignee(val login: String) {

  override def toString = login
}

case class Issue(val title: String, val number: Int, val assignee : Option[Assignee])
case class Comment(val body: String, val createdAt: String, val user : Option[Assignee])
case class Repo(val name: String, val url : String)

object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val assigneeFormat = jsonFormat(Assignee, "login")
  implicit val commentFormat = jsonFormat(Comment, "body", "created_at", "user")
  implicit val issueFormat = jsonFormat(Issue, "title", "number", "assignee")
  implicit val repoFormat = jsonFormat(Repo, "name", "url")
}

object Main extends App {

  import MyJsonProtocol._

  override def main(args: Array[String]): Unit = {
    args.length match {
      case 3 =>
        showTodayLoggedTime(args(0), args(1), args(2))
      case 4 =>
        showTodayLoggedTime(args(0), args(1), args(2), args(3).toInt, args(3).toInt - 1)
      case 5 =>
        showTodayLoggedTime(args(0), args(1), args(2), args(3).toInt, args(4).toInt - 1)
      case _ =>
        println("Zadejte prosím název společnosti, github jméno a github token")
    }
  }

  def getTimeInterval (beforeDaysFrom: Int, beforeDaysTo: Int) : (Date, Date) = {
    val date = new Date()
    val cal = Calendar.getInstance()
    cal.setTime(date)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.add(Calendar.DAY_OF_MONTH, -beforeDaysFrom)
    val zeroedDate = cal.getTime()
    cal.add(Calendar.DAY_OF_MONTH, (beforeDaysFrom - beforeDaysTo))
    val endOfTheDay = cal.getTime()
    return (zeroedDate, endOfTheDay)
  }

  def showTodayLoggedTime(companyName: String, username: String, token: String, beforeDaysFrom: Int = 0, beforeDaysTo: Int = -1) = {

    val (zeroedDate, endOfTheDay) = getTimeInterval(beforeDaysFrom, beforeDaysTo)

    val readableDateFormat = new SimpleDateFormat("dd.MM.yyyy")
    val isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    isoDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
    val nowAsISO = isoDateFormat.format(zeroedDate)

    val repos = loadCompanyRepos(companyName, token)
    val timeComments = repos.map((repo: Repo) =>
          getTime(getCommentFromRepo(username, token, repo.url + "/issues/comments?since=" + nowAsISO)
                    .filter(t => isoDateFormat.parse(t.createdAt).before(endOfTheDay))
        ).filter(_ != "")).flatten

    if(timeComments.length == 0) {
      println("Od " + readableDateFormat.format(zeroedDate) + " do " + readableDateFormat.format(endOfTheDay) + " žádné záznamy")
    } else {
      println("Zalogovaný čas za " + readableDateFormat.format(zeroedDate) + " až " + readableDateFormat.format(endOfTheDay))
      timeComments.foreach(println)
      println("==============")
      println("Celkem: " + timeComments.map(getMinutes(_)).reduceLeft(_ + _).toInt + " min")
      println("Celkem: " + "%1.2f".format(timeComments.map(getMinutes(_)).reduceLeft(_ + _) / 60) + " h")
    }
  }

  def loadCompanyRepos(company: String, token: String): List[Repo] = {
    loadDataFromGithub[Repo](token, s"https://api.github.com/orgs/${company}/repos")
  }

  def getMinutes(input: String): Float = {
    val min = if ( input.contains("min") ) Try((input.split("min").head.split("h").last.trim).toFloat).toOption else Some(0f)
    val h = if (input.contains("h") ) Try((input.split("h").head.split("min").last.trim).toFloat).toOption else Some(0f)
    min.getOrElse(0f) + h.getOrElse(0f) * 60f
  }

  def getCommentFromRepo(username: String, token: String, repo: String): List[Comment] = {
    val comments = loadDataFromGithub[Comment](token, repo)

    comments.filter(_.user.getOrElse(new Assignee("nobody")).login == username)
  }

  def loadDataFromGithub[T : JsonReader](token: String, apiUrl: String): List[T] = {
    var url = new URL(apiUrl)
    var data = List[T]()
    var brk = true

    do {

      val uc: URLConnection = url.openConnection()
      uc.setRequestProperty("Authorization", "token " + token)
      uc.setRequestProperty("Content-Type", "application/json")

      try {
        val in: InputStream = uc.getInputStream()
        val json = scala.io.Source.fromInputStream(in)("UTF-8").mkString.parseJson.asInstanceOf[JsArray]
        data = data ++ json.elements.map(elem => elem.convertTo[T])

        if (uc.getHeaderField("Link") == null) {
          brk = false
        } else {
          val ln = uc.getHeaderField("Link").split(",").filter(str => str.contains("rel=\"next\""))

          if (ln.length <= 0) {
            brk = false
          } else {
            val link = ln(0)
            val startPosition = link.indexOf("<") + 1
            val endPosition = link.indexOf(">", startPosition)
            val subS = link.substring(startPosition, endPosition)

            url = new URL(subS)
          }
        }
      } catch {
        case e: FileNotFoundException =>
          println("source not found")
          brk = false
      }

    } while(brk)

    return data
  }

  def getTime(comments: List[Comment]): List[String] = {
    comments.map(_.body.split("\n").filter(_.trim.matches(".*:clock[0-9]+:.*")).mkString("\n").replaceAll(":clock[0-9]:",""))
  }

}


