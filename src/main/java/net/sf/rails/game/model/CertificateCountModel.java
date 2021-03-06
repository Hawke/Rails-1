package net.sf.rails.game.model;

public final class CertificateCountModel extends RailsModel {
    
    public final static String ID = "CertificateCountModel"; 

    private CertificateCountModel(PortfolioModel parent) {
        super(parent, ID);
    }
  
    public static CertificateCountModel create(PortfolioModel parent){
        CertificateCountModel model = new CertificateCountModel(parent);
        // lets certificate count model update on portfolio changes
        parent.addModel(model);
        return model;
    }

    /**
     * @return restricted to PortfolioModel
     */
    @Override
    public PortfolioModel getParent() {
        return (PortfolioModel)super.getParent();
    }

    @Override
    public String toText() {
        return ("" + getParent().getCertificateCount()).replaceFirst("\\.0", "").replaceFirst("\\.5", "\u00bd");
    }

}
