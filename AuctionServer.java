package cmsc433.p1;

/**
 *  @author Yi Liu
 */


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class AuctionServer
{
	/**
	 * Singleton: the following code makes the server a Singleton. You should
	 * not edit the code in the following noted section.
	 * 
	 * For test purposes, we made the constructor protected. 
	 */

	/* Singleton: Begin code that you SHOULD NOT CHANGE! */
	protected AuctionServer()
	{
	}

	private static AuctionServer instance = new AuctionServer();

	public static AuctionServer getInstance()
	{
		return instance;
	}

	/* Singleton: End code that you SHOULD NOT CHANGE! */





	/* Statistic variables and server constants: Begin code you should likely leave alone. */


	/**
	 * Server statistic variables and access methods:
	 */
	private int soldItemsCount = 0;
	private int revenue = 0;
	private int uncollectedRevenue = 0;

	public int soldItemsCount()
	{
		synchronized (instanceLock) {
			return this.soldItemsCount;
		}
	}

	public int revenue()
	{
		synchronized (instanceLock) {
			return this.revenue;
		}
	}
	
	public int uncollectedRevenue () {
		synchronized (instanceLock) {
			return this.uncollectedRevenue;
		}
	}



	/**
	 * Server restriction constants:
	 */
	public static final int maxBidCount = 10; // The maximum number of bids at any given time for a buyer.
	public static final int maxSellerItems = 20; // The maximum number of items that a seller can submit at any given time.
	public static final int serverCapacity = 80; // The maximum number of active items at a given time.


	/* Statistic variables and server constants: End code you should likely leave alone. */



	/**
	 * Some variables we think will be of potential use as you implement the server...
	 */

	// List of items currently up for bidding (will eventually remove things that have expired).
	private List<Item> itemsUpForBidding = new ArrayList<Item>();

	// The last value used as a listing ID.  We'll assume the first thing added gets a listing ID of 0.
	private int lastListingID = -1; 

	// List of item IDs and actual items.  This is a running list with everything ever added to the auction.
	private HashMap<Integer, Item> itemsAndIDs = new HashMap<Integer, Item>();

	// List of itemIDs and the highest bid for each item.  This is a running list with everything ever bid upon.
	private HashMap<Integer, Integer> highestBids = new HashMap<Integer, Integer>();

	// List of itemIDs and the person who made the highest bid for each item.   This is a running list with everything ever bid upon.
	private HashMap<Integer, String> highestBidders = new HashMap<Integer, String>(); 
	
	// List of Bidders who have been permanently banned because they failed to pay the amount they promised for an item. 
	private HashSet<String> blacklist = new HashSet<String>();
	
	// List of sellers and how many items they have currently up for bidding.
	private HashMap<String, Integer> itemsPerSeller = new HashMap<String, Integer>();

	// List of buyers and how many items on which they are currently bidding.
	private HashMap<String, Integer> itemsPerBuyer = new HashMap<String, Integer>();

	// List of itemIDs that have been paid for. This is a running list including everything ever paid for.
	private HashSet<Integer> itemsSold = new HashSet<Integer> ();

	// Object used for instance synchronization if you need to do it at some point 
	// since as a good practice we don't use synchronized (this) if we are doing internal
	// synchronization.
	//
	private Object instanceLock = new Object();
	private Object instanceLock2 = new Object();









	/*
	 *  The code from this point forward can and should be changed to correctly and safely 
	 *  implement the methods as needed to create a working multi-threaded server for the 
	 *  system.  If you need to add Object instances here to use for locking, place a comment
	 *  with them saying what they represent.  Note that if they just represent one structure
	 *  then you should probably be using that structure's intrinsic lock.
	 */


	/**
	 * Attempt to submit an <code>Item</code> to the auction
	 * @param sellerName Name of the <code>Seller</code>
	 * @param itemName Name of the <code>Item</code>
	 * @param lowestBiddingPrice Opening price
	 * @param biddingDurationMs Bidding duration in milliseconds
	 * @return A positive, unique listing ID if the <code>Item</code> listed successfully, otherwise -1
	 */
	public int submitItem(String sellerName, String itemName, int lowestBiddingPrice, int biddingDurationMs)
	{
		// Some reminders:
		//   Make sure there's room in the auction site.
		//   If the seller is a new one, add them to the list of sellers.
		//   If the seller has too many items up for bidding, don't let them add this one.
		//   Don't forget to increment the number of things the seller has currently listed.

		synchronized (instanceLock){

			//System.out.println("now items on bid    " + itemsUpForBidding.size());

			if (serverCapacity <= itemsUpForBidding.size()) {                            //   Make sure there's room in the auction site.
				return -1;
			} else if (!itemsPerSeller.containsKey(sellerName)) {        //   If the seller is a new one, add them to the list of sellers.
				itemsPerSeller.put(sellerName, 1);
				lastListingID += 1;

				Item new_listing_item = new Item(sellerName, itemName, lastListingID, lowestBiddingPrice, biddingDurationMs);

				itemsAndIDs.put(lastListingID, new_listing_item);
				itemsUpForBidding.add(new_listing_item);


				return lastListingID;

			} else {

				if (itemsPerSeller.get(sellerName) >= maxSellerItems) {    //   If the seller has too many items up for bidding, don't let them add this one.
					return -1;
				} else {                                                            //	 Changed the number of things the seller has listed, changed the last listing item ID
					itemsPerSeller.replace(sellerName, itemsPerSeller.get(sellerName) + 1);
					lastListingID += 1;

					Item new_listing_item = new Item(sellerName, itemName, lastListingID, lowestBiddingPrice, biddingDurationMs);

					itemsAndIDs.put(lastListingID, new_listing_item);
					itemsUpForBidding.add(new_listing_item);

					return lastListingID;
				}
			}
		}
	}



	/**
	 * Get all <code>Items</code> active in the auction
	 * @return A copy of the <code>List</code> of <code>Items</code>
	 */
	public List<Item> getItems()
	{

		// Some reminders:
		//    Don't forget that whatever you return is now outside of your control.


		List<Item> list_of_items = new ArrayList<Item>();
		if (itemsUpForBidding.isEmpty()) {
			return list_of_items;
		}
		for (int i=0;i<itemsUpForBidding.size();i++){
			Item ID_to_items = itemsAndIDs.get(i);
			if (itemsUpForBidding.contains(ID_to_items)){
				list_of_items.add(ID_to_items);
			}
		}
		//System.out.println("max of list of items  "   + list_of_items.size());
		return list_of_items;


	}


	/**
	 * Attempt to submit a bid for an <code>Item</code>
	 * @param bidderName Name of the <code>Bidder</code>
	 * @param listingID Unique ID of the <code>Item</code>
	 * @param biddingAmount Total amount to bid
	 * @return True if successfully bid, false otherwise
	 */
	public boolean submitBid(int listingID, String bidderName, int biddingAmount)
	{
		// Some reminders:

		//   See if it can be bid upon.
		//   Get current bidding info.
		//   See if the new bid isn't better than the existing/opening bid floor.
		//   Decrement the former winning bidder's count
		//   Put your bid in place

		//System.out.println("items on server  " + itemsUpForBidding.size());



		synchronized (instanceLock) {

			if (!itemsAndIDs.containsKey(listingID)) {
				return false;
			}
			Item item_for_bid = itemsAndIDs.get(listingID);
			if (!itemsUpForBidding.contains(item_for_bid)) {                //   See if the item exists.
				return false;
			}

			//add a new bidder into the server
			if (!itemsPerBuyer.containsKey(bidderName)) {
				itemsPerBuyer.put(bidderName, 1);
			}


			if (itemsPerBuyer.get(bidderName) >= maxBidCount) {            //   See if this bidder has too many items in their bidding list.
				return false;
			}
			else if (blacklist.contains(bidderName)) {                            //   Make sure the bidder has not been blacklisted
				return false;
			}
			else if (highestBidders.containsKey(listingID)) {
				if (highestBidders.get(listingID) == bidderName) {
					return false;
				} else if (highestBids.get(listingID) >= biddingAmount) {
					return false;
				} else {
					String former_bidder = highestBidders.get(listingID);
					int former_bidder_bids = itemsPerBuyer.get(former_bidder);
					itemsPerBuyer.replace(former_bidder, former_bidder_bids, former_bidder_bids - 1);
					highestBids.replace(listingID, biddingAmount);
					highestBidders.put(listingID, bidderName);
				}
			}
			else {
				if (item_for_bid.lowestBiddingPrice() > biddingAmount){
					return false;
				}
				else{
					highestBids.put(listingID, biddingAmount);
					highestBidders.put(listingID, bidderName);
				}

			}
			return true;
		}
	}

	/**
	 * Check the status of a <code>Bidder</code>'s bid on an <code>Item</code>
	 * @param bidderName Name of <code>Bidder</code>
	 * @param listingID Unique ID of the <code>Item</code>
	 * @return 0 (success) if bid is over and this <code>Bidder</code> has won<br>
	 * 1 (open) if this <code>Item</code> is still up for auction<br>
	 * 2 (failed) If this <code>Bidder</code> did not win or the <code>Item</code> does not exist
	 */
	public int checkBidStatus(int listingID, String bidderName)
	{
		synchronized (instanceLock) {

			final int SUCCESS = 0, OPEN = 1, FAILURE = 2;

			// Some reminders:
			//   If the bidding is closed, clean up for that item.
			//     Remove item from the list of things up for bidding.
			//     Decrease the count of items being bid on by the winning bidder if there was any...
			//     Update the number of open bids for this seller
			//     If the item was sold to someone, update the uncollectedRevenue field appropriately

			Item ID_to_item = itemsAndIDs.get(listingID); //this ID_to_item is the item on check

			if (!itemsUpForBidding.contains(ID_to_item)) {
				return 2;
			} else if (ID_to_item.biddingOpen()) {
				return 1;
			} else if (highestBidders.get(listingID) == bidderName) {

				int seller_previous_item_count = itemsPerSeller.get(ID_to_item.seller());
				itemsPerSeller.replace(ID_to_item.seller(), seller_previous_item_count, seller_previous_item_count - 1);
				itemsUpForBidding.remove(ID_to_item);


				uncollectedRevenue += highestBids.get(listingID);
				return 0;
			} else {
				return 2;
			}
		}
	}

	/**
	 * Check the current bid for an <code>Item</code>
	 * @param listingID Unique ID of the <code>Item</code>
	 * @return The highest bid so far or the opening price if there is no bid on the <code>Item</code>,
	 * or -1 if no <code>Item</code> with the given listingID exists
	 */
	public int itemPrice(int listingID)
	{

		// Remember: once an item has been purchased, this method should continue to return the
		// highest bid, even if the buyer paid more than necessary for the item or if the buyer
		// is subsequently blacklisted
		synchronized (instanceLock) {
			if (!itemsAndIDs.containsKey(listingID)) {
				return -1;
			}
			Item item_for_checking_price = itemsAndIDs.get(listingID);

			if (highestBids.containsKey(listingID)) {
				return highestBids.get(listingID);
			} else {
				return item_for_checking_price.lowestBiddingPrice();
			}
		}
	}

	/**
	 * Check whether an <code>Item</code> has a bid on it
	 * @param listingID Unique ID of the <code>Item</code>
	 * @return True if there is no bid or the <code>Item</code> does not exist, false otherwise
	 */
	public boolean itemUnbid(int listingID)
	{
		if (!itemsAndIDs.containsKey(listingID)) {
			return true;
		}
		return !highestBids.containsKey(listingID);
	}

	/**
	 * Pay for an <code>Item</code> that has already been won.
	 * @param bidderName Name of <code>Bidder</code>
	 * @param listingID Unique ID of the <code>Item</code>
	 * @param amount The amount the <code>Bidder</code> is paying for the item 
	 * @return The name of the <code>Item</code> won, or null if the <code>Item</code> was not won by the <code>Bidder</code> or if the <code>Item</code> did not exist
	 * @throws InsufficientFundsException If the <code>Bidder</code> did not pay at least the final selling price for the <code>Item</code>
	 */
	public String payForItem (int listingID, String bidderName, int amount) throws InsufficientFundsException {

		// Remember:
		// - Check to make sure the buyer is the correct individual and can afford the item
		// - If the purchase is valid, update soldItemsCount, revenue, and uncollectedRevenue
		// - If the amount tendered is insufficient, cancel all active bids held by the buyer, 
		//   add the buyer to the blacklist, and throw an InsufficientFundsException

		synchronized (instanceLock2) {

			if (highestBids.get(listingID) > amount) {
				for (int i = 0; i <= lastListingID; i++) {
					Item ID_to_item = itemsAndIDs.get(i);
					if (itemsUpForBidding.contains(ID_to_item) && highestBidders.get(i) == bidderName) {
						highestBidders.remove(i);
						highestBids.remove(i);
					}
				}

				Item ID_to_item = itemsAndIDs.get(listingID);
				Integer seller_previous_item_count = itemsPerSeller.get(ID_to_item.seller());
				itemsPerSeller.replace(ID_to_item.seller(), seller_previous_item_count, seller_previous_item_count + 1);
				itemsUpForBidding.add(ID_to_item);

				blacklist.add(bidderName);


				throw new InsufficientFundsException();
				//return null;
			} else {
				Item sold_ID_to_item = itemsAndIDs.get(listingID);

				itemsPerBuyer.replace(bidderName,itemsPerBuyer.get(bidderName)-1);

				soldItemsCount += 1;
				revenue += amount;
				uncollectedRevenue = uncollectedRevenue - highestBids.get(listingID);
				itemsSold.add(listingID);


				System.out.println("revenue is " + revenue);
				System.out.println("unrevenue is  " + uncollectedRevenue);
				System.out.println("sold item  " + soldItemsCount);



				return sold_ID_to_item.name();
			}
		}
	}

}
