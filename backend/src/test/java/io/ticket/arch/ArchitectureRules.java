package io.ticket.arch;

import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;

/**
 * The ArchUnit rules of DOC-12 §5, parameterised by root package so tests can aim them at fixtures
 * too.
 */
final class ArchitectureRules {

  private ArchitectureRules() {}

  static JavaClasses importMain(String... packages) {
    return new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(packages);
  }

  /**
   * Fixtures live in the test source set, so unlike {@link #importMain} this keeps test classes.
   */
  static JavaClasses importFixture(String... packages) {
    return new ClassFileImporter().importPackages(packages);
  }

  /** ARC-02: entry point → service → repository/client; entry points never call each other. */
  static ArchRule layers(String root) {
    return layeredArchitecture()
        .consideringOnlyDependenciesInLayers()
        .withOptionalLayers(true)
        .layer("EntryPoint")
        .definedBy(root + "..controller..", root + "..job..", root + "..listener..")
        .layer("Service")
        .definedBy(root + "..service..")
        .layer("Data")
        .definedBy(root + "..repository..", root + "..client..")
        .whereLayer("EntryPoint")
        .mayNotBeAccessedByAnyLayer()
        .whereLayer("Service")
        .mayOnlyBeAccessedByLayers("EntryPoint", "Service")
        .whereLayer("Data")
        .mayOnlyBeAccessedByLayers("Service", "Data");
  }

  /**
   * ARC-02, second half: the layer check ignores calls inside one layer, so entry points get their
   * own rule.
   */
  static ArchRule entryPointsDoNotCallEachOther(String root) {
    DescribedPredicate<JavaClass> entryPoint =
        DescribedPredicate.describe(
            "an entry point (controller, job, listener)",
            c ->
                c.getPackageName().startsWith(root)
                    && (c.getPackageName().contains(".controller")
                        || c.getPackageName().contains(".job")
                        || c.getPackageName().contains(".listener")));
    ArchCondition<JavaClass> noOtherEntryPoint =
        new ArchCondition<>("not depend on another entry point") {
          @Override
          public void check(JavaClass item, ConditionEvents events) {
            item.getDirectDependenciesFromSelf().stream()
                .map(d -> d.getTargetClass().getBaseComponentType())
                .filter(entryPoint)
                .filter(t -> !outer(t).equals(outer(item)))
                .forEach(
                    t ->
                        events.add(
                            SimpleConditionEvent.violated(
                                item, item.getName() + " depends on entry point " + t.getName())));
          }

          private JavaClass outer(JavaClass c) {
            while (c.getEnclosingClass().isPresent()) {
              c = c.getEnclosingClass().get();
            }
            return c;
          }
        };
    return classes().that(entryPoint).should(noOtherEntryPoint).allowEmptyShould(true);
  }

  /** ARC-04: controllers speak DTOs, never entities. */
  static ArchRule controllersAvoidEntities(String root) {
    return noClasses()
        .that()
        .resideInAPackage(root + "..controller..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(root + "..entity..")
        .allowEmptyShould(true);
  }

  /** ARC-03: only services open transactions. */
  static ArchRule transactionsOnlyInServices(String root) {
    DescribedPredicate<JavaClass> usesTransactional =
        DescribedPredicate.describe(
            "use @Transactional",
            c ->
                c.isAnnotatedWith(Transactional.class)
                    || c.getMethods().stream()
                        .anyMatch(m -> m.isAnnotatedWith(Transactional.class)));
    return classes()
        .that(usesTransactional)
        .and()
        .resideInAPackage(root + "..")
        .should()
        .resideInAPackage("..service..")
        .allowEmptyShould(true);
  }

  private static final Set<String> STATUS_REPOSITORIES =
      Set.of("Reservation", "Order", "Ticket", "Event", "OutboxMessage");

  /**
   * ARC-06: no {@code save()} on aggregates with a {@code status} column except in an {@code
   * …Inserter} class; state changes are conditional UPDATEs (DOC-12 §3.2 rule 1).
   */
  static ArchRule noStatusSave(String root) {
    DescribedPredicate<com.tngtech.archunit.core.domain.JavaMethodCall> savesStatusAggregate =
        DescribedPredicate.describe(
            "save() on a repository of an aggregate with a status column",
            call -> {
              String method = call.getTarget().getName();
              String owner = call.getTargetOwner().getSimpleName();
              boolean save = method.equals("save") || method.equals("saveAll");
              return save
                  && STATUS_REPOSITORIES.stream().anyMatch(a -> owner.equals(a + "Repository"));
            });
    return noClasses()
        .that()
        .resideInAPackage(root + "..")
        .and()
        .haveSimpleNameNotEndingWith("Inserter")
        .should()
        .callMethodWhere(savesStatusAggregate)
        .allowEmptyShould(true);
  }
}
