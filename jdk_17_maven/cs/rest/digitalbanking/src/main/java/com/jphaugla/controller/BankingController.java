package com.jphaugla.controller;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jphaugla.domain.*;
import com.jphaugla.service.TopicProducer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.jphaugla.service.BankService;
import redis.clients.jedis.search.SearchResult;


@RequiredArgsConstructor
@RestController
public class BankingController {

	@Autowired
	private BankService bankService = BankService.getInstance();

	private static final Logger logger = LoggerFactory.getLogger(BankingController.class);
	private final TopicProducer topicProducer;
	// customer
	@RequestMapping("/save_customer")
	public String saveCustomer() throws ParseException {
		bankService.saveSampleCustomer();
		return "Done";
	}
//  test send message
	@GetMapping (value = "/send")
	public void send() throws ExecutionException, InterruptedException {
		topicProducer.send("Mensagem de teste enviada ao tópico", "mensagem");
	}
	//  account
	@RequestMapping("/save_account")
	public String saveAccount() throws ParseException {
		bankService.saveSampleAccount();
		return "Done";
	}

	//  transaction
	@RequestMapping("/save_transaction")
	public String saveTransaction(@RequestParam Boolean doKafka) throws ParseException, JsonProcessingException {
	    logger.info("starting save transaction with doKafka:" + doKafka);
		bankService.saveSampleTransaction(doKafka);
		return "Done";
	}

	@GetMapping("/generateData")
	@ResponseBody
	public String generateData (@RequestParam Integer noOfCustomers, @RequestParam Integer noOfTransactions,
								@RequestParam Integer noOfDays, @RequestParam String key_suffix,
								@RequestParam Boolean doKafka)
			throws ParseException, ExecutionException, InterruptedException, IllegalAccessException, JsonProcessingException {
        logger.info("starting generate data with doKafka=" + doKafka);
		bankService.generateData(noOfCustomers, noOfTransactions, noOfDays, key_suffix, doKafka);

		return "Done";
	}

	@GetMapping("/testPipeline")
	@ResponseBody
	public String testPipeline (@RequestParam Integer noOfRecords)
			throws ParseException, ExecutionException, InterruptedException, IllegalAccessException {

		// bankService.testPipeline(noOfRecords);

		return "Done";
	}
	@GetMapping("/customerByPhone")

	public Customer getCustomerByPhone(@RequestParam String phoneString) {
		logger.debug("In get customerByPhone with phone as " + phoneString);
		return bankService.getCustomerByPhone(phoneString);
	}

	@GetMapping("/customerByEmail")

	public Customer getCustomerByEmail(@RequestParam String email) {
		logger.debug("IN get customerByEmail, email is " + email);
		return bankService.getCustomerByEmail(email);
	}


	@GetMapping("/customerByStateCity")

	public SearchResult getCustomerByStateCity(@RequestParam String state, @RequestParam String city) {
		logger.debug("IN get customerByState with state as " + state + " and city=" + city);
		return bankService.getCustomerByStateCity(state, city);
	}
	@GetMapping("/customerByZipcodeLastname")

	public SearchResult getCustomerIdsbyZipcodeLastname(@RequestParam String zipcode, @RequestParam String lastname) {
		logger.debug("IN get getCustomerIdsbyZipcodeLastname with zipcode as " + zipcode + " and lastname=" + lastname);
		return bankService.getCustomerIdsbyZipcodeLastname(zipcode, lastname);
	}
	@GetMapping("/merchantCategoryTransactions")

	public SearchResult getMerchantCategoryTransactions
			(@RequestParam String merchantCategory, @RequestParam String account,
			 @RequestParam(name = "from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
			 @RequestParam(name = "to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate)
			throws ParseException {
		logger.debug("In getMerchantCategoryTransactions merchantCategory=" + merchantCategory + " account=" + account +
				" from=" + startDate + " to=" + endDate);
		return bankService.getMerchantCategoryTransactions(merchantCategory, account, startDate, endDate);
	}
	@GetMapping("/merchantTransactions")

	public SearchResult getMerchantTransactions
			(@RequestParam String merchant, @RequestParam String account,
			 @RequestParam(name = "from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
			 @RequestParam(name = "to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate)
				throws ParseException {
		logger.info("In getMerchantTransactions merchant=" + merchant + " account=" + account +
				" from=" + startDate + " to=" + endDate);
		return bankService.getMerchantTransactions(merchant, account, startDate, endDate);
	}


	@GetMapping ("/transactionStatusReport")

	public List<Map<String, Object>>  transactionStatusReport () {
		return bankService.transactionStatusReport();

	}


	@GetMapping("/returned_transactions")

	public SearchResult getReturnedTransaction () {
		logger.info("in bankcontroller getReturnedTransaction");
		return bankService.getTransactionReturns();
	}
	/*

	@GetMapping("/statusChangeTransactions")

	public AggregateResults<String> generateStatusChangeTransactions(@RequestParam String transactionStatus)
			throws ParseException, IllegalAccessException, ExecutionException, InterruptedException {
		 logger.info("generateStatusChangeTransactions transactionStatus=" + transactionStatus);
		 AggregateResults<String> changeReport = new AggregateResults<>();

		 changeReport.addAll(transactionStatusReport());
		 bankService.transactionStatusChange(transactionStatus);
		 changeReport.addAll(transactionStatusReport());

		 return changeReport;

	}
	*/

	@GetMapping("/creditCardTransactions")

	public SearchResult getCreditCardTransactions
			(@RequestParam String creditCard,
			 @RequestParam(name = "from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
			 @RequestParam(name = "to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate)
			throws ParseException {
		logger.debug("getCreditCardTransactions creditCard=" + creditCard +
				" startDate=" + startDate + " endDate=" + endDate);
		return bankService.getCreditCardTransactions(creditCard, startDate, endDate);
	}

	@GetMapping("/accountTransactions")

	public SearchResult  getAccountTransactions
			(@RequestParam String accountNo,
			 @RequestParam(name = "from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
			 @RequestParam(name = "to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate)
			throws ParseException {
		logger.debug("getCreditCardTransactions creditCard=" + accountNo +
				" startDate=" + startDate + " endDate=" + endDate);
		return bankService.getAccountTransactions(accountNo, startDate, endDate);

	}

	@GetMapping("/addTag")

	public void addTag(@RequestParam String transactionID,
					   @RequestParam String tag, @RequestParam String operation) {
		logger.debug("addTags with transactionID=" + transactionID + " tag is " + tag + " operation is " + operation);
		bankService.addTag(transactionID, tag, operation);
	}

	@GetMapping("/getTags")
	public HashSet <String> getTransactionTagList(@RequestParam String transactionID) {
		logger.debug("getTags with transactionID=" + transactionID);
		return bankService.getTransactionTagList(transactionID);
	}

	@GetMapping("/getTaggedTransactions")

	public SearchResult getTaggedTransactions
			(@RequestParam String accountNo, @RequestParam String tag)
			throws ParseException {
		logger.debug("In getTaggedTransactions accountNo=" + accountNo + " tag=" + tag );
		return bankService.getTaggedTransactions(accountNo, tag);
	}
	@GetMapping("/getTransaction")
	public Transaction getTransaction(@RequestParam String transactionID) {
		Transaction transaction = bankService.getTransaction(transactionID);
		return transaction;
	}
	@GetMapping("/mostRecentTransactions")

	public SearchResult mostRecentTransactions
			(@RequestParam String accountNo)
			throws ParseException {
		logger.debug("In mostRecentTransactions accountNo=" + accountNo  );
		return bankService.mostRecentTransactions(accountNo);
	}

	@GetMapping("/customer")

	public Optional<Customer> getCustomer(@RequestParam String customerId) {
		return bankService.getCustomer(customerId);
	}

	@GetMapping("/deleteCustomer")

	public int deleteCustomer(@RequestParam String customerString) {
		return bankService.deleteCustomer(customerString);
	}

	@GetMapping("/deleteCustomerEmail")

	public int deleteCustomerEmail(@RequestParam String customerId) {
		return bankService.deleteCustomerEmail(customerId);
	}

	@PostMapping(value = "/postCustomer", consumes = "application/json", produces = "application/json")
	public String postCustomer(@RequestBody Customer customer ) throws ParseException {
		bankService.postCustomer(customer);
		return "Done\n";
	}

	@PostMapping(value = "/postDispute", consumes = "application/json", produces = "application/json")
	public String postDispute(@RequestBody Dispute dispute ) throws ParseException {
		bankService.postDispute(dispute);
		return "Done\n";
	}

	@PutMapping(value = "/putDisputeChargeBackReason", consumes = "application/json", produces = "application/json")
	public String putDisputeChargeBackReason(@RequestParam String disputeId, @RequestParam String reasonCode )  {
		bankService.putDisputeChargeBackReason(disputeId, reasonCode);
		return "Done\n";
	}

	@PutMapping(value = "/acceptDispute", consumes = "application/json", produces = "application/json")
	public String acceptDisputeChargeBack(@RequestParam String disputeId)  {
		bankService.acceptDisputeChargeBack(disputeId);
		return "Done\n";
	}

	@PutMapping(value = "/resolvedDispute", consumes = "application/json", produces = "application/json")
	public String resolvedDispute(@RequestParam String disputeId )  {
		bankService.resolvedDispute(disputeId);
		return "Done\n";
	}
	@GetMapping("/getDispute")

	public Dispute getDispute(@RequestParam String disputeId) {
		return bankService.getDispute(disputeId);
	}

}
