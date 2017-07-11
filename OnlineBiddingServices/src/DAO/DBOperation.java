package DAO;

import java.io.IOException;
//MD5
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sun.rowset.CachedRowSetImpl;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.BinaryConnectionFactory;
//Spymemcached
import net.spy.memcached.MemcachedClient;


public class DBOperation {
	
	final static Logger logger = Logger.getLogger(DBOperation.class);
	//private static  String mysqlurl = "jdbc:mysql://localhost:3306/user_data/?autoReconnect=true&useSSL=true", sqlcmd1, query1_md5;
	private static  String mysqlurl = "jdbc:mysql://localhost:3306/user_data", sqlcmd1, query1_md5;

	private static final String mysqluser = "root";
	private static final String mysqlpassword = "Tiger";
	
	private static final String sqlcmd0="USE user_data;";	// use the database statement
	
	
	private static String MD5encode(String string1) {
		String md5=null;
 
		if (string1 == null) return null;
 
		try {
			MessageDigest digest1 = MessageDigest.getInstance("MD5");
			byte[] hash = digest1.digest(string1.getBytes());
			StringBuilder sb = new StringBuilder(2*hash.length);
 
			digest1.update(string1.getBytes());
			for(byte b : hash)
			{
				sb.append(String.format("%02x", b&0xff));
			}
			md5=sb.toString();
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		return md5;
	}
	
	
	
	public boolean isUser(String username) throws SQLException, ClassNotFoundException {
		logger.info("DATABASE: Checking if username: "+username+": present");
		
		Class.forName("com.mysql.jdbc.Driver");
		String sqlcmd1 = "SELECT * FROM user_data.users WHERE Username='" + username + "';";
		Connection conn = DriverManager.getConnection(mysqlurl, mysqluser, mysqlpassword);
		Statement stmt1 = conn.createStatement();
		
		stmt1.executeQuery(sqlcmd0);	// use the database
		ResultSet result1 = stmt1.executeQuery(sqlcmd1);
		if(result1.next()) {
			return true;
		}
		return false;
	}
	
	/**
	 * process the user login
	 * @param username
	 * @param password
	 * @return
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	public boolean userLogin(String username, String password, String location) throws IOException, SQLException, InterruptedException, ExecutionException {
		boolean result = false;
		Connection conn = null;
		
		
		//new code
		logger.info("DATABASE: Attempting login for username: "+username);
		Random generator = new Random();
		CachedRowSetImpl crsi = new CachedRowSetImpl();
		MemcachedClient memcache = new MemcachedClient(new BinaryConnectionFactory(), AddrUtil.getAddresses("localhost:11211"));
		Object object1 = null;
		int in_memcached = 0;
		int not_in_memcached=0;
		int id = 0;
 
		long start_time = System.currentTimeMillis();

		String sqlcmd1 = "SELECT * FROM user_data.users WHERE Username='" + username + "';";
		
		query1_md5 = MD5encode(sqlcmd1);
		Future<Object> f = memcache.asyncGet(query1_md5);
		
		try {
			object1 = f.get(5, TimeUnit.SECONDS);
		}
		catch (TimeoutException e) {
			f.cancel(false);
			System.out.println("Memcached timeout...");
		}
		
		try{	
		
		// connect to database
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(mysqlurl, mysqluser, mysqlpassword);
			Statement stmt1 = conn.createStatement();
			
			if (object1==null) {
				logger.info("MEMCACHE: Cache Miss");
				//System.out.print("Query result not in Memcached, ");
				not_in_memcached++;
				stmt1.executeQuery(sqlcmd0);	// use the database
				ResultSet result1 = stmt1.executeQuery(sqlcmd1);	// get the result
				crsi.populate(result1);
				result1.close();
				memcache.set(query1_md5, 60*60*24*30, crsi);
				if (crsi.next()) {// the user exists
					
					if (crsi.getString("Pass").equals(password)) {	// valid login
						result = true;
						// update Last_login
						SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						String Last_login = dateFormat.format(new Date());
						System.out.println(location);
				          String sqlcmd2 = "UPDATE users SET Last_login='" + Last_login + "',Last_login_location='" + location + "' WHERE Username ='" + username + "';";
				          stmt1.executeUpdate(sqlcmd2);
						stmt1.executeUpdate(sqlcmd2);
						System.out.println("The sql statement is " + sqlcmd2);
						System.out.println("This is a valid login.");
					} else {	// failed login: wrong password
						result = false;
						int failedLoginNum = Integer.parseInt(crsi.getString("No_failed_login"));
						failedLoginNum++;
						String sqlcmd3 = "UPDATE users SET No_failed_login='" + failedLoginNum + "' WHERE Username ='" + username + "';";
						stmt1.executeUpdate(sqlcmd3);
						System.out.println("The sql statement is " + sqlcmd3);
						System.out.println("Login failed. Wrong password.");
					}
				} else {	// the user doesn't exist
						result = false;
						System.out.println("The user doesn't exist.");
				}
				
				crsi.close();
			}
			else  {
				logger.info("MEMCACHE: Cache Hit");
				//System.out.print("Query result in Memcached, ");
				in_memcached++;
				crsi = (CachedRowSetImpl)object1;
				crsi.beforeFirst();
				if (crsi.next()) {// the user exists
					
					if (crsi.getString("Pass").equals(password)) {	// valid login
						result = true;
						// update Last_login
						SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						String Last_login = dateFormat.format(new Date());
						String sqlcmd2 = "UPDATE users SET Last_login='" + Last_login + "' WHERE Username ='" + username + "';";
						stmt1.executeUpdate(sqlcmd2);
						System.out.println("The sql statement is " + sqlcmd2);
						System.out.println("This is a valid login.");
					} else {	// failed login: wrong password
						result = false;
						int failedLoginNum = Integer.parseInt(crsi.getString("No_failed_login"));
						failedLoginNum++;
						String sqlcmd3 = "UPDATE users SET No_failed_login='" + failedLoginNum + "' WHERE Username ='" + username + "';";
						stmt1.executeUpdate(sqlcmd3);
						System.out.println("The sql statement is " + sqlcmd3);
						System.out.println("Login failed. Wrong password.");
					}
				} else {	// the user doesn't exist
						result = false;
						System.out.println("The user doesn't exist.");
				}
			}
		} catch (Exception e) {
			System.out.println("Error occurred during communicating with database.");
			e.printStackTrace();
		}
		
		return result;
		
	}

	@SuppressWarnings("unchecked")
	public JSONObject getProfile(String username) {
		
		logger.info("DATABASE: Getting profile for username: "+username);
		
		JSONObject resultJSON = new JSONObject();
		
		Connection conn = null;
		// query user information statement
		String sqlcmd1 = "SELECT * FROM user_data.users WHERE Username='" + username + "';";

		try {
			// connect to database
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(mysqlurl, mysqluser, mysqlpassword);
			Statement stmt1 = conn.createStatement();

			stmt1.executeQuery(sqlcmd0);	// use the database
			ResultSet result1 = stmt1.executeQuery(sqlcmd1);	// get the result

			System.out.println("getting user information...");
			System.out.println("The sql statement is " + sqlcmd1);

			// convert the ResultSet to JSON
			JSONArray rows = new JSONArray();
			int colNum = result1.getMetaData().getColumnCount();
			while (result1.next()) {// for all rows
				JSONObject currRow = new JSONObject();
				for (int i = 1; i <= colNum; i++) { // for 1 row
					currRow.put(result1.getMetaData().getColumnLabel(i), result1.getString(i));
	            }
				rows.add(currRow);
			}
			resultJSON.put("result", rows);// result has all rows

			result1.close();
			conn.close();
		} catch (Exception e) {
			System.out.println("Error occurred during communicating with database.");
			e.printStackTrace();
		}

		return resultJSON;
	}

	/*public static boolean AddBidToCart(String bidID, String itemName, String postUserEmail, 
			 								String bidUserEmail, String itemID, String bidderId, String postUserID, 
			 								String expDesc, String expQuality, String expPrice, String actDesc, 
			 								String actQuality, String actPrice) {
		
		logger.info("DATABASE: Adding bid: "+bidID+" :to cart ");
		
	 	boolean result = false;
		Connection conn = null;

		// query statement
		String sqlcmd1 = "INSERT INTO shoppingcart VALUES (" + null + ",'" + bidID + "','" + itemID + "','" + itemName + "','" + bidderId + "','" + postUserID + "','" + expDesc + "','" + expQuality + "','" + expPrice + "','" + actDesc + "','" + actQuality + "','" + actPrice + "','" + 1 + "','" + bidUserEmail + "','" + postUserEmail + "');";

		System.out.println("sql cmd: "+ sqlcmd1);
		try {
			// connect to database
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(mysqlurl, mysqluser, mysqlpassword);
			Statement stmt1 = conn.createStatement();

			stmt1.executeQuery(sqlcmd0);	// use the database
			stmt1.executeUpdate(sqlcmd1);
			result = true;
			System.out.println("The sql statement is " + sqlcmd1);
			System.out.println("The new bid is inserted successfully.");

			conn.close();
		} catch (Exception e) {
			System.out.println("Error occurred during communicating with database.");
			e.printStackTrace();
		}

		return result;
	}*/
	
	public static boolean prodBid(String itemName, String postUserEmail, String bidUserEmail, 
										String itemID, String bidderId, String postUserID, String expDesc,
										String expQuality,String expPrice, String actDesc, String actQuality,  String actPrice){
		
		logger.info("DATABASE: Creating new bid for "+itemName);
		
		boolean result = false;
		Connection conn = null;

		// query statement
		String sqlcmd1 = "INSERT INTO bid VALUES (" + null + ",'" + itemID + "','" + bidderId + "','" + postUserID + "','" + expDesc + "','" + expQuality + "','" + expPrice + "','" + actDesc + "','" + actQuality + "','" + actPrice + "','" + itemName + "','" + postUserEmail + "','" + bidUserEmail + "');";

		System.out.println("sql cmd: "+ sqlcmd1);
		try {
			// connect to database
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(mysqlurl, mysqluser, mysqlpassword);
			Statement stmt1 = conn.createStatement();

			stmt1.executeQuery(sqlcmd0);	// use the database
			stmt1.executeUpdate(sqlcmd1);
			result = true;
			System.out.println("The sql statement is " + sqlcmd1);
			System.out.println("The new bid is inserted successfully.");

			conn.close();
		} catch (Exception e) {
			System.out.println("Error occurred during communicating with database.");
			e.printStackTrace();
		}

		return result;
	}

	public static boolean updateUser(String username, String password, String firstName, String lastName,
									String email, String address1,String address2,String city, String state,
									String country, String dateofbirth, String phone, String gender) {

		logger.info("DATABASE: Updating user: "+username);

		
		boolean result = false;
		Connection conn = null;

		String sqlcmd1 = "SELECT * FROM user_data.users WHERE Username='" + username + "';";

		try {
			// connect to database
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(mysqlurl, mysqluser, mysqlpassword);
			Statement stmt1 = conn.createStatement();

			stmt1.executeQuery(sqlcmd0);	// use the database
			ResultSet result1 = stmt1.executeQuery(sqlcmd1);	// get the result
			System.out.println("The sql statement is " + sqlcmd1 + password + firstName + lastName + email + address1 + address2 + city + state + country + dateofbirth +phone + gender);
			if (result1.next()) {	// the user already exists
				result = true;
				//int noFailedLogin = 0;
				String sqlcmd2 = "UPDATE users SET U_First_Name='" + firstName + "',Address_Line1='" + address1 + "',Address_Line1='" + address2 + "',U_Last_Name='" + lastName + "',Pass='" + password + "',Email_Id='" + email + "',Birth_Date='" + dateofbirth + "',Gender='" + gender + "',City='" + city + "',State='" + state + "',Country='" + country + "',Ph_No='" + phone + "' WHERE Username ='" + username + "';";
				//String sqlcmd2 = "INSERT INTO users VALUES (" + null + ",'" + username + "','" + firstName + "','" + lastName + "','" + password + "','" + email + "','" + dateofbirth + "','" + gender + "','" + city + "','" + state + "','" + country + "','" + phone + "','" + address1 + "','" + address2 + "'," + null + "," + null + ",'" + noFailedLogin + "');";
				stmt1.executeUpdate(sqlcmd2);
				System.out.println("The sql statement is " + sqlcmd2);
				System.out.println("The user updated information successfully.");

			} else {	// the user doesn't exist
				result = true;
				System.out.println("The username does not exists.");
			}

			result1.close();
			conn.close();
		} catch (Exception e) {
			System.out.println("Error occurred during communicating with database.");
			e.printStackTrace();
		}

		return result;
	}
	
	
	public static boolean updateUserAddress(String username, String newAddress) {
		
		logger.info("DATABASE: Update user address: "+newAddress+": username: "+username);
		
		boolean result = false;
		Connection conn = null;

		// query statement
		String sqlcmd1 = "UPDATE USERS SET Address='" + newAddress + "' WHERE Username='" + username + "';";

		try {
			// connect to database
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(mysqlurl, mysqluser, mysqlpassword);
			Statement stmt1 = conn.createStatement();

			stmt1.executeQuery(sqlcmd0);	// use the database
			stmt1.executeUpdate(sqlcmd1);
			result = true;
			System.out.println("The sql statement is " + sqlcmd1);
			System.out.println("The address is updated successfully.");

			conn.close();
		} catch (Exception e) {
			System.out.println("Error occurred during communicating with database.");
			e.printStackTrace();
		}

		return result;
	}

	public static boolean updateUserPhone(String username, String newPhone) {
		logger.info("DATABASE: Update user phone: "+newPhone+": username: "+username);

		boolean result = false;
		Connection conn = null;

		// query statement
		String sqlcmd1 = "UPDATE USERS SET Phone='" + newPhone + "' WHERE Username='" + username + "';";

		try {
			// connect to database
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(mysqlurl, mysqluser, mysqlpassword);
			Statement stmt1 = conn.createStatement();

			stmt1.executeQuery(sqlcmd0);	// use the database
			stmt1.executeUpdate(sqlcmd1);
			result = true;
			System.out.println("The sql statement is " + sqlcmd1);
			System.out.println("The phone is updated successfully.");

			conn.close();
		} catch (Exception e) {
			System.out.println("Error occurred during communicating with database.");
			e.printStackTrace();
		}

		return result;
	}

	public static boolean prodPost(String username, String itemName, String itemPrice,String itemDesc,
									String itemCategory,String itemQuality,String add1, String add2,String country,
									String state,String city){

		logger.info("DATABASE: Creating new sell post by: "+username);
		
		boolean result = false;
		Connection conn = null;
		
		SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd" );
		Calendar calendar = Calendar.getInstance();
		java.sql.Date ourJavaDateObject = new java.sql.Date(calendar.getTime().getTime());
		calendar.add( Calendar.DATE, +3); 
		String convertedDate=dateFormat.format(calendar.getTime()); 
		
		
		// query statement
		String sqlcmd1 = "INSERT INTO product VALUES (" + null + ",'" + username + "','" + itemName + "','" + itemPrice + "','" + itemDesc + "','" + itemCategory + "','" + itemQuality + "','" + add1 + "','" + add2 + "','" + city + "','" + state + "','" + country + "','" + ourJavaDateObject + "','" + convertedDate +"');";

		try {
			// connect to database
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(mysqlurl, mysqluser, mysqlpassword);
			Statement stmt1 = conn.createStatement();

			stmt1.executeQuery(sqlcmd0);	// use the database
			stmt1.executeUpdate(sqlcmd1);
			result = true;
			System.out.println("The sql statement is " + sqlcmd1);
			System.out.println("The new product is inserted successfully.");

			conn.close();
		} catch (Exception e) {
			System.out.println("Error occurred during communicating with database.");
			e.printStackTrace();
		}

		return result;
	}

	public static boolean userSignUp(String username, String password, String firstName, String lastName, 
										String email, String address1,String address2,String city, String state,
										String country, String dateofbirth, String phone, String gender) {
		
		logger.info("DATABASE: Registering new user: "+username);

		
		boolean result = false;
		Connection conn = null;

		// query statement
		String sqlcmd1 = "SELECT * FROM user_data.users WHERE Username='" + username + "';";

		try {
			// connect to database
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(mysqlurl, mysqluser, mysqlpassword);
			Statement stmt1 = conn.createStatement();

			stmt1.executeQuery(sqlcmd0);	// use the database
			ResultSet result1 = stmt1.executeQuery(sqlcmd1);	// get the result
			System.out.println("The sql statement is " + sqlcmd1 + "','" + firstName + "','" + lastName + "','" + password + "','" + email + "','" + dateofbirth + "','" + gender + "','" + city + "','" + state + "','" + country + "','" + phone + "','" + address1 + "','" + address2);
			if (result1.next()) {	// the user already exists
				result = false;
				System.out.println("Sign up failed. The username already exists.");
			} else {	// the user doesn't exist
				result = true;
				int noFailedLogin = 0;
				String sqlcmd2 = "INSERT INTO users VALUES ('" + username + "','" + firstName + "','" + lastName + "','" + password + "','" + email + "','" + dateofbirth + "','" + gender + "','" + city + "','" + state + "','" + country + "','" + phone + "','" + address1 + "','" + address2 + "',"  + null + ",'" + noFailedLogin + "'," + null + ");";
				stmt1.executeUpdate(sqlcmd2);
				System.out.println("The sql statement is " + sqlcmd2);
				System.out.println("The user signed up successfully.");
			}

			result1.close();
			conn.close();
		} catch (Exception e) {
			System.out.println("Error occurred during communicating with database.");
			e.printStackTrace();
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	public static ArrayList<ArrayList<String>> searchBid(String title) {

		logger.info("DATABASE: Search for products: "+title);

		ArrayList<ArrayList<String>> searchResult = null;

		Connection conn = null;
		
		String sqlcmd2 = "SELECT Post_User_Id, Prod_Id, Prod_Name, P_Price, P_Description, P_Category, P_Quality, P_Address_Line1, P_Address_Line2, P_City, P_State, P_Country FROM user_data.product,users;";
		String sqlcmd1 = "SELECT Email_Id,Post_User_Id, Prod_Id, Prod_Name, P_Price, P_Description, P_Category, P_Quality, P_Address_Line1, P_Address_Line2, P_City, P_State, P_Country FROM user_data.product,user_data.users WHERE Post_User_Id=Username;";
		try {
			// connect to database
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(mysqlurl, mysqluser, mysqlpassword);
			Statement stmt1 = conn.createStatement();

			stmt1.executeQuery(sqlcmd0);	// use the database
			ResultSet result1 = stmt1.executeQuery(sqlcmd1);	// get the result
			System.out.println("The sql statement is " + sqlcmd1);

			searchResult = new ArrayList<ArrayList<String>>();
			while (result1.next()) {
				ArrayList<String> currProduct = new ArrayList<String>();

				currProduct.add(result1.getString("Post_User_Id"));
				currProduct.add(result1.getString("Prod_Id"));
				currProduct.add(result1.getString("Prod_Name"));
				currProduct.add(result1.getString("P_Price"));
				currProduct.add(result1.getString("P_Description"));
				currProduct.add(result1.getString("P_Category"));
				currProduct.add(result1.getString("P_Quality"));
				currProduct.add(result1.getString("P_Address_Line1"));
				currProduct.add(result1.getString("P_Address_Line2"));
				currProduct.add(result1.getString("P_City"));
				currProduct.add(result1.getString("P_State"));
				currProduct.add(result1.getString("P_Country"));
				currProduct.add(result1.getString("Email_Id"));
				searchResult.add(currProduct);

			}
			System.out.println("The search product result is got successfully.");

			result1.close();
			conn.close();
		} catch (Exception e) {
			System.out.println("Error occurred during communicating with database.");
			e.printStackTrace();
		}

		return searchResult;
	}
	
	public static ArrayList<ArrayList<String>> searchPostBidsByTitle(String title) {
		
		logger.info("DATABASE: Search My products: "+title);
		
		ArrayList<ArrayList<String>> searchResult = null;

		Connection conn = null;
		
		String sqlcmd1 = "SELECT * FROM user_data.bid WHERE Prod_Id ='" + title + "';";
		try {
			// connect to database
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(mysqlurl, mysqluser, mysqlpassword);
			Statement stmt1 = conn.createStatement();

			stmt1.executeQuery(sqlcmd0);	// use the database
			ResultSet result1 = stmt1.executeQuery(sqlcmd1);	// get the result
			System.out.println("The sql statement is " + sqlcmd1);

			searchResult = new ArrayList<ArrayList<String>>();
			while (result1.next()) {
				ArrayList<String> currProduct = new ArrayList<String>();
				currProduct.add(result1.getString("Bid_Id"));
				currProduct.add(result1.getString("Prod_Id"));
				currProduct.add(result1.getString("Bidder_Id"));
				currProduct.add(result1.getString("Post_User_Id"));
				currProduct.add(result1.getString("Exp_Description"));
				currProduct.add(result1.getString("Exp_Quality"));
				currProduct.add(result1.getString("Exp_Price"));
				currProduct.add(result1.getString("Act_Description"));
				currProduct.add(result1.getString("Act_Quality"));
				currProduct.add(result1.getString("Act_Price"));
				currProduct.add(result1.getString("Prod_Name"));
				currProduct.add(result1.getString("Post_Email"));
				currProduct.add(result1.getString("Bidder_Email"));
				
				searchResult.add(currProduct);
			}
			System.out.println("The search product result is got successfully.");

			result1.close();
			conn.close();
		} catch (Exception e) {
			System.out.println("Error occurred during communicating with database.");
			e.printStackTrace();
		}

		return searchResult;
	}


	@SuppressWarnings("unchecked")
	public static ArrayList<ArrayList<String>> viewProductByTitle(String title) {
		logger.info("DATABASE: View for products: "+title);
		
		ArrayList<ArrayList<String>> viewResult = null;

		Connection conn = null;
		
		String sqlcmd1 = "SELECT Prod_Id, Prod_Name, P_Price, P_Description, P_Category, P_Quality, P_Address_Line1, P_Address_Line2, P_City, P_State, P_Country,Email_Id  FROM user_data.product,users WHERE Post_User_Id ='" + title + "' AND Post_User_Id= Username;" ;
		try {
			// connect to database
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(mysqlurl, mysqluser, mysqlpassword);
			Statement stmt1 = conn.createStatement();

			stmt1.executeQuery(sqlcmd0);	// use the database
			ResultSet result1 = stmt1.executeQuery(sqlcmd1);	// get the result
			System.out.println("The sql statement is " + sqlcmd1);

			viewResult = new ArrayList<ArrayList<String>>();
			while (result1.next()) {
				ArrayList<String> currProduct = new ArrayList<String>();
				currProduct.add(result1.getString("Prod_Id"));
				currProduct.add(result1.getString("Prod_Name"));
				currProduct.add(result1.getString("P_Price"));
				currProduct.add(result1.getString("P_Description"));
				currProduct.add(result1.getString("P_Category"));
				currProduct.add(result1.getString("P_Quality"));
				currProduct.add(result1.getString("P_Address_Line1"));
				currProduct.add(result1.getString("P_Address_Line2"));
				currProduct.add(result1.getString("P_City"));
				currProduct.add(result1.getString("P_State"));
				currProduct.add(result1.getString("P_Country"));
				currProduct.add(result1.getString("Email_Id"));

				viewResult.add(currProduct);

			}
			System.out.println("The search product result is got successfully.");

			result1.close();
			conn.close();
		} catch (Exception e) {
			System.out.println("Error occurred during communicating with database.");
			e.printStackTrace();
		}

		return viewResult;
	}

	public static ArrayList<ArrayList<String>> searchBiddersByTitle(String title) {
		
		logger.info("DATABASE: Search for bidders of  products: "+title);
		
		//JSONObject resultJSON = new JSONObject();
		ArrayList<ArrayList<String>> searchResult = null;

		Connection conn = null;
		// query book information statement
		String sqlcmd1 = "SELECT * FROM user_data.bid WHERE Bidder_Id LIKE'" + title + "';";

		try {
			// connect to database
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(mysqlurl, mysqluser, mysqlpassword);
			Statement stmt1 = conn.createStatement();

			stmt1.executeQuery(sqlcmd0);	// use the database
			ResultSet result1 = stmt1.executeQuery(sqlcmd1);	// get the result
			System.out.println("The sql statement is " + sqlcmd1);

			searchResult = new ArrayList<ArrayList<String>>();
			while (result1.next()) {
				ArrayList<String> currProduct = new ArrayList<String>();
				currProduct.add(result1.getString("Bid_Id"));
				currProduct.add(result1.getString("Prod_Id"));
				currProduct.add(result1.getString("Bidder_Id"));
				currProduct.add(result1.getString("Post_User_Id"));
				currProduct.add(result1.getString("Exp_Description"));
				currProduct.add(result1.getString("Exp_Quality"));
				currProduct.add(result1.getString("Exp_Price"));
				currProduct.add(result1.getString("Act_Description"));
				currProduct.add(result1.getString("Act_Quality"));
				currProduct.add(result1.getString("Act_Price"));
				searchResult.add(currProduct);
			}
			System.out.println("The search result is got successfully.");

			result1.close();
			conn.close();
		} catch (Exception e) {
			System.out.println("Error occurred during communicating with database.");
			e.printStackTrace();
		}

		return searchResult;
	}
	
	public static ArrayList<ArrayList<String>> deleteItem(String title, String username) {
		ArrayList<ArrayList<String>> searchResult = null;

		Connection conn = null;
		String sqlcmd1 = "DELETE FROM user_data.product WHERE Prod_Name='" + title + "' AND Post_User_Id='" + username + "';";
		String sqlcmd2 = "SELECT * FROM user_data.product WHERE Post_User_Id='" + username + "';";

		try {
			// connect to database
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(mysqlurl, mysqluser, mysqlpassword);
			Statement stmt1 = conn.createStatement();

			stmt1.executeQuery(sqlcmd0);
			stmt1.executeUpdate(sqlcmd1);// use the database
			ResultSet result1 = stmt1.executeQuery(sqlcmd2);	// get the result
			System.out.println("The sql statement 1 is " + sqlcmd1);
			System.out.println("The sql statement 2 is " + sqlcmd2);

			searchResult = new ArrayList<ArrayList<String>>();
			while (result1.next()) {
				ArrayList<String> currProduct = new ArrayList<String>();
				currProduct.add(result1.getString("Post_User_Id"));
				currProduct.add(result1.getString("Prod_Name"));
				currProduct.add(result1.getString("P_Price"));
				currProduct.add(result1.getString("P_Description"));
				currProduct.add(result1.getString("P_Category"));

				//currProduct.add(result1.getString("P_Image"));
				searchResult.add(currProduct);
			}
			System.out.println("The search result is got successfully.");

			result1.close();
			conn.close();
		} catch (Exception e) {
			System.out.println("Error occurred during communicating with database.");
			e.printStackTrace();
		}

		return searchResult;
	}

/*
	public static ArrayList<ArrayList<String>> viewCart(String title) {
			ArrayList<ArrayList<String>> searchResult = null;

			Connection conn = null;
			// query book information statement
			String sqlcmd1 = "SELECT * FROM user_data.shoppingcart WHERE Post_User_Id='" + title + "';";

			try {
				// connect to database
				Class.forName("com.mysql.jdbc.Driver");
				conn = DriverManager.getConnection(mysqlurl, mysqluser, mysqlpassword);
				Statement stmt1 = conn.createStatement();

				stmt1.executeQuery(sqlcmd0);	// use the database
				ResultSet result1 = stmt1.executeQuery(sqlcmd1);	// get the result
				System.out.println("The sql statement is " + sqlcmd1);

				searchResult = new ArrayList<ArrayList<String>>();
				while (result1.next()) {
					ArrayList<String> currProduct = new ArrayList<String>();
					currProduct.add(result1.getString("Prod_Name"));
					currProduct.add(result1.getString("Act_Price"));
					currProduct.add(result1.getString("Bidder_Email"));
					currProduct.add(result1.getString("Post_Email"));
					currProduct.add(result1.getString("Item_Count"));
					currProduct.add(result1.getString("Prod_Id"));
					currProduct.add(result1.getString("Bidder_Id"));
					//currProduct.add(result1.getString("P_Image"));
					searchResult.add(currProduct);
				}
				System.out.println("The search result is got successfully.");

				result1.close();
				conn.close();
			} catch (Exception e) {
				System.out.println("Error occurred during communicating with database.");
				e.printStackTrace();
			}

			return searchResult;
		}*/

	public static ArrayList<ArrayList<String>> searchProductsByTitle(String title) {
		
		logger.info("DATABASE: Search for products1: "+title);
		//JSONObject resultJSON = new JSONObject();
		ArrayList<ArrayList<String>> searchResult = null;

		Connection conn = null;
		String sqlcmd1 = "SELECT * FROM user_data.product,users WHERE Prod_Name LIKE '" + title + "' AND Post_User_Id = Username;";

		try {
			// connect to database
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(mysqlurl, mysqluser, mysqlpassword);
			Statement stmt1 = conn.createStatement();

			stmt1.executeQuery(sqlcmd0);	// use the database
			ResultSet result1 = stmt1.executeQuery(sqlcmd1);	// get the result
			System.out.println("The sql statement is " + sqlcmd1);

			searchResult = new ArrayList<ArrayList<String>>();
			while (result1.next()) {
				ArrayList<String> currProduct = new ArrayList<String>();
				currProduct.add(result1.getString("Post_User_Id"));
				currProduct.add(result1.getString("Prod_Id"));
				currProduct.add(result1.getString("Prod_Name"));
				currProduct.add(result1.getString("P_Price"));
				currProduct.add(result1.getString("P_Description"));
				currProduct.add(result1.getString("P_Category"));
				currProduct.add(result1.getString("P_Quality"));
				currProduct.add(result1.getString("P_Address_Line1"));
				currProduct.add(result1.getString("P_Address_Line2"));
				currProduct.add(result1.getString("P_City"));
				currProduct.add(result1.getString("P_State"));
				currProduct.add(result1.getString("P_Country"));
				currProduct.add(result1.getString("Email_Id"));
				//currProduct.add(result1.getString("P_Image"));
				searchResult.add(currProduct);
			}
			System.out.println("The search result is got successfully.");

			result1.close();
			conn.close();
		} catch (Exception e) {
			System.out.println("Error occurred during communicating with database.");
			e.printStackTrace();
		}

		return searchResult;
	}

	public static ArrayList<ArrayList<String>> updateItemCount(String item, String username, String itemCount){
		ArrayList<ArrayList<String>> searchResult = null;
		//boolean result = false;
		Connection conn = null;

		// query statement
		String sqlcmd1 = "SELECT * FROM user_data.shoppingcart WHERE Post_User_Id='" + username + "';";

		try {
			// connect to database
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(mysqlurl, mysqluser, mysqlpassword);
			Statement stmt1 = conn.createStatement();

			stmt1.executeQuery(sqlcmd0);	// use the database
			ResultSet result1 = stmt1.executeQuery(sqlcmd1);	// get the result
			System.out.println("The sql statement is " + sqlcmd1 + item + username + itemCount);
			if (result1.next()) {	// the user already exists
				//result = true;
				//int noFailedLogin = 0;
				String sqlcmd2 = "UPDATE shoppingcart SET Item_Count ='" +itemCount+ "' where Prod_Name ='" +item+ "' AND Post_User_Id='" + username + "';";
				String sqlcmd3 = "SELECT * FROM user_data.shoppingcart WHERE Post_User_Id='" + username + "';";

				//String sqlcmd2 = "INSERT INTO users VALUES (" + null + ",'" + username + "','" + firstName + "','" + lastName + "','" + password + "','" + email + "','" + dateofbirth + "','" + gender + "','" + city + "','" + state + "','" + country + "','" + phone + "','" + address1 + "','" + address2 + "'," + null + "," + null + ",'" + noFailedLogin + "');";
				stmt1.executeUpdate(sqlcmd2);
				System.out.println("The sql statement is " + sqlcmd2);
				System.out.println("The item count information successfully.");
				System.out.println("For Update ITEM COUNT \n The sql statement is " + sqlcmd1);
				ResultSet result2 = stmt1.executeQuery(sqlcmd3);
				searchResult = new ArrayList<ArrayList<String>>();
				while (result2.next()) {
					ArrayList<String> currProduct = new ArrayList<String>();
					currProduct.add(result2.getString("Prod_Name"));
					currProduct.add(result2.getString("Act_Price"));
					currProduct.add(result2.getString("Bidder_Email"));
					currProduct.add(result2.getString("Post_Email"));
					currProduct.add(result2.getString("Item_Count"));
					currProduct.add(result2.getString("Prod_Id"));
					currProduct.add(result2.getString("Bidder_Id"));

					//currProduct.add(result1.getString("P_Image"));
					searchResult.add(currProduct);
				}
				System.out.println("Item Count DB Updated");
			} else {	// the user doesn't exist
				//result = true;
				System.out.println("The product does not exists.");
			}

			result1.close();
			conn.close();
		} catch (Exception e) {
			System.out.println("Error occurred during communicating with database.");
			e.printStackTrace();
		}

		return searchResult;
	}

	public static ArrayList<ArrayList<String>> emailCart(String itemId, String itemName, String itemPrice, String bidderId,
			String postUserId, String postUserEmail, String bidUserEmail, String itemCount) {

		ArrayList<ArrayList<String>> searchResult = null;
		Connection conn = null;

		// query statement
		String sqlcmd1 = "INSERT INTO user_data.orders VALUES (" + null + ",'" + itemId + "','" + itemName + "','" + bidderId + "','" + postUserId + "','" + itemPrice + "','" + itemCount + "','" + bidUserEmail + "','" + postUserEmail + "');";
		String sqlcmd2 = "DELETE FROM user_data.shoppingcart WHERE Prod_Name='" + itemName + "' AND Post_User_Id='" + postUserId + "';";
		String sqlcmd3 = "SELECT * FROM user_data.shoppingcart WHERE Post_User_Id='" + postUserId + "';";
		try {
			// connect to database
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(mysqlurl, mysqluser, mysqlpassword);
			Statement stmt1 = conn.createStatement();

			stmt1.executeQuery(sqlcmd0);
			stmt1.executeUpdate(sqlcmd1);// use the database
			stmt1.executeUpdate(sqlcmd2);// use the database

			ResultSet result1 = stmt1.executeQuery(sqlcmd3);	// get the result
			System.out.println("The sql statement 1 is " + sqlcmd1);
			System.out.println("The sql statement 2 is " + sqlcmd2);
			System.out.println("The sql statement 3 is " + sqlcmd3);


			searchResult = new ArrayList<ArrayList<String>>();
			while (result1.next()) {
				ArrayList<String> currProduct = new ArrayList<String>();
				currProduct.add(result1.getString("Prod_Name"));
				currProduct.add(result1.getString("Act_Price"));
				currProduct.add(result1.getString("Bidder_Email"));
				currProduct.add(result1.getString("Post_Email"));
				currProduct.add(result1.getString("Item_Count"));
				currProduct.add(result1.getString("Prod_Id"));
				currProduct.add(result1.getString("Bidder_Id"));

				//currProduct.add(result1.getString("P_Image"));
				searchResult.add(currProduct);
			}
			System.out.println("The search result is got successfully.");

			result1.close();
			conn.close();
		} catch (Exception e) {
			System.out.println("Error occurred during communicating with database.");
			e.printStackTrace();
		}

		return searchResult;
	}

}
