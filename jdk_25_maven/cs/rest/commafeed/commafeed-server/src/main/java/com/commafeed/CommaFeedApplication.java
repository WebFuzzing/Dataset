package com.commafeed;

import com.commafeed.backend.dao.UnitOfWork;
import com.commafeed.backend.feed.FeedRefreshEngine;
import com.commafeed.backend.feed.ImageProxyUrl;
import com.commafeed.backend.model.UserRole.Role;
import com.commafeed.backend.service.UserService;
import com.commafeed.backend.service.db.DatabaseStartupService;
import com.commafeed.backend.task.TaskScheduler;
import com.commafeed.security.password.PasswordConstraintValidator;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.SystemProperties;

import java.util.List;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class CommaFeedApplication {

    private final FeedRefreshEngine feedRefreshEngine;
    private final TaskScheduler taskScheduler;
    private final CommaFeedConfiguration config;
    // MODIFIED: dependencies used by the account seeding in start()
    private final DatabaseStartupService databaseStartupService;
    private final UnitOfWork unitOfWork;
    private final UserService userService;

    public void start(@Observes StartupEvent ev) {
        log.info("starting up...");

        // disable entity expansion limits added in JDK24+ (#1961)
        // we already strip doctype declarations in XMLCleaner to prevent xxe attacks
        // we also already limit the size of feeds we download in HttpGetter
        System.setProperty(SystemProperties.JDK_XML_MAX_GENERAL_ENTITY_SIZE_LIMIT, "0");
        System.setProperty(SystemProperties.JDK_XML_TOTAL_ENTITY_SIZE_LIMIT, "0");

        PasswordConstraintValidator.setMinimumPasswordLength(
                config.users().minimumPasswordLength());

        if (config.imageProxyEnabled()) {
            ImageProxyUrl.generateKey();
        }

        // MODIFIED
        // WFD: upstream creates the first accounts only via POST /rest/user/initialSetup,
        // so a fresh database has no users and every authenticated endpoint answers 401.
        // Seed them here instead, so fuzzing has usable credentials with no external setup step.
        // "user1" and "user2" share the same role on purpose: two same-role accounts are what
        // let a fuzzer detect broken access control between users.
        // Addresses use the reserved ".invalid" TLD (RFC 2606) so that no mail the SUT may try
        // to send during fuzzing can ever reach a real domain.
        if (databaseStartupService.isInitialSetupRequired()) {
            unitOfWork.run(
                    () -> {
                        userService.register(
                                "admin",
                                "admin123",
                                "admin@commafeed.invalid",
                                List.of(Role.ADMIN, Role.USER),
                                true);
                        userService.register(
                                "user1",
                                "user1123",
                                "user1@commafeed.invalid",
                                List.of(Role.USER),
                                true);
                        userService.register(
                                "user2",
                                "user2123",
                                "user2@commafeed.invalid",
                                List.of(Role.USER),
                                true);
                    });
        }
        // MODIFIED

        feedRefreshEngine.start();
        taskScheduler.start();
    }

    public void stop(@Observes ShutdownEvent ev) {
        log.info("shutting down...");

        feedRefreshEngine.stop();
        taskScheduler.stop();
    }
}
