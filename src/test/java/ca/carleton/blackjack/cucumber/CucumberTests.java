package ca.carleton.blackjack.cucumber;

import ca.carleton.blackjack.BlackJackApplication;
import io.cucumber.junit.CucumberOptions;
import io.cucumber.junit.Cucumber;
import io.cucumber.spring.CucumberContextConfiguration;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test cucumber test.
 * <p/>
 * Created by Mike on 10/30/2015.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    plugin = {"pretty"},
    glue = {"ca.carleton.blackjack.cucumber"},
    features = {"src/test/resources/cucumber/feature"}
)
@CucumberContextConfiguration
@SpringBootTest
public class CucumberTests {
}
