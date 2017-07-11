package services;

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

import com.google.gson.Gson;

import DAO.DBOperation;
import beans.BidBean;
import beans.ProductsBean;
import beans.RegisterBidBean;

@Path("/bidservices")
public class BidServices {
	
	final static Logger logger = Logger.getLogger(BidServices.class);

	@Path("/newbid")
	@POST
	@Consumes("application/json")
    @Produces(MediaType.APPLICATION_JSON)
	public Response addNewUser(String data) {
		boolean response = false;
		boolean isAddNewUserSuccessful = true; //should be set to false
		Gson gson = new Gson();
		RegisterBidBean bean = gson.fromJson(data, RegisterBidBean.class);
	
		String itemName= bean.getItemName();
		String postUserEmail= bean.getPostUserEmail();
		String bidUserEmail= bean.getBidUserEmail();
		String itemID =bean.getItemID();
		String bidderId =bean.getBidderId();
		String postUserID = bean.getPostUserID();
		String expDesc =bean.getExpDesc();
		String expQuality = bean.getExpQuality();
		String expPrice = bean.getExpPrice();
		String actDesc = bean.getActDesc();
		String actQuality = bean.getActQuality();
		String actPrice = bean.getActPrice();

		isAddNewUserSuccessful = DBOperation.prodBid(itemName,postUserEmail,bidUserEmail,itemID,bidderId,postUserID,expDesc,expQuality,expPrice, actDesc, actQuality, actPrice);
		System.out.println(isAddNewUserSuccessful);		
		
		if(isAddNewUserSuccessful){
			response = true;
			//System.out.println("value of string is: " + String.valueOf(response));
			logger.info("Register new bid SUCCESS");
		}
		else{
			response = false;
			logger.info("Register new bid FAIL");
		}
		//System.out.println("value of string is: " + String.valueOf(response));
		return Response.ok().entity(String.valueOf(response)).build();
	}
	
	@Path("/availableusername/{username}")
	@GET
	public String availableUsername(@PathParam("username") String username) {
		//code here to see if userName exists		
		return username + "001";
	}

}
