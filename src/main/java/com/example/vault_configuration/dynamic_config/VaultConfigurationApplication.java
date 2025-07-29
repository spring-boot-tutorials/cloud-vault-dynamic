package com.example.vault_configuration.dynamic_config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@SpringBootApplication
public class VaultConfigurationApplication {

	public static void main(String[] args) {
		SpringApplication.run(VaultConfigurationApplication.class, args);
	}

	/**
	 * TODO not being refreshed after 2 min expiry
	 * @param properties
	 * @return
	 */
	@Bean
	@RefreshScope
	public DataSource dataSource(DataSourceProperties properties) {
		System.out.println("Rebuilding dataSource: " + properties.getUsername() + " " + properties.getPassword());
		return DataSourceBuilder.create()
				.url(properties.getUrl())
				.username(properties.getUsername())
				.password(properties.getPassword())
				.build();
	}
}
