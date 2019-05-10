package fr.progilone.pgcn.repository.workflow;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import fr.progilone.pgcn.domain.library.Library;
import fr.progilone.pgcn.domain.user.User;
import fr.progilone.pgcn.domain.workflow.WorkflowGroup;

public interface WorkflowGroupRepository extends JpaRepository<WorkflowGroup, String>, WorkflowGroupRepositoryCustom {

    Long countByName(String name);

    Long countByNameAndIdentifierNot(String name, String identifier);

    @Query("select g " +
           "from WorkflowGroup g " +
           "left join g.library lib " +
           "where g.identifier = ?1")
    WorkflowGroup findByIdentifier(String identifier);

    @Query("select distinct g " +
            "from WorkflowGroup g " +
            "left join g.library lib " +
            "where lib.identifier = ?1")
    List<WorkflowGroup> findAllByLibraryIdentifier(String identifier);

    List<WorkflowGroup> findAllByUsers(User user);

    Long countByLibrary(Library library);
    
    @Query("select count(*) " +
            "from WorkflowGroup g " +
            "left join g.users users " +
            "where ?1 in users")
    Long countByUser(User user);
    
    @Query("select distinct g " +
            "from WorkflowGroup g " +
            "left join g.users users " +
            "where ?1 in users")
    List<WorkflowGroup> findAllGroupsByUser(User user);
}
