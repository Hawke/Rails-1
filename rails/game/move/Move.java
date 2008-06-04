/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/Move.java,v 1.2 2008/06/04 19:00:33 evos Exp $
 * 
 * Created on 17-Jul-2006
 * Change Log:
 */
package rails.game.move;

import org.apache.log4j.Logger;

/**
 * @author Erik Vos
 */
public abstract class Move {

    protected static Logger log =
            Logger.getLogger(Move.class.getPackage().getName());

    public abstract boolean execute();

    public abstract boolean undo();

}
