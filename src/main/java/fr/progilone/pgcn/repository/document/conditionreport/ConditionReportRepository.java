package fr.progilone.pgcn.repository.document.conditionreport;

import fr.progilone.pgcn.domain.document.DocUnit;
import fr.progilone.pgcn.domain.document.conditionreport.ConditionReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ConditionReportRepository extends JpaRepository<ConditionReport, String>, ConditionReportRepositoryCustom {

    @Query("select c.docUnit from ConditionReport c where c.identifier = ?1")
    DocUnit findDocUnitByIdentifier(String identifier);

    @Query("select c "
           + "from ConditionReport c "
           + "join fetch c.docUnit d "
           + "join fetch d.library "
           + "left join fetch c.details d "
           + "where c.docUnit.identifier = ?1")
    ConditionReport findByDocUnit(String docUnitId);

    @Query("select c "
           + "from ConditionReport c "
           + "join fetch c.docUnit d "
           + "join fetch d.library "
           + "left join fetch c.details d "
           + "where c.identifier = ?1")
    ConditionReport findByIdentifier(String identifier);

    @Query("select distinct c "
           + "from ConditionReport c "
           + "join fetch c.docUnit d "
           + "join fetch d.library "
           + "left join fetch c.details d "
           + "where c.identifier in ?1")
    List<ConditionReport> findByIdentifierIn(List<String> identifiers);

    @Query("select distinct c "
           + "from ConditionReport c "
           + "left join fetch c.details d "
           + "where c.identifier in ?1")
    List<ConditionReport> findByIdentifierInWithDetails(List<String> identifiers);

    ConditionReport findByDocUnitIdentifier(String docUnitId);

    List<ConditionReport> findByDocUnitIdentifierIn(List<String> docUnitId);

    Page<ConditionReport> findAllByDocUnitState(DocUnit.State state, Pageable pageable);
}
