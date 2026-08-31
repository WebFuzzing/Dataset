#  add a tag to a transaction.  Tags allow user to mark  transactions to be in a buckets such as Travel or Food for budgetary tracking purposes
# date can be looked up and put in range
curl -X GET -H "Content-Type: application/json"  'http://localhost:8080/addTag?transactionID=379369J&tag=Travel&operation=ADD'
