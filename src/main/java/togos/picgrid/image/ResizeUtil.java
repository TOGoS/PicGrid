package togos.picgrid.image;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

class ResizeUtil
{
	protected static final float[] sampleX = new float[] {0.6f, 0.2f, 0.8f, 0.4f, 0.5f, 0.9f, 0.3f};
	protected static final float[] sampleY = new float[] {0.0f, 0.6f, 0.9f, 0.9f, 0.5f, 0.3f, 0.1f};
	
	/**
	 * Stolen from http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
	 * 
	 * Convenience method that returns a scaled instance of the
	 * provided {@code BufferedImage}.
	 *
	 * @param img the original image to be scaled
	 * @param targetWidth the desired width of the scaled instance,
	 *	in pixels
	 * @param targetHeight the desired height of the scaled instance,
	 *	in pixels
	 * @param hint one of the rendering hints that corresponds to
	 *	{@code RenderingHints.KEY_INTERPOLATION} (e.g.
	 *	{@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
	 *	{@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
	 *	{@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
	 * @param higherQuality if true, this method will use a multi-step
	 *	scaling technique that provides higher quality than the usual
	 *	one-step technique (only useful in downscaling cases, where
	 *	{@code targetWidth} or {@code targetHeight} is
	 *	smaller than the original dimensions, and generally only when
	 *	the {@code BILINEAR} hint is specified)
	 * @return a scaled version of the original {@code BufferedImage}
	 */
	public static BufferedImage chrisResize(BufferedImage img,
	                                       int targetWidth,
	                                       int targetHeight,
	                                       Object hint,
	                                       boolean higherQuality)
	{
		int type = (img.getTransparency() == Transparency.OPAQUE) ?
			BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
		BufferedImage ret = (BufferedImage)img;
		int w, h;
		if (higherQuality) {
			// Use multi-step technique: start with original size, then
			// scale down in multiple passes with drawImage()
			// until the target size is reached
			w = img.getWidth();
			h = img.getHeight();
		} else {
			// Use one-step technique: scale directly from original
			// size to target size with a single drawImage() call
			w = targetWidth;
			h = targetHeight;
		}
		
		do {
			if (higherQuality && w > targetWidth) {
				w /= 2;
				if (w < targetWidth) {
					w = targetWidth;
				}
			}

			if (higherQuality && h > targetHeight) {
				h /= 2;
				if (h < targetHeight) {
					h = targetHeight;
				}
			}

			BufferedImage tmp = new BufferedImage(w, h, type);
			Graphics2D g2 = tmp.createGraphics();
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
			g2.drawImage(ret, 0, 0, w, h, null);
			g2.dispose();

			ret = tmp;
		} while (w != targetWidth || h != targetHeight);

		return ret;
	}
	
	public static BufferedImage chrisResize( BufferedImage img, int w, int h ) {
		return chrisResize( img, w, h, RenderingHints.VALUE_INTERPOLATION_BICUBIC, true );
	}
	
	public static final int dumbSample( BufferedImage img, float x, float y, float w, float h ) {
		float r=0, g=0, b=0;
		final int count = sampleX.length;
		for( int i=0; i<count; ++i ) {
			int sample = img.getRGB((int)(x + sampleX[i] * w), (int)(y + sampleY[i] * h));
			r += ((sample >> 16) & 0xFF);
			g += ((sample >>  8) & 0xFF);
			b += ( sample		& 0xFF);
		}
		return
			0xFF000000 |
			((int)(r/count) << 16) |
			((int)(g/count) <<  8) |
			((int)(b/count)	  );
	}
	
	public static final BufferedImage dumbResize( BufferedImage img, int newWidth, int newHeight ) {
		int[] newData = new int[newWidth*newHeight];
		
		float sampleWidth = img.getWidth() / newWidth;
		float sampleHeight = img.getHeight() / newHeight;
		
		for( int y=0, i=0; y<newHeight; ++y ) {
			for( int x=0; x<newWidth; ++x, ++i ) {
				newData[i] = dumbSample( img, x*sampleWidth, y*sampleHeight, sampleWidth, sampleHeight );
			}
		}
		BufferedImage newImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
		newImage.setRGB(0, 0, newWidth, newHeight, newData, 0, newWidth);
		return newImage;
	}
	
	public static void main(String[] args) {
		String outfile = null;
		String infile = null;
		int newWidth  = 512;
		int newHeight = 512;
		for( int i=0; i<args.length; ++i ) {
			if( "-o".equals(args[i]) ) {
				outfile = args[++i];
			} else if( "-w".equals(args[i]) ) {
				newWidth = Integer.parseInt(args[++i]);
			} else if( "-h".equals(args[i]) ) {
				newHeight = Integer.parseInt(args[++i]);
			} else if( !args[i].startsWith("-") ) {
				infile = args[i];
			} else {
				throw new RuntimeException("Unrecognised argument: "+args[i]);
			}
		}
		
		try {
			BufferedImage orig = ImageIO.read(new File(infile));
			BufferedImage resized = chrisResize( orig, newWidth, newHeight );
			ImageIO.write( resized, "PNG", new File(outfile));
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
}
