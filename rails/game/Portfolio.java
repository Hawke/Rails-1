/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Portfolio.java,v 1.46 2010/03/16 21:22:17 evos Exp $
 *
 * Created on 09-Apr-2005 by Erik Vos
 *
 * Change Log:
 */
package rails.game;

import java.util.*;

import org.apache.log4j.Logger;

import rails.common.LocalText;
import rails.game.model.*;
import rails.game.move.*;
import rails.game.special.LocatedBonus;
import rails.game.special.SpecialPropertyI;
import rails.util.Util;

/**
 * @author Erik
 */
public class Portfolio implements TokenHolder, MoveableHolder {

    /** Owned private companies */
    protected List<PrivateCompanyI> privateCompanies =
        new ArrayList<PrivateCompanyI>();
    protected PrivatesModel privatesOwnedModel =
        new PrivatesModel(privateCompanies);

    /** Owned public company certificates */
    protected List<PublicCertificateI> certificates =
        new ArrayList<PublicCertificateI>();

    /** Owned public company certificates, organised in a HashMap per company */
    protected Map<String, List<PublicCertificateI>> certPerCompany =
        new HashMap<String, List<PublicCertificateI>>();

    /**
     * Owned public company certificates, organised in a HashMap per unique
     * certificate type (company, share percentage, presidency). The key is the
     * certificate type id (see PublicCertificate), the value is the number of
     * certificates of that type.
     */
    protected Map<String, List<PublicCertificateI>> certsPerType =
        new HashMap<String, List<PublicCertificateI>>();

    /** Share model per company */
    protected Map<PublicCompanyI, ShareModel> shareModelPerCompany =
        new HashMap<PublicCompanyI, ShareModel>();

    /** Owned trains */
    protected List<TrainI> trains = new ArrayList<TrainI>();
    protected Map<TrainType, List<TrainI>> trainsPerType =
        new HashMap<TrainType, List<TrainI>>();
    protected Map<TrainCertificateType, List<TrainI>> trainsPerCertType =
        new HashMap<TrainCertificateType, List<TrainI>>();
    protected TrainsModel trainsModel = new TrainsModel(this);

    /** Owned tokens */
    // TODO Currently only used to discard expired Bonus tokens.
    protected List<TokenI> tokens = new ArrayList<TokenI>();

    /**
     * Private-independent special properties. When moved here, a special
     * property no longer depends on the private company being alive. Example:
     * 18AL named train tokens.
     */
    protected List<SpecialPropertyI> specialProperties;

    /** Who owns the portfolio */
    protected CashHolder owner;

    /** Name of portfolio */
    protected String name;
    /** Unique name (including owner class name) */
    protected String uniqueName;

    GameManagerI gameManager;

    /** Specific portfolio names */
    public static final String IPO_NAME = "IPO";
    public static final String POOL_NAME = "Pool";
    public static final String SCRAPHEAP_NAME = "ScrapHeap";
    public static final String UNAVAILABLE_NAME = "Unavailable";

    protected static Logger log =
        Logger.getLogger(Portfolio.class.getPackage().getName());

    public Portfolio(String name, CashHolder holder) {
        this.name = name;
        this.owner = holder;
        this.uniqueName = holder.getClass().getSimpleName() + "_" + name;

        gameManager = GameManager.getInstance();
        gameManager.addPortfolio(this);

        if (owner instanceof PublicCompanyI) {
            trainsModel.setOption(TrainsModel.FULL_LIST);
            privatesOwnedModel.setOption(PrivatesModel.SPACE);
        } else if (owner instanceof Bank) {
            trainsModel.setOption(TrainsModel.ABBR_LIST);
        } else if (owner instanceof Player) {
            privatesOwnedModel.setOption(PrivatesModel.BREAK);
        }
    }

    public void transferAssetsFrom(Portfolio otherPortfolio) {

        // Move trains
        Util.moveObjects(otherPortfolio.getTrainList(), this);

        // Move treasury certificates
        Util.moveObjects(otherPortfolio.getCertificates(), this);
    }

    /** Low-level method, only to be called by the local addObject() method and by initialisation code. */
    public void addPrivate(PrivateCompanyI privateCompany, int position) {

        if (!Util.addToList(privateCompanies, privateCompany, position)) return;

        privateCompany.setHolder(this);
        log.debug("Adding " + privateCompany.getName() + " to portfolio of "
                + name);
        if (privateCompany.getSpecialProperties() != null) {
            log.debug(privateCompany.getName() + " has special properties!");
        } else {
            log.debug(privateCompany.getName() + " has no special properties");
        }
        privatesOwnedModel.update();
        updatePlayerWorth ();
    }

    /** Low-level method, only to be called by the local addObject() method and by initialisation code. */
    public void addCertificate(PublicCertificateI certificate){
        addCertificate (certificate, new int[] {-1,-1,-1});
    }

    /** Low-level method, only to be called by the local addObject() method. */
    private void addCertificate(PublicCertificateI certificate, int[] position) {
        // When undoing a company start, put the President back at the top.
        if (certificate.isPresidentShare()) position = new int[] {0,0,0};

        Util.addToList(certificates, certificate, position[0]);

        String companyName = certificate.getCompany().getName();
        if (!certPerCompany.containsKey(companyName)) {
            certPerCompany.put(companyName, new ArrayList<PublicCertificateI>());
        }

        Util.addToList(certPerCompany.get(companyName), certificate, position[1]);

        String certTypeId = certificate.getTypeId();
        if (!certsPerType.containsKey(certTypeId)) {
            certsPerType.put(certTypeId, new ArrayList<PublicCertificateI>());
        }
        Util.addToList(certsPerType.get(certTypeId), certificate, position[2]);

        certificate.setPortfolio(this);

        getShareModel(certificate.getCompany()).addShare(certificate.getShare());
        updatePlayerWorth ();
    }

    /** Low-level method, only to be called by the local addObject() method. */
    private boolean removePrivate(PrivateCompanyI privateCompany) {
        boolean removed = privateCompanies.remove(privateCompany);
        if (removed) {
            privatesOwnedModel.update();
            updatePlayerWorth ();
        }
        return removed;
    }

    /** Low-level method, only to be called by the local addObject() method. */
    private void removeCertificate(PublicCertificateI certificate) {
        certificates.remove(certificate);

        String companyName = certificate.getCompany().getName();

        List<PublicCertificateI> certs = getCertificatesPerCompany(companyName);
        certs.remove(certificate);

        String certTypeId = certificate.getTypeId();
        if (certsPerType.containsKey(certTypeId)) {
            certsPerType.get(certTypeId).remove(0);
            if (certsPerType.get(certTypeId).isEmpty()) {
                certsPerType.remove(certTypeId);
            }
        }

        getShareModel(certificate.getCompany()).addShare(
                -certificate.getShare());
        updatePlayerWorth ();
    }

    protected void updatePlayerWorth () {
        if (owner instanceof Player) {
            ((Player)owner).updateWorth();
        }
    }

    public ShareModel getShareModel(PublicCompanyI company) {

        if (!shareModelPerCompany.containsKey(company)) {
            shareModelPerCompany.put(company, new ShareModel(this, company));
        }
        return shareModelPerCompany.get(company);
    }

    public List<PrivateCompanyI> getPrivateCompanies() {
        return privateCompanies;
    }

    public List<PublicCertificateI> getCertificates() {
        return certificates;
    }

    /** Get the number of certificates that count against the certificate limit */
    public float getCertificateCount() {
        float number = 0;
        
        for (PrivateCompanyI priv : privateCompanies) {
            if (priv.countsAgainstCertLimit() != false) number++;
        }
        /*
        float number = privateCompanies.size(); // May not hold for all games
        
       */ PublicCompanyI comp;

        for (PublicCertificateI cert : certificates) {
            comp = cert.getCompany();
            if (!comp.hasFloated() || !comp.hasStockPrice()
                    || !cert.getCompany().getCurrentSpace().isNoCertLimit())
                number += cert.getCertificateCount();
        }
        return number;
    }

    public Map<String, List<PublicCertificateI>> getCertsPerCompanyMap() {
        return certPerCompany;
    }

    public List<PublicCertificateI> getCertificatesPerCompany(String compName) {
        if (certPerCompany.containsKey(compName)) {
            return certPerCompany.get(compName);
        } else {
            // TODO: This is bad. If we don't find the company name
            // we should check to see if certPerCompany has been loaded
            // or possibly throw a config error.
            return new ArrayList<PublicCertificateI>();
        }
    }

    /** Return an array of length 5 that contains:<br>
     * - in element [0]: 1 if there is a presidency, 0 if not (but see below);<br>
     * - in element [1]: number of non-president certificates of size 1 (number of share units);<br>
     * - in element [2]: number of non-president certificates of size 2;<br>
     * - etc.
     * @param compName Company name
     * @param includePresident True if the president certificate must also be included in the other counts.
     * @return integer array
     */
    public int[] getCertificateTypeCountsPerCompany (String compName, boolean includePresident) {
        int[] uniqueCertCounts = new int[5];
        for (PublicCertificateI cert : getCertificatesPerCompany(compName)) {
            if (!cert.isPresidentShare() || includePresident) {
                ++uniqueCertCounts[cert.getShares()];
            }
            if (cert.isPresidentShare()) uniqueCertCounts[0] = 1;
        }
        return uniqueCertCounts;
    }

    /**
     * Find a certificate for a given company.
     *
     * @param company The public company for which a certificate is found.
     * @param president Whether we look for a president or non-president
     * certificate. If there is only one certificate, this parameter has no
     * meaning.
     * @return The certificate, or null if not found./
     */
    public PublicCertificateI findCertificate(PublicCompanyI company,
            boolean president) {
        return findCertificate(company, 1, president);
    }

    /** Find a certificate for a given company. */
    public PublicCertificateI findCertificate(PublicCompanyI company,
            int shares, boolean president) {
        String companyName = company.getName();
        if (!certPerCompany.containsKey(companyName)) {
            return null;
        }
        for (PublicCertificateI cert : certPerCompany.get(companyName)) {
            if (cert.getCompany() == company) {
                if (company.getShareUnit() == 100 || president
                        && cert.isPresidentShare() || !president
                        && !cert.isPresidentShare() && cert.getShares() == shares) {
                    return cert;
                }
            }
        }
        return null;
    }

    public Map<String, List<PublicCertificateI>> getCertsPerType() {
        return certsPerType;
    }

    public List<PublicCertificateI> getCertsOfType(String certTypeId) {
        if (certsPerType.containsKey(certTypeId)) {
            return certsPerType.get(certTypeId);
        } else {
            return null;
        }
    }

    public PublicCertificateI getCertOfType(String certTypeId) {
        if (certsPerType.containsKey(certTypeId)) {
            return certsPerType.get(certTypeId).get(0);
        } else {
            return null;
        }
    }

    /**
     * @return
     */
    public CashHolder getOwner() {
        return owner;
    }

    /**
     * @param object
     */
    public void setOwner(CashHolder owner) {
        this.owner = owner;
    }

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    /** Get unique name (prefixed by the owners class type, to avoid Bank, Player and Company
     * namespace clashes).
     * @return
     */
    public String getUniqueName () {
        return uniqueName;
    }

    /**
     * Returns percentage that a portfolio contains of one company.
     *
     * @param company
     * @return
     */
    public int getShare(PublicCompanyI company) {
        int share = 0;
        String name = company.getName();
        if (certPerCompany.containsKey(name)) {
            for (PublicCertificateI cert : certPerCompany.get(name)) {
                share += cert.getShare();
            }
        }
        return share;
    }

    public int ownsCertificates(PublicCompanyI company, int unit,
            boolean president) {
        int certs = 0;
        String name = company.getName();
        if (certPerCompany.containsKey(name)) {
            for (PublicCertificateI cert : certPerCompany.get(name)) {
                if (president) {
                    if (cert.isPresidentShare()) return 1;
                } else if (cert.getShares() == unit) {
                    certs++;
                }
            }
        }
        return certs;
    }

    /**
     * Swap this Portfolio's President certificate for common shares in another
     * Portfolio.
     *
     * @param company The company whose Presidency is handed over.
     * @param other The new President's portfolio.
     * @return The common certificates returned.
     */
    public List<PublicCertificateI> swapPresidentCertificate(
            PublicCompanyI company, Portfolio other) {
        return swapPresidentCertificate (company, other, 0);
    }

    public List<PublicCertificateI> swapPresidentCertificate(
            PublicCompanyI company, Portfolio other, int swapShareSize) {

        List<PublicCertificateI> swapped = new ArrayList<PublicCertificateI>();
        PublicCertificateI swapCert;

        // Find the President's certificate
        PublicCertificateI presCert = this.findCertificate(company, true);
        if (presCert == null) return null;
        int shares = presCert.getShares();

        // If a double cert is requested, try that first
        if (swapShareSize > 1 && other.ownsCertificates(company, swapShareSize, false)*swapShareSize >= shares) {
            swapCert = other.findCertificate(company, swapShareSize, false);
            swapCert.moveTo(this);
            swapped.add(swapCert);
        } else if (other.ownsCertificates(company, 1, false) >= shares) {
            // Check if counterparty has enough single certificates
            for (int i = 0; i < shares; i++) {
                swapCert = other.findCertificate(company, 1, false);
                swapCert.moveTo(this);
                swapped.add(swapCert);

            }
        } else if (other.ownsCertificates(company, shares, false) >= 1) {
            swapCert = other.findCertificate(company, 2, false);
            swapCert.moveTo(this);
            swapped.add(swapCert);
        } else {
            return null;
        }
        presCert.moveTo(other);

        // Make sure the old President is no longer marked as such
        getShareModel(company).setShare();

        return swapped;
    }

    /** Low-level method, only to be called by initialisation code and by the local addObject() method. */
    public void addTrain (TrainI train) {
        addTrain (train, new int[] {-1,-1,-1});
    }

    /** Low-level method, only to be called by the local addObject() method. */
    private void addTrain(TrainI train, int[] position) {

        Util.addToList(trains, train, position[0]);

        TrainType type = train.getType();
        if (!trainsPerType.containsKey(type)) {
            trainsPerType.put(type, new ArrayList<TrainI>());
        }
        Util.addToList(trainsPerType.get(type), train, position[1]);

        TrainCertificateType certType = train.getCertType();
        if (!trainsPerCertType.containsKey(certType)) {
            trainsPerCertType.put(certType, new ArrayList<TrainI>());
        }
        Util.addToList(trainsPerCertType.get(certType), train, position[2]);

        train.setHolder(this);
        trainsModel.update();
    }

    /** Low-level method, only to be called by Move objects */
    private void removeTrain(TrainI train) {
        trains.remove(train);
        // The below ifs are both needed for undo-safety 
        // (the type may have changed before or after)
        if (trainsPerType.get(train.getPreviousType()) != null) {
            trainsPerType.get(train.getPreviousType()).remove(train);
        }
        if (trainsPerType.get(train.getType()) != null) {
            trainsPerType.get(train.getType()).remove(train);
        }
        trainsPerCertType.get(train.getCertType()).remove(train);
        train.setHolder(null);
        trainsModel.update();
    }

    public void buyTrain(TrainI train, int price) {
        CashHolder oldOwner = train.getOwner();
        train.moveTo(this);
        if (price > 0) new CashMove(owner, oldOwner, price);
    }

    public void discardTrain(TrainI train) {
        train.moveTo(GameManager.getInstance().getBank().getPool());
        ReportBuffer.add(LocalText.getText("CompanyDiscardsTrain",
                name, train.getName() ));
    }

    public void updateTrainsModel() {
        trainsModel.update();
    }

    public int getNumberOfTrains() {
        return trains.size();
    }

    public List<TrainI> getTrainList() {
        return trains;
    }

    public TrainI[] getTrainsPerType(TrainType type) {

        List<TrainI> trainsFound = new ArrayList<TrainI>();
        for (TrainI train : trains) {
            if (train.getType() == type) trainsFound.add(train);
        }

        return trainsFound.toArray(new TrainI[0]);
    }

    public ModelObject getTrainsModel() {
        return trainsModel;
    }

    /** Returns one train of any type held */
    public List<TrainI> getUniqueTrains() {

        List<TrainI> trainsFound = new ArrayList<TrainI>();
        Map<TrainType, Object> trainTypesFound =
            new HashMap<TrainType, Object>();
        for (TrainI train : trains) {
            if (!trainTypesFound.containsKey(train.getType())) {
                trainsFound.add(train);
                trainTypesFound.put(train.getType(), null);
            }
        }
        return trainsFound;

    }

    public TrainI getTrainOfType(TrainCertificateType type) {
        for (TrainI train : trains) {
            if (train.getCertType() == type) return train;
        }
        return null;
    }

    /**
     * Make an abbreviated list of trains, like "2(6) 3(5)" etc, to show in the
     * IPO.
     */

    public String makeListOfTrainCertificates() {

        if (trains == null || trains.isEmpty()) return "";

        StringBuilder b = new StringBuilder();
        List<TrainI> trainsOfType;

        for (TrainCertificateType certType : gameManager.getTrainManager().getTrainCertTypes()) {
            trainsOfType = trainsPerCertType.get(certType);
            if (trainsOfType != null && !trainsOfType.isEmpty()) {
                if (b.length() > 0) b.append(" ");
                b.append(certType.getName()).append("(");
                if (certType.hasInfiniteQuantity()) {
                    b.append("+");
                } else {
                    b.append(trainsOfType.size());
                }
                b.append(")");
            }
        }

        return b.toString();
    }

    /**
     * Make a full list of trains, like "2 2 3 3", to show in any field
     * describing train possessions, except the IPO.
     */
    public String makeListOfTrains() {

        if (trains == null || trains.isEmpty()) return "";

        List<TrainI> trainsOfType;
        StringBuilder b = new StringBuilder();

        for (TrainType type : gameManager.getTrainManager().getTrainTypes()) {
            trainsOfType = trainsPerType.get(type);
            if (trainsOfType != null && !trainsOfType.isEmpty()) {
                for (TrainI train : trainsOfType) {
                    if (b.length() > 0) b.append(" ");
                    if (train.isObsolete()) b.append("[");
                    b.append(train.getName());
                    if (train.isObsolete()) b.append("]");
                }
            }
        }

        return b.toString();
    }

    /**
     * Add a special property. Used to make special properties independent of
     * the private company that originally held it.
     * Low-level method, only to be called by Move objects.
     *
     * @param property The special property object to add.
     * @return True if successful.
     */
    private boolean addSpecialProperty(SpecialPropertyI property, int position) {


        if (specialProperties == null) {
            specialProperties = new ArrayList<SpecialPropertyI>(2);
        }

        boolean result = Util.addToList(specialProperties, property, position);
        if (!result) return false;

        property.setHolder(this);

        // Special case for bonuses with predefined locations
        // TODO Does this belong here?
        if (owner instanceof PublicCompanyI && property instanceof LocatedBonus) {
            PublicCompanyI company = (PublicCompanyI)owner;
            LocatedBonus locBonus = (LocatedBonus)property;
            Bonus bonus = new Bonus(company, locBonus.getName(), locBonus.getValue(),
                    locBonus.getLocations());
            company.addBonus(bonus);
            ReportBuffer.add(LocalText.getText("AcquiresBonus",
                    owner.getName(),
                    locBonus.getName(),
                    Bank.format(locBonus.getValue()),
                    locBonus.getLocationNameString()));
        }

        return result;
    }

    /**
     * Remove a special property.
     * Low-level method, only to be called by Move objects.
     * @param property The special property object to remove.
     * @return True if successful.
     */
    private boolean removeSpecialProperty(SpecialPropertyI property) {

        boolean result = false;

        if (specialProperties != null) {
            result = specialProperties.remove(property);

            // Special case for bonuses with predefined locations
            // TODO Does this belong here?
            if (owner instanceof PublicCompanyI && property instanceof LocatedBonus) {
                PublicCompanyI company = (PublicCompanyI)owner;
                LocatedBonus locBonus = (LocatedBonus)property;
                company.removeBonus(locBonus.getName());
            }
        }

        return result;
    }

    /**
     * Add an object.
     * Low-level method, only to be called by Move objects.
     * @param object The object to add.
     * @return True if successful.
     */
    public boolean addObject(Moveable object, int[] position) {
        if (object instanceof PublicCertificateI) {
            if (position == null) position = new int[] {-1, -1, -1};
            addCertificate((PublicCertificateI) object, position);
            return true;
        } else if (object instanceof PrivateCompanyI) {
            addPrivate((PrivateCompanyI) object, position == null ? -1 : position[0]);
            return true;
        } else if (object instanceof TrainI) {
            if (position == null) position = new int[] {-1, -1, -1};
            addTrain((TrainI) object, position);
            return true;
        } else if (object instanceof SpecialPropertyI) {
            return addSpecialProperty((SpecialPropertyI) object, position == null ? -1 : position[0]);
        } else if (object instanceof TokenI) {
            return addToken((TokenI) object, position == null ? -1 : position[0]);
        } else {
            return false;
        }
    }

    /**
     * Remove an object.
     * Low-level method, only to be called by Move objects.
     *
     * @param object The object to remove.
     * @return True if successful.
     */
    public boolean removeObject(Moveable object) {
        if (object instanceof PublicCertificateI) {
            removeCertificate((PublicCertificateI) object);
            return true;
        } else if (object instanceof PrivateCompanyI) {
            removePrivate((PrivateCompanyI) object);
            return true;
        } else if (object instanceof TrainI) {
            removeTrain((TrainI) object);
            return true;
        } else if (object instanceof SpecialPropertyI) {
            return removeSpecialProperty((SpecialPropertyI) object);
        } else if (object instanceof TokenI) {
            return removeToken((TokenI) object);
        } else {
            return false;
        }
    }

    public int[] getListIndex (Moveable object) {
        if (object instanceof PublicCertificateI) {
            PublicCertificateI cert = (PublicCertificateI) object;
            return new int[] {
                    certificates.indexOf(object),
                    certPerCompany.get(cert.getCompany().getName()).indexOf(cert),
                    certsPerType.get(cert.getTypeId()).indexOf(cert)
            };
        } else if (object instanceof PrivateCompanyI) {
            return new int[] {privateCompanies.indexOf(object)};
        } else if (object instanceof TrainI) {
            TrainI train = (TrainI) object;
            return new int[] {
                    trains.indexOf(train),
                    train.getPreviousType() != null ? trainsPerType.get(train.getPreviousType()).indexOf(train) : -1,
                            trainsPerCertType.get(train.getCertType()).indexOf(train)
            };
        } else if (object instanceof SpecialPropertyI) {
            return new int[] {specialProperties.indexOf(object)};
        } else if (object instanceof TokenI) {
            return new int[] {tokens.indexOf(object)};
        } else {
            return Moveable.AT_END;
        }
    }

    /**
     * @return ArrayList of all special properties we have.
     */
    public List<SpecialPropertyI> getPersistentSpecialProperties() {
        return specialProperties;
    }

    public List<SpecialPropertyI> getAllSpecialProperties() {
        List<SpecialPropertyI> sps = new ArrayList<SpecialPropertyI>();
        if (specialProperties != null) sps.addAll(specialProperties);
        for (PrivateCompanyI priv : privateCompanies) {
            if (priv.getSpecialProperties() != null) {
                sps.addAll(priv.getSpecialProperties());
            }
        }
        return sps;
    }

    /**
     * Do we have any special properties?
     *
     * @return Boolean
     */
    public boolean hasSpecialProperties() {
        return specialProperties != null && !specialProperties.isEmpty();
    }

    @SuppressWarnings("unchecked")
    public <T extends SpecialPropertyI> List<T> getSpecialProperties(
            Class<T> clazz, boolean includeExercised) {
        List<T> result = new ArrayList<T>();
        List<SpecialPropertyI> sps;

        if (owner instanceof Player || owner instanceof PublicCompanyI) {

            for (PrivateCompanyI priv : privateCompanies) {

                sps = priv.getSpecialProperties();
                if (sps == null) continue;

                for (SpecialPropertyI sp : sps) {
                    if ((clazz == null || clazz.isAssignableFrom(sp.getClass()))
                            && sp.isExecutionable()
                            && (!sp.isExercised() || includeExercised)
                            && (owner instanceof Company && sp.isUsableIfOwnedByCompany()
                                    || owner instanceof Player && sp.isUsableIfOwnedByPlayer())) {
                        log.debug("Portfolio "+name+" has SP " + sp);
                        result.add((T) sp);
                    }
                }
            }

            // Private-independent special properties
            if (specialProperties != null) {
                for (SpecialPropertyI sp : specialProperties) {
                    if ((clazz == null || clazz.isAssignableFrom(sp.getClass()))
                            && sp.isExecutionable()
                            && (!sp.isExercised() || includeExercised)
                            && (owner instanceof Company && sp.isUsableIfOwnedByCompany()
                                    || owner instanceof Player && sp.isUsableIfOwnedByPlayer())) {
                        log.debug("Portfolio "+name+" has persistent SP " + sp);
                        result.add((T) sp);
                    }
                }
            }

        }

        return result;
    }

    public ModelObject getPrivatesOwnedModel() {
        return privatesOwnedModel;
    }

    /** Low-level method, only to be called by the local addObject() method. */
    public boolean addToken(TokenI token, int position) {

        return Util.addToList(tokens, token, position);
    }

    /** Low-level method, only to be called by the local addObject() method. */
    public boolean removeToken(TokenI token) {
        return tokens.remove(token);
    }

    public List<TokenI> getTokens() {
        return tokens;
    }

    public boolean hasTokens() {
        return tokens != null && !tokens.isEmpty();
    }

    public void rustObsoleteTrains() {

        List<TrainI> trainsToRust = new ArrayList<TrainI>();
        for (TrainI train : trains) {
            if (train.isObsolete()) {
                trainsToRust.add(train);
            }
        }
        // Need to separate selection and execution,
        // otherwise we get a ConcurrentModificationException on trains.
        for (TrainI train : trainsToRust) {
            ReportBuffer.add(LocalText.getText("TrainsObsoleteRusted",
                    train.getName(), name));
            log.debug("Obsolete train " + train.getUniqueId() + " (owned by "
                    + name + ") rusted");
            train.setRusted();
        }
        trainsModel.update();
    }

}
