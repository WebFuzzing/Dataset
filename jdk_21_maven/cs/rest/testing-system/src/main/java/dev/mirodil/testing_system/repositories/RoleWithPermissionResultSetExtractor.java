package dev.mirodil.testing_system.repositories;

import dev.mirodil.testing_system.models.Permission;
import dev.mirodil.testing_system.models.Role;
import dev.mirodil.testing_system.utils.DataUtil;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class RoleWithPermissionResultSetExtractor implements ResultSetExtractor<Role> {
    @Override
    public Role extractData(ResultSet rs) throws SQLException, DataAccessException {
        Role role = null;
        Set<Permission> permissions = new HashSet<>();

        while (rs.next()) {
            // Same role with multiple permissions
            if (role == null) {
                role = DataUtil.extractRoleFromResultSet(rs);
            }

            rs.getString("permission_name");
            if (!rs.wasNull()) {
                permissions.add(DataUtil.extractPermissionFromResultSet(rs));
            }
        }

        if (role != null) {
            role.setPermissions(permissions);
        }

        return role;
    }
}
