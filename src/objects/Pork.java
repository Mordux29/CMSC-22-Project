package objects;

// INHERITANCE: Pork extends Product, so it automatically has the id, name, price, unit and the getters of Product.
// It only writes what is different: its category name and how it is saved.
public class Pork extends Product {

    public Pork(String id, String name, String description,
                double pricePerUnit, String unit, int shelfLifeDays
                ) {
        // super(...) calls the constructor of the parent class (Product) to fill in the shared fields.
        super(id, name, description, pricePerUnit, unit, shelfLifeDays);
    }

    // @Override means this method replaces the abstract method of the parent (Product). This is POLYMORPHISM.
    @Override
    public String getCategory() {
        return "Pork";
    }
    
    @Override
    public String getDetails(){
        return getDescription();
    }

    @Override
    public String toFileString() {
        //calls the method basefilstring from product then returns with the objects porks attributes
    	return baseFileString();
    }
}

