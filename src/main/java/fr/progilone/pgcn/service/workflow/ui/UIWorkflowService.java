package fr.progilone.pgcn.service.workflow.ui;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.progilone.pgcn.domain.document.DocUnit;
import fr.progilone.pgcn.domain.dto.workflow.BooleanValueDTO;
import fr.progilone.pgcn.domain.dto.workflow.DocUnitWorkflowDTO;
import fr.progilone.pgcn.domain.dto.workflow.StateIsDoneDTO;
import fr.progilone.pgcn.domain.util.CustomUserDetails;
import fr.progilone.pgcn.domain.workflow.DocUnitWorkflow;
import fr.progilone.pgcn.domain.workflow.WorkflowStateKey;
import fr.progilone.pgcn.security.SecurityUtils;
import fr.progilone.pgcn.service.document.DigitalDocumentService;
import fr.progilone.pgcn.service.document.DocUnitService;
import fr.progilone.pgcn.service.workflow.WorkflowService;
import fr.progilone.pgcn.service.workflow.mapper.WorkflowMapper;

/**
 * Service dédié à les gestion des vues des groupes de workflow
 */
@Service
public class UIWorkflowService {

    private final WorkflowService service;
    private final DocUnitService docUnitService;
    private final DigitalDocumentService digitalDocumentService;

    @Autowired
    public UIWorkflowService(final WorkflowService service,
            final DocUnitService docUnitService,
            final DigitalDocumentService digitalDocumentService) {
        this.service = service;
        this.docUnitService = docUnitService;
        this.digitalDocumentService = digitalDocumentService;
    }
    
    @Transactional(readOnly = true)
    public StateIsDoneDTO isStateDone(final String docUnitId, final WorkflowStateKey key) {
        final StateIsDoneDTO result = new StateIsDoneDTO();
        result.setDone(service.isStateDone(docUnitId, key));
        return result;
    }

    @Transactional(readOnly = true)
    public StateIsDoneDTO isCheckStarted(final String identifier) {
        final StateIsDoneDTO result = new StateIsDoneDTO();
        result.setDone(service.isCheckInProgress(identifier));
        return result;
    }
    
    @Transactional(readOnly = true)
    public StateIsDoneDTO canReportBeValidated(final String identifier) {
        final StateIsDoneDTO result = new StateIsDoneDTO();
        result.setDone(service.areStatesRunning(identifier, WorkflowStateKey.VALIDATION_CONSTAT_ETAT, 
                                                    WorkflowStateKey.CONSTAT_ETAT_AVANT_NUMERISATION, WorkflowStateKey.CONSTAT_ETAT_APRES_NUMERISATION));
        return result;
    }
    
    @Transactional(readOnly = true)
    public StateIsDoneDTO isWorkflowStarted(final String digitalDocId) {
        final DocUnit doc = digitalDocumentService.findDocUnitByIdentifier(digitalDocId);
        final StateIsDoneDTO result = new StateIsDoneDTO();
        result.setDone(service.isWorkflowRunning(doc.getIdentifier()));
        return result;
    }
    
    @Transactional(readOnly = true)
    public StateIsDoneDTO isRejectDefinitive(final String digitalDocId) {
        final DocUnit doc = digitalDocumentService.findDocUnitByIdentifier(digitalDocId);
        final StateIsDoneDTO result = new StateIsDoneDTO();
        result.setDone(service.isRejectDefinitive(doc.getIdentifier()));
        return result;
    }    
    
    /**
     * Récupération du workflow
     * @param identifier
     * @return
     */
    @Transactional(readOnly = true)
    public DocUnitWorkflowDTO findWorkflowByIdentifier(final String identifier) {
        return WorkflowMapper.INSTANCE.workflowToWorkflowDTO(service.findOneWorkflow(identifier));
    }

    /**
     * Récupération du {@link DocUnitWorkflowDTO} par l'identifiant de son {@link DocUnit}
     * @param identifier
     * @return
     */
    @Transactional(readOnly = true)
    public DocUnitWorkflowDTO findWorkflowByDocUnitIdentifier(final String identifier) {
        final DocUnit doc = docUnitService.findOneWithAllDependenciesForWorkflow(identifier);
        final DocUnitWorkflow workflow = service.findOneWorkflowByDocUnit(identifier);
        final DocUnitWorkflowDTO result = WorkflowMapper.INSTANCE.workflowToWorkflowDTO(workflow);
        // Vérification de la complétude des tâches en cours
        if(workflow != null) {
            workflow.getStates().forEach(state -> {
                if(state.isCurrentState()) {
                    result.getStates()
                    .stream()
                    .filter(stateDTO -> state.getIdentifier().equals(stateDTO.getIdentifier()))
                    .findFirst()
                    .get()
                    .setCanStateBeProcessed(service.canStateBeProcessed(doc, state));
                }
            });
        }
        return result;
    }

    /**
     * Indique si le user peut ou non effectuer la tache.
     * Attention : systematiquement bloqué pour 1 prestataire:
     * cette méthode doit être réservée à la vue UD détail - onglet Workflow.   
     * 
     * @param docUnitId
     * @param key
     * @param isUserPresta
     * @return
     */
    @Transactional(readOnly = true)
    public BooleanValueDTO canCurrentUserProcessState(final String docUnitId, final WorkflowStateKey key, final boolean isUserPresta) {
        final CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        final BooleanValueDTO result = new BooleanValueDTO();
        // Empeche 1 presta d'effectuer les actions sur la page de gestion workflow
        if (isUserPresta) {
            result.setValue(false);
        } else {
            result.setValue(service.canUserProcessState(currentUser.getIdentifier(), docUnitId, key));
        }
        
        return result;
    }

    
    public void processState(final String identifier, final WorkflowStateKey key) {
        final CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        service.processState(identifier, key, currentUser.getIdentifier());
    }
    
    
    @Transactional
    public void resetToNumWaiting(final List<String> docIds) {
        service.resetToNumWaitingState(docIds);
    }
    
    
    /**
     * Validation en masse des constats d'etat eligibles.
     * 
     * @param docUnitIds
     * @return
     */
    @Transactional
    public void massValidateCondReports(final List<String> docUnitIds) {
        
        docUnitIds.forEach(id -> {
                                
            final DocUnit doc = docUnitService.findOneWithAllDependenciesForWorkflow(id);
            doc.getWorkflow().getStates().stream()
                    .filter(state -> WorkflowStateKey.VALIDATION_CONSTAT_ETAT == state.getKey() 
                                        || WorkflowStateKey.CONSTAT_ETAT_AVANT_NUMERISATION == state.getKey() 
                                        || WorkflowStateKey.CONSTAT_ETAT_APRES_NUMERISATION == state.getKey())
                    .filter(state -> state.isRunning() && state.isCurrentState())
                    .forEach(state -> {
                        processState(id, state.getKey());
            });
            
        });
    }
    
}
