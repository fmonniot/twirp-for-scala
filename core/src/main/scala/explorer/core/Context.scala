package explorer.core

sealed trait Context // Marker trait which contains some really nice information on the request :)

object Context {

  case object NoContext extends Context

  case class RequestContext() extends Context

}
