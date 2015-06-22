// ******************************************************************
// ******************************************************************
//
// Bases
//
// ******************************************************************
// ******************************************************************

import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;

// ---------------------------------------------------------------
// Base class for ground objects (a.k.a. bases)
// ---------------------------------------------------------------

abstract class EnemyBase extends Enemy
{
    String name;

    EnemyBase( String name, double x, double y, double energy, int points )
    {
        super( x, y, energy, points );
        recovering = 0.5;
        level      = 1;
        this.name  = name;
    }

    public void doExplode()
    {
        Wrecks.put(this);
        Explosion.doBits(this);
        Explosion.doExtraBits(this);
        super.doExplode();
    }
}

// ---------------------------------------------------------------
// Force Field station
// ---------------------------------------------------------------

class EnemyForceField extends EnemyBase
{
    int imageindex = 0;
    Image images[];

    EnemyForceField( String name, double energy, double x, double y, double factor, Image images[] )
    {
        super( name, x, y, energy, 500 );
        dimensions.set( 40, 40 );
        ff = new ForceField(this, factor);
        this.images = images;
    }

    EnemyForceField( double x, double y )
    {
        this("FF", 20, x, y, 300.0, iFFTorus);
    }

    public void draw(Graphics2D g)
    {
        g.setColor(Color.lightGray);
        //sphere2Center(g, dimensions.x);
        sphere2Center(g, dimensions.x);
        g.setColor(Color.black);
        text2Center(g, name);

        // Draw torus
        image2Center(g, images[imageindex]);
        imageindex = (imageindex + 1) % images.length;
        super.draw(g);
    }
}

class EnemyAttractor extends EnemyForceField
{
    EnemyAttractor( double x, double y )
    {
        super( "Atrct", 50, x, y, -300.0, iAttrTorus );
        recovering = 2.0;
    }
}

// ---------------------------------------------------------------
// Tank base
// ---------------------------------------------------------------
class EnemyTankBase extends EnemyBase
{
    int deploying = 0;
    int loading = 0;
    int tankType = 0;
    double probability = 0.015;

    EnemyTankBase( double x, double y, int tankType, double probability )
    {
        super( "TB" + tankType, x, y, 20, 1000 );
        this.tankType = tankType;
        dimensions.set( 70, 60 );
        loading   = 0;
        deploying = 0;
        Wrecks.background(this);
    }

    public void update()
    {
        super.update();
        if(loading > 0)
        {
            loading--;
        }
        else if(deploying > 0)
        {
            deploying++;
            if(deploying > 30)
            {
                deploying = 0;
                loading = 60;
                new EnemyTank( position.x, position.y, tankType );
            }
        }
        else if(Math.random() < probability)
        {
            //new EnemyTank( position.x, position.y, tankType );
            deploying = 1;
        }
    }

    public void doExplode()
    {
        new EnemyOperator( position.x-5, position.y, -1 );
        new EnemyOperator( position.x+5, position.y, +1 );
        super.doExplode();
    }

    public void drawHatch(Graphics2D g, Vec2d p, int phase)
    {
        g.fillRect(
            (int)(p.x-30),
            (int)(p.y-dimensions.y/2+5),
            (30-phase),
            (int)(dimensions.y-5)
        );
        g.fillRect(
            (int)(p.x+phase),
            (int)(p.y-dimensions.y/2+5),
            (30-phase),
            (int)(dimensions.y-5)
        );
    }

    public void draw(Graphics2D g)
    {
        Vec2d p = getPosition();
        int X = (int)(p.x - dimensions.x/2);
        int Y = (int)(p.y - dimensions.y/2);
        int W = (int)(dimensions.x);
        int H = (int)(dimensions.y);

        g.setColor(Color.lightGray);
        g.fillRect(X,     Y, W, 5);
        g.fillRect(X,     Y, 5, H);
        g.fillRect(X+W-5, Y, 5, H);

        /*
        g.setColor(Color.gray);
        g.fillRect(X, Y, W, H);
        */
        if(deploying > 0)
        {
            image2Center(g, iTanks[tankType]);
            drawHatch(g, p, deploying);
        }
        else if(loading > 0)
        {
            if(loading < 30) drawHatch(g, p, loading);
        }
        else
        {
            g.fillRect(
                (int)(p.x-30), Y+5,
                (60), H-5
            );
        }
        if(name != null)
        {
            g.setColor(Color.black);
            text2Center(g, name);
        }
        super.draw(g);
    }
}

// ---------------------------------------------------------------
// Bases w/ name and gray box
// ---------------------------------------------------------------
class EnemyBaseDefault extends EnemyBase
{
    Image image;

    EnemyBaseDefault( String name, double x, double y, double energy, int points )
    {
        super( name, x, y, energy, points );
    }

    public void draw(Graphics2D g)
    {
        if(image != null)
        {
            image2Center(g, image);
        }
        else
        {
            g.setColor(Color.lightGray);
            box2Center(g, dimensions.x, dimensions.y);
            if(name != null)
            {
                g.setColor(Color.black);
                text2Center(g, name);
            }
        }
        super.draw(g);
    }
}

// ---------------------------------------------------------------
// Shield
// ---------------------------------------------------------------
class EnemyBaseShield extends EnemyBaseDefault
{
    EnemyBaseShield( double x, double y )
    {
        super( null, x, y, 30, 200 );
        dimensions.set( 60, 10 );
    }
}

// ---------------------------------------------------------------
// Prison
// ---------------------------------------------------------------
class EnemyPrison extends EnemyBaseDefault
{
    EnemyPrison( double x, double y )
    {
        super( "Prison", x, y, 10, 200 );
        dimensions.set( 60, 30 );
    }
    public void doExplode()
    {
        new EnemyOperator( position.x-20, position.y - 5, -1 );
        new EnemyOperator( position.x+20, position.y - 5, +1 );
        new OwnSoldier   ( position.x-15, position.y + 5,  0 );
        new OwnSoldier   ( position.x   , position.y - 5,  0 );
        new OwnSoldier   ( position.x+15, position.y    ,  0 );
        super.doExplode();
    }
}

// ---------------------------------------------------------------
// Lab Tank
// ---------------------------------------------------------------
class EnemyLabTank extends EnemyBaseDefault
{
    int type;

    //------------------------------------------------------------
    // Pubbles in the tank
    //------------------------------------------------------------

    class Pubble
    {
        Vec2d position;
        double speed;
        int phase;
        static final int nPhase = 8;
        Particle parent;

        Pubble(Particle parent)
        {
            this.parent = parent;
            newPubble();
        }

        double rndS() { return 0.5 + Math.random()*2; }
        double rndX() { return Math.random()*20-10; }
        double rndY() { return -Math.random()*10; }
        int    rndP() { return (int)(Math.random()*nPhase); }

        void newPubble()
        {
            position = new Vec2d( rndX(), rndY() );
            phase = rndP();
            speed = rndS();
        }

        void update()
        {
            position.y += speed;
            phase = (phase + 1) % nPhase;
            if(position.y > 16) newPubble();
        }
        void draw(Graphics2D g)
        {
            Vec2d p = parent.getPosition();
            p.sub(position);
            g.setColor(Color.green);
            g.drawOval((int)(p.x + 2*Math.cos(2*Math.PI*phase/nPhase)), (int)p.y-1, 2, 2);
        }
    }

    Pubble pubbles[] = new Pubble[4];

    //------------------------------------------------------------
    // Lab tanks
    //------------------------------------------------------------

    EnemyLabTank( double x, double y, int type )
    {
        super( "Lab\nTank " + type, x, y, 10, 200 );
        image = iLabTanks[type];
        dimensions.set( 27, 35 );
        this.type = type;
        for(int i = 0; i < pubbles.length; i++) pubbles[i] = new Pubble(this);
    }

    public void update()
    {
        for(int i = 0; i < pubbles.length; i++) pubbles[i].update();
        super.update();
    }

    public void draw(Graphics2D g)
    {
        image2Center(g, iLabTanks[5]);
        for(int i = 0; i < pubbles.length; i++) pubbles[i].draw(g);
        super.draw(g);
        image2Center(g, iLabTanks[4]);
    }

    public void doExplode()
    {
        switch(type)
        {
            default:
            case 0: break;
            case 1: new OwnSoldier   (position.x, position.y, 0); break;
            case 2: new MonsterHead  (position.x, position.y, 0); break;
            case 3: new EnemyCreature(position.x, position.y, 0); break;
        }
        super.doExplode();
    }
}

// ---------------------------------------------------------------
// Lab
// ---------------------------------------------------------------
class EnemyLab extends EnemyBaseDefault
{
    EnemyLab( double x, double y )
    {
        super( "Lab", x, y, 10, 200 );
        dimensions.set( 60, 40 );
    }
    public void doExplode()
    {
        new EnemyCreature ( position.x-10, position.y-5, -0.5 );
        new EnemyCreature ( position.x+10, position.y-5, +0.5 );

        new MonsterHead   ( position.x-10, position.y+5, -0.5 );
        new MonsterHead   ( position.x+10, position.y+5, +0.5 );

        new EnemyScientist( position.x-20, position.y + 5, -1 );
        new EnemyScientist( position.x   , position.y - 5,  0 );
        new EnemyScientist( position.x+20, position.y    , +1 );

        super.doExplode();
    }
}

// ---------------------------------------------------------------
// Barracks
// ---------------------------------------------------------------
class EnemyBarracks extends EnemyBaseDefault
{
    EnemyBarracks( double x, double y )
    {
        super( "Barracks", x, y, 10, 200 );
        dimensions.set( 80, 40 );
    }
    public void doExplode()
    {
        new EnemyOperator( position.x-20, position.y + 5, -1 );
        new EnemyOperator( position.x   , position.y - 5,  0 );
        new EnemyOperator( position.x+20, position.y    , +1 );

        new EnemyTrooper ( position.x-15, position.y + 10 );
        new EnemyTrooper ( position.x   , position.y +  5 );
        new EnemyTrooper ( position.x+15, position.y + 15 );

        super.doExplode();
    }

    double probability = 0.015;
    int    loading = 0;

    public void update()
    {
        super.update();
        if(loading > 0)
        {
            loading--;
        }
        else if(Math.random() < probability)
        {
            int count = (int)(1 + 3*Math.random());
            for(int i = 0; i < count; i++)
            {
                new EnemyTrooper(
                    position.x + (Math.random()-0.5)*60,
                    position.y + 5 + (Math.random()-0.5)*5
                );
            }
            loading = 30;
        }
    }
}

// ---------------------------------------------------------------
// Gun towers
// ---------------------------------------------------------------
class EnemyGunTower extends EnemyBaseDefault
{
    EnemyGunTower( double x, double y, int type )
    {
        super( "GT" + type, x, y, 10, 200 );
        dimensions.set( 40, 40 );

        switch(type)
        {
            default:
            case 0:
            {
                Gun guns[] = {
                    new Gun(-10, 10, 0, 1, "default"),
                    new Gun(+10, 10, 0, 1, "default")
                };
                weapon = new Weapon(this, guns, 0.05, 3, 1);
                break;
            }
            case 1:
            {
                Gun guns[] = {
                    new Gun(-10, 10, 0, 1, "minigun"),
                    new Gun(+10, 10, 0, 1, "minigun"),
                };
                weapon = new Weapon(this, guns, 0.05, 3, 1);
                break;
            }
            case 2:
            {
                Gun guns[] = {
                    new Gun(-10, 10, 0, 1, "rocket"),
                    new Gun(+10, 10, 0, 1, "rocket"),
                };
                weapon = new Weapon(this, guns, 0.05, 3, 1);
                break;
            }
        }
        weapon.isFollowing = true;
    }

    public void doExplode()
    {
        new EnemyOperator( position.x-10, position.y + 5, -1 );
        new EnemyOperator( position.x+10, position.y    , +1 );

        super.doExplode();
    }
}

// ---------------------------------------------------------------
// Fuel supplies - just sitting ducks
// ---------------------------------------------------------------

class EnemyFuelTank extends EnemyBaseDefault
{
    EnemyFuelTank(double x, double y)
    {
        super("Fuel", x, y, 15, 100);
        dimensions.set(40, 40);
    }
}