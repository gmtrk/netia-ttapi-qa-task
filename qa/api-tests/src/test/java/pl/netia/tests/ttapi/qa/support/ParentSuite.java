package pl.netia.tests.ttapi.qa.support;

import io.qameta.allure.LabelAnnotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Inherited
@LabelAnnotation(name = "parentSuite")
public @interface ParentSuite {
    String value();
}
