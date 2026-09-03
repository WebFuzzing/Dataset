// MODIFIED (WFD-added, not part of the original SUT): the two known-credential accounts the
// drivers seed. Login is by member id, and only a direct write can set isAdmin=true.
CREATE (:Member {member_id: 'wfd_admin', member_name: 'wfd_admin'});
CREATE (:Member {member_id: 'wfd_user', member_name: 'wfd_user'});
