// MODIFIED (WFD-added, not part of the original SUT): the same four accounts the drivers seed.
// Airports and flights are left for the search to create (the SUT's own loader is never scheduled).
db = db.getSiblingDB('flightdatabase');

const user = function (id, hash, type) {
  return {
    "_id": id, "EMAIL": id + "@wfd.invalid", "PASSWORD": hash,
    "FIRST_NAME": id, "LAST_NAME": id, "PHONE_NUMBER": "05550000000",
    "USER_TYPE": type, "USER_STATUS": "ACTIVE",
    "_class": "com.example.demo.auth.model.entity.UserEntity"
  };
};

db.getCollection('user-collection').insertMany([
  user("wfd_admin1", "$2a$10$2xOaY0XmjU8VrHnQ6ORmme.4pAD21fsnoM9dcf8Y8NYwmXZztsDsC", "ADMIN"), // Wfd-Admin-Pass1
  user("wfd_admin2", "$2a$10$qKrL3SboXVhfaaevAMiU.eFAj1wD4.X2VVvI.XSmjVlTPf8.95Z9y", "ADMIN"), // Wfd-Admin-Pass2
  user("wfd_user1", "$2a$10$t.OcuL3NBcdCtRCUtX4SiuG6RPgGxs0yQpdvVl11J0Mv249/vu9Za", "USER"),   // Wfd-User-Pass1
  user("wfd_user2", "$2a$10$1WlQAEEMP97IO1o8vLl3ROrLeF9hdKRjd0yxlOve.ZhF.HCW9rES.", "USER")    // Wfd-User-Pass2
]);
