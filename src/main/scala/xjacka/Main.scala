package xjacka

import java.io.FileNotFoundException
import java.net.{URL, URLConnection}
import java.text.SimpleDateFormat
import java.util.{Calendar, Date, TimeZone}

import spray.json._

import scala.util.Try

case class Assignee(login: String) {
  override def toString = login
}

case class Issue(title: String, number: Int, assignee: Option[Assignee])
case class Comment(body: String, createdAt: String, user: Option[Assignee])
case class Repo(name: String, url: String)

object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val assigneeFormat = jsonFormat(Assignee, "login")
  implicit val commentFormat = jsonFormat(Comment, "body", "created_at", "user")
  implicit val issueFormat = jsonFormat(Issue, "title", "number", "assignee")
  implicit val repoFormat = jsonFormat(Repo, "name", "url")
}

object Main {

  import MyJsonProtocol._

  def main(args: Array[String]): Unit = {
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

  def getTimeInterval(beforeDaysFrom: Int, beforeDaysTo: Int): (Date, Date) = {
    val date = new Date()
    val cal = Calendar.getInstance()
    cal.setTime(date)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.add(Calendar.DAY_OF_MONTH, -beforeDaysFrom)
    val zeroedDate = cal.getTime
    cal.add(Calendar.DAY_OF_MONTH, beforeDaysFrom - beforeDaysTo)
    val endOfTheDay = cal.getTime
    (zeroedDate, endOfTheDay)
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
        .filter(t => isoDateFormat.parse(t.createdAt).before(endOfTheDay))).filter(_ != "")).flatten

    if (timeComments.length == 0) {
      println(s"Od ${readableDateFormat.format(zeroedDate)}  do ${readableDateFormat.format(endOfTheDay)} žádné záznamy")
    } else {
      println(s"Zalogovaný čas za ${readableDateFormat.format(zeroedDate)} až ${readableDateFormat.format(endOfTheDay)}")
      timeComments.foreach(println)
      println("==============")
      println(s"Celkem: ${timeComments.map(getMinutes).sum.toInt} min")
      println(s"Celkem: " + "%1.2f".format(timeComments.map(getMinutes).sum / 60) + " h")
    }
  }

  def loadCompanyRepos(company: String, token: String): List[Repo] = {
    loadDataFromGithub[Repo](token, s"https://api.github.com/orgs/$company/repos")
  }

  def getMinutes(input: String): Float = {
    val min = if (input.contains("min")) Try(input.split(":clock[0-9]:").last.trim.split("min").head.split("h").last.trim.toFloat).toOption else Some(0f)
    val h = if (input.contains("h")) Try(input.split(":clock[0-9]:").last.trim.split("h").head.split("min").last.trim.toFloat).toOption else Some(0f)
    min.getOrElse(0f) + h.getOrElse(0f) * 60f
  }

  def getCommentFromRepo(username: String, token: String, repo: String): List[Comment] = {
    val comments = loadDataFromGithub[Comment](token, repo)

    comments.filter(_.user.getOrElse(new Assignee("nobody")).login == username)
  }

  def loadDataFromGithub[T: JsonReader](token: String, apiUrl: String): List[T] = {
    def makeRequest(apiURL: String): URLConnection = {
      if (apiURL == "") return null
      val url = new URL(apiURL)
      val uc: URLConnection = url.openConnection()
      uc.setRequestProperty("Authorization", "token " + token)
      uc.setRequestProperty("Content-Type", "application/json")
      uc
    }

    def getLinkToNext(urlConnection: URLConnection): String = {
      if (!hasNext(urlConnection)) "" else {
        val ln = urlConnection.getHeaderField("Link").split(",").filter(str => str.contains("rel=\"next\""))
        val link = ln(0)
        val startPosition = link.indexOf("<") + 1
        val endPosition = link.indexOf(">", startPosition)
        link.substring(startPosition, endPosition)
      }
    }

    def hasNext(urlConnection: URLConnection): Boolean = {
      if (urlConnection.getHeaderField("Link") == null) false else {
        if (urlConnection.getHeaderField("Link").split(",").filter(str => str.contains("rel=\"next\"")).length <= 0) false
        else true
      }
    }

    def parseData(uc: URLConnection): List[T] = {
      try {
        val in = uc.getInputStream
        val json = scala.io.Source.fromInputStream(in)("UTF-8").mkString.parseJson.asInstanceOf[JsArray]
        in.close()
        return json.elements.map(elem => elem.convertTo[T]).toList
      } catch {
        case e: FileNotFoundException =>
          println("Zdroj nebyl nalezen")
      }
      List[T]()
    }

    def responses(resource: URLConnection): Stream[URLConnection] = resource #:: responses(makeRequest(getLinkToNext(resource)))

    responses(makeRequest(apiUrl)).takeWhile(_ != null).toList.flatMap(parseData)
  }

  def getTime(comments: List[Comment]): List[String] = {
    comments.flatMap(_.body.split("\n").filter(_.trim.matches(".*:clock[0-9]+:.*")))
  }

}

