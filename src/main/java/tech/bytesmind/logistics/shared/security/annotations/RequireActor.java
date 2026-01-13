package tech.bytesmind.logistics.shared.security.annotations;


import tech.bytesmind.logistics.shared.security.model.ActorType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour vérifier le type d'acteur.
 * Partie du système RBAC (ADR-005).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireActor {
    ActorType[] value();
}