//---------------------------------------------------------------
// Explosions in the space
//---------------------------------------------------------------

import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;

class Decoration extends Particle
{
    static ParticleList list = new ParticleList();

    static Color textColors[];
    static Image iExplosions[][];

    static public final int typeExplosion = 0;
    static public final int typePuff = 1;
    static public final int typeLargeHalo = 2;
    static public final int typeSmallHalo = 3;
    static public final int typeTinyHalo  = 4;
    static public final int typeBit = 5;

    static void initImages(GameEngine engine)
    {
        textColors = new Color[5];
        Helper.createTransparentColorArray(textColors, Color.white);

        iExplosions   = new Image[6][];

        /* Regular explosion */
        iExplosions[0] = Helper.cutImage
        (
            engine.getJARImage( "images/explosions.png" ),
            40, 40,         // Size of the subimages
            1,              // Padding at the images
            8, 1,           // Columns x rows
            Color.black     // Transparent color
        );

        /* Puff */
        iExplosions[1]    = new Image[4];
        iExplosions[1][0] = Helper.createHalo(6,  1.5, Color.white);
        iExplosions[1][1] = Helper.createHalo(10, 1.0, Color.white);
        iExplosions[1][2] = Helper.createHalo(12, 0.8, Color.white);
        iExplosions[1][3] = Helper.createHalo(13, 0.4, Color.white);

        /* Explosion halos */
        iExplosions[2] = Helper.createHaloArray(8, 60, new Color(0x00FFAA00));
        iExplosions[3] = Helper.createHaloArray(4, 25, new Color(0x00FFAA00));
        iExplosions[4] = Helper.createHaloArray(4, 10, new Color(0x00FFFFFF));
        iExplosions[5] = Helper.createHaloArray(16, 5, new Color(0x00FFAA44));
    }

    public boolean isDestroyed() { return false; }
}

class GotPoints extends Decoration
{
    int points;

    GotPoints(EnemyObject p)
    {
        super();
        this.position.set(p.position);
        //this.speed.set(0, -2);
        this.points = p.points;
        this.age    = 15;
        this.level  = 2;
        GameEngine.score += points;
        list.add(this);
    }

    public void draw(Graphics2D g)
    {
        g.setColor(textColors[(age < textColors.length) ? age : (textColors.length-1)]);
        //g.setColor(Color.white);
        text2Center(g, Integer.toString(points));
    }
}

public class Explosion extends Decoration
{
    //-----------------------------------------------------------
    // Static part
    //-----------------------------------------------------------
    static void doBits(Particle p)
    {
        new Explosion(p, Explosion.typeBit, 45      , -20);
        new Explosion(p, Explosion.typeBit, 45      , +20);
        new Explosion(p, Explosion.typeBit, 45 + 90, -20);
        new Explosion(p, Explosion.typeBit, 45 + 90, +20);
    }

    static void doExtraBits(Particle p)
    {
        new Explosion(p, Explosion.typeBit,  0, +20);
        new Explosion(p, Explosion.typeBit,  0, -20);
        new Explosion(p, Explosion.typeBit, 90, +20);
        new Explosion(p, Explosion.typeBit, 90, -20);
    }

    //-----------------------------------------------------------
    // Instances
    //-----------------------------------------------------------

    int type;
    Image[] images;

    public Explosion( Vec2d p, int type, int drift )
    {
        super();
        position.set( p );
        if(type == typeExplosion) level = 1;
        this.type = type;
        images = iExplosions[type];
        age = images.length - drift;
        list.add(this);
    }

    public Explosion( Particle p, int type)
    {
        this(p.getPosition(), type, 0);
    }

    public Explosion( Particle p, int type, int drift)
    {
        this(p.getPosition(), type, drift);
    }

    public Explosion( Particle p ) { this(p, typeExplosion); }

    double degCos( double angle )  { return Math.cos(Math.toRadians(angle)); }
    double degSin( double angle )  { return Math.sin(Math.toRadians(angle)); }

    public Explosion( Particle p, int type, double angle, double length )
    {
        this(p, type);
        speed.set(degCos(angle), degSin(angle));
        speed.mult(length);
    }

    public void update()
    {
        super.update();
        if(type == typeBit)
        {
            double l = speed.length();
            if(l > 1) speed.mult((l - 1) / l);
        }
    }

    public void draw(Graphics2D g)
    {
        super.draw(g);
        int index = images.length-age-1;
        if(index > 0 && index < images.length) image2Center(g, images[index]);
    }
}
