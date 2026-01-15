package tech.bytesmind.logistics.shared.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;


class ArchitectureTest {
    
    private static JavaClasses classes;
    
    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("tech.bytesmind.logistics");
    }
    
    /**
     * RÈGLE 1 : Isolation des Bounded Contexts.
     * Aucun contexte métier ne doit dépendre d'un autre.
     */
    @Test
    void boundedContexts_should_not_depend_on_each_other() {
        // Auth ne doit pas dépendre de Agency ou Parcel
        ArchRule authRule = noClasses()
            .that().resideInAPackage("..auth..")
            .should().dependOnClassesThat().resideInAnyPackage("..agency..", "..parcel..");
        authRule.check(classes);

        // Agency ne doit pas dépendre de Auth ou Parcel
        ArchRule agencyRule = noClasses()
            .that().resideInAPackage("..agency..")
            .should().dependOnClassesThat().resideInAnyPackage("..auth..", "..parcel..");
        agencyRule.check(classes);

        // Parcel ne doit pas dépendre de Auth ou Agency
        ArchRule parcelRule = noClasses()
            .that().resideInAPackage("..parcel..")
            .should().dependOnClassesThat().resideInAnyPackage("..auth..", "..agency..");
        parcelRule.check(classes);
    }
    
    /**
     * RÈGLE 2 : Le domaine ne doit jamais dépendre de l'infrastructure.
     */
    @Test
    void domain_should_not_depend_on_infrastructure() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..infrastructure..", "..api..");
        
        rule.check(classes);
    }
    
    /**
     * RÈGLE 3 : Architecture en couches respectée.
     * Note:
     * - API peut accéder au Domain pour les enums/value objects utilisés dans les DTOs.
     * - Application peut accéder à API pour les DTOs (mappers convertissent DTOs ↔ Entities).
     * Ces patterns DDD courants évitent la duplication des types.
     */
    @Test
    void layered_architecture_should_be_respected() {
        ArchRule rule = layeredArchitecture()
            .consideringAllDependencies()
            .layer("API").definedBy("..api..")
            .layer("Application").definedBy("..application..")
            .layer("Domain").definedBy("..domain..")
            .layer("Infrastructure").definedBy("..infrastructure..")

            .whereLayer("API").mayOnlyBeAccessedByLayers("Application")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("API")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("API", "Application", "Infrastructure")
            .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Application");

        rule.check(classes);
    }
    
    /**
     * RÈGLE 4 : Les controllers doivent être dans le package api.
     */
    @Test
    void controllers_should_reside_in_api_package() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Controller")
            .should().resideInAPackage("..api..");
        
        rule.check(classes);
    }
    
    /**
     * RÈGLE 5 : Les repositories doivent être dans infrastructure.
     */
    @Test
    void repositories_should_reside_in_infrastructure_package() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Repository")
            .should().resideInAPackage("..infrastructure..");
        
        rule.check(classes);
    }
}