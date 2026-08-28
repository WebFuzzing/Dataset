# get by phone and full name.  Notice the ascii string for the space
curl -X GET -H "Content-Type: application/json"  'http://localhost:8080/customerByPhone?phoneString=1000088Jw&full_name=Igor%20Golov%2000088'
