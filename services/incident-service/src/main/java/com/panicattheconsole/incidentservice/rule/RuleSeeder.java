package com.panicattheconsole.incidentservice.rule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the default rule once the schema is ready. Failures are logged, not
 * fatal: the service still starts (and rules can be created via the API).
 */
@Component
class RuleSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RuleSeeder.class);

    private final RuleService ruleService;

    RuleSeeder(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ruleService.seedDefaultRuleIfEmpty();
        } catch (Exception e) {
            log.warn("Could not seed the default rule; continuing without it", e);
        }
    }
}
