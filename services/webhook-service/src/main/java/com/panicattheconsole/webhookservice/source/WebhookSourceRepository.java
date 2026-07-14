package com.panicattheconsole.webhookservice.source;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookSourceRepository extends JpaRepository<WebhookSource, String> {

    List<WebhookSource> findAllByOrderByCreatedAtAsc();
}
