//-------------------------------------------------------------------
// Ship
//-------------------------------------------------------------------

import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;

class Ship extends Particle
{
    //---------------------------------------------------------------
    // Static part
    //---------------------------------------------------------------
    static Image iShip;

    static void initImages(GameEngine engine)
    {
        Image[] temp;

        //-----------------------------------------------------------
        // Cut shot images
        //-----------------------------------------------------------
        temp = Helper.cutImage
        (
            engine.getJARImage( "images/ship.png" ),
            40, 40,         // Size of the subimages
            1,              // Padding at the images
            4, 1,          // Columns x rows
            Color.black     // Transparent color
        );
        iShip = temp[0];
    }

    //---------------------------------------------------------------
    // Ship's weapon rack
    //---------------------------------------------------------------

    class Rack
    {
        // Weapon slots
        class Slot
        {
            Vec2d position;
            Vec2d offset;
            Weapon weapon = null;

            Slot(Vec2d position, Vec2d offset)
            {
                this.position = position;
                this.offset   = offset;
            }
            void mount( String type )
            {
                weapon = Weapon.create(position, offset, type);
            }
            void unmount() { weapon = null; }
            void fire()             { if(weapon != null) weapon.fire(); }
            void update()           { if(weapon != null) weapon.update(); }
            void reload()           { if(weapon != null) weapon.reload(); }
            void draw(Graphics2D g) { if(weapon != null) weapon.draw(g); }
            void mark(Graphics2D g)
            {
                g.setColor(Color.yellow);
                g.drawRect(
                    (int)(position.x + offset.x) - 2,
                    (int)(position.y + offset.y) - 2,
                    5,5
                );
            }
            void doExplode()        { if(weapon != null) weapon.doExplode(); }
            void doHitsTo(ParticleList p)
            {
                if(weapon == null) return ;
                p.doHitBy(weapon);
                if(weapon.energy <= 0) { weapon.doExplode(); weapon = null; }
            }
        }

        static final int slotLeftUp    =  0;
        static final int slotLeft      =  1;
        static final int slotLeftDown  =  2;
        static final int slotRightUp   =  3;
        static final int slotRight     =  4;
        static final int slotRightDown =  5;
        static final int slotHead      =  6;
        static final int slotHeadLeft  =  7;
        static final int slotHeadRight =  8;
        static final int slotRear      =  9;
        static final int slotRearLeft  = 10;
        static final int slotRearRight = 11;

        Slot slots[];
        Rack(Particle parent)
        {
            // Create slots
            slots = new Slot[12];

            slots[slotHeadLeft]  = new Slot(parent.position, new Vec2d( -8, -22));
            slots[slotHead]      = new Slot(parent.position, new Vec2d(  0, -36));
            slots[slotHeadRight] = new Slot(parent.position, new Vec2d( +8, -22));

            slots[slotLeft]      = new Slot(parent.position, new Vec2d(-30,  0));
            slots[slotLeftUp]    = new Slot(parent.position, new Vec2d(-30, -8));
            slots[slotLeftDown]  = new Slot(parent.position, new Vec2d(-30, +8));

            slots[slotRight]     = new Slot(parent.position, new Vec2d(+30,  0));
            slots[slotRightUp]   = new Slot(parent.position, new Vec2d(+30, -8));
            slots[slotRightDown] = new Slot(parent.position, new Vec2d(+30, +8));

            slots[slotRearLeft]  = new Slot(parent.position, new Vec2d(-16, +12));
            slots[slotRear]      = new Slot(parent.position, new Vec2d(  0, +22));
            slots[slotRearRight] = new Slot(parent.position, new Vec2d(+16, +12));
        }
        void mount( int slot, String type ) { slots[slot].mount(type); }
        void unmount( int slot ) { slots[slot].unmount(); }
        void fire()             { for(int i = 0; i < slots.length; i++) slots[i].fire(); }
        void update()           { for(int i = 0; i < slots.length; i++) slots[i].update(); }
        void reload()           { for(int i = 0; i < slots.length; i++) slots[i].reload(); }
        void draw(Graphics2D g) { for(int i = 0; i < slots.length; i++) slots[i].draw(g); }
        void mark(Graphics2D g) { for(int i = 0; i < slots.length; i++) slots[i].mark(g); }
        void doExplode()        { for(int i = 0; i < slots.length; i++) slots[i].doExplode(); }
        void doHitsTo(ParticleList p)
        {
            for(int i = 0; i < slots.length; i++) slots[i].doHitsTo(p);
        }
    }

    Rack racks[]   = null;
    Rack rack      = null;

    //---------------------------------------------------------------
    // Initializations
    //---------------------------------------------------------------

    public Ship()
    {
        super( 20 );
        recovering = 0.05;
        offset = thrust;

        racks = new Rack[2];
        racks[0] = new Rack(this);
        racks[1] = new Rack(this);
        init();
    }

    public void init()
    {
        energy   = maxEnergy;
        position.set(Setup.WIDTH/2, Setup.HEIGHT+30);
        dimensions.set(30, 40);
        renewRacks();
    }

    public void renewRacks()
    {
        /* Set up weapons */
        /* Default set */
        racks[0].mount(Rack.slotHeadLeft,  "rocketLauncher");
        racks[0].mount(Rack.slotHeadRight, "rocketLauncher");
        racks[0].mount(Rack.slotRearLeft,  "miniGun");
        racks[0].mount(Rack.slotRearRight, "miniGun");
        racks[0].reload();

        /* Side set */
        /*
        racks[1].mount(Rack.slotLeftUp,   "leftGun");
        racks[1].mount(Rack.slotLeftDown, "leftGun");
        racks[1].mount(Rack.slotRightUp,   "rightGun");
        racks[1].mount(Rack.slotRightDown, "rightGun");
        */
        racks[1].mount(Rack.slotLeft,     "vshield");
        racks[1].mount(Rack.slotRight,    "vshield");
        racks[1].mount(Rack.slotHead,     "hshield");
        racks[1].mount(Rack.slotRear,     "hshield");
        racks[1].reload();

        rack = racks[0];
    }

    public void useRack(int number) { rack = racks[number]; }

    public void draw(Graphics2D g)
    {
        image2Center( g, iShip );
        //rack.draw(g);
        super.draw(g);
        //rack.mark(g);
    }

    //---------------------------------------------------------------
    // Moving & calculating force effects
    //---------------------------------------------------------------
    Vec2d  thrust = new Vec2d();

    public void update()
    {
        super.update();

        // Load weapons (different weapons have different loading
        // times).
        rack.update();

        // Moving
        //position.add(speed);

        // Default force (ship drops)
        Vec2d force = new Vec2d(0, 2);
        force.add(thrust);

        int WIDTH  = GameEngine.WIDTH;
        int HEIGHT = GameEngine.HEIGHT;

        // Bounding from boundaries
        if(position.x < 40)             force.x += (int)(40 - position.x)/4;
        else if(position.x > WIDTH-40)  force.x += (int)((WIDTH-40) - position.x)/4;
        if(position.y < 2*HEIGHT/3)     force.y += (int)((2*HEIGHT/3)-position.y)/4;
        else if(position.y > HEIGHT-50) force.y += (int)((HEIGHT-50) - position.y)/4;

        // Friction force
        if(speed.x < 0) force.x += (speed.x < -3) ? +3 : +1;
        if(speed.x > 0) force.x += (speed.x > +3) ? -3 : -1;
        if(speed.y < 0) force.y += (speed.y < -3) ? +3 : +1;
        if(speed.y > 0) force.y += (speed.y > +3) ? -3 : -1;

        // Change the speed according to forces
        speed.add(force);
    }

    public void doExplode()
    {
        new Explosion(this);
        new Explosion(this, Explosion.typeLargeHalo);
        Explosion.doBits(this);
        Explosion.doExtraBits(this);
        rack.doExplode();
    }

    void doCollisions()
    {
        rack.doHitsTo(Enemy.list);
        rack.doHitsTo(Shot.hisOffenceList);
        rack.doHitsTo(Shot.hisDefenceList);
        Enemy.list.doCollisions( this );
        Shot.hisOffenceList.doCollisions( this );
        Shot.hisDefenceList.doCollisions( this );
    }
}
