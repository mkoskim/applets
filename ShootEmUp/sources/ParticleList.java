import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;

public class ParticleList
{
    //---------------------------------------------------------------
    // Static part
    //---------------------------------------------------------------
    static Vector<ParticleList> pLists = new Vector<ParticleList>();

    static void updateAll()
    {
        for(int i = 0; i < pLists.size(); i++)
        {
            ParticleList l = (ParticleList)pLists.get(i);
            l.update();
        }
    }

    static void cleanAll()
    {
        for(int i = 0; i < pLists.size(); i++)
        {
            ParticleList l = (ParticleList)pLists.get(i);
            l.clean();
        }
    }

    static void clearAll()
    {
        for(int i = 0; i < pLists.size(); i++)
        {
            ParticleList l = (ParticleList)pLists.get(i);
            l.clear();
        }
    }

    ParticleList() { pLists.add(this); }

    //---------------------------------------------------------------
    // List instances
    //---------------------------------------------------------------
    public Vector<Particle> list = new Vector<Particle>();

    public void clear() { list.clear(); }
    public void add( Particle p ) { list.add(p); }

    public void update()
    {
        for(int i = 0; i < list.size();i++)
        {
            Particle p = (Particle)list.get(i);
            p.update();
        }
    }

    public void clean()
    {
        for(int i = 0; i < list.size();)
        {
            Particle p = (Particle)list.get(i);
            if(p.outOfRange())       { p.doDispose(); list.remove(i); }
            else if(p.isOverAged())  { p.doTooOld();  list.remove(i); }
            else if(p.isDestroyed()) { p.doExplode(); list.remove(i); }
            else i++;
        }
    }

    static Comparator<Particle> cmpr = new Comparator<Particle>()
    {
        public int compare( Particle a, Particle b )
        {
            return a.level - b.level;
        }
    };

    public void draw( Graphics2D g )
    {
        Collections.sort(list, cmpr);
        for(int i = 0; i < list.size(); i++)
        {
            Particle p = (Particle)list.get(i);
            p.draw(g);
        }
    }

    public void doHitBy( Particle what )
    {
        if(what.energy <= 0) return ;
        for(int i = 0; i < list.size();i++)
        {
            Particle p = (Particle)list.get(i);
            if(p.energy > 0 && p.isHitBy(what))
            {
                p.doHitBy(what);
                if(what.energy <= 0) return;
            }
        }
    }

    public void doCollisions( ParticleList what )
    {
        for(int i = 0; i < what.list.size();i++)
        {
            Particle p = (Particle)what.list.get(i);
            doHitBy(p);
        }
    }

    public void doCollisions( Particle what )
    {
        doHitBy(what);
    }

    public boolean isEmpty() { return list.size() == 0; }
}
