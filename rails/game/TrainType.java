/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/TrainType.java,v 1.32 2010/05/11 21:47:21 stefanfrey Exp $ */
package rails.game;

import org.apache.log4j.Logger;

import rails.common.LocalText;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.state.BooleanState;

public class TrainType implements Cloneable {

    public final static int TOWN_COUNT_MAJOR = 2;
    public final static int TOWN_COUNT_MINOR = 1;
    public final static int NO_TOWN_COUNT = 0;

    protected String name;
    protected TrainCertificateType certificateType;

    protected String reachBasis = "stops";
    protected boolean countHexes = false;

    protected String countTowns = "major";
    protected int townCountIndicator = TOWN_COUNT_MAJOR;

    protected String scoreTowns = "yes";
    protected int townScoreFactor = 1;

    protected String scoreCities = "single";
    protected int cityScoreFactor = 1;

    protected int cost;
    protected int majorStops;
    protected int minorStops;
    
    /** A dual train whose certificate can be 'flipped' to the train on the 'reverse' side.
     *  A flip attempt has no effect on non-dual trains.
     */
    protected boolean flippable = false;

    protected int lastIndex = 0;

    protected BooleanState rusted;

    protected TrainManager trainManager;

    /** In some cases, trains start their life in the Pool */
    protected String initialPortfolio = "IPO";

    protected static Logger log =
        Logger.getLogger(TrainType.class.getPackage().getName());

    /**
     * @param real False for the default type, else real. The default type does
     * not have top-level attributes.
     */
    public TrainType() {
    }

    /**
     * @see rails.common.parser.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
     */
    public void configureFromXML(Tag tag) throws ConfigurationException {

        // Name
        name = tag.getAttributeAsString("name");

        // Cost
        cost = tag.getAttributeAsInteger("cost");

        // Major stops
        majorStops = tag.getAttributeAsInteger("majorStops");

        // Minor stops
        minorStops = tag.getAttributeAsInteger("minorStops");

        // Reach
        Tag reachTag = tag.getChild("Reach");
        if (reachTag != null) {
            // Reach basis
            reachBasis = reachTag.getAttributeAsString("base", reachBasis);

            // Are towns counted (only relevant is reachBasis = "stops")
            countTowns =
                reachTag.getAttributeAsString("countTowns", countTowns);
        }

        // Score
        Tag scoreTag = tag.getChild("Score");
        if (scoreTag != null) {
            // Reach basis
            scoreTowns =
                scoreTag.getAttributeAsString("scoreTowns", scoreTowns);

            // Are towns counted (only relevant is reachBasis = "stops")
            scoreCities =
                scoreTag.getAttributeAsString("scoreCities", scoreCities);
        }
        
        // Another type into which this type can be swapped 
        // (used for 'flippable' dual trains)
        Tag swapTag = tag.getChild("Flippable");
        if (swapTag != null) {
            flippable = true;
        }

        // Check the reach and score values
        countHexes = reachBasis.equals("hexes");
        townCountIndicator =
            countTowns.equals("no") ? NO_TOWN_COUNT : minorStops > 0
                    ? TOWN_COUNT_MINOR : TOWN_COUNT_MAJOR;
        cityScoreFactor = scoreCities.equals("double") ? 2 : 1;
        townScoreFactor = scoreTowns.equals("yes") ? 1 : 0;
        // Actually we should meticulously check all values....

    }

    public void finishConfiguration (GameManagerI gameManager, TrainCertificateType trainCertificateType) 
    throws ConfigurationException {

        trainManager = gameManager.getTrainManager();
        this.certificateType = trainCertificateType;
        
        if (name == null) {
            throw new ConfigurationException("No name specified for Train");
        }
        if (cost == 0) {
            throw new ConfigurationException("No price specified for Train "+name);
        }
        if (majorStops == 0) {
            throw new ConfigurationException("No major stops specified for Train "+name);
        }
    }

    public TrainCertificateType getCertificateType() {
        return certificateType;
    }

    /**
     * @return Returns the cityScoreFactor.
     */
    public int getCityScoreFactor() {
        return cityScoreFactor;
    }

    /**
     * @return Returns the cost.
     */
    public int getCost() {
        return cost;
    }

    /**
     * @return Returns the countHexes.
     */
    public boolean countsHexes() {
        return countHexes;
    }

    /**
     * @return Returns the majorStops.
     */
    public int getMajorStops() {
        return majorStops;
    }

    /**
     * @return Returns the minorStops.
     */
    public int getMinorStops() {
        return minorStops;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Returns the townCountIndicator.
     */
    public int getTownCountIndicator() {
        return townCountIndicator;
    }

    /**
     * @return Returns the townScoreFactor.
     */
    public int getTownScoreFactor() {
        return townScoreFactor;
    }
    
    protected boolean isFlippable() {
        return flippable;
    }
    
    protected boolean isDual() {
        return certificateType.isDual();
    }

    @Override
    public Object clone() {

        Object clone = null;
        try {
            clone = super.clone();
        } catch (CloneNotSupportedException e) {
            log.fatal("Cannot clone traintype " + name, e);
            return null;
        }

        return clone;
    }

    public TrainManager getTrainManager() {
        return trainManager;
    }

    public String getInfo() {
        StringBuilder b = new StringBuilder ("<html>");
        b.append(LocalText.getText("TrainInfo", name, Bank.format(cost), 0));
        if (b.length() == 6) b.append(LocalText.getText("None"));

        return b.toString();
    }

    @Override
    public String toString() {
        return name;
    }
}
