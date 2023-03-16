package edu.ics372.gp1.business.facade;

/**
 * 
 * @author Brahma Dathan and Sarnath Ramnath
 * @Copyright (c) 2010
 
 * Redistribution and use with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - the use is for academic purpose only
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   - Neither the name of Brahma Dathan or Sarnath Ramnath
 *     may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * The authors do not make any claims regarding the correctness of the code in this module
 * and are not responsible for any loss or damage resulting from its use.  
 */
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;

import edu.ics372.gp1.business.collections.Catalog;
import edu.ics372.gp1.business.collections.MemberList;
import edu.ics372.gp1.business.collections.OutstandingOrderList;
import edu.ics372.gp1.business.collections.Cart;
import edu.ics372.gp1.business.entities.Product;
import edu.ics372.gp1.business.entities.Member;
import edu.ics372.gp1.business.entities.Order;
import edu.ics372.gp1.business.entities.LineItem;
import edu.ics372.gp1.business.iterators.SafeProductIterator;
import edu.ics372.gp1.business.iterators.SafeMemberIterator;
import edu.ics372.gp1.business.iterators.SafeOrderIterator;

/**
 * The facade class handling all requests from users.
 * 
 * @author Brahma Dathan
 *
 */
public class GroceryStore implements Serializable {
	private static final long serialVersionUID = 1L;
	private Catalog catalog = Catalog.getInstance();
	private MemberList members = MemberList.getInstance();
	private OutstandingOrderList orders = OutstandingOrderList.getInstance();
	private Cart cart = Cart.getInstance();
	private static GroceryStore groceryStore;

	/**
	 * Private for the singleton pattern Creates the catalog and member collection
	 * objects
	 */
	private GroceryStore() {
	}

	/**
	 * Supports the singleton pattern
	 * 
	 * @return the singleton object
	 */
	public static GroceryStore instance() {
		if (groceryStore == null) {
			return groceryStore = new GroceryStore();
		} else {
			return groceryStore;
		}
	}

	/**
	 * Organizes the operations for adding a product
	 * 
	 * @param name  product name
	 * @param author author name
	 * @param reorderLevel     product reorderLevel
	 * @return the Product object created
	 */
	public Result addProduct(Request request) {
		Result result = new Result();
		Product product = new Product(request.getProductName(), request.getProductPrice(), request.getProductReorderLevel());
		if (catalog.insertProduct(product)) {
			result.setResultCode(Result.OPERATION_COMPLETED);
			result.setProductFields(product);
			return result;
		}
		result.setResultCode(Result.OPERATION_FAILED);
		return result;
	}
/**
 * Adds a line item to a customer's purchase
 * @param request
 * @return
 */
	public Result addLineItem(Request request) {
		Result result = new Result();
		Product product = catalog.search(request.getProductId());
		LineItem lineItem = new LineItem(request.getProductName(), request.getProductPrice(), request.getQuantityPurchased());
		if (cart.addLineItem(lineItem)) {
			result = decreaseStock(request.getProductId(), request.getQuantityPurchased());
			result.setLineItemFields(lineItem);
			return result;
		}
		result.setResultCode(Result.OPERATION_FAILED);
		return result;
	}
	/**
	 * Method to decrease stock level when a purchase is made.  If stock is decreased below
	 * reorder level and an existing order for the product does not exist, an order is placed
	 * for the product for twice the reorder level.
	 * 
	 * @param productId  The id of the product
	 * @param quantity  The quantity to remove
	 * @return  Result
	 */
	private Result decreaseStock(String productId, int quantity) {
		Result result = new Result();
		Product product = catalog.search(productId);
		product.updateStock(-quantity);
		if (product.getStock() <= product.getReorderLevel() && orders.search(productId) == null) {
			Order order = new Order(productId, product.getName(), product.getReorderLevel() * 2);
			orders.addOrder(order);
			result.setResultCode(Result.ORDER_PLACED);
		} else {
			result.setResultCode(Result.OPERATION_COMPLETED);
		}
		return result;
	}
	/**
	 * Organizes the operations for adding a member
	 * 
	 * @param name    member name
	 * @param address member address
	 * @param phone   member phone
	 * @param dateJoined member date joined
	 * @param fee	member fee
	 * @return the Member object created
	 */
	public Result addMember(Request request) {
		Result result = new Result();
		Member member = new Member(request.getMemberName(), request.getMemberAddress(),
				request.getMemberPhone(), request.getMemberDateJoined(), request.getMemberFee());
		if (members.addMember(member)) {
			result.setResultCode(Result.OPERATION_COMPLETED);
			result.setMemberFields(member);
			return result;
		}
		result.setResultCode(Result.OPERATION_FAILED);
		return result;
	}
	
	public Result removeMember(Request request) {
		Result result = new Result();
		if (members.removeMember(request.getMemberId())) {
			result.setResultCode(Result.OPERATION_COMPLETED);
			return result;
		} else {
			result.setResultCode(Result.OPERATION_FAILED);
			return result;
		}
	}
	
	/**
	 * Retrieves info for a Product given its name
	 * 
	 * @param name of product
	 * @return the Product object created
	 */
	public Result retrieveProductInfo(String name) {
		Result result = new Result();
		for(Iterator<Product> iterator = catalog.iterator(); iterator.hasNext();) {
			Product product = (Product) iterator.next();
			if(product.getName().equals(name)) {
				result.setResultCode(Result.OPERATION_COMPLETED);
				result.setProductFields(product);
				return result;
			}
		}
		result.setResultCode(Result.OPERATION_FAILED);
		return result;
	}
	
	public Result retrieveProductRequest(Request request) {
		Result result = new Result();
		Product product = catalog.search(request.getProductId());
		if (product != null)
			result.setResultCode(Result.OPERATION_COMPLETED);
		else
			result.setResultCode(Result.PRODUCT_NOT_FOUND);
		return result;
	}
	
	/**
	 * Processes an outstanding order for the GroceryStore.
	 * 
	 * @param id of product ordered
	 * @return updated product fields if order found
	 */
	public Result processShipment(Request request) {
		Result result = new Result();
		Order temp = orders.search(request.getProductId());
		if (temp != null) {
			Product product = catalog.search(temp.getProductId());
			product.updateStock(temp.getQuantityOrdered());
			orders.removeOrder(temp);
			result.setResultCode(Result.OPERATION_COMPLETED);
			result.setProductFields(product);
			return result;
		}
		else {
			result.setResultCode(Result.OPERATION_FAILED);
			return result;
		}
	}
	
	/**
	 * Method for changing the price of a product
	 * 
	 * @param product id, product new price
	 * @return product name, updated price if product found
	 */
	public Result changePrice(Request request) {
		Result result = new Result();
		Product product = catalog.search(request.getProductId());
		if (product != null) {
			product.setPrice(request.getProductPrice());
			result.setProductFields(product);
			result.setResultCode(Result.OPERATION_COMPLETED);
			return result;
		}
		result.setResultCode(Result.OPERATION_FAILED);
		return result;
	}
	
	/**
	 * Method for creating orders of a product.
	 * 
	 * @param product id, name, quantity ordered
	 * @return result code signaling pass/fail
	 */
	public Result createOrder(Request request) {
		Result result = new Result();
		Order order = new Order(request.getProductId(), request.getProductName(), request.getQuantityOrdered());
		if (orders.search(order.getProductId()).equals(order)) {
			result.setResultCode(Result.OPERATION_FAILED);
			return result;
		}
		orders.addOrder(order);
		result.setResultCode(Result.OPERATION_COMPLETED);
		return result;
	}

	/**
	 * Searches for a given member
	 * 
	 * @param memberName  of the member
	 * @return true iff the member is in the member list collection
	 */
	public Result searchMembership(Request request) {
		Result result = new Result();
		Member member = members.search(request.getMemberName());
		if (member == null) {
			result.setResultCode(Result.NO_SUCH_MEMBER);
		} else {
			result.setResultCode(Result.OPERATION_COMPLETED);
			result.setMemberFields(member);
		}
		return result;
	}

	/**
	 * Returns an iterator to the info. in transactions for a specific member on a
	 * certain date
	 * 
	 * @param memberId member id
	 * @param date     date of issue
	 * @return iterator to the collection
	 */
	public Iterator<Result> getTransactions(Request request) {
		Member member = members.search(request.getMemberId());
		if (member == null) {
			return new LinkedList<Result>().iterator();
		}
		return member.getTransactionsOnDate(request.getDate());
	}

	/**
	 * Retrieves a deserialized version of the groceryStore from disk
	 * 
	 * @return a GroceryStore object
	 */
	public static GroceryStore retrieve() {
		try {
			FileInputStream file = new FileInputStream("GroceryStoreData");
			ObjectInputStream input = new ObjectInputStream(file);
			groceryStore = (GroceryStore) input.readObject();
			Member.retrieve(input);
			return groceryStore;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		} catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
			return null;
		}
	}

	/**
	 * Serializes the GroceryStore object
	 * 
	 * @return true iff the data could be saved
	 */
	public static boolean save() {
		try {
			FileOutputStream file = new FileOutputStream("GroceryStoreData");
			ObjectOutputStream output = new ObjectOutputStream(file);
			output.writeObject(groceryStore);
			Member.save(output);
			file.close();
			return true;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return false;
		}
	}

	/**
	 * Returns an iterator to Member info. The Iterator returned is a safe one, in
	 * the sense that only copies of the Member fields are assembled into the
	 * objects returned via next().
	 * 
	 * @return an Iterator to Result - only the Member fields are valid.
	 */
	public Iterator<Result> getMembers() {
		return new SafeMemberIterator(members.iterator());
	}

	/**
	 * Returns an iterator to Product info. The Iterator returned is a safe one, in the
	 * sense that only copies of the Product fields are assembled into the objects
	 * returned via next().
	 * 
	 * @return an Iterator to Result - only the Product fields are valid.
	 */
	public Iterator<Result> getProducts() {
		return new SafeProductIterator(catalog.iterator());
	}
	
	/**
	 * Returns an iterator to OutstandingOrderList info. The Iterator returned is a
	 * safe one, in the sense that only copies of the Member fields are assembled into 
	 * the objects returned via next().
	 * 
	 * @return an Iterator to Result - only the Order fields are valid.
	 */
	public Iterator<Result> getOrders(){
		return new SafeOrderIterator(orders.iterator());
	}

	/**
	 * String form of the groceryStore
	 * 
	 */
	@Override
	public String toString() {
		return catalog + "\n" + members;
	}
}
