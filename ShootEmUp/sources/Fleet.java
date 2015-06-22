// *******************************************************************
// *******************************************************************
//
// Enemy fleets
//
// *******************************************************************
// *******************************************************************
import java.util.*;

//---------------------------------------------------------------
// Worlds
//---------------------------------------------------------------

class World
{
    static Vector<World> worlds = new Vector<World>();

    static World  get(int world)               { return (World)worlds.get(world-1); }
    static int    fleetCount(int world)        { return get(world).fleets.size(); }
    static int    count()                      { return worlds.size(); }
    static String getName(int world)           { return get(world).name; }
    static String getComment(int world)        { return get(world).comment; }
    static void   create(int world, int fleet) { get(world).create(fleet); }

    static void configure(GameEngine engine, String fileName)
    {
        Fleet.initMap();
        worlds.clear();
        String lines[] = engine.getJARFile(fileName);
        for(int i = 0; i < lines.length; i++)
        {
            new World(engine, lines[i]);
        }
    }

    String name;
    String comment;
    Vector<Fleet> fleets;

    World(GameEngine engine, String filename)
    {
        fleets = new Vector<Fleet>();
        String[] lines = engine.getJARFile(filename);
        name    = lines[0];
        comment = lines[1];

        Fleet fleet = null;
        for(int i = 2; i < lines.length; i++)
        {
            if(lines[i].equals("newfleet"))
            {
                if(fleet != null) fleets.addElement(fleet);
                fleet = new Fleet();
            }
            else
            {
                fleet.append(lines[i]);
            }
        }
        if(fleet != null) fleets.addElement(fleet);
        worlds.add(this);
    }

    void create(int fleet)
    {
        Fleet f = (Fleet)fleets.get(fleet - 1);
        f.create();
    }
}

//---------------------------------------------------------------
// Fleets in worlds
//---------------------------------------------------------------

class Fleet
{
    //----------------------------------------------------------------
    // Static part of fleet creating
    //----------------------------------------------------------------

    //----------------------------------------------------------------
    // Fleet instances
    //----------------------------------------------------------------
    Vector<String[]> bases = new Vector<String[]>();

    void append(String line)
    {
        bases.addElement(line.split("\\s+"));
    }

    void create()
    {
        for(int i = 0; i < bases.size(); i++)
        {
            String[] config = (String[])bases.get(i);
            Item item = (Item)items.get(config[0]);
            item.create( config );
        }
    }

    //---------------------------------------------------------------
    // Item classes. These are stored to a hashmap and they are
    // fetched by name. Then they are used to create enemy units
    // to the playground.
    //---------------------------------------------------------------

    // Items for creating enemy units from configurations.
    // Usage: see later.
    static abstract class Item
    {
        double asDouble(String s) { return Double.parseDouble(s); }
        int    asInt   (String s) { return Integer.parseInt  (s); }
        abstract void create(String[] fields);
    }

    // Map of configuration items
    static Map<String,Item> items = new HashMap<String,Item>();

    // Call this first to initialize the lookup table
    static void initMap()
    {
        items.clear();
        items.put("tankbase",   new ItemTankBase());
        items.put("shield",     new ItemShield());
        items.put("prison",     new ItemPrison());
        items.put("forcefield", new ItemForceField());
        items.put("attractor",  new ItemAttractor());
        items.put("fueltank",   new ItemFuelTank());
        items.put("textbox",    new ItemTextBox());
        items.put("barracks",   new ItemBarracks());
        items.put("lab",        new ItemLab());
        items.put("labtank",    new ItemLabTank());
        items.put("guntower",   new ItemGunTower());
    }

    static class ItemTankBase extends Item
    {
        void create(String[] f)
        {
            int    type = (f.length > 3) ? asInt(f[3]) : 0;
            double prob = (f.length > 4) ? asDouble(f[4]) : 0.01;
            new EnemyTankBase(asDouble(f[1]), asDouble(f[2]), type, prob);
        }
    }

    static class ItemShield extends Item
    {
        void create(String[] f) { new EnemyBaseShield(asDouble(f[1]), asDouble(f[2])); }
    }

    static class ItemPrison extends Item
    {
        void create(String[] f) { new EnemyPrison(asDouble(f[1]), asDouble(f[2])); }
    }

    static class ItemBarracks extends Item
    {
        void create(String[] f) { new EnemyBarracks(asDouble(f[1]), asDouble(f[2])); }
    }

    static class ItemForceField extends Item
    {
        void create(String[] f) { new EnemyForceField(asDouble(f[1]), asDouble(f[2])); }
    }

    static class ItemAttractor extends Item
    {
        void create(String[] f) { new EnemyAttractor(asDouble(f[1]), asDouble(f[2])); }
    }

    static class ItemFuelTank extends Item
    {
        void create(String[] f) { new EnemyFuelTank(asDouble(f[1]), asDouble(f[2])); }
    }

    static class ItemLabTank extends Item
    {
        void create(String[] f) { new EnemyLabTank(asDouble(f[1]), asDouble(f[2]), asInt(f[3])); }
    }

    static class ItemLab extends Item
    {
        void create(String[] f) { new EnemyLab(asDouble(f[1]), asDouble(f[2])); }
    }

    static class ItemGunTower extends Item
    {
        void create(String[] f) { new EnemyGunTower(asDouble(f[1]), asDouble(f[2]), asInt(f[3])); }
    }

    static class ItemTextBox extends Item
    {
        void create(String[] f)
        {
            Wrecks.putTextBox(
                asInt(f[1]), // X
                asInt(f[2]), // Y
                asInt(f[3]), // Width
                f,
                4
            );
        }
    }
}

