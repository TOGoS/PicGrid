package togos.picgrid

@SerialVersionUID(1)
class ImageInfo(
	val uri:String, val sourceUri:String,
	val width:Int, val height:Int,
	val totalImageCount:Int
) extends Serializable
