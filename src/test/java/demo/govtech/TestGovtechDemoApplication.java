package demo.govtech;

import org.springframework.boot.SpringApplication;

public class TestGovtechDemoApplication {

	public static void main(String[] args) {
		SpringApplication.from(GovtechDemoApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
