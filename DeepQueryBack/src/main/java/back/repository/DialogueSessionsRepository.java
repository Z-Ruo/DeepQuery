package back.repository;


import back.entity.DialogueSessionInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DialogueSessionsRepository extends JpaRepository<DialogueSessionInfo, Integer> {
}
