// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package explorer.generated.service

@SerialVersionUID(0L)
final case class MessageIn(
    value: _root_.scala.Predef.String = ""
    ) extends scalapb.GeneratedMessage with scalapb.Message[MessageIn] with scalapb.lenses.Updatable[MessageIn] {
    @transient
    private[this] var __serializedSizeCachedValue: _root_.scala.Int = 0
    private[this] def __computeSerializedValue(): _root_.scala.Int = {
      var __size = 0
      if (value != "") { __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, value) }
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
      {
        val __v = value
        if (__v != "") {
          _output__.writeString(1, __v)
        }
      };
    }
    def mergeFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): explorer.generated.service.MessageIn = {
      var __value = this.value
      var _done__ = false
      while (!_done__) {
        val _tag__ = _input__.readTag()
        _tag__ match {
          case 0 => _done__ = true
          case 10 =>
            __value = _input__.readString()
          case tag => _input__.skipField(tag)
        }
      }
      explorer.generated.service.MessageIn(
          value = __value
      )
    }
    def withValue(__v: _root_.scala.Predef.String): MessageIn = copy(value = __v)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => {
          val __t = value
          if (__t != "") __t else null
        }
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PString(value)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion = explorer.generated.service.MessageIn
}

object MessageIn extends scalapb.GeneratedMessageCompanion[explorer.generated.service.MessageIn] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[explorer.generated.service.MessageIn] = this
  def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): explorer.generated.service.MessageIn = {
    require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
    val __fields = javaDescriptor.getFields
    explorer.generated.service.MessageIn(
      __fieldsMap.getOrElse(__fields.get(0), "").asInstanceOf[_root_.scala.Predef.String]
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[explorer.generated.service.MessageIn] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
      explorer.generated.service.MessageIn(
        __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Predef.String]).getOrElse("")
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = ServiceProto.javaDescriptor.getMessageTypes.get(0)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = ServiceProto.scalaDescriptor.messages(0)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = explorer.generated.service.MessageIn(
  )
  implicit class MessageInLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, explorer.generated.service.MessageIn]) extends _root_.scalapb.lenses.ObjectLens[UpperPB, explorer.generated.service.MessageIn](_l) {
    def value: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.value)((c_, f_) => c_.copy(value = f_))
  }
  final val VALUE_FIELD_NUMBER = 1
}
