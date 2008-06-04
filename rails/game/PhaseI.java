/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/PhaseI.java,v 1.5 2008/06/04 19:00:30 evos Exp $ */
package rails.game;

import java.util.Map;

public interface PhaseI extends ConfigurableComponentI {
    public boolean isTileColourAllowed(String tileColour);

    /** Called when a phase gets activated */
    public void activate();

    public Map<String, Integer> getTileColours();

    public int getIndex();

    public String getName();

    public boolean doPrivatesClose();

    public boolean isPrivateSellingAllowed();

    public boolean isTrainTradingAllowed();

    public boolean canBuyMoreTrainsPerTurn();

    public boolean canBuyMoreTrainsPerTypePerTurn();

    public int getNumberOfOperatingRounds();

    public int getOffBoardRevenueStep();
}
