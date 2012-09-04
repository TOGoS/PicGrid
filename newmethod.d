#!/usr/bin/rdmd

import std.math;
import std.random;
import std.algorithm;
import std.stdio;

const class ImageInfo {
	int w, h;
	int color;
	float weight;
	this( int w, int h, int color, float weight ) {
		this.w = w; this.h = h;
		this.color = color;
		this.weight = weight;
	}
}

const class Cell {
	float x, y, w, h;
	ImageInfo image;
	this( float x, float y, float w, float h, const ImageInfo image ) {
		assert(image !is null);
		this.x = x; this.y = y;
		this.w = w; this.h = h;
		this.image = image;
	}
}

Cell[] singleRowNaiveGridify( ImageInfo[] images ) {
	Cell[] cells;
	float rowHeight = 256;
	int x = 0;
	foreach( ImageInfo image; images ) {
		float scale = rowHeight / image.h;
		Cell c = new Cell( x, 0, cast(int)(image.w*scale), cast(int)(image.h*scale), image );
		cells ~= c;
		x += c.w;
	}
	return cells;
}

const class CellTree {
	CellTree[] subTrees;
	ImageInfo image;
	this( CellTree[] subTrees ) {
		this.subTrees = subTrees;
		this.image = null;
	}
	this( ImageInfo image ) {
		this.subTrees = [];
		this.image = image;
	}
}

float weightAdjust( float w ) {
	return w * w;
}

CellTree partition( ImageInfo[] images ) {
	assert(images.length > 0);
	if( images.length == 1 ) {
		return new CellTree( images[0] );
	} else if( images.length == 2 ) {
		return new CellTree( [new CellTree(images[0]), new CellTree(images[1])] );
	}
  
	float totalWeight = 0;
	foreach( ImageInfo ii; images ) {
		totalWeight += weightAdjust(ii.weight);
	}
	int midWeightIndex = 0;
	float prevWeight=0, nextWeight=0;
	for( int i=0; i<images.length; ++i, ++midWeightIndex ) {
		nextWeight = prevWeight + weightAdjust(images[i].weight);
		if( totalWeight / 2 - prevWeight < nextWeight - totalWeight / 2 ) {
			break;
		}
		prevWeight = nextWeight;
	}
  
	// These are impossible, but happen.
	// therefore there's a bug in my program.
	if( midWeightIndex == 0 ) midWeightIndex = 1;
	if( midWeightIndex == images.length ) midWeightIndex = images.length-1;

	return new CellTree( [ partition(images[0..midWeightIndex]), partition(images[midWeightIndex..images.length]) ] );
}

const struct LayoutComponent {
	int x, y, w, h;
	Layout *l;
}

const struct Layout {
	int w, h;
	LayoutComponent[] components;
	ImageInfo image;
	this(int w, int h, const(LayoutComponent[]) components, const(ImageInfo) image) {
		this.w = w; this.h = h;
		this.components = components;
		this.image = image;
	}
	this(int w, int h, const(LayoutComponent[]) components) {
		this(w, h, components, null);
	}
	this(int w, int h, const(ImageInfo) image) {
		this(w, h, [], image);
	}
	this(Layout l) {
		this(l.w, l.h, l.components, l.image);
	}
}

void swap( ref int x, ref int y, bool actually ) {
	if( actually ) {
		int z = x;
		x = y;
		y = z;
	}
}

Layout _layout( Layout[] subLayouts, int maxWidth, int maxHeight, bool vertical ) {
	swap( maxWidth, maxHeight, vertical );
	LayoutComponent[] components;
	int x = 0, y = 0;
	foreach( Layout subLayout; subLayouts ) {
		int slw = subLayout.w, slh = subLayout.h;
		swap( slw, slh, vertical );
		int scaledWidth = cast(int)(slw * (cast(float)(maxHeight) / slh));
		int slx = x, sly = y;
		swap( slx, sly, vertical );
		slw = scaledWidth; slh = maxHeight;
		swap( slw, slh, vertical );
		LayoutComponent c = {x: slx, y:sly, w:slw, h:slh, new Layout(subLayout)};
		components ~= c;
		x += scaledWidth;
	}
	swap( x, maxHeight, vertical );
	return Layout( x, maxHeight, components );
}

float ratio( float x, float y ) {
	return x > y ? x / y : y / x;
}

Layout layout( const CellTree ct ) {
	if( ct.image !is null ) {
		return Layout(ct.image.w, ct.image.h, ct.image);
	}
	
	Layout[] subLayouts;
	int maxWidth = 0, maxHeight = 0;
	foreach( const(CellTree) subTree; ct.subTrees ) {
		Layout subLayout = layout(subTree);
		subLayouts ~= subLayout;
		maxWidth = max(maxWidth, subLayout.w);
		maxHeight = max(maxHeight, subLayout.h);
	}
	
	Layout h = _layout( subLayouts, maxWidth, maxHeight, false );
	Layout v = _layout( subLayouts, maxWidth, maxHeight, true );
	return ratio(h.w,h.h) < ratio(v.w,v.h) ? h : v;
}

void roundBounds( ref float x, ref float y, ref float w, ref float h ) {
	float endX = round(x+w), endY = round(y+h);
	x = round(x); y = round(y);
	w = endX - x; h = endY - y;
}

void layoutToCells( const(Layout) l, float x, float y, float w, float h, ref const(Cell)[] cells ) {
	if( l.image !is null ) {
		roundBounds(x,y,w,h);
		cells ~= new Cell( cast(int)x, cast(int)y, cast(int)w, cast(int)h, l.image );
	} else {
		float scaleX = cast(float)w / l.w, scaleY = cast(float)h / l.h;
		foreach( LayoutComponent lc; l.components ) {
			layoutToCells( *lc.l,
				x + scaleX*lc.x, y + scaleY*lc.y,
				    scaleX*lc.w,     scaleY*lc.h,
			cells );
		}
	}
}

void fitInside( ref float w, ref float h, float maxWidth, float maxHeight ) {
	if( w > maxWidth ) {
		h = h * (maxWidth / w);
		w = maxWidth;
	}
	if( h > maxWidth ) {
		w = w * (maxHeight / h);
		h = maxHeight;
	}
}

const(Cell)[] bpGridify( ImageInfo[] images, int maxWidth, int maxHeight ) {
	const(Cell)[] cells;
	Layout l = layout( partition(images) );
	float w = l.w, h = l.h;
	fitInside( w, h, maxWidth, maxHeight );
	layoutToCells( l, 0, 0, w, h, cells );
	return cells;
}

void main() {
	ImageInfo[] images;
	for( int i=0; i<50; ++i ) {
		int weight = uniform(1,4);
		int color;
		switch(weight) {
		case(1): color = 0xFF808080; break;
		case(2): color = 0xFF808000; break;
		case(3): color = 0xFF800000; break;
		default: color = 0xFF404040;
		}
		images ~= new ImageInfo(uniform(1,2)*uniform(100,200),uniform(1,2)*uniform(100,200),color,weight);
	}
	const(Cell)[] cells = bpGridify(images, 1280, 800);//singleRowNaiveGridify(images);

	writeln("<html><body>");
	foreach( const(Cell) c; cells ) {
		writef("<div style=\"background: #%06X; position:absolute; left:%dpx; top: %dpx; width: %dpx; height: %dpx;\"></div>\n",
			 c.image.color & 0xFFFFFF, cast(int)(c.x)+1, cast(int)(c.y)+1, cast(int)(c.w)-2, cast(int)(c.h)-2);
	}
	writeln("</body></html>");
}
