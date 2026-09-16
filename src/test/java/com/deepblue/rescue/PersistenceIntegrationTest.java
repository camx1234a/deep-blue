package com.deepblue.rescue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.Expertise;
import com.deepblue.rescue.domain.MedicalRecord;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueCenter;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.ExpertiseRepository;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.repository.RescueCenterRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;

@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("deepblue_test")
            .withUsername("deepblue")
            .withPassword("deepblue");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RescueCenterRepository rescueCenterRepository;

    @Autowired
    private RescueCaseRepository rescueCaseRepository;

    @Test
    void contextLoads() {
        assertThat(postgres.isRunning()).isTrue();
    }

    @Test
    void testFlywayMigrations() {
        Integer executedMigrations = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version IN ('1', '2')",
                Integer.class
        );
        assertThat(executedMigrations).isGreaterThanOrEqualTo(2);
    }

    @Test
    void testSaveAndFindRescueCenter() {
        RescueCenter center = new RescueCenter();
        center.setCode("CTR-001");
        center.setName("DeepBlue Caribbean Center");
        center.setCity("Santa Marta");

        RescueCenter savedCenter = rescueCenterRepository.save(center);
        
        assertThat(savedCenter.getId()).isNotNull();
        assertThat(rescueCenterRepository.count()).isGreaterThan(0L);

        var found = rescueCenterRepository.findById(savedCenter.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("DeepBlue Caribbean Center");
    }

    @Test
    void testRescueCenterOneToManyRelationship() {
        // Paso 49: Validar relación 1:N
        RescueCenter center = new RescueCenter();
        center.setCode("CTR-002");
        center.setName("DeepBlue Pacific Center");
        center.setCity("Buenaventura");
        RescueCenter savedCenter = rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-2026-001");
        rescueCase.setRescueDate(LocalDate.now());
        rescueCase.setRescueLocation("Tumaco");
        rescueCase.setStatus(RescueStatus.IN_REHABILITATION);
        
        savedCenter.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        var foundCase = rescueCaseRepository.findByCaseCode("RES-2026-001");
        assertThat(foundCase).isPresent();
        assertThat(foundCase.get().getRescueCenter().getName()).isEqualTo("DeepBlue Pacific Center");
        
        // Recargar el centro para verificar la colección actualizada desde la base de datos
        var reloadedCenter = rescueCenterRepository.findById(savedCenter.getId());
        assertThat(reloadedCenter).isPresent();
        assertThat(reloadedCenter.get().getRescueCases()).hasSize(1);
    }

   @Test
    void testRescueCaseOneToOneAnimal() {
        RescueCenter center = new RescueCenter();
        center.setCode("CTR-003");
        center.setName("DeepBlue Test Center");
        center.setCity("Cartagena");
        RescueCenter savedCenter = rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-2026-002");
        rescueCase.setRescueDate(LocalDate.now());
        rescueCase.setRescueLocation("Playa Grande");
        rescueCase.setStatus(RescueStatus.ADMITTED);

        savedCenter.addCase(rescueCase);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-2026-001");
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.FEMALE);

        rescueCase.assignAnimal(animal);
        rescueCaseRepository.save(rescueCase);

        var foundCase = rescueCaseRepository.findByCaseCode("RES-2026-002");
        assertThat(foundCase).isPresent();
        assertThat(foundCase.get().getAnimal()).isNotNull();
        assertThat(foundCase.get().getAnimal().getAnimalCode()).isEqualTo("AN-2026-001");
        assertThat(foundCase.get().getAnimal().getRescueCase()).isNotNull();
    }
    @Test
    void testAnimalOneToOneMedicalRecord() {
        RescueCenter center = new RescueCenter();
        center.setCode("CTR-004");
        center.setName("DeepBlue Medical Center");
        center.setCity("Santa Marta");
        RescueCenter savedCenter = rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-2026-003");
        rescueCase.setRescueDate(LocalDate.now());
        rescueCase.setRescueLocation("Taganga");
        rescueCase.setStatus(RescueStatus.IN_REHABILITATION);
        savedCenter.addCase(rescueCase);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-2026-002");
        animal.setCommonName("Loggerhead Turtle");
        animal.setScientificName("Caretta caretta");
        animal.setSex(AnimalSex.MALE);
        rescueCase.assignAnimal(animal);

        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setInitialWeight(new java.math.BigDecimal("28.40"));
        medicalRecord.setInitialCondition("STABLE");
        medicalRecord.setInjuries("Left front flipper injury");
        
        animal.assignMedicalRecord(medicalRecord);

        rescueCaseRepository.save(rescueCase);

        assertThat(animal.getId()).isNotNull();
        assertThat(medicalRecord.getId()).isNotNull();
        assertThat(animal.getMedicalRecord()).isNotNull();
        assertThat(animal.getMedicalRecord().getInitialWeight()).isEqualByComparingTo("28.40");
        assertThat(medicalRecord.getAnimal()).isEqualTo(animal);
    }
    @Autowired
    private ExpertiseRepository expertiseRepository;
    @Autowired
    private SpecialistRepository specialistRepository;
    @Test
    void testSpecialistExpertiseManyToMany() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma")
                .orElseGet(() -> {
                    Expertise e = new Expertise();
                    e.setName("Trauma");
                    return expertiseRepository.save(e);
                });
                
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation")
                .orElseGet(() -> {
                    Expertise e = new Expertise();
                    e.setName("Rehabilitation");
                    return expertiseRepository.save(e);
                });

        Specialist specialist = new Specialist();
        specialist.setProfessionalCode("SPEC-001");
        specialist.setFirstName("Elena");
        specialist.setLastName("Vargas");
        specialist.setEmail("elena@deepblue.org");
        specialist.setActive(true);

        specialist.addExpertise(trauma);
        specialist.addExpertise(rehabilitation);

        Specialist savedSpecialist = specialistRepository.save(specialist);

        assertThat(savedSpecialist.getId()).isNotNull();
        assertThat(savedSpecialist.getExpertiseAreas()).hasSize(2);
    }
    @Test
    void testRescueCasesByStatusQueryMethod() {
        RescueCenter center = new RescueCenter();
        center.setCode("CTR-005");
        center.setName("DeepBlue Query Center");
        center.setCity("Santa Marta");
        RescueCenter savedCenter = rescueCenterRepository.save(center);

        RescueCase case1 = new RescueCase();
        case1.setCaseCode("RES-001");
        case1.setRescueDate(LocalDate.now());
        case1.setRescueLocation("Playa 1");
        case1.setStatus(RescueStatus.IN_REHABILITATION);
        savedCenter.addCase(case1);

        RescueCase case2 = new RescueCase();
        case2.setCaseCode("RES-002");
        case2.setRescueDate(LocalDate.now());
        case2.setRescueLocation("Playa 2");
        case2.setStatus(RescueStatus.READY_FOR_RELEASE);
        savedCenter.addCase(case2);

        RescueCase case3 = new RescueCase();
        case3.setCaseCode("RES-003");
        case3.setRescueDate(LocalDate.now());
        case3.setRescueLocation("Playa 3");
        case3.setStatus(RescueStatus.IN_REHABILITATION);
        savedCenter.addCase(case3);

        rescueCaseRepository.save(case1);
        rescueCaseRepository.save(case2);
        rescueCaseRepository.save(case3);

        var rehabCases = rescueCaseRepository.findByStatus(RescueStatus.IN_REHABILITATION);
        assertThat(rehabCases).hasSize(2);
    }
    @Test
    void testFindRescueCasesByCenterCodeQueryMethod() {
        RescueCenter center = new RescueCenter();
        center.setCode("CTR-006");
        center.setName("DeepBlue Navigation Center");
        center.setCity("Santa Marta");
        RescueCenter savedCenter = rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-NAV-01");
        rescueCase.setRescueDate(LocalDate.now());
        rescueCase.setRescueLocation("Bahía Concha");
        rescueCase.setStatus(RescueStatus.ADMITTED);
        savedCenter.addCase(rescueCase);

        rescueCaseRepository.save(rescueCase);

        var casesByCenter = rescueCaseRepository.findByRescueCenterCode("CTR-006");
        assertThat(casesByCenter).hasSize(1);
        assertThat(casesByCenter.get(0).getCaseCode()).isEqualTo("RES-NAV-01");
    }
    @Autowired
    private TreatmentRepository treatmentRepository;
    @Autowired
    private AnimalRepository animalRepository;
    @Test
    void testCreateTreatments() {
        // Paso 56: Crear y persistir tratamientos para un animal[cite: 1]
        RescueCenter center = new RescueCenter();
        center.setCode("CTR-TREAT");
        center.setName("Treatment Center");
        center.setCity("Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-TREAT-01");
        rescueCase.setRescueDate(LocalDate.now());
        rescueCase.setRescueLocation("Playa");
        rescueCase.setStatus(RescueStatus.IN_REHABILITATION);
        center.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-TREAT-01");
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        animalRepository.save(animal);

        Specialist elena = new Specialist();
        elena.setProfessionalCode("SPEC-ELENA");
        elena.setFirstName("Elena");
        elena.setLastName("Vargas");
        elena.setEmail("elena.treatment@deepblue.org");
        elena.setActive(true);
        specialistRepository.save(elena);

        Specialist mateo = new Specialist();
        mateo.setProfessionalCode("SPEC-MATEO");
        mateo.setFirstName("Mateo");
        mateo.setLastName("Perez");
        mateo.setEmail("mateo.treatment@deepblue.org");
        mateo.setActive(true);
        specialistRepository.save(mateo);

        Treatment t1 = new Treatment();
        t1.setAnimal(animal);
        t1.setSpecialist(elena);
        t1.setPerformedAt(LocalDateTime.now().minusHours(3));
        t1.setType(TreatmentType.WOUND_CARE);
        t1.setDescription("Cleaning of left front flipper");

        Treatment t2 = new Treatment();
        t2.setAnimal(animal);
        t2.setSpecialist(elena);
        t2.setPerformedAt(LocalDateTime.now().minusHours(2));
        t2.setType(TreatmentType.HYDRATION);
        t2.setDescription("Subcutaneous fluid therapy");

        Treatment t3 = new Treatment();
        t3.setAnimal(animal);
        t3.setSpecialist(mateo);
        t3.setPerformedAt(LocalDateTime.now().minusHours(1));
        t3.setType(TreatmentType.OBSERVATION);
        t3.setDescription("General checkup");

        treatmentRepository.saveAll(java.util.Arrays.asList(t1, t2, t3));

        var treatments = treatmentRepository.findAll();
        assertThat(treatments).hasSizeGreaterThanOrEqualTo(3);
    }
    @Test
    void testTreatmentsByAnimalIdOrderedQueryMethod() {
        // Paso 57: Test Query Method de tratamientos ordenados cronológicamente
        RescueCenter center = new RescueCenter();
        center.setCode("CTR-TRT-ORD");
        center.setName("Treatment Ordered Center");
        center.setCity("Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-TRT-ORD-01");
        rescueCase.setRescueDate(LocalDate.now());
        rescueCase.setRescueLocation("Playa");
        rescueCase.setStatus(RescueStatus.IN_REHABILITATION);
        center.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-TRT-ORD-01");
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        animalRepository.save(animal);

        Specialist elena = new Specialist();
        elena.setProfessionalCode("SPEC-TRT-ORD");
        elena.setFirstName("Elena");
        elena.setLastName("Vargas");
        elena.setEmail("elena.trt.ord@deepblue.org");
        elena.setActive(true);
        specialistRepository.save(elena);

        Treatment t1 = new Treatment();
        t1.setAnimal(animal);
        t1.setSpecialist(elena);
        t1.setPerformedAt(LocalDateTime.now().minusHours(3));
        t1.setType(TreatmentType.WOUND_CARE);
        t1.setDescription("First treatment");

        Treatment t2 = new Treatment();
        t2.setAnimal(animal);
        t2.setSpecialist(elena);
        t2.setPerformedAt(LocalDateTime.now().minusHours(1));
        t2.setType(TreatmentType.OBSERVATION);
        t2.setDescription("Second treatment");

        treatmentRepository.saveAll(java.util.Arrays.asList(t1, t2));

        List<Treatment> orderedTreatments = treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId());
        
        assertThat(orderedTreatments).hasSize(2);
        assertThat(orderedTreatments.get(0).getDescription()).isEqualTo("First treatment");
        assertThat(orderedTreatments.get(1).getDescription()).isEqualTo("Second treatment");
    }
    @Test
    void testTreatmentsByDateIntervalJpaQuery() {
        // Paso 58: Test JPQL por intervalo de fechas
        RescueCenter center = new RescueCenter();
        center.setCode("CTR-INT");
        center.setName("Interval Center");
        center.setCity("Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-INT-01");
        rescueCase.setRescueDate(LocalDate.now());
        rescueCase.setRescueLocation("Playa");
        rescueCase.setStatus(RescueStatus.IN_REHABILITATION);
        center.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-INT-01");
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        animalRepository.save(animal);

        Specialist elena = new Specialist();
        elena.setProfessionalCode("SPEC-INT");
        elena.setFirstName("Elena");
        elena.setLastName("Vargas");
        elena.setEmail("elena.int@deepblue.org");
        elena.setActive(true);
        specialistRepository.save(elena);

        LocalDateTime baseDate = LocalDateTime.of(2026, 8, 1, 10, 0);

        Treatment t1 = new Treatment();
        t1.setAnimal(animal);
        t1.setSpecialist(elena);
        t1.setPerformedAt(baseDate); // 2026-08-01 10:00
        t1.setType(TreatmentType.WOUND_CARE);
        t1.setDescription("Early treatment");

        Treatment t2 = new Treatment();
        t2.setAnimal(animal);
        t2.setSpecialist(elena);
        t2.setPerformedAt(baseDate.plusDays(9)); // 2026-08-10 10:00
        t2.setType(TreatmentType.HYDRATION);
        t2.setDescription("Target interval treatment");

        Treatment t3 = new Treatment();
        t3.setAnimal(animal);
        t3.setSpecialist(elena);
        t3.setPerformedAt(baseDate.plusDays(19)); // 2026-08-20 10:00
        t3.setType(TreatmentType.OBSERVATION);
        t3.setDescription("Late treatment");

        treatmentRepository.saveAll(java.util.Arrays.asList(t1, t2, t3));

        LocalDateTime startInterval = LocalDateTime.of(2026, 8, 5, 0, 0);
        LocalDateTime endInterval = LocalDateTime.of(2026, 8, 15, 23, 59);

        List<Treatment> intervalTreatments = treatmentRepository.findByPerformedAtBetween(startInterval, endInterval);

        assertThat(intervalTreatments).hasSize(1);
        assertThat(intervalTreatments.get(0).getDescription()).isEqualTo("Target interval treatment");
    }
    @Test
    void testUniqueConstraintViolation() {
        RescueCenter center = new RescueCenter();
        center.setCode("CTR-UNI");
        center.setName("Unique Center");
        center.setCity("Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-UNI-01");
        rescueCase.setRescueDate(LocalDate.now());
        rescueCase.setRescueLocation("Playa");
        rescueCase.setStatus(RescueStatus.ADMITTED);
        center.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        Animal animal1 = new Animal();
        animal1.setAnimalCode("AN-DUPLICATE");
        animal1.setCommonName("Green Sea Turtle");
        animal1.setScientificName("Chelonia mydas");
        animal1.setSex(AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal1);
        animalRepository.saveAndFlush(animal1);

        RescueCase rescueCase2 = new RescueCase();
        rescueCase2.setCaseCode("RES-UNI-02");
        rescueCase2.setRescueDate(LocalDate.now());
        rescueCase2.setRescueLocation("Playa 2");
        rescueCase2.setStatus(RescueStatus.ADMITTED);
        center.addCase(rescueCase2);
        rescueCaseRepository.save(rescueCase2);

        Animal animal2 = new Animal();
        animal2.setAnimalCode("AN-DUPLICATE"); // Código duplicado que viola la regla UNIQUE
        animal2.setCommonName("Loggerhead Turtle");
        animal2.setScientificName("Caretta caretta");
        animal2.setSex(AnimalSex.MALE);
        rescueCase2.assignAnimal(animal2);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            animalRepository.saveAndFlush(animal2);
        }).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    @Test
    void testForeignKeyConstraintViolation() {
        Specialist elena = new Specialist();
        elena.setProfessionalCode("SPEC-FK");
        elena.setFirstName("Elena");
        elena.setLastName("Vargas");
        elena.setEmail("elena.fk@deepblue.org");
        elena.setActive(true);
        specialistRepository.save(elena);

        Treatment treatment = new Treatment();
        treatment.setSpecialist(elena);
        treatment.setPerformedAt(LocalDateTime.now());
        treatment.setType(TreatmentType.WOUND_CARE);
        treatment.setDescription("Treatment without animal reference");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            treatmentRepository.saveAndFlush(treatment);
        }).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    @Test
    void testCheckConstraintViolation() {
        RescueCenter center = new RescueCenter();
        center.setCode("CTR-CHECK");
        center.setName("Check Center");
        center.setCity("Santa Marta");
        rescueCenterRepository.save(center);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            jdbcTemplate.update(
                "INSERT INTO rescue_cases (case_code, rescue_date, rescue_location, status, rescue_center_id) VALUES (?, ?, ?, ?, ?)",
                "RES-CHK-01", 
                java.sql.Date.valueOf(LocalDate.now()), 
                "Playa", 
                "INVALID_STATUS", 
                center.getId()
            );
        }).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    @Test
    void testIntegratorScenario() {
       
        RescueCenter center = new RescueCenter();
        center.setCode("DB-CAR");
        center.setName("DeepBlue Caribbean");
        center.setCity("Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-2026-100");
        rescueCase.setRescueDate(LocalDate.of(2026, 8, 18));
        rescueCase.setRescueLocation("Bahía Concha");
        rescueCase.setStatus(RescueStatus.IN_REHABILITATION);
        center.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-2026-100");
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        
        MedicalRecord record = new MedicalRecord();
        record.setInitialWeight(new java.math.BigDecimal("27.80"));
        record.setInitialCondition("STABLE");
        record.setInjuries("Injury caused by fishing net");
        record.setObservations("Possible plastic ingestion");
        animal.assignMedicalRecord(record);
        
        animalRepository.save(animal);

        Expertise marineReptiles = expertiseRepository.findByNameIgnoreCase("Marine Reptiles").orElseThrow();
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

        Specialist elena = new Specialist();
        elena.setProfessionalCode("SPEC-001");
        elena.setFirstName("Elena");
        elena.setLastName("Vargas");
        elena.setEmail("elena@deepblue.org");
        elena.setActive(true);
        elena.addExpertise(marineReptiles);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);
        specialistRepository.save(elena);

        Treatment t1 = new Treatment();
        t1.setAnimal(animal);
        t1.setSpecialist(elena);
        t1.setPerformedAt(java.time.LocalDateTime.of(2026, 8, 18, 14, 0));
        t1.setType(TreatmentType.WOUND_CARE);
        t1.setDescription("Cleaning of left front flipper");

        Treatment t2 = new Treatment();
        t2.setAnimal(animal);
        t2.setSpecialist(elena);
        t2.setPerformedAt(java.time.LocalDateTime.of(2026, 8, 18, 16, 0));
        t2.setType(TreatmentType.HYDRATION);
        t2.setDescription("Subcutaneous fluid therapy");

        treatmentRepository.saveAll(java.util.Arrays.asList(t1, t2));

        var foundCase = rescueCaseRepository.findByCaseCode("RES-2026-100");
        assertThat(foundCase).isPresent();
        assertThat(foundCase.get().getStatus()).isEqualTo(RescueStatus.IN_REHABILITATION);
        
        var treatments = treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId());
        assertThat(treatments).hasSize(2);
    }
    @Test
    void testIntegratorQueries() {
        // 1. Preparar y persistir el escenario integrador de DeepBlue Caribbean
        RescueCenter center = new RescueCenter();
        center.setCode("DB-CAR");
        center.setName("DeepBlue Caribbean");
        center.setCity("Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-2026-100");
        rescueCase.setRescueDate(LocalDate.of(2026, 8, 18));
        rescueCase.setRescueLocation("Bahía Concha");
        rescueCase.setStatus(RescueStatus.IN_REHABILITATION);
        center.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-2026-100");
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        
        MedicalRecord record = new MedicalRecord();
        record.setInitialWeight(new java.math.BigDecimal("27.80"));
        record.setInitialCondition("STABLE");
        record.setInjuries("Injury caused by fishing net");
        record.setObservations("Possible plastic ingestion");
        animal.assignMedicalRecord(record);
        
        animalRepository.save(animal);

        Expertise marineReptiles = expertiseRepository.findByNameIgnoreCase("Marine Reptiles").orElseThrow();
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

        Specialist elena = new Specialist();
        elena.setProfessionalCode("SPEC-001");
        elena.setFirstName("Elena");
        elena.setLastName("Vargas");
        elena.setEmail("elena@deepblue.org");
        elena.setActive(true);
        elena.addExpertise(marineReptiles);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);
        specialistRepository.save(elena);

        Treatment t1 = new Treatment();
        t1.setAnimal(animal);
        t1.setSpecialist(elena);
        t1.setPerformedAt(LocalDateTime.of(2026, 8, 18, 14, 0));
        t1.setType(TreatmentType.WOUND_CARE);
        t1.setDescription("Cleaning of left front flipper");

        Treatment t2 = new Treatment();
        t2.setAnimal(animal);
        t2.setSpecialist(elena);
        t2.setPerformedAt(LocalDateTime.of(2026, 8, 18, 16, 0));
        t2.setType(TreatmentType.HYDRATION);
        t2.setDescription("Subcutaneous fluid therapy");

        treatmentRepository.saveAll(Arrays.asList(t1, t2));

        boolean caseExists = rescueCaseRepository.findByCaseCode("RES-2026-100").isPresent();
        assertThat(caseExists).isTrue();

        List<RescueCase> rehabCases = rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);
        assertThat(rehabCases).isNotEmpty();

        List<Animal> dbCarAnimals = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR");
        assertThat(dbCarAnimals).isNotEmpty();

        List<Animal> turtleAnimals = animalRepository.findByCommonNameContainingIgnoreCase("turtle");
        assertThat(turtleAnimals).isNotEmpty();

        List<Specialist> traumaSpecialists = specialistRepository.findActiveByExpertise("Trauma");
        assertThat(traumaSpecialists).isNotEmpty();

        List<Treatment> orderedTreatments = treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId());
        assertThat(orderedTreatments).hasSize(2);

        List<Treatment> rehabTreatments = treatmentRepository.findBySpecialistExpertiseName("Rehabilitation");
        assertThat(rehabTreatments).isNotEmpty();

        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 31, 23, 59);
        List<Treatment> intervalTreatments = treatmentRepository.findByPerformedAtBetween(start, end);
        assertThat(intervalTreatments).isNotEmpty();
    }
    @Test
    void testUnguidedChallengeQuery() {

        RescueCenter center = new RescueCenter();
        center.setCode("DB-CAR-UG");
        center.setName("Caribbean Center");
        center.setCity("Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-UG-01");
        rescueCase.setRescueDate(LocalDate.now());
        rescueCase.setRescueLocation("Playa");
        rescueCase.setStatus(RescueStatus.IN_REHABILITATION);
        center.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-UG-01");
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        animalRepository.save(animal);

        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();

        Specialist specialist = new Specialist();
        specialist.setProfessionalCode("SPEC-UG");
        specialist.setFirstName("Elena");
        specialist.setLastName("Vargas");
        specialist.setEmail("elena.ug@deepblue.org");
        specialist.setActive(true);
        specialist.addExpertise(trauma);
        specialistRepository.save(specialist);

        Treatment treatment = new Treatment();
        treatment.setAnimal(animal);
        treatment.setSpecialist(specialist);
        treatment.setPerformedAt(LocalDateTime.now());
        treatment.setType(TreatmentType.SURGERY);
        treatment.setDescription("Trauma care");
        treatmentRepository.save(treatment);

        List<Animal> traumaAnimals = animalRepository.findAnimalsInRehabilitationByExpertise(
            RescueStatus.IN_REHABILITATION, 
            "Trauma"
        );
        
        assertThat(traumaAnimals).isNotEmpty();
    }
}