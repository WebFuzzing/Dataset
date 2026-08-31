#  easiest to look up a credit card using redinsight and edit this script to find an existing credit card
# date can be looked up and put in range
curl -X GET -H "Content-Type: application/json"  'http://localhost:8080/creditCardTransactions?creditCard=9a3b56b5x5227x459fxa67dx36e157c400f7&account=Acct1076J&from=2023-07-27&to=2023-11-30'
