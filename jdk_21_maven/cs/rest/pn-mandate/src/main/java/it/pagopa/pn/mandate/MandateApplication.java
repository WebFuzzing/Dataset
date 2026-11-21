package it.pagopa.pn.mandate;

import it.pagopa.pn.commons.configs.listeners.TaskIdApplicationListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication(scanBasePackages = {
		"it.pagopa.pn.mandate",
		"it.pagopa.pn.ciechecker"
})
public class MandateApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(MandateApplication.class);
		app.addListeners(new TaskIdApplicationListener());
		app.run(args);
	}

	@RestController
	@RequestMapping("/")
	public static class RootController {

		@GetMapping("/")
		public String home() {
			return "";
		}
	}
}
