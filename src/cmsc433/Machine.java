package cmsc433;

/**
 * A Machine is used to make a particular Food.  Each Machine makes
 * just one kind of Food.  Each machine has a capacity: it can make
 * that many food items in parallel; if the machine is asked to
 * produce a food item beyond its capacity, the requester blocks.
 * Each food item takes at least item.cookTimeS seconds to produce.
 */

public class Machine implements Runnable{
	
	// Types of machines used in Ratsie's.  Recall that enum types are
	// effectively "static" and "final", so each instance of Machine
	// will use the same MachineType.
	
	public enum MachineType { fountain, fryer, grillPress, oven };
	
	// Converts Machine instances into strings based on MachineType.
	
	public String toString() {
		switch (machineType) {
		case fountain: 		return "Fountain";
		case fryer:			return "Fryer";
		case grillPress:	return "Grill Press";
		case oven:			return "Oven";
		default:			return "INVALID MACHINE";
		}
	}
	
	public final MachineType machineType;
	public final Food machineFoodType;

	//YOUR CODE GOES HERE...
	public int capacityIn;




	/**
	 * The constructor takes at least the type of the machine,
	 * the Food item it makes, and its capacity.  You may extend
	 * it with other arguments, if you wish.  Notice that the
	 * constructor currently does nothing with the capacity; you
	 * must add code to make use of this field (and do whatever
	 * initialization etc. you need).
	 */
	public Machine(MachineType machineType, Food food, int capacityIn) {
		this.machineType = machineType;
		this.machineFoodType = food;

		//YOUR CODE GOES HERE...
		this.capacityIn = capacityIn;



	}

	/**
	 * This method is called by a Cook in order to make the Machine's
	 * food item.  You can extend this method however you like, e.g.,
	 * you can have it take extra parameters or return something other
	 * than Object.  It should block if the machine is currently at full
	 * capacity.  If not, the method should return, so the Cook making
	 * the call can proceed.  You will need to implement some means to
	 * notify the calling Cook when the food item is finished.
	 */
/*	public Object makeFood() throws InterruptedException {
		//YOUR CODE GOES HERE...
		return new Object();
	}*/

	//THIS MIGHT BE A USEFUL METHOD TO HAVE AND USE BUT IS JUST ONE IDEA



	public void run() {
			//get the lock
		Object lock = new Object();
		while (Simulation.FryerUsed+Simulation.OvenUsed+Simulation.PressUsed+Simulation.FountainUsed>0) {
			if (machineType == MachineType.fryer) {
				if (Simulation.FryerUsed>0) {
					synchronized (lock) {

						try {
							Thread.sleep(400);
							Simulation.FryerUsed -= 1;
							lock.notifyAll();
						} catch (InterruptedException e) {
						}
					}
				}
			} else if (machineType == MachineType.oven) {
				if (Simulation.OvenUsed>0) {
					synchronized (lock) {
						try {
							Simulation.OvenUsed += 1;
							Thread.sleep(550);
							Simulation.OvenUsed -= 1;
							lock.notifyAll();
						} catch (InterruptedException e) {
						}
					}
				}
			} else if (machineType == MachineType.grillPress) {
				if (Simulation.PressUsed>0) {
					synchronized (lock) {
						try {
							Simulation.PressUsed += 1;
							Thread.sleep(250);
							Simulation.PressUsed -= 1;
							lock.notifyAll();
						} catch (InterruptedException e) {
						}
					}
				}
			} else {
				if (Simulation.FountainUsed>0) {
					synchronized (lock) {
						try {
							Simulation.FountainUsed += 1;
							Thread.sleep(25);
							Simulation.FountainUsed -= 1;
							lock.notifyAll();
						} catch (InterruptedException e) {
						}
					}
				}
			}
		}
	}
}
