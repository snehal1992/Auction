package services;

import java.sql.SQLException;

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
import beans.UserBean;

@Path("/signupservices")
public class SignUpServices {
	
	final static Logger logger = Logger.getLogger(SignUpServices.class);
	
	@Path("/newuser")
	@POST
	@Consumes("application/json")
    @Produces(MediaType.APPLICATION_JSON)
	public Response addNewUser(String data) {
		boolean response = false;
		boolean isAddNewUserSuccessful = true; //should be set to false
		Gson gson = new Gson();
		UserBean user = gson.fromJson(data, UserBean.class);
		
		String username = user.getUserName();
		String password = user.getPassword();
		String firstName = user.getFirstName();
		String lastName = user.getLastName();
		String emailAddress = user.getEmail();
		String phone = user.getPhone();
		String address1 = user.getAddress1();
		String address2 = user.getAddress2();
		String city = user.getCity();
		String state = user.getState();
		String gender = user.getGender();
		String country = user.getCountry();
		String dateofbirth = user.getDateOfBirth();
		
		//System.out.println("this is the email address entered " + emailAddress);
		isAddNewUserSuccessful = DBOperation.userSignUp(username, password, firstName, lastName, emailAddress, address1,address2,city,state,country,dateofbirth, phone, gender);
		//System.out.println("isAddNewUserSuccessful: " + isAddNewUserSuccessful);		
		
		if(isAddNewUserSuccessful){
			response = true;
			System.out.println("value of string is: " + String.valueOf(response));
			EmailService email = new EmailService();
			email.setEmailTo(emailAddress);
			email.setEmailFrom("noreply@auctionware.com");
			email.setHost("smtp.gmail.com");
			email.setProperties();
			email.setSession();
			
			String subject = "Auctionware successful registration";
			String msg = "Congratulations " + firstName + " you've successfully created an account " +
						"\nyour username is " + username +
						"\n\nEnjoy our service!!!";
			
			email.sendEmail(subject, msg); 
			
			logger.info("Signup request: "+username+": SUCCESS");
		}
		else{
			response = false;
			logger.info("Signup request:"+username+": FALSE");
		}
		//System.out.println("value of string is: " + String.valueOf(response));
		return Response.ok().entity(String.valueOf(response)).build();
	}
	
	@Path("/usernameavailability/{username}")
	@GET
	public Response isUsernameAvailable(@PathParam("username") String username) {
		//code here to see if userName exists
		logger.info("Username: "+username+" :availibility check");
		DBOperation dao = new DBOperation();
		boolean isPresent = false;
		try {
			isPresent = dao.isUser(username);
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException c) {
			c.printStackTrace();
		}
		return Response.ok().entity(String.valueOf(isPresent)).build();
	}

}
