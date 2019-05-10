package fr.progilone.pgcn.repository.es;

import fr.progilone.pgcn.domain.document.conditionreport.ConditionReport;
import fr.progilone.pgcn.repository.es.helper.EsSearchOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Collection;
import java.util.List;

public interface EsConditionReportRepositoryCustom {

    /**
     * Recherche paginée
     *
     * @param search
     * @param libraries
     * @param fuzzy
     * @param filters
     * @param pageable
     * @param facet
     * @return
     */
    Page<ConditionReport> search(final EsSearchOperation[] search,
                                 final List<String> libraries,
                                 final boolean fuzzy,
                                 final EsSearchOperation[] filters,
                                 final PageRequest pageable,
                                 final boolean facet);

    /**
     * Indexation des constats d'état, en spécifiant l'index
     *
     * @param index
     * @param records
     */
    void index(final String index, final Collection<ConditionReport> records);

    /**
     * Indexation (avec gestion de la relation parent/enfant)
     *
     * @param record
     */
    void indexEntity(ConditionReport record);

    /**
     * Indexation (avec gestion de la relation parent/enfant)
     *
     * @param records
     */
    void indexEntities(Collection<ConditionReport> records);

    /**
     * Suppression d'une notice de l'index
     *
     * @param record
     */
    void deleteEntity(ConditionReport record);

    /**
     * Suppression d'une collection de constats d'état de l'index
     *
     * @param records
     */
    void deleteEntities(Collection<ConditionReport> records);
}
