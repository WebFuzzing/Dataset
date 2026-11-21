package reciter.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import reciter.database.dynamodb.model.ApplicationUser;
import reciter.service.ApplicationUserService;


@Tag(name = "ReCiterPubManagerController", description = "Operations on ReCiter publication manager.")
@RestController
public class ReCiterPubManagerController {
	
	private static final Logger log = LoggerFactory.getLogger(ReCiterPubManagerController.class);
	
	@Autowired
	private ApplicationUserService applicationUserService;
	
	@Operation(summary= "Authenticate user for ReCiter publications manager", description  = "This api checks for credentials for access to reciter publication manager app.")
    @Parameters({
    	@Parameter(name = "api-key", description = "api-key for this resource",in =ParameterIn.HEADER, schema =@Schema(type ="string"))
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User authenticated"),
            @ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
            @ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
            @ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    })
    @PostMapping(value = "/reciter/publication/manager/authenticate", produces = "application/json")
    public boolean authenticate(@RequestParam(value = "username") String uid, @RequestParam String password) {
        StopWatch stopWatch = new StopWatch("Authenticate user for ReCiter publications manager");
        stopWatch.start("Authenticate user for ReCiter publications manager");

        log.info("Authenticating user with username: " + uid);
        
        ApplicationUser appUser = new ApplicationUser(uid, "", password);
        if(applicationUserService.authenticateUser(appUser)) {
            stopWatch.stop();
            log.info(stopWatch.getId() + " took " + stopWatch.getTotalTimeSeconds() + "s");
        	return true;
        }
        stopWatch.stop();
        log.info(stopWatch.getId() + " took " + stopWatch.getTotalTimeSeconds() + "s");
        return false;
    }
	
	@Operation(summary  = "Create user for ReCiter publications manager", description  = "This api create user for reciter publication manager app.")
    @Parameters({
    	@Parameter(name = "api-key", description =  "api-key for this resource", in =ParameterIn.HEADER, schema =@Schema(type ="string"))
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User created"),
            @ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
            @ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
            @ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    })
    @PostMapping(value = "/reciter/publication/manager/user/create", produces = "application/json")
    public boolean createUser(@RequestParam(value = "username") String uid, @RequestParam(value = "name") String username, @RequestParam(value = "password") String password) {
        StopWatch stopWatch = new StopWatch("Create user for ReCiter publications manager");
        stopWatch.start("Create user for ReCiter publications manager");
        log.info("Creating user with username: " + uid);
        
        ApplicationUser appUser = new ApplicationUser(uid, username, password);
        if(applicationUserService.createUser(appUser)) {
            stopWatch.stop();
            log.info(stopWatch.getId() + " took " + stopWatch.getTotalTimeSeconds() + "s");
        	return true;
        }
        stopWatch.stop();
        log.info(stopWatch.getId() + " took " + stopWatch.getTotalTimeSeconds() + "s");
        return false;
    }
}