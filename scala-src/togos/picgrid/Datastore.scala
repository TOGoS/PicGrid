package togos.picgrid
import togos.mf.value.ByteBlob

trait Datastore extends Function[String,ByteBlob] {
	def store( data:ByteBlob ):String
}
