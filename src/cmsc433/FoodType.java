package cmsc433;

/**
 * We create all food objects used by the simulation in one place, here.  
 * This allows us to safely check equality via reference, rather than by 
 * structure/values.
 *
 */
public class FoodType {
	public static final Food wings = new Food("wings",400);
	public static final Food pizza = new Food("pizza",550);
	public static final Food sub = new Food("sub",250);
	public static final Food soda = new Food("soda",25);
}
