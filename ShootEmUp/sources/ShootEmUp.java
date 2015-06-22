/*********************************************************************
 *********************************************************************
 *
 * A somewhat classical space shoot'em-up.
 *
 * The ship is moving at the bottom of the play field. Enemy fleets
 * come from the top of the screen and you should destroy them
 * all.
 *
 *--------------------------------------------------------------------
 *
 * Keyboard:
 *
 * Arrow keys       Move the ship
 * CTRL             Shoot
 * SHIFT            Shields on
 * PAUSE            Pause - any key continues
 * ESC              Ends the game
 *
 *--------------------------------------------------------------------
 *
 * The idea: you should prevent the alien invasion by destroying
 * (almost) everything. Alien invasions are divided to phases.
 * Every phase have it's "ground support", i.e. immovable ground
 * objects, which launch movable objects (alien ships & shots).
 *
 * When a ground object is destroyed, some bonus items are freed.
 * DO NOT shoot them - you should collect them with your ship.
 * If an alien (either a ship or a bullet) is hitted to a bonus
 * item, it's destroyed.
 *
 * When everything is destroyed, the player is adjancent to next
 * level.
 *
 *********************************************************************
 *********************************************************************/

import java.util.*;
import java.text.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.awt.event.*;

public class ShootEmUp
    extends GameEngine
    implements Runnable, KeyListener
{
    //---------------------------------------------------------------
    // Serial version
    //---------------------------------------------------------------

    private static final long serialVersionUID = 1;

    /****************************************************************
     ****************************************************************
     *
     * Interface to system
     *
     ****************************************************************
     ****************************************************************/

    Thread animator;    // Animator thread moves the world

    //---------------------------------------------------------------
    // Constructor
    //---------------------------------------------------------------

    public ShootEmUp()
    {
        super( Setup.WIDTH, Setup.HEIGHT );
    }

    //---------------------------------------------------------------
    // initing and destroying
    //---------------------------------------------------------------
    public void init()
    {
        super.init();

        Helper.debugging = paramAsBoolean("debugging", false);
        world = paramAsInt("world", 1);

        initImages();
        Wrecks.initialize(WIDTH, HEIGHT);
        Shot.configure(getJARFile("datafiles/shots.txt"));
        Weapon.configure(getJARFile("datafiles/weapons.txt"));
        World.configure(this, "datafiles/worlds.txt");

        animator = new Thread(this, "Animator");
        animator.start();
    }

    public void start()
    {
        super.start();
        addKeyListener( this );
        gameover = true;
        completed = false;
        score = 0;

        paused       = false;
        chConfig     = false;
        turn_left    = false;
        turn_right   = false;
        accelerate   = false;
        decelerate   = false;
        firing       = false;

        killall      = false;
        key_newgame = false;
    }

    public void stop()
    {
        super.stop();
        removeKeyListener(this);
    }

    /****************************************************************
     ****************************************************************
     *
     * The game itself
     *
     ****************************************************************
     ****************************************************************/

    //---------------------------------------------------------------
    // Colors for fading in/out things
    //---------------------------------------------------------------
    Color shaders[] = new Color[16];

    //---------------------------------------------------------------
    // Initialize the images needed by this game
    //---------------------------------------------------------------
    void initImages()
    {
        /* Create shading colors (semi-transparent) */
        Helper.createTransparentColorArray( shaders,    Color.black );
        Helper.reverse(shaders);

        Ship.initImages(this);
        Shot.initImages(this);
        Decoration.initImages(this);
        Weapon.initImages(this);
        Enemy.initImages(this);
    }

    //---------------------------------------------------------------
    // Game vars & consts
    //---------------------------------------------------------------

    int world = 1;
    int fleet = 1;
    boolean restZone = false; // Fleets without enemies
    boolean gameover = true;
    boolean completed = false;
    String  gameoverReason = "";

    boolean isGameOver() { return gameover && !completed; }

    static Ship ship = null;

    /****************************************************************
     ****************************************************************
     *
     * Keyboard handling
     *
     ****************************************************************
     ****************************************************************/

    public void keyPressed( KeyEvent event )
    {
        event.consume();
        int keycode = event.getKeyCode();
        if(isGameOver()) gameoverKeyPress(keycode); else gameKeyPress( keycode );
    }

    public void keyReleased( KeyEvent event )
    {
        event.consume();
        int keycode = event.getKeyCode();
        if(isGameOver()) gameoverKeyRelease(keycode); else gameKeyRelease( keycode );
    }

    public void keyTyped( KeyEvent event )
    {
        event.consume();
    }

    //---------------------------------------------------------------
    // Ship control variables
    //---------------------------------------------------------------

    boolean paused       = false;
    boolean chConfig     = false;
    boolean turn_left    = false;
    boolean turn_right   = false;
    boolean accelerate   = false;
    boolean decelerate   = false;
    boolean firing       = false;

    boolean killall      = false;
    boolean key_newgame = false;

    void gameKeyPress( int keycode )
    {
        if(paused) synchronized(this)
        {
            if(keycode == KeyEvent.VK_PAUSE) return ;
            //if(keycode == KeyEvent.VK_SHIFT) return ;
            paused = false;
            notifyAll();
        }

        if(keycode == KeyEvent.VK_UP   )   { accelerate = true; return; }
        if(keycode == KeyEvent.VK_DOWN )   { decelerate = true; return; }
        if(keycode == KeyEvent.VK_LEFT )   { turn_left  = true; return; }
        if(keycode == KeyEvent.VK_RIGHT)   { turn_right = true; return; }
        if(keycode == KeyEvent.VK_CONTROL) { firing     = true; return; }
        if(keycode == KeyEvent.VK_SHIFT)   { chConfig   = true; return; }
        //if(keycode == KeyEvent.VK_SHIFT)   { paused     = true; return; }

        if(keycode == KeyEvent.VK_ESCAPE)  { gameover = true; return; }
        if(keycode == KeyEvent.VK_PAUSE)   { paused   = true; return; }
        if(keycode == KeyEvent.VK_ENTER)   { key_newgame = true; return; }
        if(keycode == KeyEvent.VK_DELETE)  { killall = Helper.debugging; return; }
    }

    void gameKeyRelease( int keycode )
    {
        if(keycode == KeyEvent.VK_UP   )   { accelerate = false; return; }
        if(keycode == KeyEvent.VK_DOWN )   { decelerate = false; return; }
        if(keycode == KeyEvent.VK_LEFT )   { turn_left  = false; return; }
        if(keycode == KeyEvent.VK_RIGHT)   { turn_right = false; return; }
        if(keycode == KeyEvent.VK_CONTROL) { firing     = false; return; }
        if(keycode == KeyEvent.VK_SHIFT)   { chConfig   = false; return; }
    }

    //---------------------------------------------------------------
    // Keys during game over
    //---------------------------------------------------------------

    void gameoverKeyPress( int keycode )
    {
        if(keycode == KeyEvent.VK_ENTER) { key_newgame = true; return; }
        if(keycode == KeyEvent.VK_F1)    { key_newgame = true; fleet = 1; return; }

        if(keycode == KeyEvent.VK_PAGE_UP)
        {
            world = (world == 1) ? World.worlds.size() : world - 1;
            fleet = 1;
            return ;
        }
        if(keycode == KeyEvent.VK_PAGE_DOWN)
        {
            world = (world == World.worlds.size()) ? 1 : world + 1;
            fleet = 1;
            return ;
        }
    }

    void gameoverKeyRelease( int keycode )
    {
    }

    /****************************************************************
     ****************************************************************
     *
     * Drawing routines
     *
     ****************************************************************
     ****************************************************************/

    int sleeptime = 0;
    int scoreAtScreen = 0;
    int cHiding    = 0;
    DecimalFormat fScore = new DecimalFormat("00000000");

    // ---------------------------------------------------------------
    // Drawing the screen
    // ---------------------------------------------------------------
    public void drawScreen(Graphics2D g)
    {
        g.setFont( font );

        // Clear the area
        g.setColor( Color.black );
        g.fillRect( 0, 0, WIDTH, HEIGHT );

        /* Draw enemies, shots, ships, weapons and explosions */
        Wrecks.draw(g);
        Enemy.list.draw(g);
        Shot.drawAllHis(g);
        if(!isGameOver()) ship.draw(g);
        Shot.drawAllMine(g);
        if(!isGameOver()) ship.rack.draw(g);
        Explosion.list.draw(g);

        // Draw a "shader" (when requested) at the top of objects
        if(cHiding != 0)
        {
            cHiding--;
            if(cHiding < shaders.length)
            {
                g.setColor( shaders[cHiding] );
                g.fillRect(0, 0, WIDTH, HEIGHT);
            }
            if(cHiding == 0)
            {
                ParticleList.clearAll();
                Wrecks.clear();
                completed = false;
            }
        }

        // Update score at screen
        if(score - scoreAtScreen > 10000) scoreAtScreen += 5000 + (int)(Math.random()*2000);
        if(score - scoreAtScreen > 1000)  scoreAtScreen +=  500 + (int)(Math.random()*200);
        if(score - scoreAtScreen > 100)   scoreAtScreen +=   50 + (int)(Math.random()*20);
        if(score - scoreAtScreen > 10)    scoreAtScreen +=    5 + (int)(Math.random()*2);
        if(score > scoreAtScreen)         scoreAtScreen ++;

        // Draw points etc
        g.setColor(Color.lightGray);
        Helper.drawField( g, 2, 0, 120, "Score:", fScore.format(scoreAtScreen));
        if(Helper.debugging) g.drawString("Debug mode", 2, 28);
        if(!gameover)
            Helper.drawMeterBar( g, 2, HEIGHT-18, 118, "Energy:",  60, ship.energy, ship.maxEnergy);

        Helper.drawField( g, WIDTH-79, HEIGHT-18, 75, "CPU:", 40.0 - sleeptime, 40.0);

        // Information
        if(gameover)
        {
            Helper.text2Center(g, 0, 0, WIDTH, HEIGHT,
                "GAME OVER\n" +
                gameoverReason +
                "\n\n" +
                "World " + world + ":\n" + World.getName(world) + "\n" +
                "(" + World.getComment(world) + ")\n" +
                "Fleet: " + fleet + "/" + World.fleetCount(world)
            );
            if(!completed)
            {
                Helper.drawField( g, WIDTH/2 - 100, HEIGHT - 9*14, 200, "ENTER",      "Continue");
                Helper.drawField( g, WIDTH/2 - 100, HEIGHT - 8*14, 200, "F1",         "Start over");
                Helper.drawField( g, WIDTH/2 - 100, HEIGHT - 7*14, 200, "PG UP/DOWN", "Select world");
            }
        }
        else if(Enemy.fleetSpeed != 0)
        {
            Helper.text2Center(g, 0, 0, WIDTH, HEIGHT,
                "Get ready!\n\n\n" +
                "World " + world + ":\n" + World.getName(world) + "\n" +
                "\n" +
                "Fleet: " + fleet + "/" + World.fleetCount(world)
            );
        }
        /*
        for(int y = 0; y < Shot.DirImage.nDirs; y++)
        {
            g.drawImage(Shot.iShots[4].up[y], 50 + y*20, 50, null);
            g.drawImage(Shot.iShots[4].dn[y], 50 + y*20, 70, null);
        }
        */
    }

    /****************************************************************
     ****************************************************************
     *
     * Game playing functions
     *
     ****************************************************************
     ****************************************************************/

    boolean isFleetEmpty()
    {
        if(gameover) return false;
        if(!restZone && Enemy.list.isEmpty()) return true;
        if(restZone && key_newgame) return true;
        return false;
    }

    //---------------------------------------------------------------
    // Playing loop
    //---------------------------------------------------------------
    void Play()
    {
        long start_time = System.currentTimeMillis();

        if(!gameover)
        {
            ship.thrust.set(0,0);

            if(turn_left  && ship.speed.x > -15 ) ship.thrust.x -= 5;
            if(turn_right && ship.speed.x < +15 ) ship.thrust.x += 5;
            if(accelerate && ship.speed.y > -15 ) ship.thrust.y -= 6;
            if(decelerate && ship.speed.y < +15 ) ship.thrust.y += 4;

            ship.useRack( chConfig ? 1 : 0 );
            if(firing)   ship.rack.fire();
            if(killall)
            {
                for(int i = 0; i < Enemy.list.list.size(); i++)
                {
                    Particle p = (Particle)Enemy.list.list.get(i);
                    new Explosion(p);
                }
                Enemy.list.clear();
                killall = false;
            }
            ship.update();
        }

        /* Handling enemy fleets */
        if(Enemy.fleetSpeed != 0)
        {
            if(Enemy.fleetOffset.y < 0)
            {
                Enemy.fleetOffset.y += Enemy.fleetSpeed;
                if(Enemy.fleetOffset.y >= -45)
                {
                    Enemy.fleetSpeed--;
                    if(Enemy.fleetSpeed == 0) Enemy.fleetOffset.y = 0;
                }
            }
            else
            {
                Enemy.fleetOffset.y += Enemy.fleetSpeed;
                if(Enemy.fleetSpeed < 20) Enemy.fleetSpeed++;
                if(Enemy.fleetOffset.y > (HEIGHT-Wrecks.highest)) NewFleet();
            }
        }
        else if(isFleetEmpty())
        {
            /* Fleet destroyed */
            fleet++;
            if(fleet > World.fleetCount(world))
            {
                world++;
                fleet = 1;
            }
            if(world > World.count())
            {
                world = fleet = 1;
                gameover = true;
                completed = true;
                gameoverReason = "Game completed!";
            }
            else
            {
                Enemy.fleetSpeed    = 2;
                Enemy.fleetOffset.y = 1;
            }
        }

        /* Update all the lists */
        ParticleList.updateAll();

        /* Check for shot collisions */
        Shot.doInterCollisions();

        /* Check for enemy collisions */
        Enemy.list.doCollisions( Shot.myOffenceList );
        Enemy.list.doCollisions( Shot.myDefenceList );

        /* Do collisions for our ship */
        if(!gameover)
        {
            ship.doCollisions();
            if(ship.energy <= 0)
            {
                gameover = true;
                gameoverReason = "Ship destroyed";
            }
            else if(Enemy.gotThrough)
            {
                gameover = true;
                gameoverReason = "Enemy got through";
            }
            else if(Enemy.shotOwn)
            {
                gameover = true;
                gameoverReason = "Own soldiers killed";
            }
        }

        /* Clean all trashes (destroyed items) */
        ParticleList.cleanAll();

        /* Refresh screen */
        Refresh();

        int elapsed = (int)(System.currentTimeMillis() - start_time);
        sleeptime = 40 - elapsed;
        if(sleeptime > 0) Sleep( sleeptime );
    }

    //---------------------------------------------------------------
    // New enemy fleet
    //---------------------------------------------------------------
    void NewFleet()
    {
        ship.renewRacks();
        Wrecks.clear();
        Enemy.list.clear();

        if(fleet > World.fleetCount(world)) fleet = 1;
        World.create( world, fleet );

        Enemy.fleetOffset.set(0, 0);
        double lowestEnemy = 55;
        if(Wrecks.lowest > lowestEnemy) lowestEnemy = Wrecks.lowest;

        for(int i = 0; i < Enemy.list.list.size(); i++)
        {
            Enemy e = (Enemy)Enemy.list.list.get(i);
            double pos = e.getPosition().y + e.dimensions.y/2;
            if(lowestEnemy < pos) lowestEnemy = pos;
        }
        Enemy.fleetOffset.set(0, -55 - 10*(int)((lowestEnemy-55+9)/10));
        Enemy.fleetSpeed = 10;
        key_newgame = false;
        restZone = Enemy.list.isEmpty();
    }

    //---------------------------------------------------------------
    // New game initialization
    //---------------------------------------------------------------
    void NewGame()
    {
        gameover    = false;
        gameoverReason = "";
        completed   = false;
        key_newgame = false;
        paused      = false;

        score         = 0;
        scoreAtScreen = 0;

        Enemy.gotThrough = false;
        Enemy.shotOwn    = false;

        paused       = false;
        chConfig     = false;
        turn_left    = false;
        turn_right   = false;
        accelerate   = false;
        decelerate   = false;
        firing       = false;

        ParticleList.clearAll();
        ship.init();
        NewFleet();
    }

    /****************************************************************
     ****************************************************************
     *
     * Game thread
     *
     ****************************************************************
     ****************************************************************/

    public void run()
    {
        ship = new Ship();
        gameover = true;

        while(true)
        {
            key_newgame = false;
            /* Game over loop */
            while(!key_newgame) Play();
            cHiding = 0;
            NewGame();

            /* Game loop */
            while(!gameover)
            {
                /* Game paused */
                if(paused) synchronized(this)
                {
                    Refresh();
                    try{ wait(); } catch(InterruptedException e) { }
                }

                Play();

            }
            if(!completed) ship.doExplode();
            cHiding = 16;
        }
    }
}
