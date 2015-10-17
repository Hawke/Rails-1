package net.sf.rails.game.special;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.*;
import net.sf.rails.util.Util;


public class SpecialTileLay extends SpecialProperty {

    private String locationCodes = null;
    private List<MapHex> locations = null;
    private String tileId = null;
    private Tile tile = null;
    private String name = null;
    private boolean extra = false;
    private boolean free = false;
    private int discount = 0;
    private boolean connected = false;
    
    /** Tile colours that can be laid with this special property.
     * Default is same colours as is allowed in a a normal tile lay.
     * Don't use if specific tiles are specified! */
    protected String[] tileColours = null;

    /**
     * Used by Configure (via reflection) only
     */
    public SpecialTileLay(RailsItem parent, String id) {
        super(parent, id);
    }

    @Override
    public void configureFromXML(Tag tag) throws ConfigurationException {
        super.configureFromXML(tag);

        Tag tileLayTag = tag.getChild("SpecialTileLay");
        if (tileLayTag == null) {
            throw new ConfigurationException("<SpecialTileLay> tag missing");
        }

        locationCodes = tileLayTag.getAttributeAsString("location");
        if (!Util.hasValue(locationCodes))
            throw new ConfigurationException("SpecialTileLay: location missing");

        tileId = tileLayTag.getAttributeAsString("tile", null);

        String coloursString = tileLayTag.getAttributeAsString("colour");
        if (Util.hasValue(coloursString)) {
            tileColours = coloursString.split(",");
        }

        name = tileLayTag.getAttributeAsString("name");

        extra = tileLayTag.getAttributeAsBoolean("extra", extra);
        free = tileLayTag.getAttributeAsBoolean("free", free);
        connected = tileLayTag.getAttributeAsBoolean("connected", connected);
        discount = tileLayTag.getAttributeAsInteger("discount", discount);

        if (tileId != null) {
            description = LocalText.getText("LayNamedTileInfo",
                    tileId,
                    name != null ? name : "",
                            locationCodes,
                            (extra ? LocalText.getText("extra"):LocalText.getText("notExtra")),
                            (free ? LocalText.getText("noCost") : discount != 0 ? LocalText.getText("discount", discount) : 
                                LocalText.getText("normalCost")),
                            (connected ? LocalText.getText("connected") : LocalText.getText("unconnected"))
            );
        } else {
            description = LocalText.getText("LayTileInfo",
                    locationCodes,
                    (tileColours != null ? Arrays.toString(tileColours).replaceAll("[\\[\\]]", ""): ""),
                    (extra ? LocalText.getText("extra"):LocalText.getText("notExtra")),
                    (free ? LocalText.getText("noCost") : discount != 0 ? LocalText.getText("discount", discount) : 
                        LocalText.getText("normalCost")),
                    (connected ? LocalText.getText("connected") : LocalText.getText("unconnected"))
            );
        }

    }

    @Override
	public void finishConfiguration (RailsRoot root)
    throws ConfigurationException {

        TileManager tmgr = root.getTileManager();
        MapManager mmgr = root.getMapManager();
        MapHex hex;

        if (tileId != null) {
            tile = tmgr.getTile(tileId);
        }

        locations = new ArrayList<MapHex>();
        for (String hexName : locationCodes.split(",")) {
            hex = mmgr.getHex(hexName);
            if (hex == null)
                throw new ConfigurationException("Location " + hexName
                        + " does not exist");
            locations.add(hex);
        }

    }

    public boolean isExecutionable() {
        return true;
    }

    public boolean isExtra() {
        return extra;
    }

    public boolean isFree() {
        return free;
    }
    
    public int getDiscount() {
        return discount;
    }

    public boolean requiresConnection() {
        return connected;
    }

    public List<MapHex> getLocations() {
        return locations;
    }

    public String getLocationNameString() {
        return locationCodes;
    }

    public String getTileId() {
        return tileId;
    }

    public Tile getTile() {
        return tile;
    }

    public String[] getTileColours() {
        return tileColours;
    }

    @Override
	public String toText() {
        return "SpecialTileLay comp=" + originalCompany.getId()
        + " hex=" + locationCodes
        + " colour="+Util.joinWithDelimiter(tileColours, ",")
        + " extra=" + extra + " cost=" + free + " connected=" + connected;
    }

    @Override
    public String toMenu() {
        return description;
    }

    @Override
    public String getInfo() {
        return description;
    }
}
