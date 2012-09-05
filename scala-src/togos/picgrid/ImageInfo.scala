package togos.picgrid

@SerialVersionUID(2)
class ImageInfo(
	val uri:String, val sourceUri:String,
	val width:Int, val height:Int,
	val totalImageCount:Int,
	val totalByteCount:Long
) extends Serializable
