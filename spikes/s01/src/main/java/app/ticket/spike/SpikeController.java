package app.ticket.spike;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SpikeController {

  @GetMapping("/spike/ping")
  Map<String, String> ping() {
    return Map.of("pong", "ok");
  }

  @PostMapping("/spike/write")
  Map<String, String> write() {
    return Map.of("written", "ok");
  }
}
