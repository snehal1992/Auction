package services;


import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.gson.Gson;

import DAO.DBOperation;
import beans.UserBean;

import javax.ws.rs.PathParam;
 
@Path("/loginservices")
public class LoginServices {
	
	final static Logger logger = Logger.getLogger(LoginServices.class);

	@Path("/checkuservalidity")
	@POST
	@Consumes("application/json")
    @Produces(MediaType.APPLICATION_JSON)
	public Response isValidUser(@Context HttpHeaders headers, String data) throws IOException, SQLException, InterruptedException, ExecutionException {
		
		if(headers.getRequestHeader("secret") == null || !headers.getRequestHeader("secret").get(0).equals(AuthKey.KEY+"")) {
			System.out.println(headers.getRequestHeader("secret"));
			logger.info("Login Bad request without AuthKey");
			return Response.status(302).entity("Unauthorized access").build();
		}
		
		boolean response = false;
		Gson gson = new Gson();
		UserBean user = gson.fromJson(data, UserBean.class);
		
		String username = user.getUserName();
		String password = user.getPassword();
		String location = user.getLastLoginLocation();
		
		DBOperation dao = new DBOperation();
		boolean isValidUser = dao.userLogin(username, password, location);
		JSONObject resultJSON = dao.getProfile(username);
		JSONArray rows = (JSONArray) resultJSON.get("result");
		
		if(isValidUser) {
			response = true;
			
			for (int i = 0; i < rows.size(); i++) {//for each row
				user.setFirstName(((JSONObject)rows.get(i)).get("U_First_Name").toString());
				user.setLastName(((JSONObject)rows.get(i)).get("U_Last_Name").toString());
				user.setAddress1(((JSONObject)rows.get(i)).get("Address_Line1").toString());
				user.setAddress2(((JSONObject)rows.get(i)).get("Address_Line2").toString());
				user.setCity(((JSONObject)rows.get(i)).get("City").toString());
				user.setState(((JSONObject)rows.get(i)).get("State").toString());
				user.setCountry(((JSONObject)rows.get(i)).get("Country").toString());
				user.setGender(((JSONObject)rows.get(i)).get("Gender").toString());
				user.setDateOfBirth(((JSONObject)rows.get(i)).get("Birth_date").toString());
				user.setLastLogin(((JSONObject)rows.get(i)).get("Last_login").toString());
				user.setPhone(((JSONObject)rows.get(i)).get("Ph_No").toString());
				user.setEmail(((JSONObject)rows.get(i)).get("Email_Id").toString());
				user.setLoginAttempts(Integer.parseInt(((JSONObject)rows.get(i)).get("No_failed_login").toString()));
			}
			
			user.setLoggedIn(true);
			user.setValidation(response);	
			
			logger.info("Login request:"+username+": SUCCESS");
			
		}
		else{
			response = false;
			user.setLoginAttempts((user.getLoginAttempts()+1));
			user.setValidation(response);
			logger.info("Login request:"+username+": FAIL");
		}
		
		Gson userJson = new Gson();
		String responseData = userJson.toJson(user);
		return Response.ok().entity(responseData).build();
		
	}
	
	@Path("/availableusername/{username}")
	@GET
	public String availableUsername(@PathParam("username") String username) {
		
		return username + "001";
	}
}