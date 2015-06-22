//--------------------------------------------------------------------
// My weapons: Enemies have fixed weapon set - player's ship
// can aquire more powerful weapons.
//--------------------------------------------------------------------

import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.util.*;

class Weapon extends Particle
{
    //---------------------------------------------------------------
    // Static part
    //---------------------------------------------------------------
    static Image iGuns   [][];
    static Image iShields[][];

    // Guns:
    // 0 = default gun image
    // 1 = minigun images
    // 2 = launcher images
    // 3 = right gun
    // 4 = left gun
    //
    // Shields:
    // 0 = horizontal shield
    // 1 = vertical shield

    static void initImages(GameEngine engine)
    {
        /* Get the image and cut it to pieces */
        Image[] cutted = Helper.cutImage
        (
            engine.getJARImage( "images/weapons.png" ),
            19, 20,         // Size of the subimages
            1,              // Padding at the images
            8, 6,           // Columns x rows
            Color.black     // Transparent color
        );

        iGuns    = new Image[5][];
        iGuns[0] = Helper.sliceImageArray(cutted,  0, 4);
        iGuns[1] = Helper.sliceImageArray(cutted,  4, 2);
        iGuns[2] = Helper.sliceImageArray(cutted,  8, 8);
        iGuns[3] = Helper.sliceImageArray(cutted, 24, 4);
        iGuns[4] = Helper.sliceImageArray(cutted, 32, 4);

        iShields    = new Image[2][];
        iShields[0] = Helper.sliceImageArray(cutted, 16, 4);
        iShields[1] = Helper.sliceImageArray(cutted, 20, 4);
    }

    //---------------------------------------------------------------
    // Configurations
    //---------------------------------------------------------------
    static class Configuration
    {
        static final int typeGun    = 0;
        static final int typeShield = 1;
        int type;

        String name;
        double energy;
        double recovering;
        Image  images[];
    }
    static class GunConfig extends Configuration
    {
        Vec2d   direction = new Vec2d();
        Shot.Configuration shotConfig;
        int     firerate;
        int     shotsPerFire;
        double  distortion;
        boolean backPuff;
    }
    static class ShieldConfig extends Configuration
    {
    }

    static Map<String,Configuration> configurations = new HashMap<String,Configuration>();

    static void configure(String[] lines)
    {
        for(int i = 0; i < lines.length; i++)
        {
            String[] fields = lines[i].split("\\s+");
            Configuration c;

            if(fields[1].equals("shield"))
            {
                ShieldConfig config = new ShieldConfig();
                config.name         = fields[0];
                config.type         = Configuration.typeShield;
                config.energy       = Double.parseDouble(fields[2]);
                config.recovering   = Double.parseDouble(fields[3]);
                config.images       = iShields[ Integer.parseInt(fields[4]) ];
                c = config;
            }
            else
            {
                GunConfig config    = new GunConfig();
                config.name         = fields[0];
                config.type         = Configuration.typeGun;
                config.energy       = Double.parseDouble(fields[2]);
                config.recovering   = Double.parseDouble(fields[3]);
                config.images       = iGuns[ Integer.parseInt(fields[4]) ];
                config.direction.set(
                    Double.parseDouble(fields[5]),
                    Double.parseDouble(fields[6])
                );
                config.direction.normalize();
                config.shotConfig   = Shot.getConfig    ( fields[7] );
                config.firerate     = Integer.parseInt  ( fields[8] );
                config.shotsPerFire = Integer.parseInt  ( fields[9] );
                config.distortion   = Double.parseDouble( fields[10] );
                config.backPuff     = fields[11].equals("true");
                c = config;
            }
            configurations.put( c.name, c );
        }
    }

    static Configuration getConfig( String name )
    {
        return configurations.get(name);
    }

    static Weapon create( Vec2d p, Vec2d o, String name)
    {
        Configuration config = getConfig(name);
        if(config.type == Configuration.typeGun)
        {
            return new Gun(p, o, (GunConfig)config);
        }
        else
        {
            return new Shield(p, o, (ShieldConfig)config );
        }
    }

    //---------------------------------------------------------------
    // Instances
    //---------------------------------------------------------------

    Image images[];

    Weapon(Vec2d position, Vec2d offset, Configuration config)
    {
        super(config.energy);
        this.recovering = config.recovering;
        this.position   = position;
        this.offset     = offset;
        this.images     = config.images;
        dimensions.set(10,15);
    }

    void reload() { energy = maxEnergy; }
    void fire()   { }
    public void update() { super.update(); }

    //---------------------------------------------------------------
    // makePuff() creates a row of puffs, when the ammos are out.
    //---------------------------------------------------------------
    void makePuff()
    {
        Vec2d p = getPosition();
        for(int i = 0; i < 4; i++)
        {
            p.add( 0, -16);
            Explosion e = new Explosion( p, Explosion.typeTinyHalo, -i );
        }
    }

    void makeBackPuff()
    {
        Vec2d p = getPosition();
        for(int i = 0; i < 4; i++)
        {
            p.add( 0, 16);
            Explosion e = new Explosion( p, Explosion.typeTinyHalo, -i );
        }
    }
    public void doExplode()
    {
        new Explosion(this);
        new Explosion(this, Explosion.typeSmallHalo);
        energy = 0;
    }
}

//--------------------------------------------------------------------
// Guns: Guns are active weapons, which fire ammunitions.
//--------------------------------------------------------------------

class Gun extends Weapon
{
    Weapon.GunConfig config;
    int     loading = 0;

    Gun(Vec2d position, Vec2d offset, Weapon.GunConfig config)
    {
        super(position, offset, (Weapon.Configuration)config);
        this.config = config;
    }

    void reload()
    {
        super.reload();
        loading = 0;
    }
    void fire()
    {
        if(energy <= 0) return ;
        if(loading != 0) return ;
        loading = config.firerate + 1;

        Vec2d p = getPosition();
        for(int i = 0; i < config.shotsPerFire; i++)
        {
            p.add(
                (Math.random()-0.5)*config.distortion,
                (Math.random()-0.5)*config.distortion
            );
            Shot.createOwn(p, config.shotConfig, config.direction);
            p.addMult(config.direction, 10);
            //if(backPuff) makeBackPuff();
        }
    }
    public void update()
    {
        super.update();
        if(loading != 0) loading--;
    }
    public void draw(Graphics2D g)
    {
        if(energy > 0)
        {
            int index = (loading >= images.length) ? images.length-1 : loading;
            image2Center( g, images[index] );
        }
        //drawEnergyBar(g);
        super.draw(g);
    }
}

//--------------------------------------------------------------------
// Shields: Shields are passive weapons, which protect the rest
// of the ship. They have energy level and the recovery rate.
//--------------------------------------------------------------------

class Shield extends Weapon
{
    Weapon.ShieldConfig config;

    Shield(
        Vec2d position,
        Vec2d offset,
        Weapon.ShieldConfig config
    )
    {
        super(position, offset, (Weapon.Configuration)config);
        this.config = config;
    }

    public void draw(Graphics2D g)
    {
        if(energy > 0)
        {
            image2Center
            (
                g,
                images[(int)((images.length-1)*energy/maxEnergy)]
            );
        }
        super.draw(g);
    }
}
