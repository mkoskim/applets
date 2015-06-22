//--------------------------------------------------------------------
// Shots
//--------------------------------------------------------------------

import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.util.*;

class Shot extends Particle
{
    //----------------------------------------------------------------
    // Images
    //----------------------------------------------------------------

    static class DirImage
    {
        static final int nDirs = 13;
        Image up[];
        Image dn[];

        Image rotate(Image source, double angle)
        {
            int W = source.getWidth(null), H = source.getHeight(null);

            BufferedImage im = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D)im.getGraphics();
            g.drawImage(source, 0, 0, null);
            AffineTransform tx = new AffineTransform();
            tx.rotate(Math.toRadians(angle), W/2 + 1, H/2 + 1);
            AffineTransformOp op = new AffineTransformOp(
                tx,
                AffineTransformOp.TYPE_NEAREST_NEIGHBOR
                //AffineTransformOp.TYPE_BILINEAR
                //AffineTransformOp.TYPE_BICUBIC
            );
            //AffineTransformOp.TYPE_BILINEAR);
            return Helper.makeColorTransparent(op.filter(im, null), Color.black);
        }

        DirImage(Image source)
        {
            up = new Image[nDirs];
            dn = new Image[nDirs];

            for(int i = 0; i < nDirs; i++)
            {
                up[i] = rotate(source, 180.0*i/(nDirs-1) - 90);
                dn[i] = rotate(source, 270 - 180.0*i/(nDirs-1));
            }
        }

        void draw(Graphics2D g, Particle parent)
        {
            Vec2d d = new Vec2d(parent.speed);
            d.normalize();
            double angle = 1.0 - Math.acos(d.x)/Math.PI;
            int index = (int)(nDirs*angle*0.99);
            parent.image2Center( g, (d.y < 0) ? up[index] : dn[index] );
        }
    }

    public static DirImage iShots[];

    static void initImages(GameEngine engine)
    {
        Image[] temp;

        //-----------------------------------------------------------
        // Cut shot images
        //-----------------------------------------------------------
        temp = Helper.cutImage
        (
            engine.getJARImage( "images/shots.png" ),
            19, 19,         // Size of the subimages
            1,              // Padding at the images
            16, 2,          // Columns x rows
            null            // Transparent color
        );

        //-----------------------------------------------------------
        // Make rotatable the images
        //-----------------------------------------------------------
        iShots = new DirImage[6];

        for(int i = 0; i < iShots.length; i++)
        {
            iShots[i] = new DirImage( temp[i] );
        }
    }

    //----------------------------------------------------------------
    // Static part
    //----------------------------------------------------------------

    public static ParticleList myOffenceList  = new ParticleList();
    public static ParticleList myDefenceList  = new ParticleList();
    public static ParticleList hisOffenceList = new ParticleList();
    public static ParticleList hisDefenceList = new ParticleList();

    static void drawAllMine(Graphics2D g)
    {
        myOffenceList.draw(g);
        myDefenceList.draw(g);
    }
    static void drawAllHis(Graphics2D g)
    {
        hisOffenceList.draw(g);
        hisDefenceList.draw(g);
    }

    static void updateAll()
    {
        myOffenceList.update();
        myDefenceList.update();
        hisOffenceList.update();
        hisDefenceList.update();
    }

    static void clearAll()
    {
        myOffenceList.clear();
        myDefenceList.clear();
        hisOffenceList.clear();
        hisDefenceList.clear();
    }

    static void doInterCollisions()
    {
        myOffenceList.doCollisions(hisDefenceList);
        myDefenceList.doCollisions(hisOffenceList);
        myDefenceList.doCollisions(hisDefenceList);
    }

    //----------------------------------------------------------------
    // Shot configuration from array of strings
    //----------------------------------------------------------------
    static class Configuration
    {
        String   name;
        boolean  defensive;
        DirImage image;
        boolean  explosive;
        double   energy;
        double   acceleration;
        int      age;
        double   initSpeed;
        double   maxSpeed;
    }

    static Map<String,Configuration> configurations = new HashMap<String,Configuration>();

    static void configure( String[] lines )
    {
        for(int i = 0; i < lines.length; i++)
        {
            String[] fields = lines[i].split("\\s+");
            //System.out.println(lines[i]);
            Configuration config = new Configuration();

            config.name          = fields[0];
            config.defensive     = fields[1].equals("defence");
            config.image         = iShots[ Integer.parseInt(fields[2]) ];
            config.explosive     = fields[3].equals("true");
            config.energy        = Double.parseDouble(fields[4]);
            config.acceleration  = Double.parseDouble(fields[5]);
            config.age           = Integer.parseInt(fields[6]);
            config.initSpeed     = Double.parseDouble(fields[7]);
            config.maxSpeed      = Double.parseDouble(fields[8]);

            configurations.put( config.name, config );
        }
    }

    static Configuration getConfig(String name)
    {
        return configurations.get(name);
    }

    //----------------------------------------------------------------
    // Setting up the shot from configurations
    //----------------------------------------------------------------
    static void createOwn(Vec2d position, Configuration config, Vec2d direction)
    {
        ParticleList list = (config.defensive) ? myDefenceList : myOffenceList;
        new Shot(position, config, direction, list);
    }

    static void createHis(Vec2d position, Configuration config, Vec2d direction)
    {
        ParticleList list = (config.defensive) ? hisDefenceList : hisOffenceList;
        new Shot(position, config, direction, list);
    }

    //----------------------------------------------------------------
    // Shot instances
    //----------------------------------------------------------------
    DirImage image;
    double   maxSpeed;
    Vec2d    acceleration;

    public Shot( Vec2d position, Configuration config, Vec2d direction, ParticleList list)
    {
        super(config.energy);
        this.position.set( position );

        this.image     = config.image;
        this.explosive = config.explosive;
        //this.energy    = this.maxEnergy = config.energy;
        this.age       = config.age;
        this.maxSpeed  = config.maxSpeed;

        acceleration   = new Vec2d(direction);
        acceleration.normalize(config.acceleration);

        speed.set        (direction);
        speed.normalize  (config.initSpeed);

        dimensions.set(6, 6);
        list.add(this);
    }

    /* Launched weapons loose their speed while moving.
     * When they stop, they're exploded.
     */
    public void update()
    {
        speed.add( acceleration );
        double spd = speed.length();
        if(spd > maxSpeed) speed.normalize(maxSpeed);
        if(spd < 1) age = 0;
        super.update();
    }

    public void draw(Graphics2D g)
    {
        image.draw(g, this);
        super.draw(g);
    }

    public void doTooOld()
    {
        new Explosion(this, Explosion.typePuff);
    }

    public void doExplode()
    {
        new Explosion(this);
        new Explosion(this, Explosion.typeSmallHalo);
    }
}
