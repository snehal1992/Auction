package services;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.google.gson.Gson;

import DAO.DBOperation;
import beans.UserBean;

@Path("/updateprofile")
public class UpdateProfile {
	
	final static Logger logger = Logger.getLogger(UpdateProfile.class);
	
	@Path("/edituser")
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
		DBOperation dao = new DBOperation();
		isAddNewUserSuccessful = dao.updateUser(username, password, firstName, lastName, emailAddress, address1,address2,city,state,country,dateofbirth, phone, gender);
		System.out.println("isUpdateUserSuccessful: " + isAddNewUserSuccessful);
				
		
		if(isAddNewUserSuccessful){
			response = true;
			System.out.println("value of string is: " + String.valueOf(response));
			EmailService email = new EmailService();
			email.setEmailTo(emailAddress);
			email.setEmailFrom("noreply@auctionware.com");
			email.setHost("smtp.gmail.com");
			email.setProperties();
			email.setSession();

			String subject = "Online Bidding successful User Information Update";
			String msg = "Congratulations " + firstName + " you've successfully updated your account " +
						"\nyour username is " + username +
						"\n\nEnjoy our service!!!";
			
			email.sendEmail(subject, msg);
			logger.info("Update profile: "+username+" :SUCCESS");
		}
		else{
			response = false;
			logger.info("Update profile: "+username+" :FAIL");
		}
		//System.out.println("value of string is: " + String.valueOf(response));
		return Response.ok().entity(String.valueOf(response)).build();
	}

}
