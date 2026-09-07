package co.istad.ai_interview_app.features.admin;

import com.jayway.jsonpath.JsonPath;
import co.istad.ai_interview_app.features.job.entity.Skill;
import co.istad.ai_interview_app.features.job.service.SkillCreator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:list_hierarchy;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlatformListHierarchyIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired SkillCreator skillCreator;

    @ParameterizedTest
    @ValueSource(strings = {"industries", "job-categories", "skills"})
    void aSubcategoryCanMoveBetweenParentsWithoutChangingItsId(String list) throws Exception {
        String first = create(list, "Parent", null);
        String second = create(list, "Other parent", null);
        String child = create(list, "Child", first);
        perform(get("/api/v1/admin/" + list + "/" + child))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parentId").value(first))
                .andExpect(jsonPath("$.data.parentName").isNotEmpty());
        perform(put("/api/v1/admin/" + list + "/" + child)
                        .contentType(MediaType.APPLICATION_JSON).content(body("Moved " + UUID.randomUUID(), second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(child))
                .andExpect(jsonPath("$.data.parentId").value(second));
        perform(get("/api/v1/public/" + list))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + child + "')].parentId").value(second))
                .andExpect(jsonPath("$.data[?(@.id == '" + second + "')]").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"industries", "job-categories", "skills"})
    void thirdLevelsSelfParentsAndLevelChangesAreRejected(String list) throws Exception {
        String parent = create(list, "Parent", null);
        String child = create(list, "Child", parent);
        perform(post("/api/v1/admin/" + list).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Grandchild " + UUID.randomUUID(), child)))
                .andExpect(status().isBadRequest());
        perform(put("/api/v1/admin/" + list + "/" + child).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Self " + UUID.randomUUID(), child)))
                .andExpect(status().isBadRequest());
        perform(put("/api/v1/admin/" + list + "/" + child).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Promoted " + UUID.randomUUID(), null)))
                .andExpect(status().isBadRequest());
        perform(post("/api/v1/admin/" + list).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Missing parent " + UUID.randomUUID(), UUID.randomUUID().toString())))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"industries", "job-categories", "skills"})
    void deletingAParentRequiresRemovingItsChildrenFirst(String list) throws Exception {
        String parent = create(list, "Parent", null);
        String child = create(list, "Child", parent);
        perform(delete("/api/v1/admin/" + list + "/" + parent))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Move or delete the subcategories in this parent category before deleting it."));
        perform(delete("/api/v1/admin/" + list + "/" + child)).andExpect(status().isOk());
        perform(delete("/api/v1/admin/" + list + "/" + parent)).andExpect(status().isOk());
    }

    @Test
    void recruiterCreatedSkillsAreSubcategoriesIncludingWhenNoParentIsSpecified() throws Exception {
        String parent = create("skills", "Recruiter group", null);
        Skill explicit = skillCreator.create("Explicit " + UUID.randomUUID(), "TECHNICAL", null, UUID.fromString(parent));
        assertThat(explicit.getParent().getId()).isEqualTo(UUID.fromString(parent));
        Skill automatic = skillCreator.create("Automatic " + UUID.randomUUID(), "TECHNICAL", null);
        assertThat(automatic.getParent()).isNotNull();
        assertThat(automatic.getParent().getParent()).isNull();
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.with(jwt().jwt(token -> token.subject("list-test-moderator"))
                .authorities(new SimpleGrantedAuthority("ROLE_MODERATOR"))));
    }

    private String create(String list, String label, String parent) throws Exception {
        String response = perform(post("/api/v1/admin/" + list).contentType(MediaType.APPLICATION_JSON)
                        .content(body(label + " " + UUID.randomUUID(), parent)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.data.id");
    }

    private String body(String name, String parent) {
        return "{\"name\":\"" + name + "\",\"status\":\"ACTIVE\",\"parentId\":"
                + (parent == null ? "null" : "\"" + parent + "\"") + "}";
    }
}
