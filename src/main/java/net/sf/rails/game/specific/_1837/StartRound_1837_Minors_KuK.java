package net.sf.rails.game.specific._1837;

import java.util.ArrayList;
import java.util.List;

import rails.game.action.BuyStartItem;
import rails.game.action.NullAction;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.StartItem;

public class StartRound_1837_Minors_KuK extends StartRound_1837_Coal {

    public StartRound_1837_Minors_KuK(GameManager gameManager, String id) {
        super(gameManager, id);
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see net.sf.rails.game.specific._1837.StartRound_1837_Coal#setPossibleActions()
     */
    @Override
    public boolean setPossibleActions() {

        List<StartItem> startItems =  startPacket.getItems();
        List<StartItem> buyableItems = new ArrayList<StartItem>();
        int seeks = 0;

        if ((!startPacket.areAllSold()) ){
                 for (StartItem item : startItems) {
                     if (!item.isSold()) {
                        item.setStatus(StartItem.BUYABLE);
                        buyableItems.add(item);
                        }
                 }
                    possibleActions.clear();
             } else { // Are all Sold
                possibleActions.clear();
                return true;
            }
        

        /*
         * Repeat until we have found a player with enough money to buy some
         * item
         */
        while (possibleActions.isEmpty()) {

            Player currentPlayer = playerManager.getCurrentPlayer();
            if (currentPlayer == startPlayer) ReportBuffer.add(this,"");

            int cashToSpend = currentPlayer.getCash();

            for (StartItem item : buyableItems) {
                 if (item.getBasePrice() <= cashToSpend) {
                    /* Player does have the cash */
                    possibleActions.add(new BuyStartItem(item,
                            item.getBasePrice(), false));
                  }
    }  /* Pass is always allowed */
    possibleActions.add(new NullAction(NullAction.Mode.PASS));
                
}
        /* Pass is always allowed */
        possibleActions.add(new NullAction(NullAction.Mode.PASS));

        return true;
    }

    /* (non-Javadoc)
     * @see net.sf.rails.game.specific._1837.StartRound_1837_Coal#start()
     */
    @Override
    public void start() {
           for (StartItem item : startPacket.getItems()) {
                // New: we only include items that have not yet been sold
                // at the start of the current StartRound
                if (!item.isSold()) {
                    itemsToSell.add(item);
                }
            }
            numPasses.set(0);
            
            // init current with priority player
            startPlayer = playerManager.setCurrentToPriorityPlayer();

            ReportBuffer.add(this, LocalText.getText("StartOfInitialRound"));
            ReportBuffer.add(this, LocalText.getText("HasPriority",
                    startPlayer.getId()));
        }
}
        
