package pl.piomin.services.guga.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.piomin.services.guga.entity.Tag;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);
}
