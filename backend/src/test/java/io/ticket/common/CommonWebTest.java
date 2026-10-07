package io.ticket.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.ticket.common.config.SecurityConfig;
import io.ticket.common.error.BusinessException;
import io.ticket.common.error.ErrorCode;
import io.ticket.common.error.ProblemFactory;
import io.ticket.common.error.ProblemResponseWriter;
import io.ticket.common.i18n.I18nConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = CommonWebTest.Probe.class)
@Import({
  CommonWebTest.Probe.class,
  SecurityConfig.class,
  I18nConfig.class,
  ProblemFactory.class,
  ProblemResponseWriter.class
})
@TestPropertySource(properties = "logging.structured.format.console=ecs")
@ExtendWith(OutputCaptureExtension.class)
class CommonWebTest {

  record Body(@NotBlank String name, @Size(max = 5, message = "too_long") String note) {}

  @RestController
  static class Probe {
    @PostMapping("/probe/validate")
    String validate(@Valid @RequestBody Body body) {
      return "ok";
    }

    @GetMapping("/probe/domain")
    String domain() {
      throw new BusinessException(
          ErrorCode.SEATS_UNAVAILABLE, Map.of("unavailableSeatIds", List.of("a", "b")));
    }

    @GetMapping("/probe/boom")
    String boom() {
      throw new IllegalStateException("secret internal detail");
    }
  }

  @Autowired MockMvc mvc;

  @Test
  void validationErrorIsProblemJsonWithCodeAndRequestId() throws Exception {
    mvc.perform(
            post("/probe/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(header().string("Content-Type", "application/problem+json"))
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.type").value("https://errors.ticket.dev/VALIDATION_FAILED"))
        .andExpect(jsonPath("$.status").value(422))
        .andExpect(jsonPath("$.requestId").isNotEmpty())
        .andExpect(jsonPath("$.errors[0].field").value("name"))
        .andExpect(jsonPath("$.errors[0].rule").value("required"));
  }

  @Test
  void customConstraintMessageBecomesTheRuleWithParams() throws Exception {
    mvc.perform(
            post("/probe/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"x\",\"note\":\"toolongvalue\"}"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.errors[0].field").value("note"))
        .andExpect(jsonPath("$.errors[0].rule").value("too_long"))
        .andExpect(jsonPath("$.errors[0].params.max").value(5));
  }

  @Test
  void acceptLanguageChangesTitleAndContentLanguage() throws Exception {
    mvc.perform(get("/probe/domain").header("Accept-Language", "vi"))
        .andExpect(status().isConflict())
        .andExpect(header().string("Content-Language", "vi"))
        .andExpect(jsonPath("$.title").value("Ghế không còn trống"))
        .andExpect(jsonPath("$.unavailableSeatIds[1]").value("b"));
    mvc.perform(get("/probe/domain").header("Accept-Language", "en-US,en;q=0.9"))
        .andExpect(header().string("Content-Language", "en"))
        .andExpect(jsonPath("$.title").value("Seats no longer available"));
    mvc.perform(get("/probe/domain").header("Accept-Language", "fr-FR"))
        .andExpect(jsonPath("$.title").value("Ghế không còn trống"));
  }

  @Test
  void requestIdIsKeptWhenWellFormedAndReplacedOtherwise() throws Exception {
    mvc.perform(get("/probe/domain").header("X-Request-Id", "abcd-1234-efgh"))
        .andExpect(header().string("X-Request-Id", "abcd-1234-efgh"))
        .andExpect(jsonPath("$.requestId").value("abcd-1234-efgh"));
    String generated =
        mvc.perform(get("/probe/domain").header("X-Request-Id", "bad id!"))
            .andReturn()
            .getResponse()
            .getHeader("X-Request-Id");
    assertThat(generated).matches("[0-9a-f]{32}");
  }

  @Test
  void serverTimeIsOnEveryResponseIncludingErrors() throws Exception {
    long before = System.currentTimeMillis();
    String value =
        mvc.perform(get("/probe/boom")).andReturn().getResponse().getHeader("X-Server-Time");
    assertThat(Long.parseLong(value)).isBetween(before, System.currentTimeMillis());
  }

  @Test
  void logLinesCarryTheSameTraceIdAsTheResponseHeader(CapturedOutput output) throws Exception {
    String id = mvc.perform(get("/probe/boom")).andReturn().getResponse().getHeader("X-Request-Id");
    assertThat(output.getOut()).contains("\"trace_id\":\"" + id + "\"");
  }

  @Test
  void unexpectedExceptionIs500WithoutLeakingDetails() throws Exception {
    mvc.perform(get("/probe/boom"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
        .andExpect(
            content ->
                assertThat(content.getResponse().getContentAsString())
                    .doesNotContain("secret internal detail"));
  }

  @Test
  void malformedAndUnknownFieldBodiesAre400() throws Exception {
    mvc.perform(post("/probe/validate").contentType(MediaType.APPLICATION_JSON).content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    mvc.perform(
            post("/probe/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"x\",\"stray\":1}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
  }

  @Test
  void wrongMethodAndWrongMediaTypeAndUnknownPath() throws Exception {
    mvc.perform(post("/probe/domain"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().exists("Allow"))
        .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    mvc.perform(post("/probe/validate").contentType(MediaType.TEXT_PLAIN).content("x"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    mvc.perform(get("/nope"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }
}
