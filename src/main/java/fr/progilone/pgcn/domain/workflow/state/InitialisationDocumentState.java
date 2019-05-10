package fr.progilone.pgcn.domain.workflow.state;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import fr.progilone.pgcn.domain.user.User;
import fr.progilone.pgcn.domain.workflow.DocUnitState;
import fr.progilone.pgcn.domain.workflow.WorkflowStateKey;

@Entity
@DiscriminatorValue(value = WorkflowStateKey.Values.INITIALISATION_DOCUMENT)
public class InitialisationDocumentState extends DocUnitState {

    @Override
    public WorkflowStateKey getKey() {
        return WorkflowStateKey.INITIALISATION_DOCUMENT;
    }

    @Override
    public void process(User user) {
        processEndDate();
        processUser(user);
        processStatus();

        // Initialisation de la prochaine étape si applicable (aucune étape en cours)
        if(this.getWorkflow().getCurrentStates().isEmpty()) {
            getNextStates().forEach(state -> state.initializeState(null, null, null));
        }
    }

    @Override
    protected List<DocUnitState> getNextStates() {
        List<DocUnitState> states = new ArrayList<>();
        states.add(this.getWorkflow().getFutureOrRunningByKey(WorkflowStateKey.GENERATION_BORDEREAU));
        states.add(this.getWorkflow().getFutureOrRunningByKey(WorkflowStateKey.VALIDATION_CONSTAT_ETAT));
        cleanNullStates(states);
        return states;
    }

    @Override
    public void reject(User user) {
        // TODO Auto-generated method stub

    }

}
