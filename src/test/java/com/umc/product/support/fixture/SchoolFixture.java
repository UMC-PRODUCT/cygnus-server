package com.umc.product.support.fixture;

import org.springframework.stereotype.Component;

import com.umc.product.organization.application.port.out.command.SaveSchoolPort;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.School;

@Component
public class SchoolFixture extends FixtureSupport {

    private final SaveSchoolPort saveSchoolPort;

    public SchoolFixture(SaveSchoolPort saveSchoolPort) {
        this.saveSchoolPort = saveSchoolPort;
    }

    public School 학교(String name) {
        return saveSchoolPort.save(School.create(valueOrFixture(name, "school", 50), null, null));
    }

    public School 학교() {
        return 학교(fixtureString("school", 50));
    }

    public School 지부에_소속된_학교(String name, Chapter chapter) {
        School school = School.create(valueOrFixture(name, "school", 50), null, null);
        school.assignToChapter(chapter);
        return saveSchoolPort.save(school);
    }

    public School 지부에_소속된_학교(Chapter chapter) {
        return 지부에_소속된_학교(fixtureString("school", 50), chapter);
    }
}
