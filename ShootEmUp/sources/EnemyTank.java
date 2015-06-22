// ---------------------------------------------------------------
// Tanks try to reach the bottom of the screen
// ---------------------------------------------------------------

import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;

class EnemyTank extends Enemy
{
    Image image;

    EnemyTank(double x, double y, int type)
    {
        super( x, y, 10, 100 );
        dimensions.set(37, 21);

        switch(type)
        {
            /* Default tank */
            case 0: case 1: default:
            {
                switch(type)
                {
                    case 0: default: speed.set(0, 1); break;
                    case 1: speed.set(0, 2); break;
                }
                image = iTanks[0];
                Gun guns[] = { new Gun(0, 10, 0, 1, "default") };
                weapon = new Weapon(this, guns);
                break;
            }
            /* Assault tank */
            case 2:
            {
                speed.set(0, 2);
                image = iTanks[1];
                Gun guns[] =
                {
                    new Gun(-5, 10, 0, 1, "minigun"),
                    new Gun(+5, 10, 0, 1, "minigun")
                };
                weapon = new Weapon(this, guns, 0.01, 2, 1);
                break;
            }
            /* Rocket tank */
            case 3:
            {
                speed.set(0, 1);
                image = iTanks[2];
                Gun guns[] = { new Gun(0, 10, 0, 1, "rocket") };
                weapon = new Weapon(this, guns, 0.01, 2, 4);
                break;
            }
        }
    }

    public void update()
    {
        super.update();
    }

    public void doDispose()
    {
        gotThrough = true;
    }

    public void doExplode()
    {
        super.doExplode();
        Explosion.doBits(this);
        new EnemyOperator(position.x, position.y, 0);
    }

    public void draw(Graphics2D g)
    {
        if(position.y + dimensions.y/2 > Setup.HEIGHT-30)
        {
            g.setColor(Color.red);
            rect2Center(g, dimensions.x + 5, dimensions.y + 5);
            rect2Center(g, dimensions.x + 7, dimensions.y + 7);
        }
        //g.setColor(Color.red);
        //box2Center(g, dimensions.x, dimensions.y );
        image2Center(g, image);
        //drawDimensions(g);
        super.draw(g);
    }
}
