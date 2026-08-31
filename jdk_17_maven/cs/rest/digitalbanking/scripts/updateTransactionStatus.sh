#not working
# generate new transactions to move all of one transaction 
# Status up to the next transaction status. Parameter is target status.
#  Can choose SETTLED or POSTED
curl -X GET -H "Content-Type: application/json"  'http://localhost:8080/statusChangeTransactions?transactionStatus=POSTED'
