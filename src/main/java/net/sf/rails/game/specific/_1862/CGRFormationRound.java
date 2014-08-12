package net.sf.rails.game.specific._1862;

import java.util.*;

import rails.game.action.*;

import net.sf.rails.common.*;
import net.sf.rails.game.*;
import net.sf.rails.game.Currency;
import net.sf.rails.game.special.SellBonusToken;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.Portfolio;


public class CGRFormationRound extends SwitchableUIRound {

    private Player startingPlayer;
    private int maxLoansToRepayByPresident = 0;
    private Map<Player, List<PublicCompany>> companiesToRepayLoans = null;

    private PublicCompany currentCompany = null;
    private List<PublicCompany> mergingCompanies = new ArrayList<PublicCompany>();

    /*
     * pointers to cgr company
     */
    private String cgrName = PublicCompany_CGR.NAME;
    private PublicCompany_CGR cgr
    = (PublicCompany_CGR)gameManager.getRoot().getCompanyManager().getPublicCompany(cgrName);

    /*
     * effects from the merger, processed at the end
     * thus no need for state variables
     */
    private Set<Train> trainsToDiscardFrom = null;
    private boolean forcedTrainDiscard = true;
    private List<ExchangeableToken> tokensToExchangeFrom = null;
    private List<BaseToken> nonHomeTokens = null;

    private final IntegerState stepObject = IntegerState.create(this, "stepObject");
    private final BooleanState cgrHasDiscardedTrains = BooleanState.create(this, "cgrHasDiscardedTrains");

    public static final int STEP_REPAY_LOANS = 1;
    public static final int STEP_DISCARD_TRAINS = 2;
    public static final int STEP_EXCHANGE_TOKENS = 3;

    private static int[][] certLimitsTable = {
        {14, 19, 21, 26, 29, 31, 36, 40},
        {10, 13, 15, 18, 20, 22, 25, 28},
        {8, 10, 12, 14, 16, 18, 20, 22},
        {7, 8, 10, 11, 13, 15, 16, 18},
        {6, 7, 8, 10, 11, 12, 14, 15}
    };

    /**
     * Constructed via Configure
     */
    public CGRFormationRound(GameManager parent, String id) {
        super(parent, id);

        guiHints.setVisibilityHint(GuiDef.Panel.MAP, true);
        guiHints.setVisibilityHint(GuiDef.Panel.STATUS, true);
    }
    
    @Override
    /** This class needs the game status window to show up
     * rather than the operating round window.
     */
    public Class<? extends Round> getRoundTypeForUI () {
        return StockRound.class;
    }

    public void start (Player startingPlayer) {

        this.startingPlayer = startingPlayer;

        Player president;

        companiesToRepayLoans = null;

        ReportBuffer.add(this, LocalText.getText("StartFormationRound",
                cgrName));
        ReportBuffer.add(this, LocalText.getText("StartingPlayer", startingPlayer.getId()));

        guiHints.setCurrentRoundType(getClass());

        // Collect companies having loans
        for (PublicCompany company : setOperatingCompanies()) {
            if (company.getCurrentNumberOfLoans() > 0) {
                if (companiesToRepayLoans == null) {
                    companiesToRepayLoans
                        = new HashMap<Player, List<PublicCompany>>();
                }
                president = company.getPresident();
                if (!companiesToRepayLoans.containsKey(president)) {
                    companiesToRepayLoans.put (president, new ArrayList<PublicCompany>());
                }
                companiesToRepayLoans.get(president).add(company);
            }
        }

        if (companiesToRepayLoans == null) {
            ReportBuffer.add(this, LocalText.getText("DoesNotForm", cgr.getId()));
            finishRound();
            return;
        }

        setStep(STEP_REPAY_LOANS);

        setCurrentPlayer (startingPlayer);

        process (null);
    }

    private void setStep(int step) {
        stepObject.set(step);
    }

    private int getStep() {
        return stepObject.value();
    }

    private boolean setNextCompanyNeedingPresidentIntervention () {

        while (true) {

            while (!companiesToRepayLoans.containsKey(getCurrentPlayer())) {
                getRoot().getPlayerManager().setCurrentToNextPlayer();
                if (getCurrentPlayer().equals(startingPlayer)) {
                    return false;
                }
            }

            // Player to act already has been selected
            Player player = getCurrentPlayer();
            if (companiesToRepayLoans.get(player).isEmpty()) {
                companiesToRepayLoans.remove(player);
                continue;
            }
            currentCompany = companiesToRepayLoans.get(player).get(0);
            companiesToRepayLoans.get(player).remove(currentCompany);

            int numberOfLoans = currentCompany.getCurrentNumberOfLoans();
            if (numberOfLoans == 0) continue;

            int compCash = currentCompany.getCash();
            int presCash = player.getCash();
            int valuePerLoan = currentCompany.getValuePerLoan();
            String message;
            int payment;

            message = LocalText.getText("CompanyHasLoans",
                    currentCompany.getId(),
                    player.getId(),
                    numberOfLoans,
                    Currency.format(this, valuePerLoan),
                    Currency.format(this, numberOfLoans * valuePerLoan));
            ReportBuffer.add(this, " ");
            DisplayBuffer.add(this, " ", false);
            ReportBuffer.add(this, message);
            DisplayBuffer.add(this, message, false);

            // Let company repay all loans for which it has the cash
            int numberToRepay = Math.min(numberOfLoans,
                    compCash / valuePerLoan);
            if (numberToRepay > 0) {
                payment = numberToRepay * valuePerLoan;
                String paymentText = Currency.toBank(currentCompany, payment);
                currentCompany.addLoans(-numberToRepay);

                message = LocalText.getText("CompanyRepaysLoans",
                        currentCompany.getId(),
                        paymentText,
                        bank.getCurrency().format(numberOfLoans * valuePerLoan), // TODO: Do this nicer
                        numberToRepay,
                        bank.getCurrency().format(valuePerLoan)); // TODO: Do this nicer
                ReportBuffer.add(this, message);
                DisplayBuffer.add(this, message, false);
            }

            // If that was all, we're done with this company
            numberOfLoans = currentCompany.getCurrentNumberOfLoans();
            if (numberOfLoans == 0) {
                continue;
            }

            // Check the president's cash
            // He should be involved if at least one extra loan could be repaid
            compCash = currentCompany.getCash();
            if ((compCash + presCash) / valuePerLoan > 0) {
                int maxNumber = Math.min((compCash + presCash)/valuePerLoan, numberOfLoans);
                if (maxNumber == numberOfLoans) {
                    DisplayBuffer.add(this, LocalText.getText("YouCanRepayAllLoans",
                            player.getId(),
                            maxNumber,
                            currentCompany.getId()),
                            false);
                } else {
                    DisplayBuffer.add(this, LocalText.getText("YouCannotRepayAllLoans",
                            player.getId(),
                            maxNumber,
                            numberOfLoans,
                            currentCompany.getId()),
                            false);
                    //                    currentCompany.getLoanValueModel().setText(LocalText.getText("MERGE"));
                }
                maxLoansToRepayByPresident = maxNumber;
                break;
            } else {
                // President cannot help, this company will merge into CGR anyway
                mergingCompanies.add(currentCompany);
                //                currentCompany.getLoanValueModel().setText(LocalText.getText("MERGE"));
                message = LocalText.getText("WillMergeInto",
                        currentCompany.getId(),
                        PublicCompany_CGR.NAME);
                DisplayBuffer.add(this, message, false);
                ReportBuffer.add(this, message);
                continue;
            }
        }
        return true;
    }

    @Override
    public boolean setPossibleActions() {

        int step = getStep();
        if (step == STEP_REPAY_LOANS) {
            RepayLoans action = new RepayLoans (currentCompany, 0,
                    maxLoansToRepayByPresident,
                    currentCompany.getValuePerLoan());
            possibleActions.add(action);
            guiHints.setActivePanel(GuiDef.Panel.STATUS);
        } else if (step == STEP_EXCHANGE_TOKENS) {
            int numberToExchange = cgr.getNumberOfFreeBaseTokens();
            ExchangeTokens action = new ExchangeTokens (tokensToExchangeFrom,
                    numberToExchange, numberToExchange);
            action.setCompany(cgr);
            possibleActions.add(action);
            guiHints.setActivePanel(GuiDef.Panel.STATUS);
        } else if (step == STEP_DISCARD_TRAINS) {
            DiscardTrain action = new DiscardTrain (cgr,
                    trainsToDiscardFrom, forcedTrainDiscard);
            possibleActions.add(action);
            guiHints.setActivePanel(GuiDef.Panel.STATUS);
        }
        return true;

    }

    protected boolean repayLoans (RepayLoans action) {

        // TODO Validation skipped for now...

        
        // FIMXE: linked to previous moveset
        // changeStack.linkToPreviousMoveSet();

        PublicCompany company = action.getCompany();
        int numberRepaid = action.getNumberRepaid();
        int repayment = numberRepaid * company.getValuePerLoan();

        if (repayment > 0) {

            int repaymentByCompany = Math.min (repayment, company.getCash());
            int repaymentByPresident = repayment - repaymentByCompany;

            company.addLoans(-numberRepaid);
            if (repaymentByCompany > 0) {
                String repayCompanyText = Currency.toBank(company, repaymentByCompany);
                ReportBuffer.add(this, LocalText.getText("CompanyRepaysLoans",
                        company.getId(),
                    repayCompanyText,
                        numberRepaid,
                    Currency.format(this, company.getValuePerLoan()))); // TODO: Make this nicer
            }
            if (repaymentByPresident > 0) {
                Player president = company.getPresident();
                String repayPresidentText =  Currency.toBank(president, repaymentByPresident);
                ReportBuffer.add(this, LocalText.getText("CompanyRepaysLoansWithPresCash",
                        company.getId(),
                        repayPresidentText,
                        bank.getCurrency().format(repayment), // TODO: Make this nicer
                        numberRepaid,
                        bank.getCurrency().format(company.getValuePerLoan()), // TODO: Make this nicer
                        president.getId()));
            }
        }

        if (action.getCompany().getCurrentNumberOfLoans() > 0) {
            mergingCompanies.add(currentCompany);
            //            currentCompany.getLoanValueModel().setText(LocalText.getText("MERGE"));
            String message = LocalText.getText("WillMergeInto",
                    currentCompany.getId(),
                    PublicCompany_CGR.NAME);
            DisplayBuffer.add(this, message, true);
            ReportBuffer.add(this, message);

        }

        return true;

    }

    private void formCGR () {

        Player player;
        int count, cgrSharesUsed, oldShares, newShares;
        PublicCertificate cgrCert, poolCert;
        List<PublicCertificate> certs = new ArrayList<PublicCertificate>();
        List<PublicCompany> availableCompanies;
        Player temporaryPresident = null;
        Player newPresident = null;
        Player firstCGRowner = null;
        int maxShares = 0;

        // Exchange the player shares
        setCurrentPlayer(startingPlayer);
        cgrSharesUsed = 0;

        ReportBuffer.add(this, "");

        do {
            player = getCurrentPlayer();
            oldShares = newShares = 0;
            certs.clear();
            poolCert = null;

            for (PublicCertificate cert : player.getPortfolioModel().getCertificates()) {
                if (mergingCompanies.contains(cert.getCompany())) {
                    certs.add((cert));
                    oldShares++;
                    if (cert.isPresidentShare()) {
                        oldShares++;
                    }
                }
            }

            if (oldShares > 0) {

                count = oldShares;
                if (count >= 4 && temporaryPresident == null && cgrSharesUsed <= 18) {
                    cgrCert = cgr.getPresidentsShare();
                    cgrCert.moveTo(player);
                    count -= 4;
                    cgrSharesUsed += 2;
                    newShares += 2;
                    temporaryPresident = player;
                }
                while (count >= 2 && cgrSharesUsed <= 19) {
                    cgrCert = unavailable.findCertificate(cgr, false);
                    cgrCert.moveTo(player);
                    count -= 2;
                    cgrSharesUsed++;
                    newShares++;
                }

                String message = LocalText.getText("HasMergedShares",
                        player.getId(),
                        oldShares,
                        newShares,
                        PublicCompany_CGR.NAME);
                DisplayBuffer.add(this, message, false);
                ReportBuffer.add(this, message);

                if (count == 1) {
                    // Should work OK even if this is a president's share.
                    // In the pool we will treat all certs equally.
                    poolCert = certs.get(certs.size()-1);
                    poolCert.moveTo(pool.getParent());
                    certs.remove(poolCert);

                    message = LocalText.getText("HasPutShareInPool",
                            player.getId());
                    DisplayBuffer.add(this, message, false);
                    ReportBuffer.add(this, message);

                }
                // Note: old shares are removed when company is closed

                if (firstCGRowner == null) firstCGRowner = player;

                // Check for presidency
                if (newShares > maxShares) {
                    maxShares = newShares;
                    newPresident = player;
                }
            }

            getRoot().getPlayerManager().setCurrentToNextPlayer();

        } while (getCurrentPlayer() != startingPlayer);

        // Exchange the pool shares
        certs.clear();
        oldShares = newShares = 0;

        for (PublicCertificate cert : pool.getCertificates()) {
            if (mergingCompanies.contains(cert.getCompany())) {
                certs.add((cert));
                oldShares++;
            }
        }
        count = oldShares;
        while (count >= 2 && cgrSharesUsed <= 19) {
            cgrCert = unavailable.findCertificate(cgr, false);
            cgrCert.moveTo(pool.getParent());
            count -= 2;
            cgrSharesUsed++;
            newShares++;
        }

        String message = LocalText.getText("HasMergedShares",
                LocalText.getText("POOL"),
                oldShares,
                newShares,
                PublicCompany_CGR.NAME);
        DisplayBuffer.add(this, message);
        ReportBuffer.add(this, message);

        Portfolio.moveAll(certs, scrapHeap.getParent());
        log.info(cgrSharesUsed+" CGR shares are now in play");

        // If no more than 10 shares are in play, the CGR share
        // unit becomes 10%; otherwise it stays 5%.
        if (cgrSharesUsed <=10) {
            cgr.setShareUnit (10);
            // All superfluous shares have been removed
        }
        message = LocalText.getText("CompanyHasShares",
                cgr.getId(), 100/cgr.getShareUnit(), cgr.getShareUnit());
        DisplayBuffer.add(this, " ");
        ReportBuffer.add(this, " ");
        DisplayBuffer.add(this, message);
        ReportBuffer.add(this, message);

        // Move the remaining CGR shares to the ipo.
        Portfolio.moveAll(unavailable.getCertificates(cgr), ipo.getParent());

        // Assign the new president
        if (newPresident.getPortfolioModel().getShare(cgr) == cgr.getShareUnit()) {
            // Nobody has 2 shares, then takes the first player who has got one share
            log.debug("Nobody has two shares, creating a temp.pres.: "+firstCGRowner.getId());
            cgr.setTemporaryPresident(firstCGRowner);
            newPresident = firstCGRowner;
        } else if (temporaryPresident != null && temporaryPresident != newPresident) {
            log.debug("Moving pres.share from "+temporaryPresident.getId()
                    +" to "+newPresident.getId());
            temporaryPresident.getPortfolioModel().swapPresidentCertificate(cgr,
                        newPresident.getPortfolioModel(), 1);
        }

        // TODO: What does the following command do? I assume only trigger an update, so I uncommented
        // newPresident.getPortfolio().getShareModel(cgr).setShare();
        message = LocalText.getText("IS_NOW_PRES_OF",
                newPresident.getId(), cgrName);
        ReportBuffer.add(this, message);
        DisplayBuffer.add(this, message);

        // Determine the CGR starting price,
        // and close the absorbed companies.
        int lowestPrice = 999;
        int totalPrice = 0;
        int price;
        int numberMerged = mergingCompanies.size();
        for (PublicCompany comp : mergingCompanies) {
            price = comp.getMarketPrice();
            totalPrice += price;
            if (price < lowestPrice) lowestPrice = price;
        }
        if (numberMerged >= 3) {
            totalPrice -= lowestPrice;
            numberMerged--;
        }
        int cgrPrice = Math.max(100, (((totalPrice/numberMerged)/5))*5);

        // Find the correct start space and start the CGR
        if (cgrPrice == 100) {
            cgr.start(100);
        } else {
            int prevColPrice = 100;
            int colPrice;
            StockSpace startSpace;
            for (int col=6; col <= stockMarket.getNumberOfColumns(); col++) {
                colPrice = stockMarket.getStockSpace(0, col).getPrice();
                if (cgrPrice > colPrice) continue;
                if (cgrPrice - prevColPrice < colPrice - cgrPrice) {
                    startSpace = stockMarket.getStockSpace(0, col-1);
                } else {
                    startSpace = stockMarket.getStockSpace(0, col);
                }
                cgr.start(startSpace);
                message = LocalText.getText("START_MERGED_COMPANY",
                        PublicCompany_CGR.NAME,
                        Currency.format(this, startSpace.getPrice()),
                        startSpace.getId());
                DisplayBuffer.add(this, message);
                ReportBuffer.add(this, message);
                break;
            }
        }
        cgr.setFloated();
        ReportBuffer.add(this, LocalText.getText("Floats", PublicCompany_CGR.NAME));

        // Determine the new certificate limit.
        // The number of available companies is 11,
        // or 12 minus the number of closed companies, whichever is lower.
        //Make sure that only available companies are counted
        availableCompanies= getRoot().getGameManager().getAllPublicCompanies();
        int validCompanies= 12; //including the CGR
        //Need to find out if a company is already closed, if yes
        //decrease the validCompany value by 1
        for(PublicCompany c : availableCompanies) {
            if (c.isClosed()) {
                validCompanies--;
            }
        }
        int numCompanies = Math.min(11, validCompanies-mergingCompanies.size());
        int numPlayers = getNumberOfPlayers();
        // Need some checks here...
        int newCertLimit = certLimitsTable[numPlayers-2][numCompanies-4];
        getRoot().getPlayerManager().setPlayerCertificateLimit(newCertLimit);
        message = LocalText.getText("CertificateLimit",
                newCertLimit,
                numPlayers,
                numCompanies);
        DisplayBuffer.add(this, message);
        ReportBuffer.add(this, message);

        // Collect the old token spots, and move cash and trains
        List<BaseToken> homeTokens = new ArrayList<BaseToken>();
        nonHomeTokens = new ArrayList<BaseToken>();
        BaseToken bt;
        MapHex hex;
        Stop stop;
        for (PublicCompany comp : mergingCompanies) {

            // Exchange home tokens and collect non-home tokens
            List<MapHex> homeHexes = comp.getHomeHexes();
            for (BaseToken token :comp.getAllBaseTokens()) {
                bt = token;
                if (!bt.isPlaced()) continue;
                stop = (Stop) bt.getOwner();
                hex = stop.getParent();
                if (homeHexes != null && homeHexes.contains(hex)) {
                    homeTokens.add(bt);
                } else {
                    nonHomeTokens.add(bt);
                }
            }

            // Move any remaining cash
            if (comp.getCash() > 0) {
                Currency.wireAll(comp, cgr);
            }

            // Move any remaining trains
            Set<Train> trains = comp.getPortfolioModel().getTrainList();
            for (Train train : trains) {
                cgr.getPortfolioModel().addTrain(train);
                if (train.isPermanent()) cgr.setHadPermanentTrain(true);
            }

            // Move any still valid bonuses
            if (comp.getBonuses() != null) {
                List<Bonus> bonuses = new ArrayList<Bonus> (comp.getBonuses());
                bonuses:        for (Bonus bonus : bonuses) {
                    comp.removeBonus(bonus);
                    // Only add if the CGR does not already have the same bonus
                    if (cgr.getBonuses() != null) {
                        for (Bonus b : cgr.getBonuses()) {
                            if (b.equals(bonus)) {
                                // Remove this duplicate bonus token.
                                // Check if it should be made available again.
                                List<SellBonusToken> commonSP = gameManager.getSpecialProperties(SellBonusToken.class, true);
                                if (commonSP != null) {
                                    for (SellBonusToken sp : commonSP) {
                                        if (sp.getId().equalsIgnoreCase(b.getName())) {
                                            sp.makeResellable();
                                            log.debug("BonusToken "+b.getName()+" made sellable again");
                                            break;
                                        }
                                    }
                                }
                                log.debug("Duplicate BonusToken "+b.getName()+" not added to "+cgrName);
                                continue bonuses;
                            }
                        }
                    }
                    cgr.addBonus(new Bonus(cgr, bonus.getName(), bonus.getValue(),
                            bonus.getLocations()));
                }
            }
        }

        // Replace the home tokens
        ReportBuffer.add(this, "");
        for (BaseToken token : homeTokens) {
            stop = (Stop) token.getOwner();
            hex = stop.getParent();
            // return token to home
            token.moveTo(token.getParent());
            if (hex.layBaseToken(cgr, stop)) {
                /* TODO: the false return value must be impossible. */
                ReportBuffer.add(this, LocalText.getText("ExchangesBaseToken",
                        cgrName, token.getParent().getId(),
                        stop.getSpecificId()));
                cgr.layBaseToken(hex, 0);
            }
        }

        // Clean up any non-home tokens on cities now having a CGR token
        for (BaseToken token : new ArrayList<BaseToken>(nonHomeTokens)) {
            stop = (Stop) token.getOwner();
            hex = stop.getParent();
            Set<BaseToken> otherTokens = hex.getBaseTokens();
            if (otherTokens != null) {
                for (BaseToken token2 : otherTokens) {
                    if (token2.getParent() == cgr
                            || nonHomeTokens.contains(token2) && token2 != token) {
                        ReportBuffer.add(this, LocalText.getText("DiscardsBaseToken",
                                cgrName, token.getParent().getId(),
                                stop.getSpecificId()));
                        // return token to home
                        token.moveTo(token.getParent());
                        nonHomeTokens.remove(token);
                        break;
                    }
                }
            }
        }

        // Prepare replacing the other tokens, if possible
        if (homeTokens.size() + nonHomeTokens.size() > cgr.getNumberOfBaseTokens()) {
            // CGR cannot replace all tokens, must choose
            // First collect old names per city
            Map<String, String> oldTokens = new HashMap<String, String>();
            String cityName;
            for (BaseToken token : nonHomeTokens) {
                if (token.getOwner() instanceof Stop) {
                    cityName = ((Stop)token.getOwner()).getSpecificId();
                    if (oldTokens.containsKey(cityName)) {
                        oldTokens.put(cityName,
                                oldTokens.get(cityName)+","+token.getParent().getId());
                    } else {
                        oldTokens.put(cityName, token.getParent().getId());
                    }
                }
            }
            // Then create list of exchange spots. Sort it on hexname/city number
            tokensToExchangeFrom = new ArrayList<ExchangeableToken>();
            for (String key : new TreeSet<String> (oldTokens.keySet())) {
                tokensToExchangeFrom.add(new ExchangeableToken(
                        key, oldTokens.get(key)));
            }
        } else {
            executeExchangeTokens(nonHomeTokens);
        }

        // Close the merged companies
        for (PublicCompany comp : mergingCompanies) {
            comp.setClosed();
        }

        // Check the trains, autodiscard any excess non-permanent trains
        //        int trainLimit = cgr.getTrainLimit(gameManager.getCurrentPlayerIndex());
        int trainLimit = cgr.getCurrentTrainLimit();
        Set<Train> trains = cgr.getPortfolioModel().getTrainList();
        if (cgr.getNumberOfTrains() > trainLimit) {
            ReportBuffer.add(this, "");
            int numberToDiscard = cgr.getNumberOfTrains() - trainLimit;
            List<Train> trainsToDiscard = new ArrayList<Train>(4);
            for (Train train : trains) {
                if (!train.isPermanent()) {
                    trainsToDiscard.add(train);
                    if (--numberToDiscard == 0) break;
                }
            }
            for (Train train : trainsToDiscard) {
                pool.addTrain(train);
                ReportBuffer.add(this, LocalText.getText("CompanyDiscardsTrain",
                        cgrName, train.toText()));
            }
        }

    }

    private void executeExchangeTokens (List<BaseToken> exchangedTokens) {
        Stop stop;
        MapHex hex;
        ReportBuffer.add(this, "");
        for (BaseToken token : exchangedTokens) {
            // Remove old token
            stop = (Stop) token.getOwner();
            hex = stop.getParent();
            // return token to Company
            token.moveTo(token.getParent());
            // Replace it with a CGR token
            if (hex.layBaseToken(cgr, stop)) {
                cgr.layBaseToken(hex, 0);
            } else {
                log.error("Error in laying CGR token on "+hex.getId()+" "+hex.getStopName());
            }
        }
    }

    @Override
    public boolean process (PossibleAction action) {

        boolean result = true;

        if (action instanceof RepayLoans) {
            result = repayLoans((RepayLoans)action);
        } else if (action instanceof DiscardTrain) {
            result = discardTrain((DiscardTrain)action);
        } else if (action instanceof ExchangeTokens) {
            result = exchangeTokens((ExchangeTokens)action, true); // 2nd parameter: linked moveset
        }
        if (!result) return false;

        if (getStep() == STEP_REPAY_LOANS) {

            if (setNextCompanyNeedingPresidentIntervention()) {
                return true;
            }

            if (!mergingCompanies.isEmpty()) {
                formCGR();
                setStep (STEP_EXCHANGE_TOKENS);
            } else {
                finishRound();
            }
        }

        if (getStep() == STEP_EXCHANGE_TOKENS) {

            if (action instanceof ExchangeTokens) {
                tokensToExchangeFrom = null;
            } else if (tokensToExchangeFrom != null
                    && !tokensToExchangeFrom.isEmpty()) {
                return true;
            }
            setStep (STEP_DISCARD_TRAINS);
        }

        if (getStep() == STEP_DISCARD_TRAINS) {

            if (checkForTrainsToDiscard()) return true;
            finishRound();
        }

        return true;
    }


    private boolean checkForTrainsToDiscard () {

        // Check if CGR must discard trains
        if (cgr.getNumberOfTrains() > cgr.getCurrentTrainLimit()) {
            log.debug("CGR must discard trains");
            if (getStep() != STEP_DISCARD_TRAINS) {
                setStep(STEP_DISCARD_TRAINS);
            }
            trainsToDiscardFrom = cgr.getPortfolioModel().getTrainList();
            forcedTrainDiscard = true;
            return true;
        } else if (!this.cgrHasDiscardedTrains.value()) {
            // Check if CGR still has non-permanent trains
            // these may be discarded voluntarily
            trainsToDiscardFrom = new HashSet<Train>();
            for (Train train : cgr.getPortfolioModel().getTrainList()) {
                if (!train.isPermanent()) {
                    trainsToDiscardFrom.add(train);
                }
            }
            if (!trainsToDiscardFrom.isEmpty()) {
                if (getStep() != STEP_DISCARD_TRAINS) {
                    setStep(STEP_DISCARD_TRAINS);
                }
                forcedTrainDiscard = false;
                return true;
            }
        }
        return false;
    }

    public boolean discardTrain(DiscardTrain action) {

        Train train = action.getDiscardedTrain();
        PublicCompany company = action.getCompany();
        String companyName = company.getId();

        String errMsg = null;

        // Dummy loop to enable a quick jump out.
        while (true) {
            // Checks
            // Must be CGR
            if (company != cgr) {
                errMsg = LocalText.getText("WrongCompany",
                        company.getId(),
                        cgrName);
                break;
            }
            // Must be correct step
            if (getStep() != STEP_DISCARD_TRAINS) {
                errMsg = LocalText.getText("WrongActionNoDiscardTrain");
                break;
            }

            if (train == null && action.isForced()) {
                errMsg = LocalText.getText("NoTrainSpecified");
                break;
            }

            // Does the company own such a train?

            if (train != null && !company.getPortfolioModel().getTrainList().contains(train)) {
                errMsg =
                    LocalText.getText("CompanyDoesNotOwnTrain",
                                company.getId(),
                                train.toText() );
                break;
            }

            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotDiscardTrain",
                    companyName,
                    train.toText(),
                    errMsg ));
            return false;
        }

        /* End of validation, start of execution */
        // new: link always, see below commented
        
        // FIXME:changeStack.linkToPreviousMoveSet();

        if (train != null) {

            //            if (action.isForced()) moveStack.linkToPreviousMoveSet();
            pool.addTrain(train);
            ReportBuffer.add(this, LocalText.getText("CompanyDiscardsTrain",
                    companyName,
                    train.toText() ));

        } else {
            cgrHasDiscardedTrains.set(true);
        }

        return true;
    }

    public List<PublicCompany> getMergingCompanies() {
        return mergingCompanies;
    }

    @Override
    public String toString() {
        return "1856 CGRFormationRound";
    }


}