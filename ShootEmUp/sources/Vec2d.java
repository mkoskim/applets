//---------------------------------------------------------------
// Two-dim. vectors
//---------------------------------------------------------------
public class Vec2d
{
    public double x, y;

    public Vec2d(double x, double y) { set(x,y); }
    public Vec2d() { set(0,0); }
    public Vec2d(Vec2d v) { set(v.x, v.y); }

    public void add(double x, double y) { this.x += x; this.y += y; }
    public void sub(double x, double y) { this.x -= x; this.y -= y; }
    public void set(double x, double y) { this.x  = x; this.y  = y; }

    public void add(Vec2d a) { add(a.x, a.y); }
    public void sub(Vec2d a) { sub(a.x, a.y); }
    public void set(Vec2d a) { set(a.x, a.y); }

    public void directTo(Vec2d a)
    {
        set( y*a.x + x*a.y, y*a.y - x*a.x );
    }

    public void addMult(Vec2d a, double f) { add(f*a.x, f*a.y); }

    public double length()          { return Math.sqrt( x*x + y*y ); }
    public void   div ( double i )  { x /= i; y /= i; }
    public void   mult( double i )  { x *= i; y *= i; }

    public void normalize(double l) { mult(l/length()); }
    public void normalize()         { normalize(1.0); }
}
