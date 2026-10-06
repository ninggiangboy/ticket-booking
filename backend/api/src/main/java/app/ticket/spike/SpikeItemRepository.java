package app.ticket.spike;

import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

public interface SpikeItemRepository extends ListCrudRepository<SpikeItem, UUID> {

  @Modifying
  @Query("UPDATE spike_item SET status = :to WHERE id = :id AND status = :from")
  int transition(
      @Param("id") UUID id,
      @Param("from") SpikeItem.Status from,
      @Param("to") SpikeItem.Status to);
}
