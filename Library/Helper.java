/********************************************************************
 ********************************************************************
 *
 * Helpers
 *
 ********************************************************************
 ********************************************************************/

import java.awt.*;
import java.awt.image.*;
import java.awt.font.*;
import java.awt.geom.*;

import java.text.*;

public class Helper
{
    // **************************************************************
    // **************************************************************
    //
    // Debugging info
    //
    // **************************************************************
    // **************************************************************
    static boolean debugging = true;

    static void DEBUG(String s) { if(debugging) System.out.println(s); }

    // **************************************************************
    // **************************************************************
    //
    // Graphics helpers
    //
    // **************************************************************
    // **************************************************************

    // --------------------------------------------------------------
    // Draw lines of text to the center of selected box
    // --------------------------------------------------------------
    public static void text2Center( Graphics2D g, int X, int Y, int width, int height, String text )
    {
        Font f = g.getFont();
        FontRenderContext frc = g.getFontRenderContext();

        // Cut the text to lines
        String[]      lines = text.split("\n");
        Rectangle2D[] dims  = new Rectangle2D[lines.length];

        // Get the total height of the box
        int tot_height = 0;
        for(int i = 0; i < lines.length; i++)
        {
            dims[i] = f.getStringBounds( lines[i], frc );
            tot_height += dims[i].getHeight();
        }

        // Draw the lines
        int starty = (height - tot_height) / 2;
        for(int i = 0; i < lines.length; i++)
        {
            int padx = (width  - (int)dims[i].getWidth())/2;
            starty  += (int)dims[i].getHeight();
            g.drawString(lines[i], X + padx, Y + starty);
        }
    }

    // --------------------------------------------------------------
    // Draw fields to screen
    // --------------------------------------------------------------
    public static void drawField(
        Graphics2D g,
        int x, int y, int fieldLength,
        String name,
        String content
    )
    {
        g.setColor(Color.lightGray);
        g.drawString(name,     x, y + 14);
        Font f = g.getFont();
        FontRenderContext frc = g.getFontRenderContext();
        Rectangle2D dim = f.getStringBounds( content, frc );
        g.drawString(content, (int)(x + fieldLength - dim.getWidth()), y + 14);
    }

    // --------------------------------------------------------------
    // Draw values to screen
    // --------------------------------------------------------------
    public static void drawField(
        Graphics2D g,
        int x, int y, int fieldLength,
        String name,
        int value
    )
    {
        drawField( g, x, y, fieldLength, name, Integer.toString(value));
    }

    // --------------------------------------------------------------
    // Draw percentages
    // --------------------------------------------------------------
    public static void drawField(
        Graphics2D g,
        int x, int y, int fieldLength,
        String name,
        double value, double max
    )
    {
        drawField(g, x, y, fieldLength, name,
            Integer.toString( (int)(100*value/max) ) + " %"
        );
    }

    // --------------------------------------------------------------
    // Draw a meter
    // --------------------------------------------------------------
    public static void drawMeterBar(
        Graphics2D g,
        int x, int y, int fieldLength,
        String name, int xoffset,
        double value, double max
    )
    {
        g.setColor(Color.lightGray);
        g.drawString(name, x, y + 14);
        g.setColor(Color.gray);
        int barlength = fieldLength - xoffset-2;
        g.drawRect(x + xoffset, y+3, barlength+2, 10);
        double percentage = value/max;
        if(percentage < 0.25) g.setColor(Color.red);
        else if(percentage < 0.50) g.setColor(Color.yellow);
        else g.setColor(Color.green);
        g.fillRect(x+xoffset+1,y+4, (int)(barlength*percentage)+1, 9);
    }

    // **************************************************************
    // **************************************************************
    //
    // Image manipulation helpers
    //
    // **************************************************************
    // **************************************************************

    //---------------------------------------------------------------
    // Make a specific color transparent in an image
    //---------------------------------------------------------------
    public static Image makeColorTransparent(Image im, final Color color)
    {
        if(color == null) return im;

        ImageFilter filter = new RGBImageFilter()
        {
            // the color we are looking for... Alpha bits are set to opaque
            public int markerRGB = color.getRGB() | 0xFF000000;

            public final int filterRGB(int x, int y, int rgb)
            {
                if ( ( rgb | 0xFF000000 ) == markerRGB )
                {
                    // Mark the alpha bits as zero - transparent
                    return 0x00FFFFFF & rgb;
                }
                else
                {
                    // nothing to do
                    return rgb;
                }
            }
        };

        ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(ip);
    }

    //---------------------------------------------------------------
    // Make image semitransparent
    //---------------------------------------------------------------
    public static Image makeSemiTransparent(Image im, final double ratio)
    {
        ImageFilter filter = new RGBImageFilter()
        {
            public final int filterRGB(int x, int y, int rgb)
            {
                if( (rgb & 0x00FFFFFF) == 0) return 0x00000000;
                int alpha = (int)(255*ratio);
                if(alpha > 255) alpha = 255;
                if(alpha < 0)   alpha = 0;

                return (alpha << 24) | (rgb & 0x00FFFFFF);
            }
        };

        ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(ip);
    }

    //---------------------------------------------------------------
    // Make a halo sphere; creates a "fading" sphere with given
    // radius and color. Good to be used with explosions and
    // such.
    //---------------------------------------------------------------
    public static Image createHalo(final int radius, final double alpha, final Color color)
    {
        ImageFilter filter = new RGBImageFilter()
        {
            public final int filterRGB(int x, int y, int rgb)
            {
                double X = (radius-x), Y = (radius-y);
                double distance = 1-Math.sqrt(X*X + Y*Y)/radius;
                if(distance < 0) distance = 0;

                int a = (int)(255 * alpha * distance);
                if(a > 255) a = 255;
                return (a << 24) | (color.getRGB() & 0x00FFFFFF);
            }
        };
        Image im = new BufferedImage
        (
            2*radius, 2*radius,
            BufferedImage.TYPE_INT_ARGB
        );
        ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(ip);
    }

    //---------------------------------------------------------------
    // Make a halo torus; creates a "fading" torus with given
    // radius and color. Good to be used with explosions and
    // such.
    //---------------------------------------------------------------
    public static Image createHaloTorus(final int radius, final int width, final double alpha, final Color color)
    {
        ImageFilter filter = new RGBImageFilter()
        {
            public final int filterRGB(int x, int y, int rgb)
            {
                double X = (radius+width-x), Y = (radius+width-y);
                double distance = 1 - Math.abs(Math.sqrt(X*X + Y*Y) - radius)/width;
                if(distance < 0) distance = 0;

                int a = (int)(255 * alpha * distance);
                if(a > 255) a = 255;
                return (a << 24) | (color.getRGB() & 0x00FFFFFF);
            }
        };
        Image im = new BufferedImage
        (
            2*(radius+width), 2*(radius+width),
            BufferedImage.TYPE_INT_ARGB
        );
        ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(ip);
    }

    //---------------------------------------------------------------
    // Create shaders
    //---------------------------------------------------------------
    public static Color createTransparentColor(Color color, double alpha)
    {
        return new Color(
            color.getRed(),
            color.getGreen(),
            color.getBlue(),
            (int)((alpha <= 1.0) ? 255*alpha : 255)
        );
    }

    public static Color createTransparentColor(Color color, int index, int length)
    {
        return createTransparentColor(color, (double)index / length);
    }

    public static void createTransparentColorArray(Color[] array, Color color)
    {
        for(int i = 0; i < array.length; i++)
        {
            array[i] = createTransparentColor( color, i, array.length-1 );
        }
    }

    public static void reverse( Object[] array )
    {
        for
        (
            int left=0, right=array.length-1;
            left < right;
            left++, right--
        )
        {
            Object temp = array[left];
            array[left] = array[right];
            array[right] = temp;
        }
    }

    //---------------------------------------------------------------
    // Creates halo animations.
    //---------------------------------------------------------------
    public static Image[] createHaloArray(
        int count,
        int radius,
        Color color
    )
    {
        Image images[] = new Image[count];
        for(int i = 0; i < count; i++)
        {
            images[i] = createHalo(
                radius,
                (double)(count-i-1)/count,
                color
            );
        }
        return images;
    }

    //---------------------------------------------------------------
    // Cut large image to smaller pieces. This way animations
    // (and other game images) can be drawn to a few larger
    // images.
    //---------------------------------------------------------------
    public static Image[] cutImage
    (
        Image source,
        int width, int height, int pad,
        int xcnt, int ycnt,
        Color transparent
    )
    {
        Image[] cutted = new Image[ycnt * xcnt];
        for(int y = 0; y < ycnt; y++)
        {
            for(int x = 0; x < xcnt; x++)
            {
                Image im = new BufferedImage
                (
                    width-pad*2, height-pad*2,
                    BufferedImage.TYPE_INT_RGB
                );
                Graphics g = im.getGraphics();
                g.setColor( transparent );
                g.fillRect(0,0,width,height);
                g.drawImage(
                    source,
                    0,0,width-pad*2,height-pad*2,
                    x*width+pad, y*height+pad, (x+1)*width-pad, (y+1)*height-pad,
                    null
                );
                g.dispose();
                cutted[y*xcnt+x] = makeColorTransparent( im, transparent );
            }
        }
        return cutted;
    }

    //---------------------------------------------------------------
    // Combine two ararys together
    //---------------------------------------------------------------
    public static Image[] catImageArrays( Image[] a, Image[] b )
    {
        Image newImage[] = new Image[ a.length + b.length ];
        for(int i = 0; i < a.length; i++) newImage[i] = a[i];
        for(int i = 0; i < b.length; i++) newImage[i + a.length] = b[i];
        return newImage;
    }

    //---------------------------------------------------------------
    // Take a slice of an array
    //---------------------------------------------------------------
    public static Image[] sliceImageArray( Image[] a, int begin, int length)
    {
        Image newImage[] = new Image[length];
        for(int i = 0; i < length; i++) newImage[i] = a[begin + i];
        return newImage;
    }
}
