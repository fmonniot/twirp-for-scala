// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package explorer.generated.service

@SerialVersionUID(0L)
final case class MessageOut(
    messages: _root_.scala.collection.Seq[_root_.scala.Predef.String] = _root_.scala.collection.Seq.empty
    ) extends scalapb.GeneratedMessage with scalapb.Message[MessageOut] with scalapb.lenses.Updatable[MessageOut] {
    @transient
    private[this] var __serializedSizeCachedValue: _root_.scala.Int = 0
    private[this] def __computeSerializedValue(): _root_.scala.Int = {
      var __size = 0
      messages.foreach(messages => __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, messages))
      __size
    }
    final override def serializedSize: _root_.scala.Int = {
      var read = __serializedSizeCachedValue
      if (read == 0) {
        read = __computeSerializedValue()
        __serializedSizeCachedValue = read
      }
      read
    }
    def writeTo(`_output__`: _root_.com.google.protobuf.CodedOutputStream): _root_.scala.Unit = {
      messages.foreach { __v =>
        _output__.writeString(1, __v)
      };
    }
    def mergeFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): explorer.generated.service.MessageOut = {
      val __messages = (_root_.scala.collection.immutable.Vector.newBuilder[_root_.scala.Predef.String] ++= this.messages)
      var _done__ = false
      while (!_done__) {
        val _tag__ = _input__.readTag()
        _tag__ match {
          case 0 => _done__ = true
          case 10 =>
            __messages += _input__.readString()
          case tag => _input__.skipField(tag)
        }
      }
      explorer.generated.service.MessageOut(
          messages = __messages.result()
      )
    }
    def clearMessages = copy(messages = _root_.scala.collection.Seq.empty)
    def addMessages(__vs: _root_.scala.Predef.String*): MessageOut = addAllMessages(__vs)
    def addAllMessages(__vs: TraversableOnce[_root_.scala.Predef.String]): MessageOut = copy(messages = messages ++ __vs)
    def withMessages(__v: _root_.scala.collection.Seq[_root_.scala.Predef.String]): MessageOut = copy(messages = __v)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => messages
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PRepeated(messages.map(_root_.scalapb.descriptors.PString)(_root_.scala.collection.breakOut))
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion = explorer.generated.service.MessageOut
}

object MessageOut extends scalapb.GeneratedMessageCompanion[explorer.generated.service.MessageOut] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[explorer.generated.service.MessageOut] = this
  def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): explorer.generated.service.MessageOut = {
    require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
    val __fields = javaDescriptor.getFields
    explorer.generated.service.MessageOut(
      __fieldsMap.getOrElse(__fields.get(0), Nil).asInstanceOf[_root_.scala.collection.Seq[_root_.scala.Predef.String]]
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[explorer.generated.service.MessageOut] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
      explorer.generated.service.MessageOut(
        __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.collection.Seq[_root_.scala.Predef.String]]).getOrElse(_root_.scala.collection.Seq.empty)
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = ServiceProto.javaDescriptor.getMessageTypes.get(1)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = ServiceProto.scalaDescriptor.messages(1)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = explorer.generated.service.MessageOut(
  )
  implicit class MessageOutLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, explorer.generated.service.MessageOut]) extends _root_.scalapb.lenses.ObjectLens[UpperPB, explorer.generated.service.MessageOut](_l) {
    def messages: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.collection.Seq[_root_.scala.Predef.String]] = field(_.messages)((c_, f_) => c_.copy(messages = f_))
  }
  final val MESSAGES_FIELD_NUMBER = 1
}
