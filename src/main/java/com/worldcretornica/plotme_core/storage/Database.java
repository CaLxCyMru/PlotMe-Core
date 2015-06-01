package com.worldcretornica.plotme_core.storage;

import com.worldcretornica.plotme_core.Plot;
import com.worldcretornica.plotme_core.PlotId;
import com.worldcretornica.plotme_core.PlotMeCoreManager;
import com.worldcretornica.plotme_core.PlotMe_Core;
import com.worldcretornica.plotme_core.api.IWorld;
import com.worldcretornica.plotme_core.api.event.PlotLoadEvent;
import com.worldcretornica.plotme_core.api.event.PlotWorldLoadEvent;
import com.worldcretornica.plotme_core.utils.UUIDFetcher;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Database {

    private static final String SELECT_PLOT_COUNT = "SELECT Count(*) as plotCount FROM plotmecore_plots WHERE LOWER(world) = ?";
    final PlotMe_Core plugin;
    private final PlotMeCoreManager manager = PlotMeCoreManager.getInstance();
    public long nextPlotId;
    ConcurrentHashMap<IWorld, ArrayList<Plot>> worldToPlotMap = new ConcurrentHashMap<>();
    ArrayList<Plot> plots = new ArrayList<>();
    Connection connection;

    public Database(PlotMe_Core plugin) {
        this.plugin = plugin;
    }

    /**
     * Closes the connecection to the database.
     * This will not close the connection if the connection is null.
     */
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not close database connection: ");
                plugin.getLogger().severe(e.getMessage());
            }
        }
    }

    public abstract Connection startConnection();

    /**
     * The database connection
     * @return the connection to the database
     */
    Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                return startConnection();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Oh no! A connection error occurred:");
            plugin.getLogger().severe(e.getMessage());
        }
        return connection;
    }

    protected abstract void createTables();

    /**
     * Get the number of plots in the world
     * @param world plotworld to check
     * @return number of plots in the world
     */
    public int getPlotCount(String world) {
        int plotCount = 0;
        try (PreparedStatement ps = getConnection().prepareStatement(SELECT_PLOT_COUNT)) {

            ps.setString(1, world);

            try (ResultSet setNbPlots = ps.executeQuery()) {
                if (setNbPlots.next()) {
                    plotCount = setNbPlots.getInt(1);
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("PlotCount Exception :");
            plugin.getLogger().severe(ex.getMessage());
        }
        return plotCount;
    }

    /**
     * Get the number of plots in the database
     * @return number of plots in the world
     */
    public int getTotalPlotCount() {
        int plotCount = 0;
        try (Statement statement = getConnection().createStatement(); ResultSet setNbPlots = statement.executeQuery(
                "SELECT Count(*) as plotCount FROM plotmecore_plots")) {
            if (setNbPlots.next()) {
                plotCount = setNbPlots.getInt(1);
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("PlotCount Exception :");
            plugin.getLogger().severe(ex.getMessage());
        }
        return plotCount;
    }

    public int getPlotCount(String worldIC, UUID uuid) {
        String world = worldIC.toLowerCase();
        int plotCount = 0;
        try (PreparedStatement ps = getConnection().prepareStatement(SELECT_PLOT_COUNT + " AND ownerID = ?")) {

            ps.setString(1, world);
            ps.setBytes(2, UUIDFetcher.toBytes(uuid));

            try (ResultSet setNbPlots = ps.executeQuery()) {
                if (setNbPlots.next()) {
                    plotCount = setNbPlots.getInt(1);
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("PlotCount Exception :");
            plugin.getLogger().severe(ex.getMessage());
        }
        return plotCount;
    }

    public void addPlot(Plot plot) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "INSERT INTO plotmecore_plots(id,plotX, plotZ, world, ownerID, owner, biome, finished, finishedDate, forSale, price, protected, "
                        + "expiredDate, topX, topZ, bottomX, bottomZ, plotLikes, createdDate) VALUES (?,?,?,?,?,?,?,?,"
                        + "?,?,?,?,?,?,?,?,?,?,?)")) {
            if (plot.getInternalID() == 0) {
                ps.setLong(1, nextPlotId);
                plot.setInternalID(nextPlotId);
                incrementNextInternalPlotId();
            }
            ps.setInt(1, plot.getId().getX());
            ps.setInt(2, plot.getId().getZ());
            ps.setString(3, plot.getWorld().getName().toLowerCase());
            ps.setString(4, plot.getOwnerId().toString());
            ps.setString(5, plot.getOwner());
            ps.setString(6, plot.getBiome());
            ps.setBoolean(7, plot.isFinished());
            ps.setString(8, plot.getFinishedDate());
            ps.setBoolean(9, plot.isForSale());
            ps.setDouble(10, plot.getPrice());
            ps.setBoolean(11, plot.isProtected());
            ps.setDate(12, plot.getExpiredDate());
            ps.setInt(13, plot.getTopX());
            ps.setInt(14, plot.getTopZ());
            ps.setInt(15, plot.getBottomX());
            ps.setInt(16, plot.getBottomZ());
            ps.setInt(17, plot.getLikes());
            ps.setString(18, plot.getCreatedDate());
            ps.executeUpdate();
            getConnection().commit();
        } catch (SQLException ex) {
            plugin.getLogger().severe("Insert Exception :");
            plugin.getLogger().severe(ex.getMessage());
        }
    }

    private void savePlotProperty(int internalID, String pluginname, String propertyname, String s) {

    }

    public void addPlotDenied(String player, long plotInternalID) {
        try (PreparedStatement ps = getConnection().prepareStatement("INSERT INTO plotmecore_denied (plot_id, player) VALUES (?,?)")) {
            ps.setLong(1, plotInternalID);
            ps.setString(2, player);
            ps.execute();
            getConnection().commit();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error adding denied data for plot with internal id " + plotInternalID);
            e.printStackTrace();
        }


    }

    public void addPlotMember(String player, long plotInternalID, Plot.AccessLevel access) {
        try (PreparedStatement ps = getConnection().prepareStatement("INSERT INTO plotmecore_allowed (plot_id, player, access) VALUES(?,?,?)")) {
            ps.setLong(1, plotInternalID);
            ps.setString(2, player);
            ps.setInt(3, access.getLevel());
            ps.execute();
            getConnection().commit();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error adding allowed data for plot with internal id " + plotInternalID);
            e.printStackTrace();
        }
    }

    private void deletePlotInternal(long internalID, String table) {
        try (PreparedStatement ps = getConnection().prepareStatement("DELETE FROM " + table + " WHERE plot_id = ?")) {
            ps.setLong(1, internalID);
            ps.executeUpdate();
            getConnection().commit();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error deleting data from table " + table + " with the internal id " + internalID);
            e.printStackTrace();
        }
    }

    public void deletePlot(long internalID) {
        deletePlotInternal(internalID, "plotmecore_allowed");
        deletePlotInternal(internalID, "plotmecore_denied");
        deletePlotInternal(internalID, "plotmecore_metadata");
        deletePlotInternal(internalID, "plotmecore_likes");
        deletePlotInternal(internalID, "plotmecore_plots");
    }

    public HashSet<Plot> getPlayerPlots(UUID uuid) {
        return null;
    }

    public void updatePlotsNewUUID(UUID uuid, String name) {

    }

    public HashSet<Plot> getOwnedPlots(IWorld world, UUID uuid) {
        HashSet<Plot> ret = new HashSet<>();

        try (PreparedStatement statementPlot = getConnection()
                .prepareStatement("SELECT * FROM plotmecore_plots WHERE LOWER(world) = ? AND ownerID = ?");
                PreparedStatement statementLikes = getConnection().prepareStatement("SELECT * FROM plotmecore_likes WHERE plot_id = ?")) {
            statementPlot.setString(1, uuid.toString());
            statementPlot.setString(2, world.getName().toLowerCase());

            ResultSet setPlots = statementPlot.executeQuery();

            while (setPlots.next()) {
                long internalID = setPlots.getLong("id");
                PlotId id = new PlotId(setPlots.getInt("plotX"), setPlots.getInt("plotZ"));
                String biome = setPlots.getString("biome");
                Date expiredDate = setPlots.getDate("expiredDate");
                boolean finished = setPlots.getBoolean("finished");
                double customprice = setPlots.getDouble("price");
                boolean forsale = setPlots.getBoolean("forSale");
                String finisheddate = setPlots.getString("finishedDate");
                String createddate = setPlots.getString("createdDate");
                boolean protect = setPlots.getBoolean("protected");
                String owner = setPlots.getString("owner");
                final int bottomX = setPlots.getInt("bottomX");
                final int bottomZ = setPlots.getInt("bottomZ");
                final int topX = setPlots.getInt("topX");
                final int topZ = setPlots.getInt("topZ");
                Map<String, Map<String, String>> metadata = new HashMap<>();
                HashMap<String, Plot.AccessLevel> allowed = new HashMap<>();
                HashSet<String> denied = new HashSet<>();

                byte[] byOwner = setPlots.getBytes("ownerid");

                UUID ownerId = UUIDFetcher.fromBytes(byOwner);
                try (PreparedStatement statementAllowed = getConnection().prepareStatement("SELECT * FROM plotmecore_allowed WHERE plot_id = ?")) {
                    statementAllowed.setLong(1, internalID);
                    try (ResultSet setAllowed = statementAllowed.executeQuery()) {
                        while (setAllowed.next()) {
                            String byPlayerId = setAllowed.getString("player");
                            allowed.put(byPlayerId, Plot.AccessLevel.getAccessLevel(setAllowed.getInt("access")));
                        }
                    }
                }
                try (PreparedStatement statementDenied = getConnection().prepareStatement("SELECT * FROM plotmecore_denied WHERE plot_id = ?")) {
                    statementDenied.setLong(1, internalID);
                    try (ResultSet setDenied = statementDenied.executeQuery()) {
                        while (setDenied.next()) {
                            String byPlayerId = setDenied.getString("player");
                            denied.add(byPlayerId);
                        }
                    }
                }
                try (PreparedStatement statementMetadata = getConnection().prepareStatement("SELECT * FROM plotmecore_metadata WHERE plot_id = ?")) {
                    statementMetadata.setLong(1, internalID);
                    try (ResultSet setMetadata = statementMetadata.executeQuery()) {
                        while (setMetadata.next()) {
                            String pluginname = setMetadata.getString("pluginName");
                            String propertyname = setMetadata.getString("propertyName");
                            String propertyvalue = setMetadata.getString("propertyValue");
                            Map<String, String> value = metadata.put(pluginname, new HashMap<String, String>());
                            value.put(propertyname, propertyvalue);
                        }
                    }
                }

                Plot plot = new Plot(internalID, owner, ownerId, world, biome, expiredDate, allowed, denied, new HashSet<String>(), id,
                        customprice, forsale, finished, finisheddate, protect, metadata, 0, null, topX, bottomX, topZ, bottomZ, createddate);
                ret.add(plot);
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("getPlayerPlots Exception :");
            plugin.getLogger().severe(ex.getMessage());
        }
        return ret;
    }

    public void loadPlotsAsynchronously(final IWorld world) {
        plugin.getServerBridge().runTaskAsynchronously(new Runnable() {
            @Override
            public void run() {
                plugin.getLogger().info("Loading plots for world " + world);
                ArrayList<Plot> plots = getPlots(world);

                for (Plot plot : plots) {
                    PlotLoadEvent event = new PlotLoadEvent(world, plot);
                    plugin.getEventBus().post(event);

                }
                PlotWorldLoadEvent event = new PlotWorldLoadEvent(world, plots.size());
                plugin.getEventBus().post(event);

            }

            private ArrayList<Plot> getPlots(IWorld world) {
                ArrayList<Plot> ret = new ArrayList<>();
                Connection connection = getConnection();
                try (PreparedStatement statementPlot = connection.prepareStatement("SELECT * FROM plotmecore_plots WHERE LOWER(world) = ?");
                        PreparedStatement statementAllowed = connection.prepareStatement("SELECT * FROM plotmecore_allowed WHERE plot_id = ?");
                        PreparedStatement statementDenied = connection.prepareStatement("SELECT * FROM plotmecore_denied WHERE plot_id = ?");
                        PreparedStatement statementLikes = connection.prepareStatement("SELECT * FROM plotmecore_likes WHERE plot_id = ?");
                        PreparedStatement statementMetadata = connection.prepareStatement("SELECT * FROM plotmecore_metadata WHERE plot_id = ?")
                ) {
                    statementPlot.setString(1, world.getName().toLowerCase());
                    try (ResultSet setPlots = statementPlot.executeQuery()) {
                        while (setPlots.next()) {
                            long internalID = setPlots.getLong("id");
                            PlotId id = new PlotId(setPlots.getInt("plotX"), setPlots.getInt("plotZ"));
                            String owner = setPlots.getString("owner");
                            UUID ownerId = UUID.fromString(setPlots.getString("ownerID"));
                            String biome = setPlots.getString("biome");
                            Date expiredDate = setPlots.getDate("expiredDate");
                            boolean finished = setPlots.getBoolean("finished");
                            String finishedDate = setPlots.getString("finishedDate");
                            String createdDate = setPlots.getString("createdDate");
                            double price = setPlots.getDouble("price");
                            boolean forSale = setPlots.getBoolean("forSale");
                            boolean protect = setPlots.getBoolean("protected");
                            String plotName = setPlots.getString("plotName");
                            int plotLikes = setPlots.getInt("plotLikes");
                            int topX = setPlots.getInt("topX");
                            int topZ = setPlots.getInt("topZ");
                            int bottomX = setPlots.getInt("bottomX");
                            int bottomZ = setPlots.getInt("bottomZ");
                            HashMap<String, Map<String, String>> metadata = new HashMap<>();
                            HashMap<String, Plot.AccessLevel> allowed = new HashMap<>();
                            HashSet<String> denied = new HashSet<>();
                            HashSet<String> likers = new HashSet<>();
                            statementAllowed.setLong(1, internalID);
                            try (ResultSet setAllowed = statementAllowed.executeQuery()) {
                                while (setAllowed.next()) {
                                    allowed.put(setAllowed.getString("player"), Plot.AccessLevel.getAccessLevel(setAllowed.getInt("access")));
                                }
                            }
                            statementDenied.setLong(1, internalID);
                            try (ResultSet setDenied = statementAllowed.executeQuery()) {
                                while (setDenied.next()) {
                                    denied.add(setDenied.getString("player"));
                                }
                            }
                            statementLikes.setLong(1, internalID);
                            try (ResultSet setLikes = statementLikes.executeQuery()) {
                                while (setLikes.next()) {
                                    likers.add(setLikes.getString("player"));
                                }
                            }

                            statementMetadata.setLong(1, internalID);
                            try (ResultSet setMetadata = statementMetadata.executeQuery()) {
                                while (setMetadata.next()) {
                                    String pluginname = setMetadata.getString("pluginname");
                                    String propertyname = setMetadata.getString("propertyname");
                                    String propertyvalue = setMetadata.getString("propertyvalue");
                                    if (!metadata.containsKey(pluginname)) {
                                        metadata.put(pluginname, new HashMap<String, String>());
                                    }
                                    metadata.get(pluginname).put(propertyname, propertyvalue);
                                }
                            }

                            Plot plot =
                                    new Plot(internalID, owner, ownerId, world, biome, expiredDate, allowed, denied,
                                            likers, id, price, forSale, finished, finishedDate, protect, metadata, plotLikes, plotName, topX, bottomX,
                                            topZ, bottomZ, createdDate);
                            ret.add(plot);
                        }
                    }
                } catch (SQLException ex) {
                    plugin.getLogger().severe("Load exception :");
                    plugin.getLogger().severe(ex.getMessage());
                }
                return ret;
            }
        });
    }

    public Plot getPlot(IWorld world, PlotId id) {
        int idX = id.getX();
        int idZ = id.getZ();
        Plot plot = null;
        Connection connection = getConnection();
        try (PreparedStatement statementPlot = connection.prepareStatement("SELECT * FROM plotmecore_plots WHERE LOWER(world) = ? AND plotX = ? and "
                + "plotZ = ?");
                PreparedStatement statementAllowed = connection.prepareStatement("SELECT * FROM plotmecore_allowed WHERE plot_id = ?");
                PreparedStatement statementDenied = connection.prepareStatement("SELECT * FROM plotmecore_denied WHERE plot_id = ?");
                PreparedStatement statementLikes = connection.prepareStatement("SELECT * FROM plotmecore_likes WHERE plot_id = ?");
                PreparedStatement statementMetadata = connection.prepareStatement("SELECT * FROM plotmecore_metadata WHERE plot_id = ?")) {
            statementPlot.setString(1, world.getName().toLowerCase());
            statementPlot.setInt(2, idX);
            statementPlot.setInt(3, idZ);
            try (ResultSet setPlots = statementPlot.executeQuery()) {
                if (setPlots.next()) {
                    long internalID = setPlots.getLong("id");
                    String byOwner = setPlots.getString("ownerID");
                    UUID ownerId = UUID.fromString(byOwner);
                    String owner = setPlots.getString("owner");
                    String biome = setPlots.getString("biome");
                    boolean finished = setPlots.getBoolean("finished");
                    String finishedDate = setPlots.getString("finishedDate");
                    String createdDate = setPlots.getString("createdDate");
                    boolean forSale = setPlots.getBoolean("forSale");
                    double price = setPlots.getDouble("price");
                    boolean protect = setPlots.getBoolean("protected");
                    Date expiredDate = setPlots.getDate("expiredDate");
                    String plotName = setPlots.getString("plotName");
                    int plotLikes = setPlots.getInt("plotLikes");
                    int topX = setPlots.getInt("topX");
                    int topZ = setPlots.getInt("topZ");
                    int bottomX = setPlots.getInt("bottomX");
                    int bottomZ = setPlots.getInt("bottomZ");

                    HashMap<String, Plot.AccessLevel> allowed = new HashMap<>();
                    HashSet<String> denied = new HashSet<>();
                    HashSet<String> likers = new HashSet<>();
                    Map<String, Map<String, String>> metadata = new HashMap<>();

                    statementAllowed.setLong(1, internalID);
                    try (ResultSet setAllowed = statementAllowed.executeQuery()) {
                        while (setAllowed.next()) {
                            allowed.put(setAllowed.getString("player"), Plot.AccessLevel.getAccessLevel(setAllowed.getInt("access")));
                        }
                    }

                    statementLikes.setLong(1, internalID);
                    try (ResultSet setLikes = statementLikes.executeQuery()) {
                        while (setLikes.next()) {
                            likers.add(setLikes.getString("player"));
                        }
                    }

                    statementDenied.setLong(1, internalID);
                    try (ResultSet setDenied = statementDenied.executeQuery()) {
                        while (setDenied.next()) {
                            denied.add(setDenied.getString("player"));
                        }
                    }

                    statementMetadata.setLong(1, internalID);

                    try (ResultSet setMetadata = statementMetadata.executeQuery()) {

                        while (setMetadata.next()) {
                            String pluginname = setMetadata.getString("pluginName");
                            String propertyname = setMetadata.getString("propertyName");
                            String propertyvalue = setMetadata.getString("propertyValue");
                            if (!metadata.containsKey(pluginname)) {
                                metadata.put(pluginname, new HashMap<String, String>());
                            }
                            metadata.get(pluginname).put(propertyname, propertyvalue);
                        }
                    }

                    plot = new Plot(internalID, owner, ownerId, world, biome, expiredDate, allowed, denied, likers, id, price, forSale,
                            finished,
                            finishedDate, protect, metadata, plotLikes, plotName, topX, bottomX, topZ, bottomZ, createdDate);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return plot;
    }

    public void updatePlot(PlotId id, IWorld name, String biome, Object name1) {
        //TODO Get rid of this ugly method
    }

    public void deletePlotAllowed(long internalID, String player) {
        internalDeletePlotMember("plotmecore_allowed", internalID, player);
    }

    public void deletePlotDenied(long internalID, String name) {
        internalDeletePlotMember("plotmecore_denied", internalID, name);
    }

    public void internalDeletePlotMember(String table, long internalID, String player) {
        Connection connection = getConnection();
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM " + table + " WHERE plot_id = ? AND player = ?")) {
            statement.setLong(1, internalID);
            statement.setString(2, player);
            statement.execute();
            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**
     * This deletes all the players added to this plot. Including trusted players.
     * @param internalID
     */
    public void deleteAllAllowed(long internalID) {
        Connection connection = getConnection();
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM plotmecore_allowed WHERE plot_id = ?")) {
            statement.setLong(1, internalID);
            statement.execute();
            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public void deleteAllDenied(long internalID) {
        Connection connection = getConnection();
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM plotmecore_denied WHERE deniedID = ?")) {
            statement.setLong(1, internalID);
            statement.execute();
            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


    public boolean savePlotProperty(PlotId id, IWorld world, String pluginname, String property, String value) {
        return false;
    }

    public HashSet<Plot> getExpiredPlots(IWorld world) {
        HashSet<Plot> ret = new HashSet<>();

        try (PreparedStatement statementExpired = getConnection().prepareStatement("SELECT * FROM plotmecore_plots WHERE LOWER(world) = ? AND "
                + "protected = 0 AND finished = 0 AND expireddate < ? ORDER BY expireddate")) {
            Calendar cal = Calendar.getInstance();
            java.util.Date utilDate = cal.getTime();
            Date sqlDate = new Date(utilDate.getTime());
            statementExpired.setString(1, world.getName().toLowerCase());
            statementExpired.setDate(2, sqlDate);
            try (ResultSet setPlots = statementExpired.executeQuery()) {
                while (setPlots.next()) {
                    PlotId id = new PlotId(setPlots.getInt("idX"), setPlots.getInt("idZ"));
                    String owner = setPlots.getString("owner");
                    Date expireddate = setPlots.getDate("expireddate");
                    Plot plot = null;
                    plot.setOwner(owner);
                    plot.setId(id);
                    plot.setExpiredDate(expireddate);

                    ret.add(plot);
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("ExpiredPlots Exception :");
            plugin.getLogger().severe(ex.getMessage());
        }
        return ret;
    }

    public HashSet<Plot> getFinishedPlots(String name, int page, int i) {
        return null;
    }

    public void incrementNextInternalPlotId() {
        this.setNextInternalPlotId(this.nextPlotId + 1);
    }

    public void setNextInternalPlotId(long id) {
        this.nextPlotId = id;

        try (Statement statement = getConnection().createStatement()) {
            statement.execute("DELETE FROM plotmecore_nextplotid;");
            statement.execute("INSERT INTO plotmecore_nextplotid VALUES (" + id + ");");
        } catch (SQLException e) {
        }
    }
}
