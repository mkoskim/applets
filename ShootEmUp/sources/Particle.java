import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;

//---------------------------------------------------------------
// Particles in the space
//---------------------------------------------------------------

abstract public class Particle
{
    public boolean immortal  = false;   // If true, object is not destructible
    public boolean explosive = false;   // Explosives explode on collisions
    public double  energy = 0;          // Energy level
    public double  maxEnergy = 0;       // Maximum energy level
    public double  recovering = 0;      // Energy recovering amount

    public int     age    = -1;
    public int     level  = 0; // Level for drawing - the lowest are drawn first

    public Vec2d position   = new Vec2d();
    public Vec2d offset     = new Vec2d();
    public Vec2d dimensions = new Vec2d();
    public Vec2d speed      = new Vec2d();

    public int compare(Object a, Object b)
    {
        Particle A = (Particle)a;
        Particle B = (Particle)b;
        return A.level - B.level;
    }

    public Particle()
    {
    }

    public Particle(double maxEnergy)
    {
        this.energy = this.maxEnergy = maxEnergy;
    }

    public void update()
    {
        position.add( speed );
        updateBoundBox();

        if(age > 0) age--;

        energy += recovering;
        if(energy > maxEnergy) energy = maxEnergy;
    }

    // ----------------------------------------------------------
    // Getting positions
    // ----------------------------------------------------------
    Vec2d getPosition()
    {
        Vec2d p = new Vec2d(position);
        p.add(offset);
        return p;
    }

    // ----------------------------------------------------------
    // Helper drawing functions
    // ----------------------------------------------------------
    public void image2Center( Graphics2D g, Image i )
    {
        Vec2d p = getPosition();

        g.drawImage(
            i,
            (int)(p.x - i.getWidth(null)/2),
            (int)(p.y - i.getHeight(null)/2),
            null
        );
    }

    public void rect2Center( Graphics2D g, double width, double height )
    {
        Vec2d p = getPosition();
        g.drawRect(
            (int)(p.x - width/2),
            (int)(p.y - height/2),
            (int)width,
            (int)height
        );
    }

    public void box2Center( Graphics2D g, double width, double height )
    {
        Vec2d p = getPosition();
        g.fillRect(
            (int)(p.x - width/2),
            (int)(p.y - height/2),
            (int)width,
            (int)height
        );
    }

    public void sphere2Center( Graphics2D g, double diameter )
    {
        Vec2d p = getPosition();
        g.fillOval(
            (int)(p.x - diameter/2),
            (int)(p.y - diameter/2),
            (int)diameter, (int)diameter
        );
    }

    void text2Center( Graphics2D g, String text )
    {
        Vec2d p = getPosition();
        Helper.text2Center(g,
            (int)(p.x - dimensions.x/2),
            (int)(p.y - dimensions.y/2),
            (int)dimensions.x,
            (int)dimensions.y,
            text
        );
    }

    void drawDimensions(Graphics2D g)
    {
        Vec2d p = getPosition();
        g.setColor(Color.green);
        g.drawRect(
            (int)(p.x - dimensions.x/2),
            (int)(p.y - dimensions.y/2),
            (int)dimensions.x,
            (int)dimensions.y
        );
    }

    void drawEnergyBar(Graphics2D g)
    {
        if(maxEnergy <= 0) return ;

        Vec2d p = getPosition();
        p.sub(0, dimensions.y/2 + 5);

        int eLevel = (int)(18*(energy/maxEnergy));
        if(eLevel < 5)
        {
            g.setColor(Color.red);
        }
        else if(eLevel < 10)
        {
            g.setColor(Color.yellow);
        }
        else
        {
            g.setColor(Color.green);
        }
        g.fillRect(
            (int)(p.x - 9),
            (int)(p.y - 2),
            eLevel,
            4
        );
        g.setColor(Color.green);
        g.drawRect(
            (int)(p.x - 10),
            (int)(p.y - 3),
            19,
             5
        );
    }
    // ----------------------------------------------------------
    // Drawing
    // ----------------------------------------------------------
    public void draw(Graphics2D g)
    {
        //if(energy > 0) bb.draw(g);
        //drawDimensions(g);
    }

    // ----------------------------------------------------------
    // Collisions etc
    // ----------------------------------------------------------
    public boolean outOfRange()
    {
        Vec2d p = getPosition();

        return
            (p.x + dimensions.x/2) < 0 ||
            (p.x - dimensions.x/2) > Setup.WIDTH ||
            (p.y + dimensions.y/2) < 0 ||
            (p.y - dimensions.y/2) > Setup.HEIGHT;
    }

    public boolean isOverAged()  { return age == 0; }
    public boolean isDestroyed() { return energy <= 0; }

    public void doTooOld() { }
    public void doDispose() { }
    public void doExplode() { }

    public void doHitBy( Particle what )
    {
        if(what.energy > 0 && energy > 0)
        {
            double e1 = energy, e2 = what.energy;
            energy      = (explosive)      ? 0 : e1 - e2;
            what.energy = (what.explosive) ? 0 : e2 - e1;
        }
    }

    // ----------------------------------------------------------
    // Getting collision rectangle
    // ----------------------------------------------------------
    class BoundBox
    {
        double x1, y1;
        double x2, y2;
        void draw(Graphics2D g)
        {
            g.setColor(Color.green);
            g.drawRect((int)x1, (int)y1, (int)(x2-x1), (int)(y2-y1));
        }
    }

    BoundBox bb = new BoundBox();

    void updateBoundBox()
    {
        Vec2d p = getPosition();

        bb.x1 = p.x;
        bb.x2 = p.x - speed.x;
        if(bb.x1 > bb.x2) { double t = bb.x2; bb.x2 = bb.x1; bb.x1 = t; }
        bb.x1 -= dimensions.x/2;
        bb.x2 += dimensions.x/2;

        bb.y1 = p.y;
        bb.y2 = p.y - speed.y;
        if(bb.y1 > bb.y2) { double t = bb.y2; bb.y2 = bb.y1; bb.y1 = t; }
        bb.y1 -= dimensions.y/2;
        bb.y2 += dimensions.y/2;
    }

    public boolean isHitBy( Particle what )
    {
        if(energy <= 0 || what.energy <= 0) return false;
        if(immortal || what.immortal) return false;

        if(bb.x2 < what.bb.x1) return false;
        if(bb.x1 > what.bb.x2) return false;
        if(bb.y2 < what.bb.y1) return false;
        if(bb.y1 > what.bb.y2) return false;
        return true;
    }
}

