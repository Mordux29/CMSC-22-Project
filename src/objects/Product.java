package objects;

// ABSTRACTION: Product is an abstract class. You cannot write "new Product(...)", you can only create
// its children (Pork, Beef, Chicken, Egg). It describes what every product has and can do, and leaves the
// details that differ (see the abstract methods below) to the children.
// INHERITANCE: the children "extend" this class, so they reuse all of this code instead of copying it.
public abstract class Product {

   	// String array that contains the only available categories such as pork, beef, or egg, and when a manager adds a
   	// product it will used as a checker in the function (isInList) if the category and unit inputed by the manager is found on both the arrays
   	// and after that it assigns the unit attribute from the UNITS array based on the managers input of unit measurement.
    public static final String[] CATEGORIES = { "Pork", "Beef", "Chicken", "Egg" };
    public static final String[] UNITS = { "kg", "pieces" };

    // only this class can touch these attributes and appropriate getters and setters can be seen below.
    // ENCAPSULATION: the fields are private, so other classes cannot change them directly.
    // They have to use the getters and setters below, where we could add checks if needed.
    private String id; //product id
    private String name; // product name
    private String description; //description of the product
    private double pricePerUnit; //the price per kg or pieces
    private String unit;          // unit of the product can be kg or pieces
    private int shelfLifeDays;    // days can last before expiration

    // A constructor runs when an object is created (new Pork(...)). The children call it with super(...).
    public Product(String id, String name, String description,
                   double pricePerUnit, String unit, int shelfLifeDays) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.pricePerUnit = pricePerUnit;
        this.unit = unit;
        this.shelfLifeDays = shelfLifeDays;
        
        //Constructors
        
    }

    // GETTERS AND SETTERS FOR PRODUCTS ATTRIBUTESS
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPricePerUnit() { return pricePerUnit; }
    public void setPricePerUnit(double pricePerUnit) { this.pricePerUnit = pricePerUnit; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public int getShelfLifeDays() { return shelfLifeDays; }
    public void setShelfLifeDays(int shelfLifeDays) { this.shelfLifeDays = shelfLifeDays; }

    // ABSTRACT METHODS THAT SUBCLASSES NEEDS TO IMPLEMENT

    // Returns "Pork", "Beef", "Chicken" or "Egg".
    // ABSTRACTION: only the method name is written here, there is no body. Every child must write its own body.
    // POLYMORPHISM: when the program calls getCategory() on any Product, the child's own version runs.
    public abstract String getCategory();

    // Returns a short text describing the category specific details.
    public abstract String getDetails();

    // Returns one line of text used to save the product in a file.
    public abstract String toFileString();

    

    //Calculates the product price by multiplying the products price per unit by the quantity bought
    public double computePrice(double quantity) {
        return pricePerUnit * quantity;
    }

    
    // This formats the products information in one string to be saved in a text file,
    // it's formatted by writing them by having a seperator "|" for easier file reading
    // category|id|name|description|price|unit|shelfLifeDays
    // used clean function to replace any "|" in name to be put as "/" in file so that when reading
    // it won't be confused as a attribute
    // PERSISTENCE: turns the product into one line of text so it can be saved in products.txt.
    // "protected" means this class and its children can use it, but classes outside the family cannot.
    protected String baseFileString() {
        return getCategory() + "|" + id + "|" + clean(name) + "|" + clean(description)
                + "|" + pricePerUnit + "|" + unit + "|" + shelfLifeDays;
    }

    // Changes any "|" in name and description so that when reading the file it wont be confused
    // as a separator "|"
    protected String clean(String text) {
        return text.replace("|", "/").replace("\n", " ");
    }

    // Checks if a value is inside a list (ignoring upper/lower case).
    // Checks the category and unit inputted is in the arrays CATEGORIES and UNITS which contains
    // only the available unit measurement and categories
    // Used for loop to loop through the list while comparing the inputed string (ignores the case regardless)
    // "static" means the method belongs to the class itself, so it is called as Product.isInList(...) without an object.
    public static boolean isInList(String value, String[] list) {
        for (int i = 0; i < list.length; i++) {
            if (list[i].equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}
