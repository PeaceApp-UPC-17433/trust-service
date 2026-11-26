package pe.upc.peaceapp.trust;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
public class TrustServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrustServiceApplication.class, args);
    }
}
