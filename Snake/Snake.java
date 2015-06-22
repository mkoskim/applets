/*********************************************************************
 *
 * Worm game
 *
 *********************************************************************/

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.applet.*;

public class Snake
    extends Applet
    implements Runnable, KeyListener
{
    /* Size of the game board */
    final int padsize = 20;
    final int width   = 20;
    final int height  = 20;

    Font  font;         // Default font
    Image offScreen;    // Offscreen image for double buffering

    Image[] snakeHeads;
    Image   snakeBody;
    Image[] diamonds;

    /* Snake */
    Point[] snake;
    Point   goingTo;
    int     snakeLength;
    int     direction = 0;

    Point herkku   = new Point(-1,-1);
    int herkkuType = 1;
    
    int points = 0;
    boolean stopped = true;

    /*****************************************************************
     * This helper function makes specific color transparent
     *****************************************************************/
    public static Image makeColorTransparent(Image im, final Color color)
    {
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

    /*****************************************************************
     * Helper function to cut images to smaller pieces
     *****************************************************************/
    Image[] imageCutter( Image source, int width, int height, int pad, int xcnt, int ycnt, Color transparent )
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
                cutted[y*xcnt+x] = makeColorTransparent( im, Color.black );
            }
        }
        return cutted;
    }

    /****************************************************************
     * Initializations
     ****************************************************************/

    public Snake()
    {
        offScreen = new BufferedImage(
            width*padsize, height*padsize,
            BufferedImage.TYPE_INT_RGB
        );
        addKeyListener( this );
    }

    /*****************************************************************
     * Init applet
     *****************************************************************/
    public void init()
    {
        System.out.println("init()");
        /* Initialize graphics etc */
        font = new Font( "SansSerif", Font.BOLD, 14 );
        try { initImages(); } catch(InterruptedException e) { }

        /* Finally, create a thread to move the snake */
        animator = new Thread(this);
        animator.start();
    }

    public void destroy()
    {
        super.destroy();
        System.out.println("destroy()");
    }

    /*****************************************************************
     * Init the images needed by the applet.
     *****************************************************************/
    void initImages() throws InterruptedException
    {
        /* We use a media tracker for waiting the image loading */
        MediaTracker mt = new MediaTracker(this);

        /* Start image loading... */
        Image images = getImage( getDocumentBase(), "images.png" );
        mt.addImage( images, 0 );

        /* During loading, do some extra initializations...
         */

        /* ...And then wait, until they have been loaded */
        mt.waitForAll();

        /* Cutting the picture. The image contains 4 x 5 matrix
         * of images (size: 20 x 20). These smaller images are
         * cutted from the main image
         */
        Image[] cutted = imageCutter( images, 24, 24, 2, 4, 5, Color.black );

        snakeHeads = new Image[16];
        for(int i = 0; i < 16; i++) snakeHeads[i] = cutted[i];
        snakeBody = cutted[16];
        diamonds = new Image[3];
        for(int i = 0; i < 3; i++) diamonds[i] = cutted[17+i];
    }

    /* ---------------------------------------------------------------
     * Drawing the screen
     * ---------------------------------------------------------------
     */
    void initSnake()
    {
        snake = new Point[5];
        goingTo  = new Point(10,  9);
        snake[0] = new Point(10, 10);
        snake[1] = new Point(10, 11);
        snake[2] = new Point(10, 12);
        snake[3] = new Point(10, 13);
        snake[4] = new Point(10, 14);
        snakeLength = 5;
    }

    void draw2Center( Graphics2D g, int X, int Y, int width, int height, String text )
    {
        FontRenderContext c = g.getFontRenderContext();
        Rectangle2D rect = font.getStringBounds( text, c );
        int padx   = (width  - (int)rect.getWidth())/2;
        int pady   = (height + (int)rect.getHeight())/2;
        g.drawString(text, X + padx, Y + pady);
    }

    /* ---------------------------------------------------------------
     * Drawing the screen
     * ---------------------------------------------------------------
     */
    int animationPhase = 0;
    int lastDirection = 0;

    void drawScreen(Graphics2D g)
    {
        /* Clear the area */
        g.setColor( Color.black );
        g.fillRect( 0, 0, width*padsize, height*padsize );

        /* Draw snake body */
        for(int i = 1; i < snakeLength; i++)
        {
            int dx = (snake[i-1].x - snake[i].x)*animationPhase*(padsize/4);
            int dy = (snake[i-1].y - snake[i].y)*animationPhase*(padsize/4);
            g.drawImage(
                snakeBody,
                snake[i].x * padsize + dx, snake[i].y * padsize + dy,
                null
            );
        }

        /* Draw herkku, so that herkku is over snake body */
        g.drawImage(
            diamonds[herkkuType],
            herkku.x * padsize, herkku.y * padsize,
            null
        );

        /* Draw snake head - this way the head is on the top of herkku */
        {
            int dx = (goingTo.x - snake[0].x)*animationPhase*(padsize/4);
            int dy = (goingTo.y - snake[0].y)*animationPhase*(padsize/4);
            g.drawImage(
                snakeHeads[lastDirection*4 + ((animationPhase+1) % 4)],
                snake[0].x * padsize + dx, snake[0].y * padsize + dy,
                null
            );
        }

        /* Draw points etc */
        g.setFont( font );
        g.setColor( Color.white );
        g.drawString( "Points: " + points, 2, 12 );

        if(stopped)
        {
            draw2Center(g, 0, 0, width*padsize, height*padsize, "Game over!");
        }
    }

    /* ---------------------------------------------------------------
     * Interfacing to a system
     * ---------------------------------------------------------------
     */
    public void paint( Graphics g )
    {
        Graphics2D osg = (Graphics2D)offScreen.getGraphics();
        drawScreen( osg );
        osg.dispose();
        g.drawImage( offScreen, 0, 0, null );
    }

    public void update( Graphics g ) { paint(g); }

    /* ---------------------------------------------------------------
     * Keyboard handling
     * ---------------------------------------------------------------
     */
    public void keyPressed( KeyEvent event )
    {
        /* If game is over, notify the animator thread to
         * start a new game.
         */
        if(stopped)
        {
            stopped = false;
            return ;
        }

        int keycode = event.getKeyCode();
        if( keycode == KeyEvent.VK_ESCAPE )   { stopped = true; }
        else if(keycode == KeyEvent.VK_UP    && lastDirection != 1) { direction = 0; }
        else if(keycode == KeyEvent.VK_DOWN  && lastDirection != 0) { direction = 1; }
        else if(keycode == KeyEvent.VK_LEFT  && lastDirection != 3) { direction = 2; }
        else if(keycode == KeyEvent.VK_RIGHT && lastDirection != 2) { direction = 3; }
    }

    public void keyReleased( KeyEvent event ) { }

    public void keyTyped( KeyEvent event ) { }

    /* ---------------------------------------------------------------
     * Thread for moving the snake
     * ---------------------------------------------------------------
     */
    Thread animator;    // Animator thread moves the world

    public void run()
    {
        newHerkku();
        initSnake();
        while(true)
        {
            WaitForKey();
            Play();
            System.out.println("Game over!");
        }

    }

    void Sleep( int msec )
    {
        try { Thread.sleep(msec); } catch(InterruptedException e) { }
    }

    void newHerkku()
    {
        herkku.x = (int)(Math.random()*(width-4)) + 2;
        herkku.y = (int)(Math.random()*(height-4)) + 2;
        herkkuType = (int)(Math.random()*3);
    }

    void WaitForKey()
    {
        stopped = true;
        repaint();
        while(stopped) Sleep(250);
    }

    void Play()
    {
        newHerkku();
        initSnake();
        points = 0;
        stopped = false;
        direction = lastDirection = 0;

        while(!stopped)
        {
            for(int i = 0; i < 4; i++)
            {
                animationPhase = i;
                repaint();
                Sleep(50);
            }

            /* Break, if the snake runs out of the board */
            if(
                goingTo.x < 0 || goingTo.x >= width ||
                goingTo.y < 0 || goingTo.y >= height)
            {
                break;
            }

            /* Check, if snake gets the herkku */
            if(goingTo.equals(herkku))
            {
                Point[] newSnake = new Point[snakeLength+1];
                for(int i = 0; i < snakeLength; i++)
                {
                    newSnake[i] = snake[i];
                }
                newSnake[snakeLength] = snake[snakeLength-1];
                snake = newSnake;
                snakeLength++;
                newHerkku();
                points += 10;
            }
            else /* Otherwise, check, if the snake hits itself */
            for(int i = 1; i < snakeLength; i++)
            {
                if(goingTo.equals( snake[i] )) stopped = true;
            }
            if(stopped) break;

            /* New directions */
            for(int i = snakeLength-1; i > 0; i--)
            {
                snake[i] = snake[i-1];
            }
            snake[0] = new Point(goingTo);

            switch( direction )
            {
                case 0: goingTo.y--; break;
                case 1: goingTo.y++; break;
                case 2: goingTo.x--; break;
                case 3: goingTo.x++; break;
            }
            lastDirection = direction;
        }
    }
}
