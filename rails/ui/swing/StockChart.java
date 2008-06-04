/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/StockChart.java,v 1.5 2008/06/04 19:00:33 evos Exp $*/
package rails.ui.swing;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import rails.game.*;
import rails.ui.swing.elements.GUIStockSpace;
import rails.util.LocalText;

/**
 * This class displays the StockMarket Window.
 */

public class StockChart extends JFrame implements WindowListener, KeyListener {
    private static final long serialVersionUID = 1L;
    private JPanel stockPanel;
    private Box horLabels, verLabels;

    private GridLayout stockGrid;
    private GridBagConstraints gc;
    private StockSpaceI[][] market;

    public StockChart() {
        super();

        initialize();
        populateStockPanel();

        stockPanel.setBackground(Color.LIGHT_GRAY);

        addWindowListener(this);
        addKeyListener(this);
        pack();
    }

    private void initialize() {
        setTitle("Rails: Stock Chart");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());

        stockPanel = new JPanel();
        horLabels = Box.createHorizontalBox();
        verLabels = Box.createVerticalBox();

        stockGrid = new GridLayout();
        stockGrid.setHgap(0);
        stockGrid.setVgap(0);
        stockPanel.setLayout(stockGrid);

        gc = new GridBagConstraints();

        market = Game.getStockMarket().getStockChart();

        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.BOTH;
        getContentPane().add(stockPanel, BorderLayout.CENTER);
        getContentPane().add(horLabels, BorderLayout.NORTH);
        getContentPane().add(verLabels, BorderLayout.WEST);

    }

    private void populateStockPanel() {
        stockGrid.setColumns(market[0].length);
        stockGrid.setRows(market.length);
        JLabel l;

        for (int i = 0; i < market.length; i++) {
            l = new JLabel("" + (i + 1), JLabel.CENTER);
            l.setAlignmentX(Component.CENTER_ALIGNMENT);

            verLabels.add(Box.createRigidArea(new Dimension(1, i == 0 ? 1 : 12)));
            verLabels.add(Box.createVerticalGlue());
            verLabels.add(l);
            for (int j = 0; j < market[0].length; j++) {
                if (i == 0) {
                    l =
                            new JLabel(Character.toString((char) ('A' + j)),
                                    JLabel.CENTER);
                    l.setAlignmentX(Component.CENTER_ALIGNMENT);

                    horLabels.add(Box.createRigidArea(new Dimension(j == 0 ? 12
                            : 12, 1)));
                    horLabels.add(Box.createHorizontalGlue());
                    horLabels.add(l);
                }
                stockPanel.add(new GUIStockSpace(i, j, market[i][j]));
            }
        }
        verLabels.add(Box.createVerticalGlue());
        horLabels.add(Box.createHorizontalGlue());
    }

    public void windowActivated(WindowEvent e) {}

    public void windowClosed(WindowEvent e) {}

    public void windowClosing(WindowEvent e) {
        StatusWindow.uncheckMenuItemBox(LocalText.getText("MARKET"));
        dispose();
    }

    public void windowDeactivated(WindowEvent e) {}

    public void windowDeiconified(WindowEvent e) {}

    public void windowIconified(WindowEvent e) {}

    public void windowOpened(WindowEvent e) {}

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_F1) {
            HelpWindow.displayHelp(GameManager.getInstance().getHelp());
            e.consume();
        }
    }

    public void keyReleased(KeyEvent e) {}

    public void keyTyped(KeyEvent e) {}

}
