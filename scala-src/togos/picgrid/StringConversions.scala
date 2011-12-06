package togos.picgrid

object StringConversions
{
	implicit def stringAsByteArray( s:String ):Array[Byte] = if( s == null ) null else s.getBytes("UTF-8")
	implicit def byteArrayAsString( a:Array[Byte] ):String = if( a == null ) null else new String(a,"UTF-8")
}
