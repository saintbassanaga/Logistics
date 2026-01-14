package tech.bytesmind.logistics.auth.application.service.impls;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.bytesmind.logistics.auth.api.dto.CreateRoleRequest;
import tech.bytesmind.logistics.auth.api.dto.UpdateRoleRequest;
import tech.bytesmind.logistics.auth.application.mapper.RoleMapper;
import tech.bytesmind.logistics.auth.application.service.RoleService;
import tech.bytesmind.logistics.auth.domain.model.Role;
import tech.bytesmind.logistics.auth.domain.model.RoleScope;
import tech.bytesmind.logistics.auth.domain.service.RoleDomainService;
import tech.bytesmind.logistics.auth.infrastructure.repository.RoleRepository;
import tech.bytesmind.logistics.shared.exceptions.BusinessException;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of the RoleService interface, providing methods for managing roles
 * such as creation, retrieval, updating, activation, and deactivation.
 * This service leverages RoleRepository for persistence operations and RoleDomainService
 * for domain logic validation and manipulation. Transaction management is used
 * to ensure consistency in database operations.
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleServiceImpl.class);

    private final RoleRepository roleRepository;
    private final RoleDomainService roleDomainService;
    private final RoleMapper roleMapper;

    @Override
    @Transactional
    public Role createRole(CreateRoleRequest request) {
        log.info("Creating role: {}", request.code());

        // Convertir DTO → Entity via RoleMapper
        Role role = roleMapper.toEntity(request);

        // Valider l'entité via le service de domaine
        roleDomainService.validateRole(role);

        // Vérifier unicité du code
        if (roleRepository.existsByCode(request.code())) {
            throw new BusinessException("Role with code '" + request.code() + "' already exists");
        }

        // Sauvegarder
        Role saved = roleRepository.save(role);
        log.info("Role created successfully: {}", saved.getId());

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Role getRoleById(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Role not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Role getRoleByCode(String code) {
        return roleRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException("Role not found: " + code));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> listAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> listActiveRoles() {
        return roleRepository.findByActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> listRolesByScope(RoleScope scope) {
        return roleRepository.findByScope(scope);
    }

    @Override
    @Transactional
    public Role updateRole(UUID roleId, UpdateRoleRequest request) {
        log.info("Updating role: {}", roleId);

        Role existing = getRoleById(roleId);

        // Utiliser le mapper pour mettre à jour les champs modifiables
        roleMapper.updateEntity(request, existing);

        // Valider après mise à jour
        roleDomainService.validateRole(existing);

        Role updated = roleRepository.save(existing);
        log.info("Role updated: {}", roleId);

        return updated;
    }

    @Override
    @Transactional
    public void deactivateRole(UUID roleId) {
        log.info("Deactivating role: {}", roleId);

        Role role = getRoleById(roleId);
        roleDomainService.deactivate(role);
        roleRepository.save(role);

        log.info("Role deactivated successfully");
    }

    @Override
    @Transactional
    public void activateRole(UUID roleId) {
        log.info("Activating role: {}", roleId);

        Role role = getRoleById(roleId);
        roleDomainService.activate(role);
        roleRepository.save(role);

        log.info("Role activated successfully");
    }
}