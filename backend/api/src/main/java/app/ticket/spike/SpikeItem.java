package app.ticket.spike;

import java.util.Set;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

@Table("spike_item")
public record SpikeItem(
    @Id UUID id,
    Status status,
    JsonDoc payload,
    @MappedCollection(idColumn = "item_id") Set<SpikeLine> lines) {

  public enum Status {
    HELD,
    CONFIRMED,
    EXPIRED
  }

  @Table("spike_line")
  public record SpikeLine(String sku) {}
}
