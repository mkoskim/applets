/********************************************************************
 ********************************************************************
 *
 * Diamonds
 *
 ********************************************************************
 ********************************************************************/

/*-------------------------------------------------------------------
 * Import some needed packages
 *-------------------------------------------------------------------
 */
import java.util.*;
import java.text.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.awt.event.*;

public class Diamonds
    extends GameEngine
    implements KeyListener, MouseListener
{

    //---------------------------------------------------------------
    // initing and destroying
    //---------------------------------------------------------------
    private static final long serialVersionUID = 1;
    
    //---------------------------------------------------------------
    // initing and destroying
    //---------------------------------------------------------------
    public Diamonds()
    {
        super(Setup.WIDTH, Setup.HEIGHT);
    }

    public void init()
    {
        super.init();

        Helper.debugging = paramAsBoolean("debugging", false);

        initImages();
    }

    public void start()
    {
        super.start();
        addKeyListener( this );
        addMouseListener(this);

        score = 0;
        playing = false;
        ClearBoard();
        Refresh();
    }

    public void stop()
    {
        super.stop();
        removeKeyListener(this);
        removeMouseListener(this);
    }

    /*****************************************************************
     * Constants
     *****************************************************************/
    public final int width     = 8;
    public final int height    = 8;
    public final int padsize   = 40;
    public final int nDiamonds = 7;

    boolean playing = false;

    /* Current score */
    int score = 0;

    /* For animating explosions */
    boolean explosionOn = false;
    int explosionPhase = 0;

    /*****************************************************************
     * Images
     *****************************************************************/

    Image[] diamonds;
    Image[] explosions;
    Image selection;
    Image background;

    /*****************************************************************
     * Init the images needed by the applet.
     *****************************************************************/
    void initImages()
    {
        explosions = Helper.cutImage
        (
            getJARImage( "images/explosions.png" ),
            40, 40,         // Size of the subimages
            1,              // Padding at the images
            4, 2,           // Columns x rows
            Color.white     // Transparent color
        );

        diamonds = Helper.cutImage
        (
            getJARImage( "images/diamonds.png" ),
            40, 40,         // Size of the subimages
            1,              // Padding at the images
            4, 2,           // Columns x rows
            Color.white     // Transparent color
        );
        
        // The last "diamond" is selection cursor
        selection = diamonds[nDiamonds];

        createBackground();
    }

    void createBackground()
    {
        /* Create the game background; with this it's easy to "clear"
         * the background.
         */
        background = new BufferedImage
        (
            width * padsize, height*padsize + 20,
            BufferedImage.TYPE_INT_RGB
        );

        /* The image contains a 320x320 playing area and at the bottom
         * some extra (+20) for showing the score. The whole area
         * is first filled to white...
         */
        Graphics g = background.getGraphics();
        g.setFont( font );
        g.setColor( Color.white );
        g.fillRect(0,0,width*padsize, height*padsize + 20);

        /* ...And then the background (chess pattern) is drawn.
         */
        for(int x = 0; x < width; x++)
        {
            for(int y = 0; y < height; y++)
            {
                int X = x*padsize, Y = y*padsize;
                g.setColor( (((x&1) ^ (y&1)) == 0) ? Color.gray : Color.lightGray );
                g.fillRect(X,Y,padsize,padsize);
            }
        }
        /* Put the "Score" text ready to the background.
         */
        g.setColor( Color.black );
        g.drawString( "Score:", 0, height * padsize + 14);
        g.dispose();
    }

    /*****************************************************************
     *
     * Initializations
     *
     *****************************************************************/
    
    /*****************************************************************
     * Board
     *****************************************************************/
    int[][] board  = new int[width][height];

    void ClearBoard()
    {
        for(int x = 0; x < width; x++)
        {
            for(int y = 0; y < height; y++)
            {
                board[x][y] = 0;
            }
        }
    }
    
    /* Creating new diamonds w/ random */
    void Random( int x, int y )
    {
        board[x][y] = (int)(nDiamonds*Math.random()) + 1;
    }

    /* Make an empty board and drop in the diamonds. Ensure, that there's
     * no existing rows and that there's movable diamonds. */
    void initBoard()
    {
        do
        {
            for(int x = 0; x < width; x++)
            {
                for(int y = 0; y < height; y++)
                {
                    do Random(x,y); while(hasRow(x,y));
                }
            }
        } while(movables() == 0);
    }

    boolean hasColor( int x, int y, int c )
    {
        if(x < 0 || x >= width || y < 0 || y >= height) return false;
        return board[x][y] == c;
    }

    boolean hasRow( int x, int y )
    {
        int c = board[x][y];
        return
            (hasColor(x-1,y,c) && hasColor(x-2,y,c)) ||
            (hasColor(x+1,y,c) && hasColor(x+2,y,c)) ||
            (hasColor(x,y-1,c) && hasColor(x,y-2,c)) ||
            (hasColor(x,y+1,c) && hasColor(x,y+2,c));
    }

    /*****************************************************************
     * Checking out for possible diamonds to move - if there's none,
     * the game is over.
     *****************************************************************/

    /* Checking, if a diamond can be moved */
    boolean isMovable( int x, int y )
    {
        int c = board[x][y];
        if(c == 0) return false;

        if( (hasColor(x-2, y, c) && hasColor(x-3, y, c)) ||
            (hasColor(x+2, y, c) && hasColor(x+3, y, c)) ||
            (hasColor(x, y-2, c) && hasColor(x, y-3, c)) ||
            (hasColor(x, y+2, c) && hasColor(x, y+3, c)))
            return true;

        if( hasColor(x-1, y-1, c) &&
            (hasColor(x-1, y+1,c ) ||
             hasColor(x+1, y-1, c) ||
             hasColor(x-1, y-2, c) ||
             hasColor(x-2, y-1, c)))
            return true;

        if( hasColor(x+1, y-1, c) &&
            (hasColor(x+1, y+1, c) ||
             hasColor(x-1, y-1, c) ||
             hasColor(x+1, y-2, c) ||
             hasColor(x+2, y-1, c)))
            return true;

        if( hasColor(x-1, y+1, c) &&
            (hasColor(x-1, y-1, c) ||
             hasColor(x+1, y+1, c) ||
             hasColor(x-2, y+1, c) ||
             hasColor(x-1, y+2, c)))
            return true;

        if( hasColor(x+1, y+1, c) &&
            (hasColor(x+1, y-1, c) ||
             hasColor(x-1, y+1, c) ||
             hasColor(x+2, y+1, c) ||
             hasColor(x+1, y+2, c)))
            return true;

        return false;
    }

    /* Count movable diamonds - used to determine end of game */
    int movables()
    {
        int moves = 0;
        for(int x = 0; x < width; x++)
        {
            for(int y = 0; y < height; y++)
            {
                if(isMovable(x,y)) moves++;
            }
        }
        return moves;
    }

    /*****************************************************************
     * Diamond destroying & dropping
     *****************************************************************/

    /* Score array stores the aquired points for each destroyed
     * diamond (used for showing this info). The points starts
     * from zero and increase by +10 for every adjancent destroyed
     * diamond - so, the more you can destroy with one move, the
     * more score you get.
     */
    int[][] scores = new int[width][height];
    int points;

    /* Destroy diamond at give location and update scoring */
    void destroyAt( int x, int y )
    {
        board [x][y] = 0;
        scores[x][y] = points;
        score  += points;
        points += 10;
    }

    /* Clear scoring table */
    void clearScoreTable()
    {
        for(int x = 0; x < width; x++)
        {
            for(int y = 0; y < height; y++)
            {
                scores[x][y] = 0;
            }
        }
    }

    /* Try to destroy a row at (x,y) */
    boolean tryDestroy( int x, int y )
    {
        int c = board[x][y];
        if(c == 0) return false;
        boolean success = false;

        int x1, x2;
        for(x1 = x;   x1 > 0     && board[x1-1][y] == c; x1--);
        for(x2 = x+1; x2 < width && board[x2][y]   == c; x2++);
        if(x2-x1 >= 3)
        {
            for(int i = x1; i < x2; i++) destroyAt(i,y);
            success = true;
        }

        int y1, y2;
        for(y1 = y;   y1 > 0      && board[x][y1-1] == c; y1--);
        for(y2 = y+1; y2 < height && board[x][y2]   == c; y2++);
        if(y2-y1 >= 3)
        {
            for(int i = y1; i < y2; i++) destroyAt(x,i);
            success = true;
        }

        return success;
    }

    public boolean tryDestroy( Point from )
    {
        return tryDestroy( from.x, from.y );
    }

    public boolean tryDestroy()
    {
        boolean hasDestroyed = false;
        for(int y = 0; y < height; y++)
        {
            for(int x = 0; x < width; x++)
            {
                hasDestroyed |= tryDestroy( x, y );
            }
        }
        return hasDestroyed;
    }

    /* For diamond scrolling */
    int[] lowestEmpty = new int[width];
    boolean scrollOn = false;
    int scrollPhase = 0;

    void dropDiamonds()
    {
        boolean hadDrops = false;

        do
        {
            /* Go through the diamonds and try to destroy something.
             * If something is found, then we scroll the table again.
             */
            /* Animate explosions */
            explosionOn = true;
            for(explosionPhase = 0; explosionPhase < 8; explosionPhase++)
            {
                Refresh();
                Sleep(75);
            }
            explosionOn = false;
            clearScoreTable();

            /* Drop the diamonds: For each column, find the lowest
             * empty location. Scroll the diamonds, select a new
             * one and animate transfer of diamonds to that location.
             */
            do
            {
                scrollOn = false;

                for(int x=0; x < width; x++)
                {
                    /* Find the lowest empty location */
                    int i;
                    for(i = height; i-- != 0; )
                    {
                        if(board[x][i] == 0) break;
                    }
                    lowestEmpty[x] = i;

                    /* Scroll one step */
                    if(i != -1)
                    {
                        for(; i-- != 0;)
                        {
                            board[x][i+1] = board[x][i];
                        }
                        Random(x, 0);
                        scrollOn = true;
                    }
                }

                for(scrollPhase = 0; scrollPhase < padsize; scrollPhase += 5)
                {
                    Refresh();
                    Sleep(10);
                }

            } while( scrollOn );
        } while(tryDestroy());
        if(movables() == 0) doGameOver();
    }

    /*****************************************************************
     * Used mouse events
     *****************************************************************/
    Point mouseAt  = new Point(-1, -1);
    Point selected = new Point(-1, -1);

    boolean IsLegalSelected(int x, int y)
    {
        int dx = x - selected.x;
        int dy = y - selected.y;
        return
            (dx ==  0 && dy ==  1) ||
            (dx ==  0 && dy == -1) ||
            (dx ==  1 && dy ==  0) ||
            (dx == -1 && dy ==  0);
    }

    void getMousePos( MouseEvent event )
    {
        int x = event.getX() / padsize;
        int y = event.getY() / padsize;
        if(x < 0 || x >= width || y < 0 || y >= height)
        {
            mouseAt.x = mouseAt.y = -1;
        }
        else
        {
            mouseAt.x = x;
            mouseAt.y = y;
        }
    }

    public void mouseClicked( MouseEvent event )
    {
        /* Mouse clicked after a game over */
        if(!playing)
        {
            initBoard();
            score = 0;
            selected.setLocation(-1, -1);
            playing = true;
            Refresh();
            return ;
        }

        /* Mouse clicked during the game */
        points = 0;
        getMousePos( event );

        if(mouseAt.x == -1 && mouseAt.y == -1) return;
        if(mouseAt.equals(selected))
        {
            selected.setLocation(-1, -1);
        }
        else if(
            selected.x != -1 &&
            selected.y != -1 &&
            IsLegalSelected(mouseAt.x, mouseAt.y)
        )
        {
            /* Exchange the colors */
            int c1 = board[mouseAt.x][mouseAt.y];
            int c2 = board[selected.x][selected.y];
            board[mouseAt.x][mouseAt.y]   = c2;
            board[selected.x][selected.y] = c1;

            Refresh(); Sleep( 200 );

            /* Try to destroy */
            boolean b1 = tryDestroy(mouseAt);
            boolean b2 = tryDestroy(selected);

            /* No destroy, change diamonds back */
            if(!b1 && !b2)
            {
                board[mouseAt.x][mouseAt.y]   = c1;
                board[selected.x][selected.y] = c2;
            }
            /* Drop the diamonds */
            else
            {
                dropDiamonds();
            }
            selected.setLocation(-1, -1);
        }
        else
        {
            selected.x = mouseAt.x;
            selected.y = mouseAt.y;
        }
        Refresh();
    }

    public void mouseEntered(MouseEvent event)  {}
    public void mouseExited(MouseEvent event)  {}
    public void mousePressed(MouseEvent event)  {}
    public void mouseReleased(MouseEvent event) {}

    /*****************************************************************
     * Key pressing - by pressing SHIFT key, the movable diamond
     * are shown.
     *****************************************************************/

    boolean showMovables = false;

    public void keyPressed( KeyEvent event )
    {
        if(event.getKeyCode() == KeyEvent.VK_SHIFT)
        {
            showMovables = true;
            Refresh();
        }
        if(event.getKeyCode() == KeyEvent.VK_ESCAPE)
        {
            doGameOver();
        }
    }

    public void keyReleased( KeyEvent event )
    {
        if(event.getKeyCode() == KeyEvent.VK_SHIFT)
        {
            showMovables = false;
            Refresh();
        }
    }

    public void keyTyped( KeyEvent event )
    {
    }

    /*****************************************************************
     * Do game over
     *****************************************************************/
    void doGameOver()
    {
        if(!playing) return ;
        
        Helper.DEBUG("Game over!");

        ClearBoard();
        /* Animate explosions */
        explosionOn = true;
        for(explosionPhase = 0; explosionPhase < 8; explosionPhase++)
        {
            Refresh();
            Sleep(75);
        }
        explosionOn = false;
        playing = false;
        Refresh();
    }
    
    /*****************************************************************
     * Drawing routines
     *****************************************************************/

    public void drawScreen(Graphics2D g)
    {
        g.drawImage( background, 0, 0, null );

        g.setColor(Color.black);

        /* Draw board */
        for(int x = 0; x < width; x++)
        {
            for(int y = 0; y < height; y++)
            {
                int     X = x*padsize + 1, Y = y*padsize + 1;
                boolean isSelected = (selected.x == x && selected.y == y);
                boolean hasMouse   = (mouseAt.x == x && mouseAt.y == y);

                /* After a game over, just show the situation */
                if(!playing)
                {
                    if(board[x][y] != 0)
                    {
                        g.drawImage( diamonds[ board[x][y]-1 ], X, Y, null );
                    }
                    continue;
                }

                /* Draw empty board locations */
                if(board[x][y] == 0)
                {
                    /* If explosion animation is going on, draw the explosion and
                     * the score aquired for this deletion.
                     */
                    if(explosionOn)
                    {
                        g.drawImage(
                            explosions[ explosionPhase ],
                            X, Y,
                            null
                        );
                        if(scores[x][y] != 0)
                        {
                            Helper.text2Center(
                                g,
                                X, Y, padsize, padsize, 
                                "" + scores[x][y]
                            );
                        }
                    }
                }
                /* Draw diamond - if scrolling is going on, use the offset */
                else
                {
                    int offset = 0;
                    if(scrollOn && y <= lowestEmpty[x])
                    {
                        offset = -padsize + scrollPhase;
                    }

                    if(showMovables && isMovable(x,y))
                    {
                        g.drawRect(X+0, Y+offset+0, padsize-3, padsize-3);
                        g.drawRect(X+1, Y+offset+1, padsize-5, padsize-5);
                    }
                    g.drawImage( diamonds[ board[x][y]-1 ], X, Y + offset, null );
                }

                if(isSelected)
                {
                    g.drawImage( selection, X, Y, null );
                }
            }
        }

        if(!playing)
        {
            Helper.text2Center(
                g, 
                0, 0, width*padsize, height*padsize,
                "GAME OVER"
            );
        }

        g.drawString( score + "", 60, height * padsize + 14);
        if(showMovables)
        {
            g.drawString( "Movables: " + movables(), 160, height * padsize + 14);
        }
    }
}

