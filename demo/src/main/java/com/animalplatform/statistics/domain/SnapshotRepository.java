package com.animalplatform.statistics.domain;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import org.springframework.util.StringUtils;

public interface SnapshotRepository extends JpaRepository<AnimalSnapshot, String>, JpaSpecificationExecutor<AnimalSnapshot> {

    @Query("""
            select coalesce(snapshot.processState, '') as label, count(snapshot) as count
            from AnimalSnapshot snapshot
            group by snapshot.processState
            """)
    List<CountByValue> countByProcessState();

    @Query("""
            select coalesce(snapshot.sido, '') as label, count(snapshot) as count
            from AnimalSnapshot snapshot
            where snapshot.collectedAt > :collectedAfter
            group by snapshot.sido
            """)
    List<CountByValue> countBySidoAndCollectedAtAfter(LocalDateTime collectedAfter);

    @Query("""
            select coalesce(snapshot.sexCd, '') as label, count(snapshot) as count
            from AnimalSnapshot snapshot
            group by snapshot.sexCd
            """)
    List<CountByValue> countBySexCd();

    @Query("""
            select snapshot.careRegNo as careRegNo,
                   snapshot.careNm as careNm,
                   snapshot.careAddr as careAddr,
                   snapshot.careTel as careTel,
                   snapshot.orgNm as orgNm,
                   count(snapshot) as animalCount,
                   sum(case when snapshot.processState = '보호중' then 1 else 0 end) as criticalCount
            from AnimalSnapshot snapshot
            where snapshot.careNm is not null and snapshot.careNm <> ''
            group by snapshot.careRegNo, snapshot.careNm, snapshot.careAddr, snapshot.careTel, snapshot.orgNm
            """)
    List<ShelterSummary> findShelterSummaries();

    Optional<AnimalSnapshot> findTopByOrderByCollectedAtDesc();

    Optional<AnimalSnapshot> findByDesertionNo(String desertionNo);

    Optional<AnimalSnapshot> findByNoticeNo(String noticeNo);

    List<AnimalSnapshot> findByCareRegNo(String careRegNo);

    Optional<AnimalSnapshot> findFirstByCareRegNo(String careRegNo);

    List<AnimalSnapshot> findByCareNm(String careNm);

    List<AnimalSnapshot> findAllByHappenDtBetween(String startDate, String endDate);

    long countByProcessStateContaining(String processState);

    @Query("""
            select count(snapshot)
            from AnimalSnapshot snapshot
            where snapshot.specialMark like '%치료%'
               or snapshot.specialMark like '%수술%'
               or snapshot.specialMark like '%부상%'
            """)
    long countMedicalSupportCandidates();

    @Query("""
            select count(distinct snapshot.careNm)
            from AnimalSnapshot snapshot
            where snapshot.careNm is not null and snapshot.careNm <> ''
            """)
    long countDistinctSheltersByCareName();

    @Query("""
            select snapshot
            from AnimalSnapshot snapshot
            where snapshot.processState like '%보호%'
              and snapshot.noticeEdt >= :today
              and snapshot.noticeEdt <= :urgentDate
            order by snapshot.noticeEdt asc
            """)
    List<AnimalSnapshot> findUrgentAnimals(
            @Param("today") String today,
            @Param("urgentDate") String urgentDate,
            Pageable pageable
    );

    @Query("""
            select snapshot
            from AnimalSnapshot snapshot
            where snapshot.processState like '%보호%'
            order by
                case
                    when snapshot.noticeEdt >= :today and snapshot.noticeEdt <= :urgentDate then 0
                    when snapshot.noticeSdt <= :longWaitDate then 1
                    else 2
                end,
                function('RAND')
            """)
    List<AnimalSnapshot> findRecommended(
            @Param("today") String today,
            @Param("urgentDate") String urgentDate,
            @Param("longWaitDate") String longWaitDate,
            Pageable pageable
    );

    @Query("""
            select snapshot
            from AnimalSnapshot snapshot
            where snapshot.processState like '%보호%'
              and (
                    snapshot.orgNm like concat('%', :orgNm, '%')
                    or snapshot.sido like concat('%', :orgNm, '%')
                    or snapshot.careAddr like concat('%', :orgNm, '%')
                    or snapshot.happenPlace like concat('%', :orgNm, '%')
              )
            order by
                case
                    when snapshot.noticeEdt >= :today and snapshot.noticeEdt <= :urgentDate then 0
                    when snapshot.noticeSdt <= :longWaitDate then 1
                    else 2
                end,
                function('RAND')
            """)
    List<AnimalSnapshot> findRecommendedByRegion(
            @Param("orgNm") String orgNm,
            @Param("today") String today,
            @Param("urgentDate") String urgentDate,
            @Param("longWaitDate") String longWaitDate,
            Pageable pageable
    );

    default Page<AnimalSnapshot> findByFilters(
            String upkind,
            String orgNm,
            String processState,
            String sexCd,
            String neuterYn,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    ) {
        return findAll(animalSnapshotFilter(upkind, orgNm, processState, sexCd, neuterYn, from, to), pageable);
    }

    private Specification<AnimalSnapshot> animalSnapshotFilter(
            String upkind,
            String orgNm,
            String processState,
            String sexCd,
            String neuterYn,
            LocalDate from,
            LocalDate to
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(upkind)) {
                String keyword = "%" + upkind.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("kindCd")), keyword));
            }

            if (StringUtils.hasText(orgNm)) {
                String keyword = "%" + orgNm.trim() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("orgNm"), keyword),
                        criteriaBuilder.like(root.get("sido"), keyword),
                        criteriaBuilder.like(root.get("careAddr"), keyword),
                        criteriaBuilder.like(root.get("happenPlace"), keyword)
                ));
            }

            if (StringUtils.hasText(processState)) {
                predicates.add(processStatePredicate(root.get("processState").as(String.class), processState, criteriaBuilder));
            }

            if (StringUtils.hasText(sexCd)) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.upper(root.get("sexCd")), normalizeSexCode(sexCd)));
            }

            if (StringUtils.hasText(neuterYn)) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.upper(root.get("neuterYn")), normalizeNeuterCode(neuterYn)));
            }

            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("happenDt"), from.format(DateTimeFormatter.BASIC_ISO_DATE)));
            }

            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("happenDt"), to.format(DateTimeFormatter.BASIC_ISO_DATE)));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Predicate processStatePredicate(
            jakarta.persistence.criteria.Expression<String> path,
            String processState,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder
    ) {
        String normalized = processState.trim().toUpperCase();
        if ("PROTECTING".equals(normalized) || "ADOPTABLE".equals(normalized)) {
            return criteriaBuilder.or(
                    criteriaBuilder.like(path, "%보호%"),
                    criteriaBuilder.like(path, "%공고%")
            );
        }
        if ("IN_COUNSELING".equals(normalized)) {
            return criteriaBuilder.or(
                    criteriaBuilder.like(path, "%상담%"),
                    criteriaBuilder.like(path, "%예약%"),
                    criteriaBuilder.like(path, "%진행%")
            );
        }
        if ("COMPLETED".equals(normalized) || "ADOPTED".equals(normalized)) {
            return criteriaBuilder.or(
                    criteriaBuilder.like(path, "%종료%"),
                    criteriaBuilder.like(path, "%입양%"),
                    criteriaBuilder.like(path, "%반환%"),
                    criteriaBuilder.like(path, "%안락사%"),
                    criteriaBuilder.like(path, "%자연사%")
            );
        }
        return criteriaBuilder.like(path, "%" + processState.trim() + "%");
    }

    private String normalizeSexCode(String sexCd) {
        String value = sexCd.trim().toUpperCase();
        if ("수컷".equals(sexCd) || "MALE".equals(value)) {
            return "M";
        }
        if ("암컷".equals(sexCd) || "FEMALE".equals(value)) {
            return "F";
        }
        return value;
    }

    private String normalizeNeuterCode(String neuterYn) {
        String value = neuterYn.trim().toUpperCase();
        if ("예".equals(neuterYn) || "YES".equals(value) || "TRUE".equals(value)) {
            return "Y";
        }
        if ("아니오".equals(neuterYn) || "NO".equals(value) || "FALSE".equals(value)) {
            return "N";
        }
        return value;
    }

    interface CountByValue {
        String getLabel();

        Long getCount();
    }

    interface ShelterSummary {
        String getCareRegNo();

        String getCareNm();

        String getCareAddr();

        String getCareTel();

        String getOrgNm();

        Long getAnimalCount();

        Long getCriticalCount();
    }
}
