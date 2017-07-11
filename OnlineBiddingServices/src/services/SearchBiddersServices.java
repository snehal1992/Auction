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
import beans.ProductsBean;
import beans.RegisterBidBean;
import beans.SearchBean;
import beans.UserBean;
import beans.RegisterBidsBean;

@Path("/searchbiddersservices")
public class SearchBiddersServices {
	
	final static Logger logger = Logger.getLogger(SearchBiddersServices.class);

	@Path("/search")
	@POST
	@Consumes("application/json")
    @Produces(MediaType.APPLICATION_JSON)
	public Response addNewUser(String data) {
		
		boolean response = false;
		
		Gson gson = new Gson();
		SearchBean search = gson.fromJson(data, SearchBean.class);
		RegisterBidsBean products = new RegisterBidsBean();
		
		String title = search.getSearch();
		
		ArrayList<ArrayList<String>> searchResult = DBOperation.searchBiddersByTitle(title);
		search.setsearchResult(searchResult);
		
		if(searchResult != null){
			response = true;
			search.setsearchResult(searchResult);
			products.setValidationSearch(response);
			
			
			for(int index=0;index < searchResult.size();index++)
			{
				RegisterBidBean product = new RegisterBidBean();
				product.setBidID(searchResult.get(index).get(0));
				product.setItemID(searchResult.get(index).get(1));
				product.setBidderId(searchResult.get(index).get(2));
				product.setPostUserID(searchResult.get(index).get(3));
				product.setExpDesc(searchResult.get(index).get(4));
				product.setExpQuality(searchResult.get(index).get(5));
				product.setExpPrice(searchResult.get(index).get(6));
				product.setActDesc(searchResult.get(index).get(7));
				product.setActQuality(searchResult.get(index).get(8));
				
				product.setActPrice(searchResult.get(index).get(9));
				
				products.addProducts(product);
			}
			logger.info("Search bidder by title: "+title+": SUCCESS");
		}
		else
		{
			response = false;
			search.setValidation(response);
			logger.info("Search bidder by title: "+title+": FAIL");
		}

		Gson searchResultJson = new Gson();
		String responseData = searchResultJson.toJson(products);
		//System.out.println("value of string is: " + responseData);
		return Response.ok().entity(responseData).build();
	}

}
