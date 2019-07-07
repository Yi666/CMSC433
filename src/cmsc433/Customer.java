package cmsc433;

import java.util.List;

/**
 * Customers are simulation actors that have two fields: a name, and a list
 * of Food items that constitute the Customer's order.  When running, an
 * customer attempts to enter the Ratsie's (only successful if the
 * Ratsie's has a free table), place its order, and then leave the
 * Ratsie's when the order is complete.
 */
public class Customer implements Runnable {
	//JUST ONE SET OF IDEAS ON HOW TO SET THINGS UP...
	private final String name;
	public final List<Food> order;
	public final int orderNum;    //the id of the order from this customer

	private static int runningCounter = 0;


	private final int numTable;

	public int tableAssigned;


	/**
	 * You can feel free modify this constructor.  It must take at
	 * least the name and order but may take other parameters if you
	 * would find adding them useful.
	 */
	public Customer(String name, List<Food> order, int numTable) {
		this.name = name;
		this.order = order;
		this.orderNum = ++runningCounter;
		this.numTable = numTable;
	}

	public String toString() {
		return name;
	}

	/** 
	 * This method defines what an Customer does: The customer attempts to
	 * enter the Ratsie's (only successful when the Ratsie's has a
	 * free table), place its order, and then leave the Ratsie's
	 * when the order is complete.
	 */



	public void run() {
		//YOUR CODE GOES HERE...
		Simulation.logEvent(SimulationEvent.customerStarting(this));

		try{
			synchronized (Simulation.freeTables){
				if (!Simulation.freeTables.isEmpty()){
					tableAssigned = Simulation.freeTables.remove(0);
				}
				else{
					Simulation.freeTables.wait();
				}
			}
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		Simulation.logEvent(SimulationEvent.customerEnteredRatsies(this));
		Simulation.logEvent(SimulationEvent.customerPlacedOrder(this,order,orderNum));

		synchronized (Simulation.hungryCustomer){
			Simulation.hungryCustomer.add(this);

			Simulation.hungryCustomer.notifyAll();
		}


		synchronized (this) {
/*			System.out.println("this waiting customer is     " + this.name);
			System.out.println("hungry customers       " + Simulation.hungryCustomer);*/

				//wait();
			}
		 /*catch (InterruptedException e) {
			e.printStackTrace();
		}*/

		Simulation.logEvent(SimulationEvent.customerReceivedOrder(this,order,orderNum));

		try {
			Thread.sleep(5000);
					} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Simulation.logEvent((SimulationEvent.customerLeavingRatsies(this)));

		synchronized (Simulation.freeTables){
			Simulation.freeTables.add(tableAssigned);
			Simulation.freeTables.notifyAll();
		}

	}
}
