package back.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;


@Getter
@Setter
@Entity
@Table(name = "daily_question_info")
public class DailyQuestionInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "disease_id", nullable = false)
    private Integer id;

    @Column(name = "disease_name", nullable = false, columnDefinition = "text")
    private String diseaseName;

    
    @Column(name = "date_time", nullable = false, unique = true)
    private LocalDate dateTime;
}
