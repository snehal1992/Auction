package services;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import com.google.gson.Gson;

import DAO.DBOperation;
import beans.ProductBean;
//import beans.Books;
import beans.PostBean;
import beans.ProductsBean;
import beans.RegisterBidBean;
import beans.RegisterBidsBean;
//import beans.SearchBean;
import beans.UserBean;

@Path("/displaypostbids")
public class DisplayPostBids {
	
	final static Logger logger = Logger.getLogger(DisplayPostBids.class);

	@Path("/display")
	@POST
	@Consumes("application/json")
    @Produces(MediaType.APPLICATION_JSON)
	public Response addNewUser(String data) 
	{
		
		boolean response = false;
		
		Gson gson = new Gson();
		PostBean post = gson.fromJson(data, PostBean.class);
		RegisterBidsBean products = new RegisterBidsBean();
		
		String title = post.getPost();
		//System.out.println("The title is: " + title);
		ArrayList<ArrayList<String>> postResult = DBOperation.searchPostBidsByTitle(title);
		//System.out.println("The search result is: " + postResult);
		post.setpostResult(postResult);
		
		if(postResult != null){
			response = true;
			products.setValidation(response);
			//System.out.println("post result size " + postResult.size());
			
			for(int index=0;index < postResult.size();index++)
			{
				RegisterBidBean product = new RegisterBidBean();
				product.setBidID(postResult.get(index).get(0));
				product.setItemID(postResult.get(index).get(1));
				product.setBidderId(postResult.get(index).get(2));
				product.setPostUserID(postResult.get(index).get(3));
				product.setExpDesc(postResult.get(index).get(4));
				product.setExpQuality(postResult.get(index).get(5));
				product.setExpPrice(postResult.get(index).get(6));
				product.setActDesc(postResult.get(index).get(7));
				product.setActQuality(postResult.get(index).get(8));
				product.setActPrice(postResult.get(index).get(9));
				product.setItemName(postResult.get(index).get(10));
				product.setPostUserEmail(postResult.get(index).get(11));
				product.setBidUserEmail(postResult.get(index).get(12));
				
				
				System.out.println(product.getItemID());
				
				products.addProducts(product);
				//System.out.println(products.getUSERNAME(index));
				//System.out.println("book isbn is: " + book.getIsbn());
				
				//System.out.println(searchResult.get(index).get(0));
		
			}
			
		}
		else {
			response = false;
			post.setValidation(response);
			
		}

		
		Gson searchResultJson = new Gson();
		String responseData = searchResultJson.toJson(products);
		//System.out.println("value of string is: " + responseData);
		return Response.ok().entity(responseData).build();
	}
	
	@Path("/availableusername/{username}")
	@GET
	public String availableUsername(@PathParam("username") String username) {
		//code here to see if userName exists		
		return username + "001";
	}

}
