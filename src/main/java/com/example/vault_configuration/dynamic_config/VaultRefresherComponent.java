package com.example.vault_configuration.dynamic_config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.lease.SecretLeaseContainer;
import org.springframework.vault.core.lease.event.SecretLeaseExpiredEvent;

@Component
public class VaultRefresherComponent {

    VaultRefresherComponent(@Value("${spring.cloud.vault.database.role}") String databaseRole,
                            @Value("${spring.cloud.vault.database.backend}") String databaseBackend,
                            SecretLeaseContainer secretLeaseContainer,
                            ContextRefresher contextRefresher) {
        var vaultCredsPath = String.format("%s/creds/%s", databaseBackend, databaseRole);
        secretLeaseContainer.addLeaseListener(e -> {
            if (vaultCredsPath.equals(e.getSource().getPath())) {
                if (e instanceof SecretLeaseExpiredEvent) {
                    contextRefresher.refresh();
                    System.out.println("refreshing database credentials");
                }
            }
        });
    }
}
