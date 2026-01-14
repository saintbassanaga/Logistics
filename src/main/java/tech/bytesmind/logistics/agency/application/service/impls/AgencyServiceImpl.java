package tech.bytesmind.logistics.agency.application.service.impls;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.bytesmind.logistics.agency.api.dto.CreateAgencyRequest;
import tech.bytesmind.logistics.agency.application.mapper.AgencyMapper;
import tech.bytesmind.logistics.agency.application.service.AgencyService;
import tech.bytesmind.logistics.agency.domain.event.AgencyCreatedEvent;
import tech.bytesmind.logistics.agency.domain.event.AgencySuspendedEvent;
import tech.bytesmind.logistics.agency.domain.model.Agency;
import tech.bytesmind.logistics.agency.domain.model.AgencyLocation;
import tech.bytesmind.logistics.agency.domain.model.LocationType;
import tech.bytesmind.logistics.agency.domain.service.AgencyCodeGenerator;
import tech.bytesmind.logistics.agency.domain.service.AgencyDomainService;
import tech.bytesmind.logistics.agency.infrastructure.repository.AgencyRepository;
import tech.bytesmind.logistics.shared.event.publisher.TransactionalEventPublisher;
import tech.bytesmind.logistics.shared.exceptions.BusinessException;

import java.util.List;
import java.util.UUID;

@Service
public class AgencyServiceImpl implements AgencyService {

    private static final Logger log = LoggerFactory.getLogger(AgencyServiceImpl.class);

    private final AgencyRepository agencyRepository;
    private final AgencyDomainService domainService;
    private final AgencyCodeGenerator codeGenerator;
    private final AgencyMapper agencyMapper;
    private final TransactionalEventPublisher eventPublisher;

    public AgencyServiceImpl(
            AgencyRepository agencyRepository,
            AgencyDomainService domainService,
            AgencyCodeGenerator codeGenerator,
            AgencyMapper agencyMapper,
            TransactionalEventPublisher eventPublisher
    ) {
        this.agencyRepository = agencyRepository;
        this.domainService = domainService;
        this.codeGenerator = codeGenerator;
        this.agencyMapper = agencyMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Agency createAgency(CreateAgencyRequest request) {
        log.info("Creating agency: {}", request.name());

        // Vérifier unicité de l'email
        if (agencyRepository.existsByEmail(request.email())) {
            throw new BusinessException("Agency with email '" + request.email() + "' already exists");
        }

        // Générer le code automatiquement
        String generatedCode = codeGenerator.generateUniqueCode();
        log.info("Generated agency code: {}", generatedCode);

        // Convertir DTO → Entity
        Agency agency = agencyMapper.toEntity(request);

        // Assigner le code généré
        agency.setCode(generatedCode);

        // Sauvegarder l'agence
        agency = agencyRepository.save(agency);

        // Créer le siège social automatiquement
        AgencyLocation headquarters = createHeadquarters(agency, request);
        agency.getAgencyLocations().add(headquarters);
        headquarters.setAgency(agency);
        headquarters.setAgencyId(agency.getId());

        agency = agencyRepository.save(agency);

        // Publier événement
        eventPublisher.publish(new AgencyCreatedEvent(
                agency.getId(),
                agency.getCode(),
                agency.getName()
        ));

        log.info("Agency created successfully: {}", agency.getId());
        return agency;
    }

    private AgencyLocation createHeadquarters(Agency agency, CreateAgencyRequest request) {
        AgencyLocation hq = new AgencyLocation();
        hq.setCode("HQ");
        hq.setName(agency.getName() + " - Headquarters");
        hq.setLocationType(LocationType.HEADQUARTERS);
        hq.setAddressLine1(request.addressLine1());
        hq.setAddressLine2(request.addressLine2());
        hq.setCity(request.city());
        hq.setStateRegion(request.stateRegion());
        hq.setPostalCode(request.postalCode());
        hq.setCountry(request.country());
        hq.setEmail(request.email());
        hq.setPhone(request.phone());
        return hq;
    }

    @Override
    @Transactional(readOnly = true)
    public Agency getAgencyById(UUID id) {
        return agencyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Agency not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Agency getAgencyWithLocations(UUID id) {
        return agencyRepository.findByIdWithLocations(id)
                .orElseThrow(() -> new BusinessException("Agency not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Agency> listAllAgencies() {
        return agencyRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Agency> listActiveAgencies() {
        return agencyRepository.findByActiveTrue();
    }

    @Override
    @Transactional
    public void suspendAgency(UUID agencyId, String reason) {
        log.info("Suspending agency: {}", agencyId);

        Agency agency = getAgencyById(agencyId);
        domainService.suspend(agency, reason);
        agencyRepository.save(agency);

        eventPublisher.publish(new AgencySuspendedEvent(agencyId, reason));
    }

    @Override
    @Transactional
    public void unsuspendAgency(UUID agencyId) {
        log.info("Unsuspending agency: {}", agencyId);

        Agency agency = getAgencyById(agencyId);
        domainService.unsuspend(agency);
        agencyRepository.save(agency);
    }
}
