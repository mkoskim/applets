import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;

// ---------------------------------------------------------------
// Enemy infantry base class
// ---------------------------------------------------------------

class EnemyInfantry extends Enemy
{
    int phase = 0;
    int cImmortal = 0;
    Image[] iRegular;
    Image[] iImmortal;

    EnemyInfantry(
        double x, double y,
        double energy,
        int points,
        int cImmortal,
        Image[] regularImages,
        Image[] immortalImages
    )
    {
        super(x, y, energy, points);
        dimensions.set(9,12);
        this.cImmortal = cImmortal;
        immortal = cImmortal > 0;
        this.iRegular  = regularImages;
        this.iImmortal = immortalImages;
    }
    public void update()
    {
        super.update();
        if(immortal = (cImmortal > 0)) { cImmortal--; }
    }
    public void draw(Graphics2D g)
    {
        Image[] images = (immortal ? iImmortal : iRegular);
        phase = (phase+1) % images.length;
        image2Center(g, images[phase]);
        super.draw(g);
    }
    public void doExplode()
    {
        super.doExplode();
        //Wrecks.put(this, iEnemy[4][0]);
    }
}

// ---------------------------------------------------------------
// Small soldiers
// ---------------------------------------------------------------

class EnemyOperator extends EnemyInfantry
{
    EnemyOperator(double x, double y, double spdX)
    {
        super(x, y, 2, 50, 10, iSoldiers[1], iSoldiers[0]);
        speed.set(spdX, -2);
    }
}

class EnemyScientist extends EnemyInfantry
{
    EnemyScientist(double x, double y, double spdX)
    {
        super(x, y, 2, 50, 10, iSoldiers[6], iSoldiers[7]);
        speed.set(spdX, -2);
    }
}

class EnemyCreature extends EnemyInfantry
{
    EnemyCreature(double x, double y, double spdX)
    {
        super(x, y, 2, 50, 20, iCreatures[1], iCreatures[0]);
        speed.set(spdX, -2);
    }
}

class MonsterHead extends EnemyInfantry
{
    MonsterHead(double x, double y, double spdX)
    {
        super(x, y, 5, 0, 10, iSoldiers[3], iSoldiers[0]);
        dimensions.set(9, 15);
        speed.set(spdX, +3);
    }
    public void doDispose() { super.doDispose(); gotThrough = true; }
    public void draw(Graphics2D g)
    {
        if(position.y + dimensions.y/2 > Setup.HEIGHT-30)
        {
            g.setColor(Color.red);
            rect2Center(g, dimensions.x + 5, dimensions.y + 5);
            rect2Center(g, dimensions.x + 7, dimensions.y + 7);
        }
        super.draw(g);
    }
}

class OwnSoldier extends EnemyInfantry
{
    OwnSoldier(double x, double y, double spdX)
    {
        super(x, y, 5, 0, 10, iSoldiers[2], iSoldiers[0]);
        speed.set(spdX, +3);
    }

    public void doExplode()
    {
        super.doExplode();
        shotOwn = true;
    }
}

// ---------------------------------------------------------------
// Enemy troopers are like tanks (they try to break through).
// Their special ability is to get ground - then they are
// immortal, but they cannot move.
// ---------------------------------------------------------------

class EnemyTrooper extends EnemyInfantry
{
    int cRun;

    int runRandom() { return 10 + (int)(10*Math.random()); }
    int gndRandom() { return 25 + (int)(25*4*Math.random()); }

    EnemyTrooper(double x, double y)
    {
        super(x, y, 10, 100, 10, iSoldiers[4], iSoldiers[5]);
        Gun guns[] = { new Gun(0, 0, 0, 1, "minigun") };
        weapon = new Weapon(this, guns, 0.01, 2, 1);
        cRun = runRandom();
    }

    public void update()
    {
        if(immortal) speed.set(0,0); else speed.set(0,4);
        super.update();
        if(!immortal)
        {
            cRun--;
            if(cRun == 0)
            {
                immortal = true;
                cImmortal = gndRandom();
                cRun = runRandom();
            }
        }
    }

    public void doDispose() { super.doDispose(); gotThrough = true; }
}
