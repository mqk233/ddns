package top.mqk233.ddns;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


/**
 * 启动类
 *
 * @author mqk233
 * @since 2021-6-11
 */
@SpringBootApplication
@EnableScheduling
public class DdnsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DdnsApplication.class, args);
    }

}
