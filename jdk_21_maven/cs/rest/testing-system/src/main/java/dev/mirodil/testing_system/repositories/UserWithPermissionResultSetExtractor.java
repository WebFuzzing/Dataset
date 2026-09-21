package dev.mirodil.testing_system.repositories;

import dev.mirodil.testing_system.models.User;
import dev.mirodil.testing_system.models.enums.PermissionType;
import dev.mirodil.testing_system.utils.DataUtil;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class UserWithPermissionResultSetExtractor implements ResultSetExtractor<User> {
    @Override
    public User extractData(ResultSet rs) throws SQLException, DataAccessException {
        User user = null;
        Set<PermissionType> permissionNames = new HashSet<>();

        while (rs.next()) {
            // Same user with multiple permissions names
            if (user == null) {
                user = DataUtil.extractUserFromResultSet(rs);
            }

            if (DataUtil.isColumnSet(rs, "permission_names")) {
                Array permissionNamesArray = rs.getArray("permission_names");
                String[] permissionNamesString = (String[]) permissionNamesArray.getArray();
                for (String permissionName : permissionNamesString) {
                    permissionNames.add(PermissionType.valueOf(permissionName));
                }
            }
        }

        if (user != null) {
            user.setPermissionNames(permissionNames);
        }

        return user;
    }
}
