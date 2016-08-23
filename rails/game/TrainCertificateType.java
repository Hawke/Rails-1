package rails.game;

import java.util.*;

import org.apache.log4j.Logger;

import rails.common.LocalText;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.state.BooleanState;
import rails.game.state.IntegerState;
import rails.util.*;

public class TrainCertificateType {

    protected String name;
    protected int quantity = 0;
    protected boolean infiniteQuantity = false;

    protected List<TrainType> potentialTrainTypes = new ArrayList<TrainType>(2);
    
    protected Map<Integer, String> newPhaseNames;
    
    protected boolean permanent = true;
    protected boolean obsoleting = false;

    protected boolean canBeExchanged = false;
    protected int cost;
    protected int exchangeCost;
    
    protected String trainClassName = "rails.game.Train";
    protected Class<? extends Train> trainClass;

    // State variables
    protected IntegerState numberBoughtFromIPO;
    protected BooleanState available;
    protected BooleanState rusted;

    // References
    protected TrainManager trainManager;

    /** In some cases, trains start their life in the Pool */
    protected String initialPortfolio = "IPO";

    protected static Logger log =
        Logger.getLogger(TrainCertificateType.class.getPackage().getName());
    
    public TrainCertificateType () {
    }
    
    public void configureFromXML(Tag tag) throws ConfigurationException {

        trainClassName = tag.getAttributeAsString("class", trainClassName);
        try {
            trainClass = Class.forName(trainClassName).asSubclass(Train.class);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException("Class " + trainClassName
                    + "not found", e);
        }

        // Name
        name = tag.getAttributeAsString("name");

        // Quantity
        quantity = tag.getAttributeAsInteger("quantity", quantity);
        quantity += tag.getAttributeAsInteger("quantityIncrement", 0);

        // From where is this type initially available
        initialPortfolio =
            tag.getAttributeAsString("initialPortfolio",
                    initialPortfolio);
        
        // New style phase changes (to replace 'startPhase' attribute and <Sub> tag)
        List<Tag> newPhaseTags = tag.getChildren("NewPhase");
        if (newPhaseTags != null) {
            int index;
            String phaseName;
            newPhaseNames = new HashMap<Integer, String>();
            for (Tag newPhaseTag : newPhaseTags) {
                phaseName = newPhaseTag.getAttributeAsString("phaseName");
                if (!Util.hasValue(phaseName)) {
                    throw new ConfigurationException ("TrainType "+name+" has NewPhase without phase name");
                }
                index = newPhaseTag.getAttributeAsInteger("trainIndex", 1);
                newPhaseNames.put(index, phaseName);
            }
        }
 
        // Exchangeable
        Tag swapTag = tag.getChild("Exchange");
        if (swapTag != null) {
            exchangeCost = swapTag.getAttributeAsInteger("cost", 0);
            canBeExchanged = (exchangeCost > 0);
        }

        // Can run as obsolete train
        obsoleting = tag.getAttributeAsBoolean("obsoleting");

        // Final initialisations
        numberBoughtFromIPO = new IntegerState(name + "-trains_Bought", 0);
        available = new BooleanState(name + "-trains_Available", false);
        rusted = new BooleanState(name + "-trains_Rusted", false);

    }

    public void finishConfiguration (GameManagerI gameManager) 
    throws ConfigurationException {

        trainManager = gameManager.getTrainManager();
        
        if (name == null) {
            throw new ConfigurationException("No name specified for TrainType");
        }
        
        if (quantity == -1) {
            infiniteQuantity = true;
        } else if (quantity <= 0) {
            throw new ConfigurationException("Invalid quantity "+quantity+" for train cert type "+name);
        }
    }

    public Map<Integer, String> getNewPhaseNames() {
        return newPhaseNames;
    }

    public TrainI createTrain () throws ConfigurationException {

        TrainI train;
        try {
            train = trainClass.newInstance();
        } catch (InstantiationException e) {
            throw new ConfigurationException(
                    "Cannot instantiate class " + trainClassName, e);
        } catch (IllegalAccessException e) {
            throw new ConfigurationException("Cannot access class "
                    + trainClassName
                    + "constructor", e);
        }
        return train;
    }

    public List<TrainType> getPotentialTrainTypes() {
        return potentialTrainTypes;
    }

    protected void addPotentialTrainType (TrainType type) {
        potentialTrainTypes.add(type);
    }
    
    /**
     * @return Returns the available.
     */
    public boolean isAvailable() {
        return available.booleanValue();
    }
    
    public boolean isDual() {
        return potentialTrainTypes.size() == 2;
    }

    /**
     * Make a train type available for buying by public companies.
     */
    public void setAvailable() {
        available.set(true);
    }

    public void setRusted() {
        rusted.set(true);
    }

    public boolean hasRusted() {
        return rusted.booleanValue();
    }
    
    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    public boolean isPermanent() {
        return permanent;
    }

    public boolean isObsoleting() {
        return obsoleting;
    }

    public void setPermanent(boolean permanent) {
        this.permanent = permanent;
    }

    public int getQuantity() {
        return quantity;
    }

    public boolean hasInfiniteQuantity() {
        return infiniteQuantity;
    }

    public boolean nextCanBeExchanged() {
        return canBeExchanged;
    }

    public void addToBoughtFromIPO() {
        numberBoughtFromIPO.add(1);
    }

    public int getNumberBoughtFromIPO() {
        return numberBoughtFromIPO.intValue();
    }

    public int getCost() {
        return cost;
    }

    public int getExchangeCost() {
        return exchangeCost;
    }

    public String getInitialPortfolio() {
        return initialPortfolio;
    }

    public String getInfo() {
        StringBuilder b = new StringBuilder ("<html>");
        b.append(LocalText.getText("TrainInfo", name, Bank.format(cost), quantity));
        if (b.length() == 6) b.append(LocalText.getText("None"));

        return b.toString();
    }

    private void appendInfoText (StringBuilder b, String text) {
        if (text == null || text.length() == 0) return;
        if (b.length() > 6) b.append("<br>");
        b.append(text);
    }

    public String toString() {
        return name;
    }
}
