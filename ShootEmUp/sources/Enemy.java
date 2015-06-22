// ---------------------------------------------------------------
// Enemies
// ---------------------------------------------------------------

import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;

abstract class EnemyObject extends Particle
{
    static Vec2d fleetOffset = new Vec2d();
    static int   fleetSpeed  = 0;

    int points = 0;

    EnemyObject() { super(); }
    EnemyObject(double energy) { super(energy); }

    Vec2d getPosition()
    {
        Vec2d p = super.getPosition();
        p.add(fleetOffset);
        return p;
    }

    public void doExplode()
    {
        if(points > 0) new GotPoints(this);
        super.doExplode();
    }
}

// ---------------------------------------------------------------
// Generic enemy class
// ---------------------------------------------------------------

abstract class Enemy extends EnemyObject
{
    //----------------------------------------------------------------
    // Images
    //----------------------------------------------------------------
    static Image[]   iTanks;
    static Image[]   iLabTanks;
    static Image[][] iSoldiers;
    static Image[][] iCreatures;
    static Image[]   iFFTorus, iAttrTorus;

    static void initImages(GameEngine engine)
    {
        Image[] temp;

        //-----------------------------------------------------------
        // Cut tank images
        //-----------------------------------------------------------
        iTanks = Helper.cutImage
        (
            engine.getJARImage( "images/enemyTanks.png" ),
            40, 40,         // Size of the subimages
            1,              // Padding at the images
            4, 3,           // Columns x rows
            Color.black     // Transparent color
        );

        //-----------------------------------------------------------
        // Cut infantry
        //-----------------------------------------------------------
        temp = Helper.cutImage(
            engine.getJARImage("images/enemyInfantry.png"),
            19,19,
            1,
            8,3,
            Color.BLACK
        );

        /* Cut the small men */
        iSoldiers = new Image[8][];
        iSoldiers[0] = Helper.sliceImageArray(temp, 0, 2); // White guy
        iSoldiers[1] = Helper.sliceImageArray(temp, 2, 2); // Red fleeing enemy
        iSoldiers[2] = Helper.sliceImageArray(temp, 4, 2); // blue own guy
        iSoldiers[3] = Helper.sliceImageArray(temp, 6, 2); // blue monsterhead
        iSoldiers[4] = Helper.sliceImageArray(temp, 8, 2); // moving enemy trooper
        iSoldiers[5] = Helper.sliceImageArray(temp,10, 2); // Grounded enemy trooper
        iSoldiers[6] = Helper.sliceImageArray(temp,12, 2); // Enemy scientist
        iSoldiers[7] = Helper.sliceImageArray(temp,14, 2); // Immortal scientist

        iCreatures = new Image[2][];
        iCreatures[0] = Helper.sliceImageArray(temp,16, 4); // Immortal creature
        iCreatures[1] = Helper.sliceImageArray(temp,20, 4); // Green creature

        //-----------------------------------------------------------
        // Cut lab tanks
        //-----------------------------------------------------------
        iLabTanks = Helper.cutImage
        (
            engine.getJARImage( "images/enemyLabTanks.png" ),
            40, 40,         // Size of the subimages
            1,              // Padding at the images
            4, 3,           // Columns x rows
            Color.black     // Transparent color
        );

        //-----------------------------------------------------------
        // Create toruses for force fields
        //-----------------------------------------------------------
        iFFTorus = new Image[2];
        for(int i = 0; i < 2; i++)
        {
            iFFTorus[i] = Helper.createHaloTorus(20, 5 + i, 1.0, new Color(0x77CCFF));
        }
        iAttrTorus = new Image[2];
        for(int i = 0; i < 2; i++)
        {
            iAttrTorus[i] = Helper.createHaloTorus(20, 5 + i, 1.0, new Color(0xFFCC77));
        }
    }

    //----------------------------------------------------------------
    // Static part
    //----------------------------------------------------------------
    static boolean gotThrough = false;
    static boolean shotOwn    = false;

    static ParticleList list = new ParticleList();

    //----------------------------------------------------------------
    // Enemy instances
    //----------------------------------------------------------------
    public Enemy( double x, double y, double energy, int points)
    {
        super(energy);
        this.points = points;
        position.set( x, y );
        list.add(this);
    }

    //*
    public boolean outOfRange()
    {
        Vec2d p = getPosition();
        if(speed.y > 0) return p.y - dimensions.y/2 > Setup.HEIGHT;
        if(speed.y < 0) return p.y + dimensions.y/2 < 0;
        return false;
    }
    /* */

    public void doExplode()
    {
        new Explosion(this);
        new Explosion(this, Explosion.typeLargeHalo);
        super.doExplode();
    }

    public void draw(Graphics2D g)
    {
        if(weapon != null) weapon.draw(g);
        drawEnergyBar(g);
        super.draw(g);
    }

    // -----------------------------------------------------------
    // Enemy weapons
    // -----------------------------------------------------------
    class Gun
    {
        Vec2d pos;
        Vec2d   direction;
        Shot.Configuration shotConfig;
        Gun(double x, double y, double dx, double dy, String shotType)
        {
            pos = new Vec2d(x,y);
            direction = new Vec2d(dx, dy);
            shotConfig = Shot.getConfig(shotType);
        }
    }

    class Weapon
    {
        Particle parent;
        Gun[] guns;

        double fireProbability;
        int    firerate;
        int    nSerie;

        Vec2d   direction;
        boolean isFollowing = false;

        int loading = 0;
        int serie   = 0;

        Weapon( Particle parent, Gun[] guns, double probability, int series, int rate )
        {
            this.parent = parent;
            this.guns = guns;
            this.direction = new Vec2d(0, 1);
            this.fireProbability = probability;
            this.nSerie = series;
            this.firerate = rate;
        }

        Weapon(Particle parent, Gun guns[])
        {
            this(parent, guns, 0.03, 1, 8);
        }

        void update()
        {
            if(isFollowing)
            {
                direction.set(ShootEmUp.ship.getPosition());
                direction.sub(parent.getPosition());
                direction.normalize();
            }

            if(loading > 0)
            {
                loading--;
            }
            else if(serie > 0)
            {
                Vec2d p = new Vec2d(), d = new Vec2d();

                for(int i = 0; i < guns.length; i++)
                {
                    /* Position */
                    p.set(guns[i].pos);
                    p.directTo(direction);
                    p.add(parent.getPosition());

                    /* Direction */
                    d.set(guns[i].direction);
                    d.directTo(direction);

                    Shot.createHis(p, guns[i].shotConfig, d);
                }
                loading = firerate;
                serie--;
            }
            else if(Math.random() < fireProbability) serie = nSerie;
        }

        void draw(Graphics2D g)
        {
            Vec2d p = new Vec2d();
            g.setColor(Color.yellow);
            for(int i = 0; i < guns.length; i++)
            {
                /* Position */
                p.set(guns[i].pos);
                p.directTo(direction);
                p.add(parent.getPosition());
                g.drawOval( (int)p.x-2, (int)p.y-2, 5, 5);
            }
        }
    }

    Weapon weapon = null;

    // -----------------------------------------------------------
    // Enemy force fields (against shots)
    // -----------------------------------------------------------
    class ForceField
    {
        Particle parent = null;
        double factor;

        ForceField(Particle parent, double factor)
        {
            this.parent = parent;
            this.factor = factor;
        }
        ForceField(Particle parent) { this(parent, 300.0); }

        void doForce(Vec2d position, Particle p)
        {
            Vec2d distance = new Vec2d(p.getPosition());
            distance.sub(position);
            double length = distance.length();
            if(length < 10) length = 10;
            distance.normalize( factor / length );
            p.speed.add(distance);
        }

        void update()
        {
            Vec2d position = parent.getPosition();

            for(int i = 0; i < Shot.myDefenceList.list.size(); i++)
            {
                doForce( position, (Particle)Shot.myDefenceList.list.get(i) );
            }
            for(int i = 0; i < Shot.myOffenceList.list.size(); i++)
            {
                doForce( position, (Particle)Shot.myOffenceList.list.get(i) );
            }
        }
    }

    ForceField ff = null;

    // -----------------------------------------------------------
    // Updating the additional parts
    // -----------------------------------------------------------

    public void update()
    {
        super.update();
        if(weapon != null) weapon.update();
        if(ff     != null) ff.update();
    }
}
