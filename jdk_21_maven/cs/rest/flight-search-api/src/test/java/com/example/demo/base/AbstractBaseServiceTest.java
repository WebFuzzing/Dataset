package com.example.demo.base;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Abstract base test class for service layer tests. This class is extended by all service test classes
 * and ensures that necessary test setups and configurations are in place.
 * This class is annotated with {@link MockitoExtension} to enable Mockito support and
 * with {@link MockitoSettings} to configure the strictness of mocks.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public abstract class AbstractBaseServiceTest {

}
