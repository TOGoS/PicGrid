package togos.picgrid

import togos.mf.value.ByteBlob

trait Datasink extends Function[String,ByteBlob]
{
	def store( data:ByteBlob ):String
}
