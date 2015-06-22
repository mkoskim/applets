//--------------------------------------------------------------------
// Wrecks at the battle field
//--------------------------------------------------------------------

import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.util.*;

class Wrecks
{
    /* Image of wrecks */
    static BufferedImage image;
    static Graphics2D gWreck;
    static int highest;
    static int lowest;

    //---------------------------------------------------------------
    // Initialize
    //---------------------------------------------------------------
    static void initialize(int width, int height)
    {
        image  = new BufferedImage( width, height, BufferedImage.TYPE_INT_RGB);
        gWreck = (Graphics2D)image.getGraphics();
        clear();
    }

    //---------------------------------------------------------------
    // Clear wrecks
    //---------------------------------------------------------------
    static void clear()
    {
        gWreck.setColor(Color.black);
        gWreck.fillRect(0,0,image.getWidth(), image.getHeight());
        highest = image.getHeight();
        lowest  = 0;
    }

    //---------------------------------------------------------------
    // Put wrecks
    //---------------------------------------------------------------
    static void put(double x, double y, double w, double h)
    {
        int X = (int)(x-w/2)-1, Y = (int)(y-h/2)-1;

        gWreck.setColor(Color.black);
        gWreck.fillRect(X, Y, (int)w+2, (int)h+2);
        gWreck.setColor(Color.gray);
        gWreck.drawRect(X, Y, (int)w+2, (int)h+2);
        if(Y < highest) highest = Y;
        if(Y + h + 2 > lowest) lowest = (int)(Y+h+2);
    }

    static void put(double x, double y, Image image)
    {
        int X = (int)(x-image.getWidth(null)/2)-1;
        int Y = (int)(y-image.getHeight(null)/2)-1;
        gWreck.drawImage(image, X, Y, null);
        if(Y < highest) highest = Y;
        if(Y + image.getHeight(null) > lowest) lowest = (int)(Y+image.getHeight(null));
    }

    static void put(Particle p)
    {
        put(p.position.x, p.position.y, p.dimensions.x, p.dimensions.y);
    }

    static void put(Particle p, Image image)
    {
        put(p.position.x, p.position.y, image);
    }

    //---------------------------------------------------------------
    // Backgrounds for (stationary) objects
    //---------------------------------------------------------------
    static void background(Particle p)
    {
        gWreck.setColor(Color.gray);
        gWreck.fillRect(
            (int)(p.position.x - p.dimensions.x/2),
            (int)(p.position.y - p.dimensions.y/2),
            (int)(p.dimensions.x),
            (int)(p.dimensions.y)
        );
    }

    //---------------------------------------------------------------
    // Draw wrecks
    //---------------------------------------------------------------
    static void draw(Graphics2D g)
    {
        g.drawImage(image, 0, (int)Enemy.fleetOffset.y, null);
    }

    // --------------------------------------------------------------
    // Makes a text box
    // --------------------------------------------------------------
    static void putTextBox(int x, int y, int width, String[] words, int startIndex)
    {
        if(startIndex >= words.length) return;

        Font f = GameEngine.font;
        Graphics2D g = gWreck;
        FontRenderContext frc = g.getFontRenderContext();

        Vector<String> lines = new Vector<String>();
        int height = 0;

        String prevLine = words[startIndex];
        Rectangle2D prevDim = f.getStringBounds( prevLine, frc );

        for(int i = startIndex+1; i < words.length; i++)
        {
            if(!words[i].equals("\\n"))
            {
                String nextLine = prevLine + " " + words[i];
                Rectangle2D nextDim = f.getStringBounds( nextLine, frc );
                if(nextDim.getWidth() <= width)
                {
                    prevLine = nextLine;
                    prevDim  = nextDim;
                    continue;
                }
            }
            else
            {
                i++; if(i >= words.length) break;
            }
            lines.add(prevLine);
            height += prevDim.getHeight();
            prevLine = words[i];
            prevDim  = f.getStringBounds( prevLine, frc );
        }
        lines.add(prevLine);
        height += prevDim.getHeight();

        if(height == 0) return;

        g.setColor(Color.lightGray);
        g.setFont(f);

        int X = x;
        int Y = y - height/2;

        if(Y < highest) highest = Y;
        if(Y + height > lowest) lowest = Y + height;
        for(int i = 0; i < lines.size(); i++)
        {
            String line = (String)lines.get(i);
            g.drawString( line, X, Y );
            Rectangle2D dim = f.getStringBounds( line, frc );
            Y += dim.getHeight();
        }
    }
}
