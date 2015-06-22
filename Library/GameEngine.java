/*********************************************************************
 *********************************************************************
 *
 * Game engine
 *
 *********************************************************************
 *********************************************************************/

import java.applet.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;

import java.io.*;
import java.net.*;

abstract public class GameEngine
    extends Applet
{
    public static int score = 0;

    final static int WIDTH  = Setup.WIDTH;
    final static int HEIGHT = Setup.HEIGHT;

    static Image offScreen = null;  // Offscreen image for double buffering
    static Graphics2D osg = null;
    static Font  font = null;       // Default font

    public GameEngine( int width, int height )
    {
        super();
        offScreen = new BufferedImage
        (
            width, height,
            BufferedImage.TYPE_INT_RGB
        );
        osg  = (Graphics2D)offScreen.getGraphics();
        osg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        font = new Font( "SansSerif", Font.BOLD, 14 );
        osg.setFont(font);
        System.out.println(getClass().getName() + "()");
    }

    public void init()
    {
        super.init();
        System.out.println("Codebase: " + getCodeBase());
        System.out.println("Docbase.: " + getDocumentBase());
        System.out.println(getClass().getName() + "::init()");
    }

    public void start()
    {
        System.out.println(getClass().getName() + "::start()");
        super.start();
    }

    public void stop()
    {
        System.out.println(getClass().getName() + "::stop()");
        super.stop();
    }

    public void destroy()
    {
        System.out.println(getClass().getName() + "::destroy()");
        super.destroy();
    }

    //---------------------------------------------------------------
    // Off-Screen painting
    //---------------------------------------------------------------
    abstract public void drawScreen( Graphics2D osg );

    public void Refresh()
    {
        drawScreen( osg );
        Graphics g = getGraphics();
        paint( g );
        //g.dispose();
    }

    public void paint( Graphics g )
    {
        if(g != null) g.drawImage(offScreen, 0, 0, null);
    }

    public void update( Graphics g ) { paint(g); }

    // **************************************************************
    // **************************************************************
    //
    // Helper functions
    //
    // **************************************************************
    // **************************************************************

    //---------------------------------------------------------------
    // Parameter handling
    //---------------------------------------------------------------

    String paramAsString(String name, String defValue)
    {
        try { return getParameter(name); }
        catch(Exception e) { return defValue; }
    }

    int paramAsInt(String name, int defValue)
    {
        try { return Integer.parseInt(getParameter(name)); }
        catch(Exception e) { return defValue; }
    }

    boolean paramAsBoolean(String name, boolean defValue)
    {
        try
        {
            String value = getParameter(name);
            return value.equals("yes") || value.equals("on") || value.equals("true");
        }
        catch(Exception e) { return defValue; }
    }

    //---------------------------------------------------------------
    // Sleep for a while
    //---------------------------------------------------------------
    void Sleep( int msec )
    {
        try { Thread.sleep(msec); } catch(InterruptedException e) { }
    }

    //---------------------------------------------------------------
    // Forming urls
    //---------------------------------------------------------------

    public URL getURL( String fileName ) throws MalformedURLException
    {
        return getClass().getClassLoader().getResource(fileName);
    }

    public InputStream getURLAsStream(String fileName) throws MalformedURLException, IOException
    {
        return getURL(fileName).openStream();
    }

    //---------------------------------------------------------------
    // Read an image
    //---------------------------------------------------------------

    public Image getJARImage( String fileName )
    {
        try
        {
            Helper.DEBUG( "Loading: " + fileName );
        
            MediaTracker mt = new MediaTracker(this);
            Image image = getImage(getURL(fileName));
            mt.addImage( image, 0 );
            mt.waitForAll();
            return image;
        }
        catch(MalformedURLException e) { e.printStackTrace(); }
        catch(InterruptedException e)  { e.printStackTrace(); }
        return null;
    }

    //---------------------------------------------------------------
    // Read a file
    //---------------------------------------------------------------
    public String[] getJARFile( String fileName )
    {
        try
        {
            Helper.DEBUG( "Loading: " + fileName );
            String[] result;

            InputStream inStream  = getURLAsStream(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));

            Vector<String> lines = new Vector<String>();
            for(String line = reader.readLine(); line != null; line = reader.readLine())
            {
                // Cut the comments
                if(line.indexOf("#") != -1)
                {
                    line = line.substring(0, line.indexOf("#"));
                }

                // Trim the line
                line = line.trim();
                if(line.length() == 0) continue;

                // Append, if line starts with \
                if(lines.size() > 0 && line.charAt(0) == '\\')
                {
                    int lastLine = lines.size()-1;
                    String prevLine = (String)lines.get(lastLine);
                    lines.set(lastLine, prevLine += " " + line.substring(1));
                }
                else
                {
                    lines.add(line);
                }
            }

            reader.close();
            inStream.close();

            result = new String[lines.size()];
            for(int i = 0; i < lines.size(); i++)
            {
                result[i] = (String)(lines.get(i));
            }
            return result;
        }
        catch(MalformedURLException e) { e.printStackTrace(); }
        catch(IOException e)           { e.printStackTrace(); }
        return null;
    }
}
