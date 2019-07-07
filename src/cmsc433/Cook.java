package cmsc433;


import java.util.LinkedList;
import java.util.List;

/**
 * Cooks are simulation actors that have at least one field, a name.
 * When running, a cook attempts to retrieve outstanding orders placed
 * by Eaters and process them.
 */
public class Cook implements Runnable {
	private final String name;



	/**
	 * You can feel free to modify this constructor.  It must
	 * take at least the name, but may take other parameters
	 * if you would find adding them useful. 
	 *
	 * @param: the name of the cook
	 */
	public Cook(String name) {
		this.name = name;
	}

	public String toString() {
		return name;
	}

	Customer CustomerServed;



	/**
	 * This method executes as follows.  The cook tries to retrieve
	 * orders placed by Customers.  For each order, a List<Food>, the
	 * cook submits each Food item in the List to an appropriate
	 * Machine, by calling makeFood().  Once all machines have
	 * produced the desired Food, the order is complete, and the Customer
	 * is notified.  The cook can then go to process the next order.
	 * If during its execution the cook is interrupted (i.e., some
	 * other thread calls the interrupt() method on it, which could
	 * raise InterruptedException if the cook is blocking), then it
	 * terminates.
	 */
	public void run() {
		Thread[] machines = new Thread[4];
		Machine machine0 = new Machine(Machine.MachineType.fryer,FoodType.wings,Simulation.MachineCap);
		Machine machine1 = new Machine(Machine.MachineType.oven,FoodType.pizza,Simulation.MachineCap);
		Machine machine2 = new Machine(Machine.MachineType.grillPress,FoodType.sub,Simulation.MachineCap);
		Machine machine3 = new Machine(Machine.MachineType.fountain,FoodType.soda,Simulation.MachineCap);
		machines[0] = new Thread(machine0);
		machines[1] = new Thread(machine1);
		machines[2] = new Thread(machine2);
		machines[3] = new Thread(machine3);
		machines[0].start();
		machines[1].start();
		machines[2].start();
		machines[3].start();



			Simulation.logEvent(SimulationEvent.cookStarting(this));
			try {
				while (true) {
					synchronized (Simulation.hungryCustomer) {
						while (Simulation.hungryCustomer.isEmpty()) {
							Simulation.hungryCustomer.wait();
						}
						CustomerServed = Simulation.hungryCustomer.remove(0);


						List<Food> orderCook = new LinkedList<>();
						for (int i=0;i<CustomerServed.order.size();i++){
							orderCook.add(CustomerServed.order.get(i));
						}


						int orderNum = CustomerServed.orderNum;
						Simulation.logEvent((SimulationEvent.cookReceivedOrder(this, orderCook, orderNum)));


						while (!orderCook.isEmpty()) {
							if (orderCook.get(0).name == "wings") {
								Simulation.logEvent(SimulationEvent.cookStartedFood(this, orderCook.get(0), orderNum));
								Simulation.logEvent(SimulationEvent.machineStarting(machine0, orderCook.get(0), Simulation.MachineCap));
								Simulation.logEvent(SimulationEvent.machineCookingFood(machine0, orderCook.get(0)));
								Simulation.FryerUsed+=1;

								Simulation.logEvent(SimulationEvent.machineDoneFood(machine0, orderCook.get(0)));
								Simulation.logEvent(SimulationEvent.cookFinishedFood(this, orderCook.get(0), orderNum));
								orderCook.remove(0);
							} else if (orderCook.get(0).name == "pizza") {
								Simulation.logEvent(SimulationEvent.cookStartedFood(this, orderCook.get(0), orderNum));
								Simulation.logEvent(SimulationEvent.machineStarting(machine0, orderCook.get(0), Simulation.MachineCap));
								Simulation.logEvent(SimulationEvent.machineCookingFood(machine1, orderCook.get(0)));
								Simulation.OvenUsed+=1;

								Simulation.logEvent(SimulationEvent.machineDoneFood(machine0, orderCook.get(0)));
								Simulation.logEvent(SimulationEvent.cookFinishedFood(this, orderCook.get(0), orderNum));
								orderCook.remove(0);
							} else if (orderCook.get(0).name == "sub") {
								Simulation.logEvent(SimulationEvent.cookStartedFood(this, orderCook.get(0), orderNum));
								Simulation.logEvent(SimulationEvent.machineStarting(machine0, orderCook.get(0), Simulation.MachineCap));
								Simulation.logEvent(SimulationEvent.machineCookingFood(machine2, orderCook.get(0)));
								Simulation.PressUsed+=1;
								Simulation.logEvent(SimulationEvent.machineDoneFood(machine0, orderCook.get(0)));
								Simulation.logEvent(SimulationEvent.cookFinishedFood(this, orderCook.get(0), orderNum));
								orderCook.remove(0);
							} else {
								Simulation.logEvent(SimulationEvent.cookStartedFood(this, orderCook.get(0), orderNum));
								Simulation.logEvent(SimulationEvent.machineStarting(machine0, orderCook.get(0), Simulation.MachineCap));
								Simulation.logEvent(SimulationEvent.machineCookingFood(machine3, orderCook.get(0)));
								Simulation.PressUsed+=1;
								Simulation.logEvent(SimulationEvent.machineDoneFood(machine0, orderCook.get(0)));
								Simulation.logEvent(SimulationEvent.cookFinishedFood(this, orderCook.get(0), orderNum));
								orderCook.remove(0);
							}
						}

						Simulation.hungryCustomer.notifyAll();
					}
				}
			} catch(InterruptedException e){
					// This code assumes the provided code in the Simulation class
					// that interrupts each cook thread when all customers are done.
					// You might need to change this if you change how things are
					// done in the Simulation class.
				Simulation.logEvent(SimulationEvent.machineEnding(machine0));
				Simulation.logEvent(SimulationEvent.machineEnding(machine1));
				Simulation.logEvent(SimulationEvent.machineEnding(machine2));
				Simulation.logEvent(SimulationEvent.machineEnding(machine3));

				machines[0].interrupt();
				machines[1].interrupt();
				machines[2].interrupt();
				machines[3].interrupt();



				Simulation.logEvent(SimulationEvent.cookEnding(this));
			}


	}
}
