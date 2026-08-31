#  easiest to look up an account using redinsight and edit this script to find an existing account
# date can be looked up and put in range
curl -X GET -H "Content-Type: application/json"  'http://localhost:8080/accountTransactions?accountNo=Acct1076J&from=2023-07-21&to=2023-11-21'
