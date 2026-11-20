package com.bank.banking_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class BankingSystemApplication {

	public static void main(String[] args) {
		// Fallback for environments/ISPs where Neon DNS cannot be resolved directly
		if (System.getProperty("jdk.net.hosts.file") == null) {
			String userHome = System.getProperty("user.home");
			File userHosts = new File(userHome, "custom_hosts");
			if (userHosts.exists()) {
				System.setProperty("jdk.net.hosts.file", userHosts.getAbsolutePath());
			} else {
				File localHosts = new File("custom_hosts");
				if (localHosts.exists()) {
					System.setProperty("jdk.net.hosts.file", localHosts.getAbsolutePath());
				}
			}
		}

		SpringApplication.run(BankingSystemApplication.class, args);
	}

}
