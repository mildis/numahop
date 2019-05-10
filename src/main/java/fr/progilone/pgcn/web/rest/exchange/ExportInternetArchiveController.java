package fr.progilone.pgcn.web.rest.exchange;

import com.codahale.metrics.annotation.Timed;
import fr.progilone.pgcn.domain.document.DocUnit;
import fr.progilone.pgcn.domain.exchange.internetarchive.InternetArchiveReport;
import fr.progilone.pgcn.exception.PgcnTechnicalException;
import fr.progilone.pgcn.security.SecurityUtils;
import fr.progilone.pgcn.service.document.DocUnitService;
import fr.progilone.pgcn.service.es.EsInternetArchiveReportService;
import fr.progilone.pgcn.service.exchange.internetarchive.InternetArchiveItemDTO;
import fr.progilone.pgcn.service.exchange.internetarchive.InternetArchiveService;
import fr.progilone.pgcn.service.exchange.internetarchive.InternetArchiveServiceAsync;
import fr.progilone.pgcn.web.rest.AbstractRestController;
import fr.progilone.pgcn.web.util.AccessHelper;
import fr.progilone.pgcn.web.util.LibraryAccesssHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static fr.progilone.pgcn.web.rest.exchange.security.AuthorizationConstants.*;

/**
 * Export vers Internet Archive
 *
 * @author jbrunet
 * Créé le 24 avr. 2017
 */
@RestController
@RequestMapping(value = "/api/rest/internet_archive")
public class ExportInternetArchiveController extends AbstractRestController {

    private final DocUnitService docUnitService;
    private final EsInternetArchiveReportService esIaReportService;
    private final InternetArchiveService iaService;
    private final InternetArchiveServiceAsync iaServiceAsync;
    private final AccessHelper accessHelper;
    private final LibraryAccesssHelper libraryAccesssHelper;

    @Autowired
    public ExportInternetArchiveController(final InternetArchiveService iaService,
                                           final DocUnitService docUnitService,
                                           final EsInternetArchiveReportService esIaReportService,
                                           final InternetArchiveServiceAsync iaServiceAsync,
                                           final AccessHelper accessHelper,
                                           final LibraryAccesssHelper libraryAccesssHelper) {
        this.iaService = iaService;
        this.docUnitService = docUnitService;
        this.esIaReportService = esIaReportService;
        this.iaServiceAsync = iaServiceAsync;
        this.accessHelper = accessHelper;
        this.libraryAccesssHelper = libraryAccesssHelper;
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, params = {"prepare"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @RolesAllowed({EXPORT_INTERNET_ARCHIVE_HAB0})
    public ResponseEntity<InternetArchiveItemDTO> prepare(final HttpServletRequest request, @PathVariable("id") final String identifier) throws
                                                                                                                                         PgcnTechnicalException {
        final DocUnit docUnit = docUnitService.findOneWithAllDependencies(identifier);
        if (docUnit == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        // Vérification des droits d'accès par rapport à la bibliothèque de l'utilisateur, pour le conf à importer
        if (!libraryAccesssHelper.checkLibrary(request, docUnit, DocUnit::getLibrary)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        // Pré paramétrage & sauvegarde
        final InternetArchiveItemDTO item = iaService.prepareItem(docUnit);
        //iaService.saveItem(docUnit, item);
        return new ResponseEntity<>(item, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.POST, params = {"create"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @RolesAllowed({EXPORT_INTERNET_ARCHIVE_HAB0})
    public ResponseEntity<?> create(final HttpServletRequest request,
                                    @PathVariable("id") final String identifier,
                                    @RequestBody final InternetArchiveItemDTO item) throws PgcnTechnicalException {

        final DocUnit docUnit = docUnitService.findOneWithAllDependencies(identifier);
        if (docUnit == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        // Vérification des droits d'accès par rapport à la bibliothèque de l'utilisateur, pour le conf à importer
        if (!libraryAccesssHelper.checkLibrary(request, docUnit, DocUnit::getLibrary)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        // Envoi vers Internet Archive
        final String currentUserId = SecurityUtils.getCurrentUserId();
        iaServiceAsync.createItem(docUnit, item, currentUserId);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Sauvegarde l'objet de diffusion IA pour un usage ultérieur.
     *
     * @param request
     * @param identifier
     * @param item
     * @return
     * @throws PgcnTechnicalException
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.POST, params = {"save"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @RolesAllowed({EXPORT_INTERNET_ARCHIVE_HAB0})
    public ResponseEntity<?> save(final HttpServletRequest request,
                                  @PathVariable("id") final String identifier,
                                  @RequestBody final InternetArchiveItemDTO item) throws PgcnTechnicalException {

        final DocUnit docUnit = docUnitService.findOneWithAllDependencies(identifier);
        if (docUnit == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        // Vérification des droits d'accès par rapport à la bibliothèque de l'utilisateur, pour le conf à importer
        if (!libraryAccesssHelper.checkLibrary(request, docUnit, DocUnit::getLibrary)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        iaService.saveItem(docUnit, item);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Déclenche l'export IA de la liste de documents spécifiés.
     *
     * @throws PgcnTechnicalException
     */
    @RequestMapping(method = RequestMethod.GET, params = {"mass_export"})
    @Timed
    @RolesAllowed({EXPORT_INTERNET_ARCHIVE_HAB0})
    public void massExport(final HttpServletRequest request, @RequestParam(name = "docs") final List<String> docs) throws PgcnTechnicalException {
        // droits d'accès à l'ud
        final Collection<DocUnit> filteredDocUnits = accessHelper.filterDocUnits(docs);
        if (filteredDocUnits.isEmpty()) {
            return;
        }
        final List<String> reports = new ArrayList<>();
        for (final DocUnit docUnit : filteredDocUnits) {
            final InternetArchiveReport report = iaService.createItem(docUnit.getIdentifier());
            reports.add(report.getIdentifier());
        }
        // Indexation
        esIaReportService.indexAsync(reports);
    }
}
