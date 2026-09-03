// MODIFIED (WFD-added, not part of the original SUT): the two known-credential accounts the
// drivers seed. Login is by member id, and only a direct write can set isAdmin=true.
db = db.getSiblingDB('joinUs');
db.getCollection('members').insertMany([
  {
    "_id": "wfd_admin",
    "member_name": "wfd_admin",
    "bio": "",
    "topics": [],
    "event_count": 0,
    "group_count": 0,
    "upcoming_events": [],
    "password": "$2a$10$wMFYFpqtbELhPxBYXz/Cy.0qhbq/C1H0RRjVZKmSzdAulP8tCCtLS",
    "isAdmin": true,
    "_class": "com.example.joinUs.model.mongodb.User"
  },
  {
    "_id": "wfd_user",
    "member_name": "wfd_user",
    "bio": "",
    "topics": [],
    "event_count": 0,
    "group_count": 0,
    "upcoming_events": [],
    "password": "$2a$10$p5FTsGJHk1g1pCncGxcgFeTJ4hxL8JZWibhVkuE6hVsOB8BeMi8nm",
    "isAdmin": false,
    "_class": "com.example.joinUs.model.mongodb.User"
  }
]);
