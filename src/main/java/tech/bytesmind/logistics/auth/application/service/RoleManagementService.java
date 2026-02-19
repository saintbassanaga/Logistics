package tech.bytesmind.logistics.auth.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.bytesmind.logistics.auth.api.dto.CreateRoleRequest;
import tech.bytesmind.logistics.auth.api.dto.UpdateRoleRequest;
import tech.bytesmind.logistics.auth.application.mapper.RoleMapper;
import tech.bytesmind.logistics.auth.domain.model.Role;
import tech.bytesmind.logistics.auth.domain.model.RoleScope;
import tech.bytesmind.logistics.auth.infrastructure.repository.RoleRepository;
import tech.bytesmind.logistics.auth.infrastructure.repository.UserRepository;
import tech.bytesmind.logistics.shared.exceptions.BusinessException;

import java.util.List;
import java.util.UUID;

/**
 * Application service for role management.
 *
 * <p>All operations are local — roles are stored in the {@code role} table.
 * Role codes are uppercase with underscores (enforced by a DB CHECK constraint).
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RoleManagementService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final RoleMapper roleMapper;

    // =========================================================================
    // Queries
    // =========================================================================

    @Transactional(readOnly = true)
    public Role findById(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Role not found: " + id));
    }

    @Transactional(readOnly = true)
    public Role findByCode(String code) {
        return roleRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException("Role not found: " + code));
    }

    @Transactional(readOnly = true)
    public List<Role> listAll() {
        return roleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Role> listActive() {
        return roleRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<Role> listByScope(RoleScope scope) {
        return roleRepository.findByScope(scope);
    }

    @Transactional(readOnly = true)
    public List<Role> listActiveByScopeAndActive(RoleScope scope) {
        return roleRepository.findByScopeAndActiveTrue(scope);
    }

    /** Returns user IDs that have this role assigned. */
    @Transactional(readOnly = true)
    public List<UUID> listUserIdsWithRole(UUID roleId) {
        Role role = findById(roleId);
        return userRepository.findAll().stream()
                .filter(u -> u.getRoles().contains(role))
                .map(u -> u.getId())
                .toList();
    }

    // =========================================================================
    // Create
    // =========================================================================

    public Role createRole(CreateRoleRequest request) {
        log.debug("Creating role: code={}, scope={}", request.code(), request.scope());

        if (roleRepository.findByCode(request.code()).isPresent()) {
            throw new BusinessException("Role with code '" + request.code() + "' already exists");
        }

        Role role = roleMapper.toEntity(request);
        Role saved = roleRepository.save(role);
        log.info("Role created: id={}, code={}", saved.getId(), saved.getCode());
        return saved;
    }

    // =========================================================================
    // Update
    // =========================================================================

    public Role updateRole(UUID id, UpdateRoleRequest request) {
        log.debug("Updating role: id={}", id);
        Role role = findById(id);
        roleMapper.updateEntity(request, role);
        Role saved = roleRepository.save(role);
        log.info("Role updated: id={}, code={}", id, saved.getCode());
        return saved;
    }

    // =========================================================================
    // Activation
    // =========================================================================

    public Role activateRole(UUID id) {
        Role role = findById(id);
        if (role.isActive()) {
            throw new BusinessException("Role is already active");
        }
        role.setActive(true);
        return roleRepository.save(role);
    }

    public Role deactivateRole(UUID id) {
        Role role = findById(id);
        if (!role.isActive()) {
            throw new BusinessException("Role is already inactive");
        }
        role.setActive(false);
        return roleRepository.save(role);
    }

    // =========================================================================
    // Delete
    // =========================================================================

    public void deleteRole(UUID id) {
        Role role = findById(id);
        roleRepository.delete(role);
        log.info("Role deleted: id={}, code={}", id, role.getCode());
    }
}
