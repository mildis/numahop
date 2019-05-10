package fr.progilone.pgcn.service.es;

import fr.progilone.pgcn.domain.AbstractDomainObject_;
import fr.progilone.pgcn.domain.document.DocUnit;
import fr.progilone.pgcn.domain.exchange.internetarchive.InternetArchiveReport;
import fr.progilone.pgcn.repository.es.EsInternetArchiveReportRepository;
import fr.progilone.pgcn.repository.exchange.internetarchive.InternetArchiveReportRepository;
import fr.progilone.pgcn.service.util.transaction.TransactionService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EsInternetArchiveReportService extends AbstractElasticsearchOperations<InternetArchiveReport> {

    private static final Logger LOG = LoggerFactory.getLogger(EsInternetArchiveReportService.class);

    private final InternetArchiveReportRepository internetArchiveReportRepository;
    private final EsInternetArchiveReportRepository esInternetArchiveReportRepository;
    private final TransactionService transactionService;

    @Value("${elasticsearch.bulk_size}")
    private Integer bulkSize;

    @Autowired
    public EsInternetArchiveReportService(final InternetArchiveReportRepository internetArchiveReportRepository,
                                          final EsInternetArchiveReportRepository esInternetArchiveReportRepository,
                                          final TransactionService transactionService) {
        this.internetArchiveReportRepository = internetArchiveReportRepository;
        this.esInternetArchiveReportRepository = esInternetArchiveReportRepository;
        this.transactionService = transactionService;
    }

    /**
     * Indexation de la notice dans le moteur de recherche
     *
     * @param identifier
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public InternetArchiveReport index(final String identifier) {
        final InternetArchiveReport report = internetArchiveReportRepository.findOne(identifier);
        if (report != null) {
            esInternetArchiveReportRepository.indexEntity(report);
            return report;
        }
        return null;
    }

    /**
     * Indexation de la notice dans le moteur de recherche
     *
     * @param identifiers
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public Iterable<InternetArchiveReport> index(final List<String> identifiers) {
        if (CollectionUtils.isEmpty(identifiers)) {
            return Collections.emptyList();
        }
        final List<InternetArchiveReport> reports = internetArchiveReportRepository.findAllByIdentifierIn(identifiers);
        final List<InternetArchiveReport> filteredReports =
            reports.stream().filter(rec -> StringUtils.isNotEmpty(rec.getDocUnitId())).collect(Collectors.toList());

        if (reports.isEmpty()) {
            return Collections.emptyList();
        }
        esInternetArchiveReportRepository.indexEntities(filteredReports);
        return filteredReports;
    }

    /**
     * Suppression de la notice du moteur de recherche
     *
     * @param report
     */
    @Override
    public void delete(final InternetArchiveReport report) {
        esInternetArchiveReportRepository.deleteEntity(report);
    }

    @Override
    public void delete(final Collection<InternetArchiveReport> reports) {
        esInternetArchiveReportRepository.deleteEntities(reports);
    }

    /**
     * Réindexation de toutes les notices disponibles
     *
     * @param index
     * @return
     */
    public long reindex(final String index) {
        long nbImported = 0;
        Page<InternetArchiveReport> pageOfObjects = null;    // Chargement des objets par page de bulkSize éléments

        do {
            final TransactionStatus status = transactionService.startTransaction(true);

            // Chargement des objets
            final Pageable pageable = pageOfObjects == null ?
                                      new PageRequest(0, bulkSize, Sort.Direction.ASC, AbstractDomainObject_.identifier.getName()) :
                                      pageOfObjects.nextPageable();
            pageOfObjects = internetArchiveReportRepository.findAllByDocUnitState(DocUnit.State.AVAILABLE, pageable);

            // Traitement des unités documentaires
            final List<InternetArchiveReport> entities = pageOfObjects.getContent();
            esInternetArchiveReportRepository.index(index, entities);

            transactionService.commitTransaction(status);

            nbImported += entities.size();
            LOG.trace("{} / {} éléments indexés", nbImported, pageOfObjects.getTotalElements());

        } while (pageOfObjects.hasNext());

        return nbImported;
    }
}
