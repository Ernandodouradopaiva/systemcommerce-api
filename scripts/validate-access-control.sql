-- Validação ACL antes/depois (Prompt 167)
-- Executar manualmente no PostgreSQL

SELECT 'users_total' AS metric, COUNT(*)::text AS value FROM users
UNION ALL SELECT 'users_active', COUNT(*)::text FROM users WHERE active AND status = 'ACTIVE'
UNION ALL SELECT 'users_without_uga', COUNT(*)::text FROM users u
  WHERE NOT EXISTS (SELECT 1 FROM user_group_assignments uga WHERE uga.user_id = u.id AND uga.active)
    AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id)
UNION ALL SELECT 'roles', COUNT(*)::text FROM roles
UNION ALL SELECT 'permissions', COUNT(*)::text FROM permissions
UNION ALL SELECT 'gpa', COUNT(*)::text FROM group_permission_assignments WHERE active
UNION ALL SELECT 'uga', COUNT(*)::text FROM user_group_assignments WHERE active
UNION ALL SELECT 'admins', COUNT(DISTINCT uga.user_id)::text
  FROM user_group_assignments uga
  JOIN roles r ON r.id = uga.group_id
  WHERE r.code IN ('ADMIN', 'ADMIN_CONTINGENCY') AND uga.active AND uga.status = 'ACTIVE'
UNION ALL SELECT 'orphan_gpa_perms', COUNT(*)::text
  FROM group_permission_assignments gpa
  LEFT JOIN permissions p ON p.id = gpa.permission_id
  WHERE p.id IS NULL;
