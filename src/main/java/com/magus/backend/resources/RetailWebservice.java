package com.magus.backend.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.magus.backend.client.LoanAPIClient;
import com.magus.backend.client.RetailAPIClient;
import com.magus.backend.model.AccountBalance;
import com.magus.backend.model.AccountSummary;
import com.magus.backend.model.BehaviourScore;
import com.magus.backend.model.BranchAtmLocator;
import com.magus.backend.model.FundsTransfer;
import com.magus.backend.model.LoanAccountSummary;
import com.magus.backend.model.LoanCustomerDetails;
import com.magus.backend.model.LoanEMIDetails;
import com.magus.backend.model.LoanTransactionDetails;
import com.magus.backend.model.Payee;
import com.magus.backend.model.Percentages;
import com.magus.backend.model.TransactionHistory;
import com.magus.backend.model.Transactions;

@Path("/retail")
public class RetailWebservice extends AbstractService {

	RetailAPIClient client = new RetailAPIClient();
	LoanAPIClient loanClient = new LoanAPIClient();

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String getResponse() {
		return "Successful";
	}

	@GET
	@Path("/accountSummary")
	@Produces(MediaType.APPLICATION_JSON)
	public AccountSummary getMessages(@QueryParam("accountNumber") String accNo,
			@QueryParam("customerId") String custId) throws JsonParseException, JsonMappingException, IOException {
		return convertToJSON(client.accountSummary(accNo, custId), AccountSummary.class);
	}

	@GET
	@Path("/balance")
	@Produces(MediaType.APPLICATION_JSON)
	public AccountBalance getAccountBalance(@QueryParam("accountNumber") String accNo)
			throws JsonParseException, JsonMappingException, IOException {
		return convertToJSON(client.balanceEnquiry(accNo), AccountBalance.class);
	}

	@GET
	@Path("/miniStatement")
	@Produces(MediaType.APPLICATION_JSON)
	public TransactionHistory[] getMiniStetement(@QueryParam("accountNumber") String accNo)
			throws JsonParseException, JsonMappingException, IOException {
		return convertToJSON(client.miniStatement(accNo), TransactionHistory[].class);
	}

	// http://localhost:8080/magus/ws/retail/transactionHistoryNDays?accountNumber=5555666677770329&days=90
	@GET
	@Path("/transactionHistoryNDays")
	@Produces(MediaType.APPLICATION_JSON)
	public Transactions getTransactionHistoryNDays(@QueryParam("accountNumber") String accNo,
			@QueryParam("days") int days) throws JsonParseException, JsonMappingException, IOException {
		return convertToJSON(client.transactionHistoryNDays(accNo, days), Transactions.class);
	}

	@GET
	@Path("/transactionHistoryInterval")
	@Produces(MediaType.APPLICATION_JSON)
	public Transactions getTransactionHistoryInterval(@QueryParam("accountNumber") String accNo,
			@QueryParam("fromDate") String fromDate, @QueryParam("toDate") String toDate)
					throws JsonParseException, JsonMappingException, IOException {
		Transactions transactions = convertToJSON(client.transactionHistoryInterval(accNo, fromDate, toDate),
				Transactions.class);
		return transactions;
	}

	@GET
	@Path("/spentOnBetween")
	@Produces(MediaType.APPLICATION_JSON)
	public String getTotalSpentOn(@QueryParam("accountNumber") String accNo, @QueryParam("fromDate") String fromDate,
			@QueryParam("toDate") String toDate, @QueryParam("type") String type)
					throws JsonParseException, JsonMappingException, IOException {
		Transactions transactions = convertToJSON(client.transactionHistoryInterval(accNo, fromDate, toDate),
				Transactions.class);
		Double amountSpent = 0.0;
		for (TransactionHistory th : transactions.getSource()) {
			if ("Dr.".equalsIgnoreCase(th.getCredit_debit_flag()) && type.equalsIgnoreCase(th.getRemark())) {
				amountSpent += Double.valueOf(th.getAmount());
			}
		}
		return String.valueOf(amountSpent);
	}

	@GET
	@Path("/spentOnLast")
	@Produces(MediaType.APPLICATION_JSON)
	public String getTotalSpentOnLast(@QueryParam("accountNumber") String accNo, @QueryParam("days") int days,
			@QueryParam("type") String type) throws JsonParseException, JsonMappingException, IOException {
		Transactions transactions = convertToJSON(client.transactionHistoryNDays(accNo, days), Transactions.class);
		Double amountSpent = 0.0;
		for (TransactionHistory th : transactions.getSource()) {
			if ("Dr.".equalsIgnoreCase(th.getCredit_debit_flag()) && type.equalsIgnoreCase(th.getRemark())) {
				amountSpent += Double.valueOf(th.getAmount());
			}
		}
		return String.valueOf(amountSpent);
	}

	@GET
	@Path("/spentOnTransaction")
	@Produces(MediaType.APPLICATION_JSON)
	public Transactions getSpentOnTransaction(@QueryParam("accountNumber") String accNo, @QueryParam("days") int days, @QueryParam("type") String type)
			throws JsonParseException, JsonMappingException, IOException {
		System.out.println("spentOnTransaction : " + days + " " + type);
		Transactions transactions = convertToJSON(client.transactionHistoryNDays(accNo, days), Transactions.class);
		Transactions transactionType = new Transactions();
		transactionType.setSource(new ArrayList<TransactionHistory>());
		for (TransactionHistory th : transactions.getSource()) {
			if ("Dr.".equalsIgnoreCase(th.getCredit_debit_flag()) && type.equalsIgnoreCase(th.getRemark())) {
				transactionType.getSource().add(th);
			}
		}
		
		for(TransactionHistory t : transactionType.getSource()){
			System.out.println(t);
		}
		return transactionType;
	}
	
	@GET
	@Path("/spentOnPercentages")
	@Produces(MediaType.APPLICATION_JSON)
	public Percentages getSpentOnPercentage(@QueryParam("accountNumber") String accNo, @QueryParam("days") int days)
			throws JsonParseException, JsonMappingException, IOException {
		Transactions transactions = convertToJSON(client.transactionHistoryNDays(accNo, days), Transactions.class);
		Map<String, Double> map = new HashMap<>();
		Double total = 0.0;
		for (TransactionHistory th : transactions.getSource()) {
			if ("Dr.".equalsIgnoreCase(th.getCredit_debit_flag())) {
				Double amount = map.get(th.getRemark());
				String amount2 = th.getAmount();
				if (amount == null) {
					map.put(th.getRemark(), Double.valueOf(amount2));
				} else {
					map.put(th.getRemark(), Double.valueOf(amount2) + map.get(th.getRemark()));
				}

				total += Double.valueOf(amount2);
			}
		}

		Percentages percentages = new Percentages(total, map);
		percentages.setMap(map);

		return percentages;
	}

	@GET
	@Path("/behaviourScore")
	@Produces(MediaType.APPLICATION_JSON)
	public BehaviourScore getBehaviourScore(@QueryParam("accountNumber") String accNo)
			throws JsonParseException, JsonMappingException, IOException {
		return convertToJSON(client.behaviourScore(accNo), BehaviourScore.class);
	}

	@GET
	@Path("/atmLocator")
	@Produces(MediaType.APPLICATION_JSON)
	public BranchAtmLocator getAtmLocator(@QueryParam("lat") String latitude, @QueryParam("long") String longitude)
			throws JsonParseException, JsonMappingException, IOException {
		return convertToJSON(client.branchAtmLocator(latitude, longitude), BranchAtmLocator.class);
	}

	// http://localhost:8080/magus/ws/retail/listPayee?custId=88881328
	@GET
	@Path("/listPayee")
	@Produces(MediaType.APPLICATION_JSON)
	public Payee[] getPayeeList(@QueryParam("custId") String custId)
			throws JsonParseException, JsonMappingException, IOException {
		return convertToJSON(client.listPayee(custId), Payee[].class);
	}

	// http://localhost:8080/magus/ws/retail/fundsTransfer?srcAccount=5555666677770329&destAccount=5555666677779999&amt=25000&payeeDesc=A9999&payeeId=593&type=Electricity
	@GET
	@Path("/fundsTransfer")
	@Produces(MediaType.APPLICATION_JSON)
	public FundsTransfer transferFunds(@QueryParam("srcAccount") String srcAccNo,
			@QueryParam("destAccount") String destAccNo, @QueryParam("amt") double amt,
			@QueryParam("payeeDesc") String payeeDesc, @QueryParam("payeeId") int payeeId,
			@QueryParam("type") String type) throws JsonParseException, JsonMappingException, IOException {
		return convertToJSON(client.fundsTransfer(srcAccNo, destAccNo, amt, payeeDesc, payeeId, type),
				FundsTransfer.class);
	}

	@GET
	@Path("/loanAccountSummary/{input}")
	@Produces(MediaType.APPLICATION_JSON)
	public LoanAccountSummary getLoanAccountSummary(@PathParam("input") String input)
			throws JsonParseException, JsonMappingException, IOException {
		if (input.length() == 8) {
			return convertToJSON(loanClient.loanAccountSummaryByCustId(input), LoanAccountSummary.class);
		} else {
			return convertToJSON(loanClient.loanAccountSummaryByAccountNo(input), LoanAccountSummary.class);
		}
	}

	@GET
	@Path("/customerDetails/{mobileNo}")
	@Produces(MediaType.APPLICATION_JSON)
	public LoanCustomerDetails getLoanCustomerDetails(@PathParam("mobileNo") String mobileNo)
			throws JsonParseException, JsonMappingException, IOException {
		return convertToJSON(loanClient.loanCustomerDetails(mobileNo), LoanCustomerDetails.class);
	}

	@GET
	@Path("/EMIDetails/{loanNo}/{agreeId}")
	@Produces(MediaType.APPLICATION_JSON)
	public LoanEMIDetails getLoanEMIDetails(@PathParam("loanNo") String loanNo, @PathParam("agreeId") String agreeId)
			throws JsonParseException, JsonMappingException, IOException {
		return convertToJSON(loanClient.loanEMIDetails(loanNo, agreeId), LoanEMIDetails.class);
	}

	@GET
	@Path("/LoanTransactionDetails/{loanNo}/{agreeId}")
	@Produces(MediaType.APPLICATION_JSON)
	public LoanTransactionDetails getLoanTransactionDetails(@PathParam("loanNo") String loanNo,
			@PathParam("agreeId") String agreeId) throws JsonParseException, JsonMappingException, IOException {
		return convertToJSON(loanClient.loanTransactionDetails(loanNo, agreeId), LoanTransactionDetails.class);
	}

}
