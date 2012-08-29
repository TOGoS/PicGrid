package togos.picgrid.layout

import togos.picgrid.ImageEntry
import togos.picgrid.image.CompoundImageComponent

trait Layouter
{
	def configString:String
	def gridify( images:Seq[ImageEntry] ):List[CompoundImageComponent]
}
