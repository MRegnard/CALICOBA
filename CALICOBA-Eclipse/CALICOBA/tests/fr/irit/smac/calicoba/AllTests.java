package fr.irit.smac.calicoba;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.runner.RunWith;

// https://stackoverflow.com/questions/50485339/create-testsuite-in-junit5-eclipse
@RunWith(JUnitPlatform.class)
@SelectPackages({ "fr.irit.smac.calicoba.mas", })
public class AllTests {
}
