package org.nrg.xnat.eventservice.services.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.xnat.eventservice.config.EventServiceTestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nullable;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(classes = EventServiceTestConfig.class)
public class InternalRestClientServiceTest {
    private static final Logger log = LoggerFactory.getLogger(InternalRestClientServiceTest.class);

    @Autowired
    private ObjectMapper mapper;


    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testGetTransaction() throws Throwable {
        String url = "https://jsonplaceholder.typicode.com/posts";

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response
                = restTemplate.getForEntity(url, String.class);

        JsonNode root = mapper.readTree(response.getBody());
        JsonNode name = root.path("name");
        assertThat(name.asText(), notNullValue());

        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));

        HttpHeaders httpHeaders = restTemplate
                .headForHeaders(url);
        assertTrue(httpHeaders.getContentType()
                              .includes(MediaType.APPLICATION_JSON));
    }

    @Test
    public void testPostTransaction() throws Throwable {
        String url = "https://jsonplaceholder.typicode.com/posts";
        String post = "{\n" +
                "  \"userId\": 1,\n" +
                "  \"title\": \"sunt aut facere repellat provident occaecati excepturi optio reprehenderit\",\n" +
                "  \"body\": \"quia et suscipit\\nsuscipit recusandae consequuntur expedita et cum\\nreprehenderit molestiae ut ut quas totam\\nnostrum rerum est autem sunt rem eveniet architecto\"\n" +
                "}";

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> postResponse = restTemplate.postForEntity(url, post, String.class);
        System.out.println("Response for Post Request: " + postResponse.getBody());

    }

    public class Foo {
     @Nullable @JsonProperty public Integer userId;
     @Nullable @JsonProperty public Integer id;
     @Nullable @JsonProperty public String title;
     @Nullable @JsonProperty public String body;

        public Foo(){}

        public Foo(@Nullable Integer userId, @Nullable Integer id, @Nullable String title, @Nullable String body) {
            this.userId = userId;
            this.id = id;
            this.title = title;
            this.body = body;
        }

        @Nullable
        public Integer getUserId() {
            return userId;
        }

        public void setUserId(@Nullable Integer userId) {
            this.userId = userId;
        }

        @Nullable
        public Integer getId() {
            return id;
        }

        public void setId(@Nullable Integer id) {
            this.id = id;
        }

        @Nullable
        public String getTitle() {
            return title;
        }

        public void setTitle(@Nullable String title) {
            this.title = title;
        }

        @Nullable
        public String getBody() {
            return body;
        }

        public void setBody(@Nullable String body) {
            this.body = body;
        }
    }
}