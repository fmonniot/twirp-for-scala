package explorer

// Everything in this doesn't needs to be generated
// It does have to be in the class path at runtime though
object core {

  sealed trait Context // Marker trait which contains some really nice information on the request :)
  case object NoContext extends Context
  case class RequestContext() extends Context

}
