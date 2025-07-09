package csw.fcfs;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableConfigurationProperties(csw.fcfs.storage.StorageProperties.class)
@EnableAsync
public class FcfsApplication {

	public static void main(String[] args) {
		SpringApplication.run(FcfsApplication.class, args);
	}

	@Bean
	CommandLineRunner init(csw.fcfs.storage.StorageService storageService) {
		return (args) -> {
			storageService.deleteAll();
			storageService.init();
		};
	}

}
