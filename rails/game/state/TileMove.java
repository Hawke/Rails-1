/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/TileMove.java,v 1.8 2008/06/04 19:00:33 evos Exp $
 *
 * Created on 17-Jul-2006
 * Change Log:
 */
package rails.game.state;

import java.util.List;

import org.apache.log4j.Logger;

import rails.game.*;

/**
 * should be replaced with stateful variables
 * @author Erik Vos
 */
@Deprecated
public class TileMove implements Move {

    protected static Logger log =
        Logger.getLogger(TileMove.class.getPackage().getName());

    MapHex hex;
    TileI oldTile;
    int oldTileOrientation;
    List<Stop> oldStations;
    TileI newTile;
    int newTileOrientation;
    List<Stop> newStations;

    public TileMove(MapHex hex, TileI oldTile, int oldTileOrientation,
            List<Stop> oldStations, TileI newTile, int newTileOrientation,
            List<Stop> newStations) {

        this.hex = hex;
        this.oldTile = oldTile;
        this.oldTileOrientation = oldTileOrientation;
        this.oldStations = oldStations;
        this.newTile = newTile;
        this.newTileOrientation = newTileOrientation;
        this.newStations = newStations;

//        MoveSet.add(this);
    }

    public boolean execute() {

        hex.replaceTile(oldTile, newTile, newTileOrientation, newStations);
        return true;
    }

    public boolean undo() {

        hex.replaceTile(newTile, oldTile, oldTileOrientation, oldStations);
        log.debug("-Undone: " + toString());
        return true;
    }

    @Override
    public String toString() {
        return "TileMove: hex " + hex.getId() + " from #" + oldTile.getNb()
               + "/" + oldTileOrientation + " to #" + newTile.getNb() + "/"
               + newTileOrientation;
    }

}